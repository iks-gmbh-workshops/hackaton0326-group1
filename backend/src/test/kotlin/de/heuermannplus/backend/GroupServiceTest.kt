package de.heuermannplus.backend

import de.heuermannplus.backend.group.CreateGroupRequest
import de.heuermannplus.backend.group.CreateMembershipRequest
import de.heuermannplus.backend.group.CurrentUser
import de.heuermannplus.backend.group.Group
import de.heuermannplus.backend.group.GroupException
import de.heuermannplus.backend.group.GroupInvitation
import de.heuermannplus.backend.group.GroupInvitationChannel
import de.heuermannplus.backend.group.GroupInvitationMailType
import de.heuermannplus.backend.group.GroupInvitationStore
import de.heuermannplus.backend.group.GroupInvitationSummaryResponse
import de.heuermannplus.backend.group.GroupInvitationToken
import de.heuermannplus.backend.group.GroupInvitationTokenStore
import de.heuermannplus.backend.group.GroupJoinRequest
import de.heuermannplus.backend.group.GroupJoinRequestStatus
import de.heuermannplus.backend.group.GroupJoinRequestStore
import de.heuermannplus.backend.group.GroupMailService
import de.heuermannplus.backend.group.GroupMembership
import de.heuermannplus.backend.group.GroupMembershipStatus
import de.heuermannplus.backend.group.GroupMembershipStore
import de.heuermannplus.backend.group.GroupProperties
import de.heuermannplus.backend.group.GroupService
import de.heuermannplus.backend.group.GroupStore
import de.heuermannplus.backend.group.InviteGroupMemberRequest
import de.heuermannplus.backend.group.JoinGroupByTokenRequest
import de.heuermannplus.backend.group.MembershipDecisionRequest
import de.heuermannplus.backend.registration.AppUser
import de.heuermannplus.backend.registration.AppUserStatus
import de.heuermannplus.backend.registration.AppUserStore
import de.heuermannplus.backend.registration.VerificationTokenService
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class GroupServiceTest {

    @Test
    fun `create group assigns creator as active admin`() {
        val fixture = groupFixture()

        val response = fixture.service.createGroup(
            CreateGroupRequest(name = "Band 1", description = "Probegruppe"),
            fixture.owner
        )

        assertEquals("Band 1", response.name)
        assertTrue(response.currentUserAdmin)
        assertEquals(GroupMembershipStatus.ACTIVE, response.currentMembershipStatus)
        assertEquals(1, response.members.size)
        assertTrue(response.members.single().admin)
    }

    @Test
    fun `invite known user creates invited membership and known mail`() {
        val fixture = groupFixture()
        val groupId = fixture.service.createGroup(CreateGroupRequest(name = "Band 1"), fixture.owner).id

        val response = fixture.service.inviteMember(
            groupId,
            InviteGroupMemberRequest(nicknameOrEmail = "alice"),
            fixture.owner
        )

        val invitedMember = response.members.firstOrNull { it.displayName == "alice" }
        assertNotNull(invitedMember)
        assertEquals(GroupMembershipStatus.INVITED, invitedMember.status)
        assertEquals(1, fixture.mailService.knownInvites.size)
    }

    @Test
    fun `invite suggestions show initial candidates and exclude existing relations`() {
        val fixture = groupFixture()
        val groupId = fixture.service.createGroup(CreateGroupRequest(name = "Band 1"), fixture.owner).id

        fixture.service.inviteMember(
            groupId,
            InviteGroupMemberRequest(nicknameOrEmail = "alice"),
            fixture.owner
        )

        val suggestions = fixture.service.inviteSuggestions(groupId, "", fixture.owner)

        assertTrue(suggestions.any { it.nickname == "bob" })
        assertTrue(suggestions.any { it.nickname == "guest" })
        assertTrue(suggestions.none { it.nickname == "owner" })
        assertTrue(suggestions.none { it.nickname == "alice" })
    }

    @Test
    fun `invite suggestions match nickname and email case insensitive`() {
        val fixture = groupFixture()
        val groupId = fixture.service.createGroup(CreateGroupRequest(name = "Band 1"), fixture.owner).id

        val nicknameMatches = fixture.service.inviteSuggestions(groupId, "ALI", fixture.owner)
        val emailMatches = fixture.service.inviteSuggestions(groupId, "GUEST@", fixture.owner)

        assertEquals(listOf("alice"), nicknameMatches.map { it.nickname })
        assertEquals(listOf("guest"), emailMatches.map { it.nickname })
    }

    @Test
    fun `invite suggestions require admin membership`() {
        val fixture = groupFixture()
        val groupId = fixture.service.createGroup(CreateGroupRequest(name = "Band 1"), fixture.owner).id

        val exception = assertFailsWith<GroupException> {
            fixture.service.inviteSuggestions(groupId, "", fixture.member)
        }

        assertEquals("FORBIDDEN_GROUP_ADMIN", exception.code)
    }

    @Test
    fun `unknown email invitation is claimed on list and can be accepted`() {
        val fixture = groupFixture()
        val groupId = fixture.service.createGroup(CreateGroupRequest(name = "Band 1"), fixture.owner).id

        fixture.service.inviteMember(
            groupId,
            InviteGroupMemberRequest(nicknameOrEmail = "guest@example.org"),
            fixture.owner
        )

        val invitations = fixture.service.list(fixture.guest).invitations
        assertEquals(1, invitations.size)

        fixture.service.acceptInvitation(groupId, invitations.single().membershipId, fixture.guest)

        val detail = fixture.service.getGroup(groupId, fixture.guest)
        assertEquals(GroupMembershipStatus.ACTIVE, detail.currentMembershipStatus)
    }

    @Test
    fun `token join adds user as active member`() {
        val fixture = groupFixture()
        val groupId = fixture.service.createGroup(CreateGroupRequest(name = "Band 1"), fixture.owner).id

        val token = fixture.service.createInvitationToken(groupId, fixture.owner)

        val response = fixture.service.joinByToken(
            JoinGroupByTokenRequest(token = token.token),
            fixture.member
        )

        assertEquals(GroupMembershipStatus.ACTIVE, response.currentMembershipStatus)
        assertTrue(response.members.any { it.displayName == "bob" && it.status == GroupMembershipStatus.ACTIVE })
    }

    @Test
    fun `membership request can be approved by admin`() {
        val fixture = groupFixture()
        val groupId = fixture.service.createGroup(CreateGroupRequest(name = "Band 1"), fixture.owner).id

        fixture.service.requestMembership(groupId, CreateMembershipRequest(comment = "Ich moechte mitspielen"), fixture.member)
        val detail = fixture.service.getGroup(groupId, fixture.owner)
        val requestId = detail.joinRequests.single().id

        val approved = fixture.service.approveJoinRequest(
            groupId,
            requestId,
            MembershipDecisionRequest(),
            fixture.owner
        )

        assertTrue(approved.members.any { it.displayName == "bob" && it.status == GroupMembershipStatus.ACTIVE })
    }

    @Test
    fun `group details are visible to invited members`() {
        val fixture = groupFixture()
        val groupId = fixture.service.createGroup(CreateGroupRequest(name = "Band 1"), fixture.owner).id

        fixture.service.inviteMember(
            groupId,
            InviteGroupMemberRequest(nicknameOrEmail = "guest@example.org"),
            fixture.owner
        )

        val detail = fixture.service.getGroup(groupId, fixture.guest)

        assertEquals(GroupMembershipStatus.INVITED, detail.currentMembershipStatus)
        assertEquals(2, detail.members.size)
        assertEquals(false, detail.currentUserAdmin)
    }

    @Test
    fun `group details are forbidden for users without membership`() {
        val fixture = groupFixture()
        val groupId = fixture.service.createGroup(CreateGroupRequest(name = "Band 1"), fixture.owner).id

        val exception = assertFailsWith<GroupException> {
            fixture.service.getGroup(groupId, fixture.member)
        }

        assertEquals("FORBIDDEN_GROUP_MEMBER", exception.code)
    }

    @Test
    fun `accept invitation rejects other users invitation`() {
        val fixture = groupFixture()
        val groupId = fixture.service.createGroup(CreateGroupRequest(name = "Band 1"), fixture.owner).id

        val invitation = fixture.service.inviteMember(
            groupId,
            InviteGroupMemberRequest(nicknameOrEmail = "guest@example.org"),
            fixture.owner
        ).members.first { it.displayName == "guest" }

        val exception = assertFailsWith<GroupException> {
            fixture.service.acceptInvitation(groupId, invitation.id, fixture.member)
        }

        assertEquals("FORBIDDEN_GROUP_MEMBER", exception.code)
    }

    @Test
    fun `last admin cannot leave or be revoked`() {
        val fixture = groupFixture()
        val groupId = fixture.service.createGroup(CreateGroupRequest(name = "Band 1"), fixture.owner).id
        val ownerMembershipId = fixture.service.getGroup(groupId, fixture.owner).currentMembershipId!!

        val leaveException = assertFailsWith<GroupException> {
            fixture.service.leaveGroup(groupId, fixture.owner)
        }
        assertEquals("LAST_ADMIN_REQUIRED", leaveException.code)

        val revokeException = assertFailsWith<GroupException> {
            fixture.service.revokeAdmin(groupId, ownerMembershipId, fixture.owner)
        }
        assertEquals("LAST_ADMIN_REQUIRED", revokeException.code)
    }

    private fun groupFixture(): GroupFixture {
        val clock = Clock.fixed(Instant.parse("2026-03-25T08:00:00Z"), ZoneId.of("UTC"))
        val appUserStore = GroupTestAppUserStore(
            listOf(
                appUser("owner-1", "owner", "owner@example.org"),
                appUser("member-1", "bob", "bob@example.org"),
                appUser("guest-1", "guest", "guest@example.org"),
                appUser("known-1", "alice", "alice@example.org")
            )
        )
        val mailService = FakeGroupMailService()
        val service = GroupService(
            groupStore = InMemoryGroupStore(),
            membershipStore = InMemoryGroupMembershipStore(),
            invitationStore = InMemoryGroupInvitationStore(),
            tokenStore = InMemoryGroupTokenStore(),
            joinRequestStore = InMemoryGroupJoinRequestStore(),
            appUserStore = appUserStore,
            groupMailService = mailService,
            tokenService = VerificationTokenService(),
            groupProperties = GroupProperties(frontendBaseUrl = "http://localhost:3000"),
            clock = clock
        )

        return GroupFixture(
            service = service,
            owner = CurrentUser("owner-1", "owner", "owner@example.org"),
            member = CurrentUser("member-1", "bob", "bob@example.org"),
            guest = CurrentUser("guest-1", "guest", "guest@example.org"),
            mailService = mailService
        )
    }

    private fun appUser(id: String, nickname: String, email: String): AppUser =
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
}

