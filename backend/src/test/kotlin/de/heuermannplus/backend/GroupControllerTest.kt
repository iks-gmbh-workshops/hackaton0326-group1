package de.heuermannplus.backend

import de.heuermannplus.backend.api.GroupController
import de.heuermannplus.backend.group.CreateGroupRequest
import de.heuermannplus.backend.group.CreateMembershipRequest
import de.heuermannplus.backend.group.CurrentUser
import de.heuermannplus.backend.group.GroupKnownUserInvitationMail
import de.heuermannplus.backend.group.GroupMailService
import de.heuermannplus.backend.group.GroupMembershipStatus
import de.heuermannplus.backend.group.GroupProperties
import de.heuermannplus.backend.group.GroupService
import de.heuermannplus.backend.group.InviteGroupMemberRequest
import de.heuermannplus.backend.group.JoinGroupByTokenRequest
import de.heuermannplus.backend.group.MembershipDecisionRequest
import de.heuermannplus.backend.group.UpdateGroupRequest
import de.heuermannplus.backend.registration.AppUser
import de.heuermannplus.backend.registration.AppUserStatus
import de.heuermannplus.backend.registration.VerificationTokenService
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken

class GroupControllerTest {

    @Test
    fun `list create get update and delete delegate through controller`() {
        val fixture = groupControllerFixture()

        val created = fixture.controller.create(
            CreateGroupRequest(name = "Band 1", description = "Probegruppe"),
            authenticationToken("owner-1")
        )
        val listed = fixture.controller.list(authenticationToken("owner-1"))
        val fetched = fixture.controller.get(created.id, authenticationToken("owner-1"))
        val updated = fixture.controller.update(
            created.id,
            UpdateGroupRequest(name = "Band 1 Updated", description = "Neue Beschreibung"),
            authenticationToken("owner-1")
        )

        fixture.controller.delete(created.id, authenticationToken("owner-1"))

        assertEquals(listOf("Band 1"), listed.groups.map { it.name })
        assertEquals(created.id, fetched.id)
        assertEquals("Band 1 Updated", updated.name)
        assertTrue(fixture.controller.list(authenticationToken("owner-1")).groups.isEmpty())
    }

    @Test
    fun `invite suggestions accept invitation and remove member work through controller`() {
        val fixture = groupControllerFixture()
        val groupId = fixture.controller.create(
            CreateGroupRequest(name = "Band 1"),
            authenticationToken("owner-1")
        ).id

        val knownInvite = fixture.controller.invite(
            groupId,
            InviteGroupMemberRequest(nicknameOrEmail = "alice"),
            authenticationToken("owner-1")
        )
        val unknownInvite = fixture.controller.invite(
            groupId,
            InviteGroupMemberRequest(nicknameOrEmail = "guest@example.org"),
            authenticationToken("owner-1")
        )
        val suggestions = fixture.controller.inviteSuggestions(groupId, "bo", authenticationToken("owner-1"))
        val guestInvitation = fixture.controller.list(authenticationToken("guest-1")).invitations.single()
        val accepted = fixture.controller.acceptInvitation(groupId, guestInvitation.membershipId, authenticationToken("guest-1"))
        val guestMembership = accepted.members.first { it.displayName == "guest" && it.status == GroupMembershipStatus.ACTIVE }
        val removed = fixture.controller.removeMember(groupId, guestMembership.id, authenticationToken("owner-1"))

        assertTrue(knownInvite.members.any { it.displayName == "alice" && it.status == GroupMembershipStatus.INVITED })
        assertTrue(unknownInvite.members.any { it.displayName == "guest" && it.status == GroupMembershipStatus.INVITED })
        assertEquals(listOf("bob"), suggestions.map { it.nickname })
        assertFalse(removed.members.any { it.id == guestMembership.id && it.status == GroupMembershipStatus.ACTIVE })
    }

