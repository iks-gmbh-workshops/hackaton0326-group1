package de.heuermannplus.backend.group

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Component

interface GroupStore {
    fun save(group: Group): Group

    fun findActiveById(id: Long): Group?

    fun findActiveByNormalizedName(normalizedName: String): Group?

    fun findAllActive(): List<Group>
}

interface GroupMembershipStore {
    fun save(membership: GroupMembership): GroupMembership

    fun findById(id: Long): GroupMembership?

    fun findByGroupId(groupId: Long): List<GroupMembership>

    fun findByGroupIdAndUserId(groupId: Long, userId: String): GroupMembership?

    fun findByUserIdAndStatuses(userId: String, statuses: Set<GroupMembershipStatus>): List<GroupMembership>

    fun findInvitedByEmailWithoutUser(normalizedEmail: String): List<GroupMembership>

    fun findByGroupIdAndNormalizedInviteEmailAndStatuses(
        groupId: Long,
        normalizedInviteEmail: String,
        statuses: Set<GroupMembershipStatus>
    ): List<GroupMembership>
}

interface GroupInvitationStore {
    fun save(invitation: GroupInvitation): GroupInvitation

    fun findByGroupId(groupId: Long): List<GroupInvitation>

    fun findByMembershipIds(membershipIds: Collection<Long>): List<GroupInvitation>

    fun findByMembershipId(membershipId: Long): List<GroupInvitation>

    fun findByTokenId(tokenId: Long): List<GroupInvitation>
}

interface GroupInvitationTokenStore {
    fun save(token: GroupInvitationToken): GroupInvitationToken

    fun findById(id: Long): GroupInvitationToken?

    fun findByGroupId(groupId: Long): List<GroupInvitationToken>

    fun findByTokenHash(tokenHash: String): GroupInvitationToken?
}

interface GroupJoinRequestStore {
    fun save(request: GroupJoinRequest): GroupJoinRequest

    fun findById(id: Long): GroupJoinRequest?

    fun findByGroupId(groupId: Long): List<GroupJoinRequest>

    fun findByRequestedByUserIdAndStatus(userId: String, status: GroupJoinRequestStatus): List<GroupJoinRequest>

    fun findPendingByGroupIdAndUserId(groupId: Long, userId: String): GroupJoinRequest?
}

@Entity
@Table(name = "app_group")
class GroupEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,
    @Column(nullable = false)
    var name: String = "",
    @Column(name = "normalized_name", nullable = false)
    var normalizedName: String = "",
    @Column(columnDefinition = "TEXT")
    var description: String? = null,
    @Column(name = "created_by_user_id", nullable = false)
    var createdByUserId: String = "",
    @Column(name = "created_at", nullable = false)
    var createdAt: Instant = Instant.now(),
    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now(),
    @Column(name = "deleted_at")
    var deletedAt: Instant? = null
)

@Entity
@Table(name = "group_membership")
class GroupMembershipEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,
    @Column(name = "group_id", nullable = false)
    var groupId: Long = 0,
    @Column(name = "user_id")
    var userId: String? = null,
    @Column(name = "invite_email")
    var inviteEmail: String? = null,
    @Column(name = "normalized_invite_email")
    var normalizedInviteEmail: String? = null,
    @Column(name = "display_name", nullable = false)
    var displayName: String = "",
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var status: GroupMembershipStatus = GroupMembershipStatus.INVITED,
    @Column(name = "open_user_relation")
    var openUserRelation: Boolean? = null,
    @Column(name = "open_email_relation")
    var openEmailRelation: Boolean? = null,
    @Column(name = "is_admin", nullable = false)
    var isAdmin: Boolean = false,
    @Column(name = "created_at", nullable = false)
    var createdAt: Instant = Instant.now(),
    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now(),
    @Column(name = "joined_at")
    var joinedAt: Instant? = null,
    @Column(name = "left_at")
    var leftAt: Instant? = null,
    @Column(name = "removed_at")
    var removedAt: Instant? = null
)

