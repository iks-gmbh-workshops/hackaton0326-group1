package de.heuermannplus.backend

import de.heuermannplus.backend.activity.ActivityResponseRequest
import de.heuermannplus.backend.activity.ActivityResponseStatus
import de.heuermannplus.backend.activity.ActivityService
import de.heuermannplus.backend.activity.AddActivityParticipantRequest
import de.heuermannplus.backend.activity.CreateActivityRequest
import de.heuermannplus.backend.activity.UpdateActivityRequest
import de.heuermannplus.backend.api.ActivityController
import de.heuermannplus.backend.group.CurrentUser
import de.heuermannplus.backend.group.Group
import de.heuermannplus.backend.group.GroupMembership
import de.heuermannplus.backend.group.GroupMembershipStatus
import de.heuermannplus.backend.registration.AppUser
import de.heuermannplus.backend.registration.AppUserStatus
import de.heuermannplus.backend.registration.AppUserStore
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken

class ActivityControllerCoverageTest {

    @Test
    fun `list and detail endpoints resolve current user from jwt`() {
        val fixture = activityControllerFixture()
        val detail = fixture.createDefaultActivity()

        val upcoming = fixture.controller.listUpcoming(authenticationToken("member-1"))
        val groupActivities = fixture.controller.listGroupActivities(fixture.groupId, authenticationToken("member-1"))
        val activity = fixture.controller.getActivity(fixture.groupId, detail.id, authenticationToken("member-1"))

        assertEquals(listOf("Probeabend"), upcoming.activities.map { it.description })
        assertEquals(listOf("Probeabend"), groupActivities.activities.map { it.description })
        assertEquals(detail.id, activity.id)
    }

    @Test
    fun `mutation endpoints delegate to service with resolved user`() {
        val fixture = activityControllerFixture()
        val created = fixture.controller.createActivity(
            fixture.groupId,
            CreateActivityRequest(
                description = "Probeabend",
                details = null,
                location = "Studio",
                scheduledAt = "2026-03-26T18:30:00Z"
            ),
            authenticationToken("owner-1")
        )
        val guestMembership = fixture.memberships.first { it.displayName == "guest" }

        val updated = fixture.controller.updateActivity(
            fixture.groupId,
            created.id,
            UpdateActivityRequest(
                description = "Probeabend XL",
                details = "Instrumente mitbringen",
                location = "Studio B",
                scheduledAt = "2026-03-27T19:00:00Z"
            ),
            authenticationToken("owner-1")
        )
        val existingGuestParticipant = created.participants.first {
            it.groupMembershipId == guestMembership.id && it.removedAt == null
        }
        val afterRemove = fixture.controller.removeParticipant(
            fixture.groupId,
            created.id,
            existingGuestParticipant.id,
            authenticationToken("owner-1")
        )
        val afterAdd = fixture.controller.addParticipant(
            fixture.groupId,
            created.id,
            AddActivityParticipantRequest(groupMembershipId = guestMembership.id),
            authenticationToken("owner-1")
        )
        val guestParticipant = afterAdd.participants.first { it.groupMembershipId == guestMembership.id && it.removedAt == null }
        val afterRespond = fixture.controller.respond(
            fixture.groupId,
            created.id,
            ActivityResponseRequest(
                responseStatus = ActivityResponseStatus.ACCEPTED,
                responseNote = "Bin dabei"
            ),
            authenticationToken("member-1")
        )

        fixture.controller.deleteActivity(fixture.groupId, created.id, authenticationToken("owner-1"))

        assertEquals("Probeabend XL", updated.description)
        assertTrue(afterRemove.participants.any { it.id == existingGuestParticipant.id && it.removedAt != null })
        assertTrue(afterAdd.participants.any { it.id == guestParticipant.id && it.removedAt == null })
        assertEquals(ActivityResponseStatus.ACCEPTED, afterRespond.currentUserResponseStatus)
        assertTrue(fixture.controller.listGroupActivities(fixture.groupId, authenticationToken("owner-1")).activities.isEmpty())
    }
}

private data class ActivityControllerFixture(
    val controller: ActivityController,
    val groupId: Long,
    val memberships: List<GroupMembership>,
    val owner: CurrentUser
) {
    fun createDefaultActivity() =
        controller.createActivity(
            groupId,
            CreateActivityRequest(
                description = "Probeabend",
                details = null,
                location = "Studio",
                scheduledAt = "2026-03-26T18:30:00Z"
            ),
            authenticationToken("owner-1")
        )
}