    @Test
    fun `membership token and leave flows cover remaining controller endpoints`() {
        val fixture = groupControllerFixture()
        val joinApprovalGroupId = fixture.controller.create(
            CreateGroupRequest(name = "Approval Group"),
            authenticationToken("owner-1")
        ).id
        val joinRejectionGroupId = fixture.controller.create(
            CreateGroupRequest(name = "Rejection Group"),
            authenticationToken("owner-1")
        ).id
        val tokenGroupId = fixture.controller.create(
            CreateGroupRequest(name = "Token Group"),
            authenticationToken("owner-1")
        ).id

        val requestMembership = fixture.controller.requestMembership(
            joinApprovalGroupId,
            CreateMembershipRequest(comment = "Ich moechte mitspielen"),
            authenticationToken("member-1")
        )
        val approvalRequestId = fixture.controller.get(joinApprovalGroupId, authenticationToken("owner-1")).joinRequests.single().id
        val approved = fixture.controller.approveJoinRequest(
            joinApprovalGroupId,
            approvalRequestId,
            null,
            authenticationToken("owner-1")
        )
        val memberMembershipId = approved.members.first { it.displayName == "bob" && it.status == GroupMembershipStatus.ACTIVE }.id
        val granted = fixture.controller.grantAdmin(joinApprovalGroupId, memberMembershipId, authenticationToken("owner-1"))
        val revoked = fixture.controller.revokeAdmin(joinApprovalGroupId, memberMembershipId, authenticationToken("owner-1"))

        fixture.controller.requestMembership(
            joinRejectionGroupId,
            CreateMembershipRequest(comment = "Bitte aufnehmen"),
            authenticationToken("guest-1")
        )
        val rejectionRequestId = fixture.controller.get(joinRejectionGroupId, authenticationToken("owner-1")).joinRequests.single().id
        val rejected = fixture.controller.rejectJoinRequest(
            joinRejectionGroupId,
            rejectionRequestId,
            MembershipDecisionRequest(comment = "Spaeter"),
            authenticationToken("owner-1")
        )

        val token = fixture.controller.createToken(tokenGroupId, authenticationToken("owner-1"))
        val joinedByToken = fixture.controller.joinByToken(
            JoinGroupByTokenRequest(token = token.token),
            authenticationToken("guest-1")
        )
        val afterLeave = fixture.controller.leaveGroup(tokenGroupId, authenticationToken("guest-1"))

        assertTrue(requestMembership.joinRequests.any { it.groupId == joinApprovalGroupId && it.requestedByUserId == "member-1" })
        assertTrue(granted.members.first { it.id == memberMembershipId }.admin)
        assertFalse(revoked.members.first { it.id == memberMembershipId }.admin)
        assertTrue(rejected.joinRequests.none { it.id == rejectionRequestId })
        assertEquals(GroupMembershipStatus.ACTIVE, joinedByToken.currentMembershipStatus)
        assertTrue(afterLeave.availableGroups.any { it.id == tokenGroupId })
    }
}

private data class GroupControllerFixture(
    val controller: GroupController,
    val owner: CurrentUser,
    val member: CurrentUser,
    val guest: CurrentUser
)

private fun groupControllerFixture(): GroupControllerFixture {
    val clock = Clock.fixed(Instant.parse("2026-03-25T08:00:00Z"), ZoneId.of("UTC"))
    val appUserStore = GroupTestAppUserStore(
        listOf(
            groupTestAppUser("owner-1", "owner", "owner@example.org"),
            groupTestAppUser("member-1", "bob", "bob@example.org"),
            groupTestAppUser("guest-1", "guest", "guest@example.org"),
            groupTestAppUser("known-1", "alice", "alice@example.org")
        )
    )
    val service = GroupService(
        groupStore = InMemoryGroupStore(),
        membershipStore = InMemoryGroupMembershipStore(),
        invitationStore = InMemoryGroupInvitationStore(),
        tokenStore = InMemoryGroupTokenStore(),
        joinRequestStore = InMemoryGroupJoinRequestStore(),
        activityStore = InMemoryActivityStore(),
        appUserStore = appUserStore,
        groupMailService = NoopGroupMailService(),
        tokenService = VerificationTokenService(),
        groupProperties = GroupProperties(frontendBaseUrl = "http://localhost:3000"),
        clock = clock
    )

    return GroupControllerFixture(
        controller = GroupController(service, appUserStore),
        owner = CurrentUser("owner-1", "owner", "owner@example.org"),
        member = CurrentUser("member-1", "bob", "bob@example.org"),
        guest = CurrentUser("guest-1", "guest", "guest@example.org")
    )
}

private fun groupTestAppUser(id: String, nickname: String, email: String): AppUser =
    AppUser(
        keycloakUserId = id,
        nickname = nickname,
        email = email,
        firstName = null,
        lastName = null,
        status = AppUserStatus.ACTIVE,
        enabled = true,
        emailVerified = true,
        keycloakRole = "app-user"
    )

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

private class NoopGroupMailService : GroupMailService {
    override fun sendKnownUserInvitation(invitation: GroupKnownUserInvitationMail) = Unit

    override fun sendUnknownEmailInvitation(email: String, groupName: String, inviterName: String) = Unit
}