@Entity
@Table(name = "group_invitation")
class GroupInvitationEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,
    @Column(name = "group_id", nullable = false)
    var groupId: Long = 0,
    @Column(name = "membership_id")
    var membershipId: Long? = null,
    @Column(name = "token_id")
    var tokenId: Long? = null,
    @Column(name = "invited_by_user_id", nullable = false)
    var invitedByUserId: String = "",
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var channel: GroupInvitationChannel = GroupInvitationChannel.EMAIL,
    @Enumerated(EnumType.STRING)
    @Column(name = "mail_type", nullable = false)
    var mailType: GroupInvitationMailType = GroupInvitationMailType.KNOWN_USER,
    @Column(name = "target_label", nullable = false)
    var targetLabel: String = "",
    @Column(name = "target_email")
    var targetEmail: String? = null,
    @Column(name = "normalized_target_email")
    var normalizedTargetEmail: String? = null,
    @Column(name = "created_at", nullable = false)
    var createdAt: Instant = Instant.now(),
    @Column(name = "claimed_at")
    var claimedAt: Instant? = null,
    @Column(name = "claimed_by_user_id")
    var claimedByUserId: String? = null
)

@Entity
@Table(name = "group_invitation_token")
class GroupInvitationTokenEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,
    @Column(name = "group_id", nullable = false)
    var groupId: Long = 0,
    @Column(name = "created_by_user_id", nullable = false)
    var createdByUserId: String = "",
    @Column(name = "token_hash", nullable = false, unique = true)
    var tokenHash: String = "",
    @Column(name = "created_at", nullable = false)
    var createdAt: Instant = Instant.now(),
    @Column(name = "expires_at", nullable = false)
    var expiresAt: Instant = Instant.now(),
    @Column(name = "used_at")
    var usedAt: Instant? = null,
    @Column(name = "used_by_user_id")
    var usedByUserId: String? = null
)

@Entity
@Table(name = "group_join_request")
class GroupJoinRequestEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,
    @Column(name = "group_id", nullable = false)
    var groupId: Long = 0,
    @Column(name = "requested_by_user_id", nullable = false)
    var requestedByUserId: String = "",
    @Column(name = "requested_by_display_name", nullable = false)
    var requestedByDisplayName: String = "",
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var status: GroupJoinRequestStatus = GroupJoinRequestStatus.PENDING,
    @Column(columnDefinition = "TEXT")
    var comment: String? = null,
    @Column(name = "review_comment", columnDefinition = "TEXT")
    var reviewComment: String? = null,
    @Column(name = "reviewed_by_user_id")
    var reviewedByUserId: String? = null,
    @Column(name = "created_at", nullable = false)
    var createdAt: Instant = Instant.now(),
    @Column(name = "reviewed_at")
    var reviewedAt: Instant? = null
)

interface GroupJpaRepository : JpaRepository<GroupEntity, Long> {
    fun findByIdAndDeletedAtIsNull(id: Long): GroupEntity?

    fun findByNormalizedNameAndDeletedAtIsNull(normalizedName: String): GroupEntity?

    fun findAllByDeletedAtIsNullOrderByNameAsc(): List<GroupEntity>
}

interface GroupMembershipJpaRepository : JpaRepository<GroupMembershipEntity, Long> {
    fun findAllByGroupId(groupId: Long): List<GroupMembershipEntity>

    fun findAllByGroupIdAndUserIdOrderByCreatedAtDesc(groupId: Long, userId: String): List<GroupMembershipEntity>

    fun findAllByUserIdAndStatusIn(userId: String, statuses: Collection<GroupMembershipStatus>): List<GroupMembershipEntity>

    fun findAllByUserIdIsNullAndNormalizedInviteEmailAndStatus(
        normalizedInviteEmail: String,
        status: GroupMembershipStatus
    ): List<GroupMembershipEntity>

    fun findAllByGroupIdAndNormalizedInviteEmailAndStatusIn(
        groupId: Long,
        normalizedInviteEmail: String,
        statuses: Collection<GroupMembershipStatus>
    ): List<GroupMembershipEntity>
}

