package de.heuermannplus.backend.activity

import de.heuermannplus.backend.group.CurrentUser
import de.heuermannplus.backend.group.Group
import de.heuermannplus.backend.group.GroupMembership
import de.heuermannplus.backend.group.GroupMembershipStatus
import de.heuermannplus.backend.group.GroupMembershipStore
import de.heuermannplus.backend.group.GroupStore
import java.time.Clock
import java.time.Instant
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.util.StringUtils

@Service
class ActivityService(
    private val activityStore: ActivityStore,
    private val participantStore: ActivityParticipantStore,
    private val groupStore: GroupStore,
    private val membershipStore: GroupMembershipStore,
    private val clock: Clock
) {

    @Transactional(readOnly = true)
    fun listUpcomingForCurrentUser(currentUser: CurrentUser): ActivityListResponse {
        val activeMemberships = membershipStore.findByUserIdAndStatuses(currentUser.userId, setOf(GroupMembershipStatus.ACTIVE))
        val membershipsById = activeMemberships.associateBy { it.id!! }
        val participants = participantStore.findAllActiveByGroupMembershipIds(membershipsById.keys)
        val now = Instant.now(clock)
        val activities = activityStore.findActiveByIds(participants.map { it.activityId }.distinct())
            .filter { !it.scheduledAt.isBefore(now) }
            .sortedBy { it.scheduledAt }

        val groupsById = activities.associate { it.groupId to requireGroup(it.groupId) }
        val participantsByActivity = participants.groupBy { it.activityId }

        return ActivityListResponse(
            activities = activities.mapNotNull { activity ->
                val currentMembership = activeMemberships.firstOrNull { it.groupId == activity.groupId } ?: return@mapNotNull null
                val currentParticipant = participantsByActivity[activity.id].orEmpty()
                    .firstOrNull { it.groupMembershipId == currentMembership.id }
                activity.toSummary(
                    group = groupsById.getValue(activity.groupId),
                    currentMembership = currentMembership,
                    currentParticipant = currentParticipant,
                    activeParticipants = participantsByActivity[activity.id].orEmpty()
                )
            }
        )
    }

    @Transactional(readOnly = true)
    fun listGroupActivities(groupId: Long, currentUser: CurrentUser): ActivityListResponse {
        val currentMembership = requireActiveMembership(groupId, currentUser.userId)
        val group = requireGroup(groupId)
        val now = Instant.now(clock)
        val activities = activityStore.findAllActiveByGroupId(groupId)
            .filter { !it.scheduledAt.isBefore(now) }
        val participantsByActivity = participantStore.findAllActiveByActivityIds(activities.mapNotNull { it.id })
            .groupBy { it.activityId }

        return ActivityListResponse(
            activities = activities.map { activity ->
                val currentParticipant = participantsByActivity[activity.id].orEmpty()
                    .firstOrNull { it.groupMembershipId == currentMembership.id }
                activity.toSummary(
                    group = group,
                    currentMembership = currentMembership,
                    currentParticipant = currentParticipant,
                    activeParticipants = participantsByActivity[activity.id].orEmpty()
                )
            }
        )
    }

    @Transactional
    fun createActivity(groupId: Long, request: CreateActivityRequest, currentUser: CurrentUser): ActivityDetailResponse {
        requireAdminMembership(groupId, currentUser.userId)
        val group = requireGroup(groupId)
        val now = Instant.now(clock)
        val activity = activityStore.save(
            Activity(
                groupId = groupId,
                description = request.description.requireField("description", "Bitte Beschreibung eingeben"),
                details = request.details.normalizeOptional(),
                location = request.location.requireField("location", "Bitte Ort eingeben"),
                scheduledAt = request.scheduledAt.requireInstant("scheduledAt", "Bitte Datum und Uhrzeit eingeben"),
                createdByUserId = currentUser.userId,
                createdAt = now,
                updatedAt = now
            )
        )

        membershipStore.findByGroupId(groupId)
            .filter { it.status == GroupMembershipStatus.ACTIVE }
            .forEach { membership ->
                participantStore.save(
                    ActivityParticipant(
                        activityId = activity.id!!,
                        groupMembershipId = membership.id!!,
                        responseStatus = ActivityResponseStatus.OPEN,
                        responseNote = null,
                        respondedAt = null,
                        createdAt = now,
                        updatedAt = now
                    )
                )
            }

        return buildActivityDetail(activity, group, currentUser)
    }

    @Transactional(readOnly = true)
    fun getActivity(groupId: Long, activityId: Long, currentUser: CurrentUser): ActivityDetailResponse {
        requireActiveMembership(groupId, currentUser.userId)
        val activity = requireActivity(groupId, activityId)
        val group = requireGroup(groupId)
        return buildActivityDetail(activity, group, currentUser)
    }

    @Transactional
    fun updateActivity(groupId: Long, activityId: Long, request: UpdateActivityRequest, currentUser: CurrentUser): ActivityDetailResponse {
        requireAdminMembership(groupId, currentUser.userId)
        val activity = requireActivity(groupId, activityId)
        val updated = activityStore.save(
            activity.copy(
                description = request.description.requireField("description", "Bitte Beschreibung eingeben"),
                details = request.details.normalizeOptional(),
                location = request.location.requireField("location", "Bitte Ort eingeben"),
                scheduledAt = request.scheduledAt.requireInstant("scheduledAt", "Bitte Datum und Uhrzeit eingeben"),
                updatedAt = Instant.now(clock)
            )
        )
        return buildActivityDetail(updated, requireGroup(groupId), currentUser)
    }

    @Transactional
    fun deleteActivity(groupId: Long, activityId: Long, currentUser: CurrentUser) {
        requireAdminMembership(groupId, currentUser.userId)
        val activity = requireActivity(groupId, activityId)
        activityStore.save(activity.copy(deletedAt = Instant.now(clock), updatedAt = Instant.now(clock)))
    }

    @Transactional
    fun addParticipant(
        groupId: Long,
        activityId: Long,
        request: AddActivityParticipantRequest,
        currentUser: CurrentUser
    ): ActivityDetailResponse {
        requireAdminMembership(groupId, currentUser.userId)
        val activity = requireActivity(groupId, activityId)
        val membershipId = request.groupMembershipId ?: throw ActivityException(
            HttpStatus.BAD_REQUEST,
            "FIELD_REQUIRED",
            "Bitte Mitglied auswählen",
            "groupMembershipId"
        )
        val membership = requireGroupMembership(groupId, membershipId)
        if (membership.status != GroupMembershipStatus.ACTIVE) {
            throw ActivityException(HttpStatus.BAD_REQUEST, "GROUP_MEMBER_NOT_ACTIVE", "Nur aktive Gruppenmitglieder können hinzugefügt werden")
        }
        if (participantStore.findActiveByActivityIdAndGroupMembershipId(activityId, membershipId) != null) {
            throw ActivityException(HttpStatus.CONFLICT, "ACTIVITY_PARTICIPANT_ALREADY_EXISTS", "Mitglied ist der Aktivität bereits zugewiesen")
        }

        participantStore.save(
            ActivityParticipant(
                activityId = activityId,
                groupMembershipId = membershipId,
                responseStatus = ActivityResponseStatus.OPEN,
                responseNote = null,
                respondedAt = null,
                createdAt = Instant.now(clock),
                updatedAt = Instant.now(clock)
            )
        )

        return buildActivityDetail(activity, requireGroup(groupId), currentUser)
    }

    @Transactional
    fun removeParticipant(groupId: Long, activityId: Long, participantId: Long, currentUser: CurrentUser): ActivityDetailResponse {
        requireAdminMembership(groupId, currentUser.userId)
        val activity = requireActivity(groupId, activityId)
        val participant = requireParticipant(activityId, participantId)
        if (participant.removedAt == null) {
            val now = Instant.now(clock)
            participantStore.save(
                participant.copy(
                    updatedAt = now,
                    removedAt = now
                )
            )
        }

        return buildActivityDetail(activity, requireGroup(groupId), currentUser)
    }

    @Transactional
    fun respond(groupId: Long, activityId: Long, request: ActivityResponseRequest, currentUser: CurrentUser): ActivityDetailResponse {
        val currentMembership = requireActiveMembership(groupId, currentUser.userId)
        val activity = requireActivity(groupId, activityId)
        val participant = participantStore.findActiveByActivityIdAndGroupMembershipId(activityId, currentMembership.id!!)
            ?: throw ActivityException(HttpStatus.FORBIDDEN, "FORBIDDEN_ACTIVITY_RESPONSE", "Nur zugewiesene Mitglieder können antworten")
        val responseStatus = request.responseStatus ?: throw ActivityException(
            HttpStatus.BAD_REQUEST,
            "FIELD_REQUIRED",
            "Bitte Antwortstatus auswählen",
            "responseStatus"
        )
        val now = Instant.now(clock)
        participantStore.save(
            participant.copy(
                responseStatus = responseStatus,
                responseNote = request.responseNote.normalizeOptional(),
                respondedAt = now,
                updatedAt = now
            )
        )

        return buildActivityDetail(activity, requireGroup(groupId), currentUser)
    }

    private fun buildActivityDetail(activity: Activity, group: Group, currentUser: CurrentUser): ActivityDetailResponse {
        val currentMembership = requireActiveMembership(group.id!!, currentUser.userId)
        val currentUserCanManage = currentMembership.isAdmin
        val membershipsById = membershipStore.findByGroupId(group.id)
            .filter { it.status == GroupMembershipStatus.ACTIVE || it.status == GroupMembershipStatus.INVITED }
            .associateBy { it.id!! }
        val participants = participantStore.findAllByActivityId(activity.id!!)
        val activeParticipants = participants.filter { it.removedAt == null }
        val currentParticipant = activeParticipants.firstOrNull { it.groupMembershipId == currentMembership.id }

        return ActivityDetailResponse(
            id = activity.id,
            groupId = group.id,
            groupName = group.name,
            description = activity.description,
            details = activity.details,
            location = activity.location,
            scheduledAt = activity.scheduledAt,
            createdAt = activity.createdAt,
            updatedAt = activity.updatedAt,
            currentUserParticipantId = currentParticipant?.id,
            currentUserResponseStatus = currentParticipant?.responseStatus,
            currentUserCanManage = currentUserCanManage,
            currentUserCanRespond = currentParticipant != null,
            participantCounts = activeParticipants.toCounts(),
            participants = participants.mapNotNull { participant ->
                val membership = membershipsById[participant.groupMembershipId] ?: return@mapNotNull null
                participant.toResponse(membership)
            }.sortedBy { it.displayName.lowercase() }
        )
    }

    private fun requireGroup(groupId: Long): Group =
        groupStore.findActiveById(groupId)
            ?: throw ActivityException(HttpStatus.NOT_FOUND, "GROUP_NOT_FOUND", "Gruppe nicht gefunden")

    private fun requireActivity(groupId: Long, activityId: Long): Activity {
        val activity = activityStore.findActiveById(activityId)
            ?: throw ActivityException(HttpStatus.NOT_FOUND, "ACTIVITY_NOT_FOUND", "Aktivität nicht gefunden")
        if (activity.groupId != groupId) {
            throw ActivityException(HttpStatus.NOT_FOUND, "ACTIVITY_NOT_FOUND", "Aktivität nicht gefunden")
        }
        return activity
    }

    private fun requireParticipant(activityId: Long, participantId: Long): ActivityParticipant {
        val participant = participantStore.findById(participantId)
            ?: throw ActivityException(HttpStatus.NOT_FOUND, "ACTIVITY_PARTICIPANT_NOT_FOUND", "Teilnehmer nicht gefunden")
        if (participant.activityId != activityId) {
            throw ActivityException(HttpStatus.NOT_FOUND, "ACTIVITY_PARTICIPANT_NOT_FOUND", "Teilnehmer nicht gefunden")
        }
        return participant
    }

    private fun requireActiveMembership(groupId: Long, userId: String): GroupMembership =
        membershipStore.findByGroupIdAndUserId(groupId, userId)
            ?.takeIf { it.status == GroupMembershipStatus.ACTIVE }
            ?: throw ActivityException(HttpStatus.FORBIDDEN, "FORBIDDEN_ACTIVITY_ACCESS", "Aktivitäten können nur von aktiven Gruppenmitgliedern eingesehen werden")

    private fun requireAdminMembership(groupId: Long, userId: String): GroupMembership =
        requireActiveMembership(groupId, userId).takeIf { it.isAdmin }
            ?: throw ActivityException(HttpStatus.FORBIDDEN, "FORBIDDEN_ACTIVITY_ADMIN", "Aktion ist nur für Gruppenverwalter erlaubt")

    private fun requireGroupMembership(groupId: Long, membershipId: Long): GroupMembership {
        val membership = membershipStore.findById(membershipId)
            ?: throw ActivityException(HttpStatus.NOT_FOUND, "GROUP_MEMBER_NOT_FOUND", "Gruppenmitglied nicht gefunden")
        if (membership.groupId != groupId) {
            throw ActivityException(HttpStatus.NOT_FOUND, "GROUP_MEMBER_NOT_FOUND", "Gruppenmitglied nicht gefunden")
        }
        return membership
    }

    private fun Activity.toSummary(
        group: Group,
        currentMembership: GroupMembership,
        currentParticipant: ActivityParticipant?,
        activeParticipants: List<ActivityParticipant>
    ): ActivitySummaryResponse =
        ActivitySummaryResponse(
            id = id!!,
            groupId = group.id!!,
            groupName = group.name,
            description = description,
            location = location,
            scheduledAt = scheduledAt,
            currentUserResponseStatus = currentParticipant?.responseStatus,
            currentUserCanManage = currentMembership.isAdmin,
            currentUserCanRespond = currentParticipant != null,
            participantCounts = activeParticipants.toCounts()
        )

    private fun ActivityParticipant.toResponse(membership: GroupMembership): ActivityParticipantResponse =
        ActivityParticipantResponse(
            id = id!!,
            groupMembershipId = groupMembershipId,
            userId = membership.userId,
            displayName = membership.displayName,
            inviteEmail = membership.inviteEmail,
            admin = membership.isAdmin,
            membershipStatus = membership.status,
            responseStatus = responseStatus,
            responseNote = responseNote,
            respondedAt = respondedAt,
            createdAt = createdAt,
            removedAt = removedAt
        )

    private fun List<ActivityParticipant>.toCounts(): ActivityParticipantCountsResponse =
        ActivityParticipantCountsResponse(
            open = count { it.responseStatus == ActivityResponseStatus.OPEN },
            accepted = count { it.responseStatus == ActivityResponseStatus.ACCEPTED },
            declined = count { it.responseStatus == ActivityResponseStatus.DECLINED },
            maybe = count { it.responseStatus == ActivityResponseStatus.MAYBE }
        )

    private fun String?.requireField(field: String, message: String): String {
        val value = this?.trim()
        if (!StringUtils.hasText(value)) {
            throw ActivityException(HttpStatus.BAD_REQUEST, "FIELD_REQUIRED", message, field)
        }
        return value!!
    }

    private fun String?.requireInstant(field: String, message: String): Instant {
        val value = this?.trim()
        if (!StringUtils.hasText(value)) {
            throw ActivityException(HttpStatus.BAD_REQUEST, "FIELD_REQUIRED", message, field)
        }

        return runCatching { Instant.parse(value) }
            .getOrElse {
                throw ActivityException(HttpStatus.BAD_REQUEST, "FIELD_INVALID", message, field)
            }
    }

    private fun String?.normalizeOptional(): String? = this?.trim()?.takeIf(StringUtils::hasText)
}
