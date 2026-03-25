package de.heuermannplus.backend.activity

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

interface ActivityStore {
    fun save(activity: Activity): Activity

    fun findActiveById(id: Long): Activity?

    fun findActiveByIds(ids: Collection<Long>): List<Activity>

    fun findAllActiveByGroupId(groupId: Long): List<Activity>
}

interface ActivityParticipantStore {
    fun save(participant: ActivityParticipant): ActivityParticipant

    fun findById(id: Long): ActivityParticipant?

    fun findAllByActivityId(activityId: Long): List<ActivityParticipant>

    fun findAllActiveByActivityIds(activityIds: Collection<Long>): List<ActivityParticipant>

    fun findAllActiveByGroupMembershipIds(groupMembershipIds: Collection<Long>): List<ActivityParticipant>

    fun findActiveByActivityIdAndGroupMembershipId(activityId: Long, groupMembershipId: Long): ActivityParticipant?
}

@Entity
@Table(name = "group_activity")
class ActivityEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,
    @Column(name = "group_id", nullable = false)
    var groupId: Long = 0,
    @Column(nullable = false)
    var description: String = "",
    @Column(columnDefinition = "TEXT")
    var details: String? = null,
    @Column(nullable = false)
    var location: String = "",
    @Column(name = "scheduled_at", nullable = false)
    var scheduledAt: Instant = Instant.now(),
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
@Table(name = "activity_participant")
class ActivityParticipantEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,
    @Column(name = "activity_id", nullable = false)
    var activityId: Long = 0,
    @Column(name = "group_membership_id", nullable = false)
    var groupMembershipId: Long = 0,
    @Enumerated(EnumType.STRING)
    @Column(name = "response_status", nullable = false)
    var responseStatus: ActivityResponseStatus = ActivityResponseStatus.OPEN,
    @Column(name = "response_note", columnDefinition = "TEXT")
    var responseNote: String? = null,
    @Column(name = "responded_at")
    var respondedAt: Instant? = null,
    @Column(name = "created_at", nullable = false)
    var createdAt: Instant = Instant.now(),
    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now(),
    @Column(name = "removed_at")
    var removedAt: Instant? = null,
    @Column(name = "assignment_slot", nullable = false)
    var assignmentSlot: Long = 0
)

interface ActivityJpaRepository : JpaRepository<ActivityEntity, Long> {
    fun findByIdAndDeletedAtIsNull(id: Long): ActivityEntity?

    fun findAllByIdInAndDeletedAtIsNull(ids: Collection<Long>): List<ActivityEntity>

    fun findAllByGroupIdAndDeletedAtIsNullOrderByScheduledAtAsc(groupId: Long): List<ActivityEntity>
}

interface ActivityParticipantJpaRepository : JpaRepository<ActivityParticipantEntity, Long> {
    fun findAllByActivityIdOrderByCreatedAtAsc(activityId: Long): List<ActivityParticipantEntity>

    fun findAllByActivityIdInAndRemovedAtIsNullOrderByCreatedAtAsc(activityIds: Collection<Long>): List<ActivityParticipantEntity>

    fun findAllByGroupMembershipIdInAndRemovedAtIsNullOrderByCreatedAtAsc(
        groupMembershipIds: Collection<Long>
    ): List<ActivityParticipantEntity>

    fun findByActivityIdAndGroupMembershipIdAndRemovedAtIsNull(
        activityId: Long,
        groupMembershipId: Long
    ): ActivityParticipantEntity?
}

@Component
class JpaActivityStore(
    private val repository: ActivityJpaRepository
) : ActivityStore {

    override fun save(activity: Activity): Activity =
        repository.save(activity.toEntity()).toDomain()

    override fun findActiveById(id: Long): Activity? =
        repository.findByIdAndDeletedAtIsNull(id)?.toDomain()

    override fun findActiveByIds(ids: Collection<Long>): List<Activity> {
        if (ids.isEmpty()) {
            return emptyList()
        }

        return repository.findAllByIdInAndDeletedAtIsNull(ids).map(ActivityEntity::toDomain)
    }

    override fun findAllActiveByGroupId(groupId: Long): List<Activity> =
        repository.findAllByGroupIdAndDeletedAtIsNullOrderByScheduledAtAsc(groupId).map(ActivityEntity::toDomain)
}

@Component
class JpaActivityParticipantStore(
    private val repository: ActivityParticipantJpaRepository
) : ActivityParticipantStore {

    override fun save(participant: ActivityParticipant): ActivityParticipant =
        repository.save(participant.toEntity()).toDomain()

    override fun findById(id: Long): ActivityParticipant? =
        repository.findById(id).orElse(null)?.toDomain()

    override fun findAllByActivityId(activityId: Long): List<ActivityParticipant> =
        repository.findAllByActivityIdOrderByCreatedAtAsc(activityId).map(ActivityParticipantEntity::toDomain)

    override fun findAllActiveByActivityIds(activityIds: Collection<Long>): List<ActivityParticipant> {
        if (activityIds.isEmpty()) {
            return emptyList()
        }

        return repository.findAllByActivityIdInAndRemovedAtIsNullOrderByCreatedAtAsc(activityIds)
            .map(ActivityParticipantEntity::toDomain)
    }

    override fun findAllActiveByGroupMembershipIds(groupMembershipIds: Collection<Long>): List<ActivityParticipant> {
        if (groupMembershipIds.isEmpty()) {
            return emptyList()
        }

        return repository.findAllByGroupMembershipIdInAndRemovedAtIsNullOrderByCreatedAtAsc(groupMembershipIds)
            .map(ActivityParticipantEntity::toDomain)
    }

    override fun findActiveByActivityIdAndGroupMembershipId(activityId: Long, groupMembershipId: Long): ActivityParticipant? =
        repository.findByActivityIdAndGroupMembershipIdAndRemovedAtIsNull(activityId, groupMembershipId)?.toDomain()
}

private fun Activity.toEntity(): ActivityEntity =
    ActivityEntity(
        id = id,
        groupId = groupId,
        description = description,
        details = details,
        location = location,
        scheduledAt = scheduledAt,
        createdByUserId = createdByUserId,
        createdAt = createdAt,
        updatedAt = updatedAt,
        deletedAt = deletedAt
    )

private fun ActivityEntity.toDomain(): Activity =
    Activity(
        id = id,
        groupId = groupId,
        description = description,
        details = details,
        location = location,
        scheduledAt = scheduledAt,
        createdByUserId = createdByUserId,
        createdAt = createdAt,
        updatedAt = updatedAt,
        deletedAt = deletedAt
    )

private fun ActivityParticipant.toEntity(): ActivityParticipantEntity =
    ActivityParticipantEntity(
        id = id,
        activityId = activityId,
        groupMembershipId = groupMembershipId,
        responseStatus = responseStatus,
        responseNote = responseNote,
        respondedAt = respondedAt,
        createdAt = createdAt,
        updatedAt = updatedAt,
        removedAt = removedAt,
        assignmentSlot = if (removedAt == null) 0 else id ?: createdAt.toEpochMilli()
    )

private fun ActivityParticipantEntity.toDomain(): ActivityParticipant =
    ActivityParticipant(
        id = id,
        activityId = activityId,
        groupMembershipId = groupMembershipId,
        responseStatus = responseStatus,
        responseNote = responseNote,
        respondedAt = respondedAt,
        createdAt = createdAt,
        updatedAt = updatedAt,
        removedAt = removedAt
    )