interface GroupInvitationJpaRepository : JpaRepository<GroupInvitationEntity, Long> {
    fun findAllByGroupIdOrderByCreatedAtDesc(groupId: Long): List<GroupInvitationEntity>

    fun findAllByMembershipIdInOrderByCreatedAtDesc(membershipIds: Collection<Long>): List<GroupInvitationEntity>

    fun findAllByMembershipIdOrderByCreatedAtDesc(membershipId: Long): List<GroupInvitationEntity>

    fun findAllByTokenIdOrderByCreatedAtDesc(tokenId: Long): List<GroupInvitationEntity>
}

interface GroupInvitationTokenJpaRepository : JpaRepository<GroupInvitationTokenEntity, Long> {
    fun findAllByGroupIdOrderByCreatedAtDesc(groupId: Long): List<GroupInvitationTokenEntity>

    fun findByTokenHash(tokenHash: String): GroupInvitationTokenEntity?
}

interface GroupJoinRequestJpaRepository : JpaRepository<GroupJoinRequestEntity, Long> {
    fun findAllByGroupIdOrderByCreatedAtAsc(groupId: Long): List<GroupJoinRequestEntity>

    fun findAllByRequestedByUserIdAndStatusOrderByCreatedAtDesc(
        requestedByUserId: String,
        status: GroupJoinRequestStatus
    ): List<GroupJoinRequestEntity>

    fun findByGroupIdAndRequestedByUserIdAndStatus(
        groupId: Long,
        requestedByUserId: String,
        status: GroupJoinRequestStatus
    ): GroupJoinRequestEntity?
}

@Component
class JpaGroupStore(
    private val repository: GroupJpaRepository
) : GroupStore {

    override fun save(group: Group): Group =
        repository.save(group.toEntity()).toDomain()

    override fun findActiveById(id: Long): Group? =
        repository.findByIdAndDeletedAtIsNull(id)?.toDomain()

    override fun findActiveByNormalizedName(normalizedName: String): Group? =
        repository.findByNormalizedNameAndDeletedAtIsNull(normalizedName)?.toDomain()

    override fun findAllActive(): List<Group> =
        repository.findAllByDeletedAtIsNullOrderByNameAsc().map(GroupEntity::toDomain)
}

@Component
class JpaGroupMembershipStore(
    private val repository: GroupMembershipJpaRepository
) : GroupMembershipStore {

    override fun save(membership: GroupMembership): GroupMembership =
        repository.save(membership.toEntity()).toDomain()

    override fun findById(id: Long): GroupMembership? =
        repository.findById(id).orElse(null)?.toDomain()

    override fun findByGroupId(groupId: Long): List<GroupMembership> =
        repository.findAllByGroupId(groupId).map(GroupMembershipEntity::toDomain)

    override fun findByGroupIdAndUserId(groupId: Long, userId: String): GroupMembership? =
        repository.findAllByGroupIdAndUserIdOrderByCreatedAtDesc(groupId, userId).firstOrNull()?.toDomain()

    override fun findByUserIdAndStatuses(userId: String, statuses: Set<GroupMembershipStatus>): List<GroupMembership> =
        repository.findAllByUserIdAndStatusIn(userId, statuses).map(GroupMembershipEntity::toDomain)

    override fun findInvitedByEmailWithoutUser(normalizedEmail: String): List<GroupMembership> =
        repository.findAllByUserIdIsNullAndNormalizedInviteEmailAndStatus(
            normalizedEmail,
            GroupMembershipStatus.INVITED
        ).map(GroupMembershipEntity::toDomain)

    override fun findByGroupIdAndNormalizedInviteEmailAndStatuses(
        groupId: Long,
        normalizedInviteEmail: String,
        statuses: Set<GroupMembershipStatus>
    ): List<GroupMembership> =
        repository.findAllByGroupIdAndNormalizedInviteEmailAndStatusIn(groupId, normalizedInviteEmail, statuses)
            .map(GroupMembershipEntity::toDomain)
}