private fun activityControllerFixture(): ActivityControllerFixture {
    val clock = Clock.fixed(Instant.parse("2026-03-25T08:00:00Z"), ZoneId.of("UTC"))
    val groupStore = ActivityTestGroupStore()
    val membershipStore = ActivityTestGroupMembershipStore()
    val activityStore = InMemoryActivityStore()
    val participantStore = InMemoryActivityParticipantStore()
    val group = groupStore.save(
        Group(
            name = "Band 1",
            normalizedName = "band 1",
            description = "Probegruppe",
            createdByUserId = "owner-1",
            createdAt = Instant.parse("2026-03-20T10:00:00Z"),
            updatedAt = Instant.parse("2026-03-20T10:00:00Z")
        )
    )

    val memberships = listOf(
        membershipStore.save(
            GroupMembership(
                groupId = group.id!!,
                userId = "owner-1",
                inviteEmail = "owner@example.org",
                normalizedInviteEmail = "owner@example.org",
                displayName = "owner",
                status = GroupMembershipStatus.ACTIVE,
                isAdmin = true,
                createdAt = Instant.parse("2026-03-20T10:00:00Z"),
                updatedAt = Instant.parse("2026-03-20T10:00:00Z"),
                joinedAt = Instant.parse("2026-03-20T10:00:00Z")
            )
        ),
        membershipStore.save(
            GroupMembership(
                groupId = group.id,
                userId = "member-1",
                inviteEmail = "member@example.org",
                normalizedInviteEmail = "member@example.org",
                displayName = "member",
                status = GroupMembershipStatus.ACTIVE,
                isAdmin = false,
                createdAt = Instant.parse("2026-03-20T10:00:00Z"),
                updatedAt = Instant.parse("2026-03-20T10:00:00Z"),
                joinedAt = Instant.parse("2026-03-20T10:00:00Z")
            )
        ),
        membershipStore.save(
            GroupMembership(
                groupId = group.id,
                userId = "guest-1",
                inviteEmail = "guest@example.org",
                normalizedInviteEmail = "guest@example.org",
                displayName = "guest",
                status = GroupMembershipStatus.ACTIVE,
                isAdmin = false,
                createdAt = Instant.parse("2026-03-20T10:00:00Z"),
                updatedAt = Instant.parse("2026-03-20T10:00:00Z"),
                joinedAt = Instant.parse("2026-03-20T10:00:00Z")
            )
        )
    )

    val service = ActivityService(
        activityStore = activityStore,
        participantStore = participantStore,
        groupStore = groupStore,
        membershipStore = membershipStore,
        clock = clock
    )
    val controller = ActivityController(
        activityService = service,
        appUserStore = ActivityControllerAppUserStore(
            AppUser(
                keycloakUserId = "owner-1",
                nickname = "owner",
                email = "owner@example.org",
                firstName = null,
                lastName = null,
                status = AppUserStatus.ACTIVE,
                enabled = true,
                emailVerified = true,
                keycloakRole = "app-user"
            ),
            AppUser(
                keycloakUserId = "member-1",
                nickname = "member",
                email = "member@example.org",
                firstName = null,
                lastName = null,
                status = AppUserStatus.ACTIVE,
                enabled = true,
                emailVerified = true,
                keycloakRole = "app-user"
            ),
            AppUser(
                keycloakUserId = "guest-1",
                nickname = "guest",
                email = "guest@example.org",
                firstName = null,
                lastName = null,
                status = AppUserStatus.ACTIVE,
                enabled = true,
                emailVerified = true,
                keycloakRole = "app-user"
            )
        )
    )

    return ActivityControllerFixture(
        controller = controller,
        groupId = group.id,
        memberships = memberships,
        owner = CurrentUser("owner-1", "owner", "owner@example.org")
    )
}

private class ActivityControllerAppUserStore(
    private vararg val users: AppUser
) : AppUserStore {

    override fun save(user: AppUser): AppUser = user

    override fun findById(keycloakUserId: String): AppUser? =
        users.firstOrNull { it.keycloakUserId == keycloakUserId }

    override fun findByNickname(nickname: String): AppUser? =
        users.firstOrNull { it.nickname == nickname }

    override fun findByEmail(email: String): AppUser? =
        users.firstOrNull { it.email == email }

    override fun deleteById(keycloakUserId: String) {
    }

    override fun searchInviteSuggestions(query: String, excludedUserId: String, limit: Int): List<AppUser> = emptyList()
}

private fun authenticationToken(subject: String): JwtAuthenticationToken =
    JwtAuthenticationToken(
        Jwt(
            "token",
            Instant.parse("2026-03-24T12:00:00Z"),
            Instant.parse("2026-03-24T13:00:00Z"),
            mapOf("alg" to "none"),
            mapOf(
                "iss" to "http://localhost:8081/realms/heuermannplus",
                "sub" to subject
            )
        )
    )
