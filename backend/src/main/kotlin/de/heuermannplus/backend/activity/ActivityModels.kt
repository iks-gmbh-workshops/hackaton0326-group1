package de.heuermannplus.backend.activity

import de.heuermannplus.backend.group.GroupMembershipStatus
import java.time.Instant

enum class ActivityResponseStatus {
    OPEN,
    ACCEPTED,
    DECLINED,
    MAYBE
}

data class Activity(
    val id: Long? = null,
    val groupId: Long,
    val description: String,
    val details: String?,
    val location: String,
    val scheduledAt: Instant,
    val createdByUserId: String,
    val createdAt: Instant,
    val updatedAt: Instant,
    val deletedAt: Instant? = null
)

data class ActivityParticipant(
    val id: Long? = null,
    val activityId: Long,
    val groupMembershipId: Long,
    val responseStatus: ActivityResponseStatus,
    val responseNote: String?,
    val respondedAt: Instant?,
    val createdAt: Instant,
    val updatedAt: Instant,
    val removedAt: Instant? = null
)

data class CreateActivityRequest(
    val description: String?,
    val details: String? = null,
    val location: String?,
    val scheduledAt: String?
)

data class UpdateActivityRequest(
    val description: String?,
    val details: String? = null,
    val location: String?,
    val scheduledAt: String?
)

data class AddActivityParticipantRequest(
    val groupMembershipId: Long?
)

data class ActivityResponseRequest(
    val responseStatus: ActivityResponseStatus?,
    val responseNote: String? = null
)

data class ActivityParticipantCountsResponse(
    val open: Int,
    val accepted: Int,
    val declined: Int,
    val maybe: Int
)

data class ActivityParticipantResponse(
    val id: Long,
    val groupMembershipId: Long,
    val userId: String?,
    val displayName: String,
    val inviteEmail: String?,
    val admin: Boolean,
    val membershipStatus: GroupMembershipStatus,
    val responseStatus: ActivityResponseStatus,
    val responseNote: String?,
    val respondedAt: Instant?,
    val createdAt: Instant,
    val removedAt: Instant?
)

data class ActivitySummaryResponse(
    val id: Long,
    val groupId: Long,
    val groupName: String,
    val description: String,
    val location: String,
    val scheduledAt: Instant,
    val currentUserResponseStatus: ActivityResponseStatus?,
    val currentUserCanManage: Boolean,
    val currentUserCanRespond: Boolean,
    val participantCounts: ActivityParticipantCountsResponse
)

data class ActivityDetailResponse(
    val id: Long,
    val groupId: Long,
    val groupName: String,
    val description: String,
    val details: String?,
    val location: String,
    val scheduledAt: Instant,
    val createdAt: Instant,
    val updatedAt: Instant,
    val currentUserParticipantId: Long?,
    val currentUserResponseStatus: ActivityResponseStatus?,
    val currentUserCanManage: Boolean,
    val currentUserCanRespond: Boolean,
    val participantCounts: ActivityParticipantCountsResponse,
    val participants: List<ActivityParticipantResponse>
)

data class ActivityListResponse(
    val activities: List<ActivitySummaryResponse>
)
