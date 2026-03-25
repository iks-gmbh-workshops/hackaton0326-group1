package de.heuermannplus.backend

import de.heuermannplus.backend.activity.ActivityService
import de.heuermannplus.backend.activity.CreateActivityRequest
import de.heuermannplus.backend.api.ActivityController
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
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken

class ActivityControllerTest {

    @Test
    fun `create activity resolves current user from jwt and delegates to service`() {
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
            appUserStore = FakeActivityAppUserStore(
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
                )
            )
        )

        val response = controller.createActivity(
            group.id,
            CreateActivityRequest(
                description = "Probe",
                details = null,
                location = "Studio",
                scheduledAt = "2026-03-26T18:30:00Z"
            ),
            authenticationToken("owner-1")
        )

        assertEquals(group.id, response.groupId)
        assertEquals("Probe", response.description)
        assertEquals(1, response.participants.size)
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
}

private class FakeActivityAppUserStore(
    private vararg val users: AppUser
) : AppUserStore {

    override fun save(user: AppUser): AppUser = user

    override fun findById(keycloakUserId: String): AppUser? =
        users.firstOrNull { it.keycloakUserId == keycloakUserId }

    override fun findByNickname(nickname: String): AppUser? =
        users.firstOrNull { it.nickname == nickname }

    override fun findByEmail(email: String): AppUser? =
        users.firstOrNull { it.email == email }

    override fun searchInviteSuggestions(query: String, excludedUserId: String, limit: Int): List<AppUser> = emptyList()
}