@Component
class JpaGroupInvitationStore(
    private val repository: GroupInvitationJpaRepository
) : GroupInvitationStore {

    override fun save(invitation: GroupInvitation): GroupInvitation =
        repository.save(invitation.toEntity()).toDomain()

    override fun findByGroupId(groupId: Long): List<GroupInvitation> =
        repository.findAllByGroupIdOrderByCreatedAtDesc(groupId).map(GroupInvitationEntity::toDomain)

    override fun findByMembershipIds(membershipIds: Collection<Long>): List<GroupInvitation> =
        if (membershipIds.isEmpty()) emptyList()
        else repository.findAllByMembershipIdInOrderByCreatedAtDesc(membershipIds).map(GroupInvitationEntity::toDomain)

    override fun findByMembershipId(membershipId: Long): List<GroupInvitation> =
        repository.findAllByMembershipIdOrderByCreatedAtDesc(membershipId).map(GroupInvitationEntity::toDomain)

    override fun findByTokenId(tokenId: Long): List<GroupInvitation> =
        repository.findAllByTokenIdOrderByCreatedAtDesc(tokenId).map(GroupInvitationEntity::toDomain)
}

@Component
class JpaGroupInvitationTokenStore(
    private val repository: GroupInvitationTokenJpaRepository
) : GroupInvitationTokenStore {

    override fun save(token: GroupInvitationToken): GroupInvitationToken =
        repository.save(token.toEntity()).toDomain()

    override fun findById(id: Long): GroupInvitationToken? =
        repository.findById(id).orElse(null)?.toDomain()

    override fun findByGroupId(groupId: Long): List<GroupInvitationToken> =
        repository.findAllByGroupIdOrderByCreatedAtDesc(groupId).map(GroupInvitationTokenEntity::toDomain)

    override fun findByTokenHash(tokenHash: String): GroupInvitationToken? =
        repository.findByTokenHash(tokenHash)?.toDomain()
}

@Component
class JpaGroupJoinRequestStore(
    private val repository: GroupJoinRequestJpaRepository
) : GroupJoinRequestStore {

    override fun save(request: GroupJoinRequest): GroupJoinRequest =
        repository.save(request.toEntity()).toDomain()

    override fun findById(id: Long): GroupJoinRequest? =
        repository.findById(id).orElse(null)?.toDomain()

    override fun findByGroupId(groupId: Long): List<GroupJoinRequest> =
        repository.findAllByGroupIdOrderByCreatedAtAsc(groupId).map(GroupJoinRequestEntity::toDomain)

    override fun findByRequestedByUserIdAndStatus(userId: String, status: GroupJoinRequestStatus): List<GroupJoinRequest> =
        repository.findAllByRequestedByUserIdAndStatusOrderByCreatedAtDesc(userId, status).map(GroupJoinRequestEntity::toDomain)

    override fun findPendingByGroupIdAndUserId(groupId: Long, userId: String): GroupJoinRequest? =
        repository.findByGroupIdAndRequestedByUserIdAndStatus(groupId, userId, GroupJoinRequestStatus.PENDING)?.toDomain()
}

private fun Group.toEntity(): GroupEntity =
    GroupEntity(
        id = id,
        name = name,
        normalizedName = normalizedName,
        description = description,
        createdByUserId = createdByUserId,
        createdAt = createdAt,
        updatedAt = updatedAt,
        deletedAt = deletedAt
    )

private fun GroupEntity.toDomain(): Group =
    Group(
        id = id,
        name = name,
        normalizedName = normalizedName,
        description = description,
        createdByUserId = createdByUserId,
        createdAt = createdAt,
        updatedAt = updatedAt,
        deletedAt = deletedAt
    )

