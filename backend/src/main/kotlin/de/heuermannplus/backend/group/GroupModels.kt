package de.heuermannplus.backend.group

import java.time.Instant

enum class GroupMembershipStatus {
    INVITED,
    ACTIVE,
    LEFT,
    REMOVED
}

enum class GroupInvitationChannel {
    NICKNAME,
    EMAIL,
    TOKEN
}

enum class GroupInvitationMailType {
    KNOWN_USER,
    UNKNOWN_EMAIL,
    TOKEN
}

enum class GroupJoinRequestStatus {
    PENDING,
    APPROVED,
    REJECTED
}

data class CurrentUser(
    val userId: String,
    val nickname: String,
    val email: String?
)

data class Group(
    val id: Long? = null,
    val name: String,
    val normalizedName: String,
    val description: String?,
    val createdByUserId: String,
    val createdAt: Instant,
    val updatedAt: Instant,
    val deletedAt: Instant? = null
)

data class GroupMembership(
    val id: Long? = null,
    val groupId: Long,
    val userId: String?,
    val inviteEmail: String?,
    val normalizedInviteEmail: String?,
    val displayName: String,
    val status: GroupMembershipStatus,
    val isAdmin: Boolean,
    val createdAt: Instant,
    val updatedAt: Instant,
    val joinedAt: Instant? = null,
    val leftAt: Instant? = null,
    val removedAt: Instant? = null
)

data class GroupInvitation(
    val id: Long? = null,
    val groupId: Long,
    val membershipId: Long?,
    val tokenId: Long?,
    val invitedByUserId: String,
    val channel: GroupInvitationChannel,
    val mailType: GroupInvitationMailType,
    val targetLabel: String,
    val targetEmail: String?,
    val normalizedTargetEmail: String?,
    val createdAt: Instant,
    val claimedAt: Instant? = null,
    val claimedByUserId: String? = null
)

data class GroupInvitationToken(
    val id: Long? = null,
    val groupId: Long,
    val createdByUserId: String,
    val tokenHash: String,
    val createdAt: Instant,
    val expiresAt: Instant,
    val usedAt: Instant? = null,
    val usedByUserId: String? = null
)

data class GroupJoinRequest(
    val id: Long? = null,
    val groupId: Long,
    val requestedByUserId: String,
    val requestedByDisplayName: String,
    val status: GroupJoinRequestStatus,
    val comment: String?,
    val reviewComment: String?,
    val reviewedByUserId: String?,
    val createdAt: Instant,
    val reviewedAt: Instant? = null
)

data class CreateGroupRequest(
    val name: String?,
    val description: String? = null
)

data class UpdateGroupRequest(
    val name: String?,
    val description: String? = null
)

data class InviteGroupMemberRequest(
    val nicknameOrEmail: String?
)

data class JoinGroupByTokenRequest(
    val token: String?
)

data class CreateMembershipRequest(
    val comment: String? = null
)

data class MembershipDecisionRequest(
    val comment: String? = null
)

data class GroupSummaryResponse(
    val id: Long,
    val name: String,
    val description: String?,
    val createdAt: Instant,
    val membershipStatus: GroupMembershipStatus?,
    val currentUserAdmin: Boolean,
    val memberCount: Int
)

data class GroupInvitationSummaryResponse(
    val membershipId: Long,
    val groupId: Long,
    val groupName: String,
    val displayName: String,
    val inviteEmail: String?,
    val invitedAt: Instant
)

data class GroupJoinRequestResponse(
    val id: Long,
    val groupId: Long,
    val groupName: String,
    val requestedByUserId: String,
    val requestedByDisplayName: String,
    val status: GroupJoinRequestStatus,
    val comment: String?,
    val reviewComment: String?,
    val createdAt: Instant,
    val reviewedAt: Instant?
)

data class GroupMemberResponse(
    val id: Long,
    val userId: String?,
    val displayName: String,
    val inviteEmail: String?,
    val status: GroupMembershipStatus,
    val admin: Boolean,
    val createdAt: Instant,
    val joinedAt: Instant?
)

data class GroupInvitationResponse(
    val id: Long,
    val membershipId: Long?,
    val channel: GroupInvitationChannel,
    val mailType: GroupInvitationMailType,
    val targetLabel: String,
    val targetEmail: String?,
    val createdAt: Instant,
    val claimedAt: Instant?
)

data class GroupTokenResponse(
    val id: Long,
    val token: String? = null,
    val createdAt: Instant,
    val expiresAt: Instant,
    val usedAt: Instant?
)

data class GroupResponse(
    val id: Long,
    val name: String,
    val description: String?,
    val createdAt: Instant,
    val updatedAt: Instant,
    val currentMembershipId: Long?,
    val currentMembershipStatus: GroupMembershipStatus?,
    val currentUserAdmin: Boolean,
    val members: List<GroupMemberResponse>,
    val invitations: List<GroupInvitationResponse>,
    val joinRequests: List<GroupJoinRequestResponse>,
    val tokens: List<GroupTokenResponse>
)

data class GroupListResponse(
    val groups: List<GroupSummaryResponse>,
    val invitations: List<GroupInvitationSummaryResponse>,
    val joinRequests: List<GroupJoinRequestResponse>,
    val availableGroups: List<GroupSummaryResponse>
)