private data class GroupFixture(
    val service: GroupService,
    val owner: CurrentUser,
    val member: CurrentUser,
    val guest: CurrentUser,
    val mailService: FakeGroupMailService
)

internal class FakeGroupMailService : GroupMailService {
    val knownInvites = mutableListOf<String>()
    val unknownInvites = mutableListOf<String>()

    override fun sendKnownUserInvitation(email: String, inviteeName: String, groupName: String, inviterName: String) {
        knownInvites += "$email:$groupName"
    }

    override fun sendUnknownEmailInvitation(email: String, groupName: String, inviterName: String) {
        unknownInvites += "$email:$groupName"
    }
}

internal class GroupTestAppUserStore(users: List<AppUser>) : AppUserStore {
    private val records = users.associateBy { it.keycloakUserId }.toMutableMap()

    override fun save(user: AppUser): AppUser {
        records[user.keycloakUserId] = user
        return user
    }

    override fun findById(keycloakUserId: String): AppUser? = records[keycloakUserId]

    override fun findByNickname(nickname: String): AppUser? = records.values.firstOrNull { it.nickname == nickname }

    override fun findByEmail(email: String): AppUser? = records.values.firstOrNull { it.email == email }

    override fun deleteById(keycloakUserId: String) {
        records.remove(keycloakUserId)
    }