private fun GroupMembership.toEntity(): GroupMembershipEntity =
    GroupMembershipEntity(
        id = id,
        groupId = groupId,
        userId = userId,
        inviteEmail = inviteEmail,
        normalizedInviteEmail = normalizedInviteEmail,
        displayName = displayName,
        status = status,
        openUserRelation = openUserRelation(),
        openEmailRelation = openEmailRelation(),
        isAdmin = isAdmin,
        createdAt = createdAt,
        updatedAt = updatedAt,
        joinedAt = joinedAt,
        leftAt = leftAt,
        removedAt = removedAt
    )

private fun GroupMembershipEntity.toDomain(): GroupMembership =
    GroupMembership(
        id = id,
        groupId = groupId,
        userId = userId,
        inviteEmail = inviteEmail,
        normalizedInviteEmail = normalizedInviteEmail,
        displayName = displayName,
        status = status,
        isAdmin = isAdmin,
        createdAt = createdAt,
        updatedAt = updatedAt,
        joinedAt = joinedAt,
        leftAt = leftAt,
        removedAt = removedAt
    )

private fun GroupInvitation.toEntity(): GroupInvitationEntity =
    GroupInvitationEntity(
        id = id,
        groupId = groupId,
        membershipId = membershipId,
        tokenId = tokenId,
        invitedByUserId = invitedByUserId,
        channel = channel,
        mailType = mailType,
        targetLabel = targetLabel,
        targetEmail = targetEmail,
        normalizedTargetEmail = normalizedTargetEmail,
        createdAt = createdAt,
        claimedAt = claimedAt,
        claimedByUserId = claimedByUserId
    )

private fun GroupInvitationEntity.toDomain(): GroupInvitation =
    GroupInvitation(
        id = id,
        groupId = groupId,
        membershipId = membershipId,
        tokenId = tokenId,
        invitedByUserId = invitedByUserId,
        channel = channel,
        mailType = mailType,
        targetLabel = targetLabel,
        targetEmail = targetEmail,
        normalizedTargetEmail = normalizedTargetEmail,
        createdAt = createdAt,
        claimedAt = claimedAt,
        claimedByUserId = claimedByUserId
    )

private fun GroupInvitationToken.toEntity(): GroupInvitationTokenEntity =
    GroupInvitationTokenEntity(
        id = id,
        groupId = groupId,
        createdByUserId = createdByUserId,
        tokenHash = tokenHash,
        createdAt = createdAt,
        expiresAt = expiresAt,
        usedAt = usedAt,
        usedByUserId = usedByUserId
    )

private fun GroupInvitationTokenEntity.toDomain(): GroupInvitationToken =
    GroupInvitationToken(
        id = id,
        groupId = groupId,
        createdByUserId = createdByUserId,
        tokenHash = tokenHash,
        createdAt = createdAt,
        expiresAt = expiresAt,
        usedAt = usedAt,
        usedByUserId = usedByUserId
    )

private fun GroupJoinRequest.toEntity(): GroupJoinRequestEntity =
    GroupJoinRequestEntity(
        id = id,
        groupId = groupId,
        requestedByUserId = requestedByUserId,
        requestedByDisplayName = requestedByDisplayName,
        status = status,
        comment = comment,
        reviewComment = reviewComment,
        reviewedByUserId = reviewedByUserId,
        createdAt = createdAt,
        reviewedAt = reviewedAt
    )

private fun GroupJoinRequestEntity.toDomain(): GroupJoinRequest =
    GroupJoinRequest(
        id = id,
        groupId = groupId,
        requestedByUserId = requestedByUserId,
        requestedByDisplayName = requestedByDisplayName,
        status = status,
        comment = comment,
        reviewComment = reviewComment,
        reviewedByUserId = reviewedByUserId,
        createdAt = createdAt,
        reviewedAt = reviewedAt
    )

private fun GroupMembership.openUserRelation(): Boolean? =
    if (userId != null && status.isOpenRelation()) true else null

private fun GroupMembership.openEmailRelation(): Boolean? =
    if (normalizedInviteEmail != null && status.isOpenRelation()) true else null

private fun GroupMembershipStatus.isOpenRelation(): Boolean =
    this == GroupMembershipStatus.ACTIVE || this == GroupMembershipStatus.INVITED