    override fun searchInviteSuggestions(query: String, excludedUserId: String, limit: Int): List<AppUser> =
        records.values
            .asSequence()
            .filter { it.status == AppUserStatus.ACTIVE }
            .filterNot { it.keycloakUserId.equals(excludedUserId, ignoreCase = true) }
            .filter {
                val normalizedQuery = query.trim()
                normalizedQuery.isBlank() ||
                    it.nickname.contains(normalizedQuery, ignoreCase = true) ||
                    it.email.contains(normalizedQuery, ignoreCase = true)
            }
            .sortedWith(compareBy<AppUser>({ it.nickname.lowercase() }, { it.email.lowercase() }))
            .take(limit)
            .toList()
}

internal class InMemoryGroupStore : GroupStore {
    private val records = linkedMapOf<Long, Group>()
    private var nextId = 1L

    override fun save(group: Group): Group {
        val id = group.id ?: nextId++
        val saved = group.copy(id = id)
        records[id] = saved
        return saved
    }

    override fun findActiveById(id: Long): Group? = records[id]?.takeIf { it.deletedAt == null }

    override fun findActiveByNormalizedName(normalizedName: String): Group? =
        records.values.firstOrNull { it.deletedAt == null && it.normalizedName == normalizedName }

    override fun findAllActive(): List<Group> = records.values.filter { it.deletedAt == null }.sortedBy { it.name }
}

internal class InMemoryGroupMembershipStore : GroupMembershipStore {
    private val records = linkedMapOf<Long, GroupMembership>()
    private var nextId = 1L

    override fun save(membership: GroupMembership): GroupMembership {
        val id = membership.id ?: nextId++
        val saved = membership.copy(id = id)
        records[id] = saved
        return saved
    }

    override fun findById(id: Long): GroupMembership? = records[id]

    override fun findByGroupId(groupId: Long): List<GroupMembership> = records.values.filter { it.groupId == groupId }

    override fun findByGroupIdAndUserId(groupId: Long, userId: String): GroupMembership? =
        records.values.firstOrNull { it.groupId == groupId && it.userId == userId }

    override fun findByUserIdAndStatuses(userId: String, statuses: Set<GroupMembershipStatus>): List<GroupMembership> =
        records.values.filter { it.userId == userId && it.status in statuses }

    override fun findInvitedByEmailWithoutUser(normalizedEmail: String): List<GroupMembership> =
        records.values.filter {
            it.userId == null &&
                it.normalizedInviteEmail == normalizedEmail &&
                it.status == GroupMembershipStatus.INVITED
        }

    override fun findByGroupIdAndNormalizedInviteEmailAndStatuses(
        groupId: Long,
        normalizedInviteEmail: String,
        statuses: Set<GroupMembershipStatus>
    ): List<GroupMembership> =
        records.values.filter {
            it.groupId == groupId &&
                it.normalizedInviteEmail == normalizedInviteEmail &&
                it.status in statuses
        }
}

internal class InMemoryGroupInvitationStore : GroupInvitationStore {
    private val records = linkedMapOf<Long, GroupInvitation>()
    private var nextId = 1L

    override fun save(invitation: GroupInvitation): GroupInvitation {
        val id = invitation.id ?: nextId++
        val saved = invitation.copy(id = id)
        records[id] = saved
        return saved
    }

    override fun findByGroupId(groupId: Long): List<GroupInvitation> =
        records.values.filter { it.groupId == groupId }.sortedByDescending { it.createdAt }

    override fun findByMembershipIds(membershipIds: Collection<Long>): List<GroupInvitation> =
        records.values.filter { it.membershipId != null && it.membershipId in membershipIds }

    override fun findByMembershipId(membershipId: Long): List<GroupInvitation> =
        records.values.filter { it.membershipId == membershipId }

    override fun findByTokenId(tokenId: Long): List<GroupInvitation> =
        records.values.filter { it.tokenId == tokenId }
}

internal class InMemoryGroupTokenStore : GroupInvitationTokenStore {
    private val records = linkedMapOf<Long, GroupInvitationToken>()
    private var nextId = 1L

    override fun save(token: GroupInvitationToken): GroupInvitationToken {
        val id = token.id ?: nextId++
        val saved = token.copy(id = id)
        records[id] = saved
        return saved
    }

    override fun findByGroupId(groupId: Long): List<GroupInvitationToken> =
        records.values.filter { it.groupId == groupId }.sortedByDescending { it.createdAt }

    override fun findByTokenHash(tokenHash: String): GroupInvitationToken? =
        records.values.firstOrNull { it.tokenHash == tokenHash }
}

internal class InMemoryGroupJoinRequestStore : GroupJoinRequestStore {
    private val records = linkedMapOf<Long, GroupJoinRequest>()
    private var nextId = 1L

    override fun save(request: GroupJoinRequest): GroupJoinRequest {
        val id = request.id ?: nextId++
        val saved = request.copy(id = id)
        records[id] = saved
        return saved
    }

    override fun findById(id: Long): GroupJoinRequest? = records[id]

    override fun findByGroupId(groupId: Long): List<GroupJoinRequest> =
        records.values.filter { it.groupId == groupId }.sortedBy { it.createdAt }

    override fun findByRequestedByUserIdAndStatus(userId: String, status: GroupJoinRequestStatus): List<GroupJoinRequest> =
        records.values.filter { it.requestedByUserId == userId && it.status == status }

    override fun findPendingByGroupIdAndUserId(groupId: Long, userId: String): GroupJoinRequest? =
        records.values.firstOrNull {
            it.groupId == groupId &&
                it.requestedByUserId == userId &&
                it.status == GroupJoinRequestStatus.PENDING
        }
}
