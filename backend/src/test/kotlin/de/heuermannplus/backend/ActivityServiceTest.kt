package de.heuermannplus.backend

import de.heuermannplus.backend.activity.Activity
import de.heuermannplus.backend.activity.ActivityException
import de.heuermannplus.backend.activity.ActivityParticipant
import de.heuermannplus.backend.activity.ActivityParticipantStore
import de.heuermannplus.backend.activity.ActivityResponseRequest
import de.heuermannplus.backend.activity.ActivityResponseStatus
import de.heuermannplus.backend.activity.ActivityService
import de.heuermannplus.backend.activity.ActivityStore
import de.heuermannplus.backend.activity.AddActivityParticipantRequest
import de.heuermannplus.backend.activity.CreateActivityRequest
import de.heuermannplus.backend.activity.UpdateActivityRequest
import de.heuermannplus.backend.group.CurrentUser
import de.heuermannplus.backend.group.Group
import de.heuermannplus.backend.group.GroupMembership
import de.heuermannplus.backend.group.GroupMembershipStatus
import de.heuermannplus.backend.group.GroupMembershipStore
import de.heuermannplus.backend.group.GroupStore
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class ActivityServiceTest {

    @Test
    fun `create activity assigns all active members as open participants`() {
        val fixture = activityFixture()
        val detail = fixture.service.createActivity(
            fixture.groupId,
            CreateActivityRequest(
                description = "Probe",
                details = "Instrumente mitbringen",
                location = "Studio",
                scheduledAt = "2026-03-26T18:30:00Z"
            ),
            fixture.owner
        )

        assertEquals("Probe", detail.description)
        assertEquals(3, detail.participants.count { it.removedAt == null })
        assertTrue(detail.participants.all { it.responseStatus == ActivityResponseStatus.OPEN })
        assertTrue(detail.participants.none { it.displayName == "invited" })
    }

    @Test
    fun `create activity validates required description`() {
        val fixture = activityFixture()

        val exception = assertFailsWith<ActivityException> {
            fixture.service.createActivity(
                fixture.groupId,
                CreateActivityRequest(
                    description = " ",
                    details = null,
                    location = "Studio",
                    scheduledAt = "2026-03-26T18:30:00Z"
                ),
                fixture.owner
            )
        }

        assertEquals("FIELD_REQUIRED", exception.code)
        assertEquals("description", exception.field)
    }

    @Test
    fun `participant can update response multiple times`() {
        val fixture = activityFixture()
        val detail = fixture.createDefaultActivity()

        val accepted = fixture.service.respond(
            fixture.groupId,
            detail.id,
            ActivityResponseRequest(responseStatus = ActivityResponseStatus.ACCEPTED, responseNote = "Bin dabei"),
            fixture.member
        )
        val declined = fixture.service.respond(
            fixture.groupId,
            detail.id,
            ActivityResponseRequest(responseStatus = ActivityResponseStatus.DECLINED, responseNote = "Doch nicht"),
            fixture.member
        )

        assertEquals(ActivityResponseStatus.ACCEPTED, accepted.currentUserResponseStatus)
        assertEquals(ActivityResponseStatus.DECLINED, declined.currentUserResponseStatus)
        assertEquals(1, declined.participantCounts.declined)
        assertEquals("Doch nicht", declined.participants.first { it.displayName == "member" }.responseNote)
    }

    @Test
    fun `admin can add remove and readd participant without duplicates`() {
        val fixture = activityFixture()
        val detail = fixture.createDefaultActivity()
        val guestMembership = fixture.memberships.first { it.displayName == "guest" }

        val duplicate = assertFailsWith<ActivityException> {
            fixture.service.addParticipant(
                fixture.groupId,
                detail.id,
                AddActivityParticipantRequest(groupMembershipId = guestMembership.id),
                fixture.owner
            )
        }
        assertEquals("ACTIVITY_PARTICIPANT_ALREADY_EXISTS", duplicate.code)

        val initialGuest = detail.participants.first { it.groupMembershipId == guestMembership.id && it.removedAt == null }
        fixture.service.removeParticipant(fixture.groupId, detail.id, initialGuest.id, fixture.owner)

        val readded = fixture.service.addParticipant(
            fixture.groupId,
            detail.id,
            AddActivityParticipantRequest(groupMembershipId = guestMembership.id),
            fixture.owner
        )

        val activeGuest = readded.participants.firstOrNull { it.groupMembershipId == guestMembership.id && it.removedAt == null }
        assertNotNull(activeGuest)
        assertEquals(1, readded.participants.count { it.groupMembershipId == guestMembership.id && it.removedAt == null })
    }

    @Test
    fun `non assigned active member can view but not respond`() {
        val fixture = activityFixture()
        val detail = fixture.createDefaultActivity()
        val guestMembership = fixture.memberships.first { it.displayName == "guest" }
        val guestParticipant = detail.participants.first { it.groupMembershipId == guestMembership.id }
        fixture.service.removeParticipant(fixture.groupId, detail.id, guestParticipant.id, fixture.owner)

        val visible = fixture.service.getActivity(fixture.groupId, detail.id, fixture.guest)
        assertEquals(false, visible.currentUserCanRespond)

        val exception = assertFailsWith<ActivityException> {
            fixture.service.respond(
                fixture.groupId,
                detail.id,
                ActivityResponseRequest(responseStatus = ActivityResponseStatus.MAYBE),
                fixture.guest
            )
        }

        assertEquals("FORBIDDEN_ACTIVITY_RESPONSE", exception.code)
    }

    @Test
    fun `upcoming list only contains future assigned activities`() {
        val fixture = activityFixture()
        fixture.service.createActivity(
            fixture.groupId,
            CreateActivityRequest(
                description = "Vergangenheit",
                details = null,
                location = "Alt",
                scheduledAt = "2026-03-24T18:30:00Z"
            ),
            fixture.owner
        )
        fixture.service.createActivity(
            fixture.groupId,
            CreateActivityRequest(
                description = "Zukunft",
                details = null,
                location = "Neu",
                scheduledAt = "2026-03-27T18:30:00Z"
            ),
            fixture.owner
        )

        val list = fixture.service.listUpcomingForCurrentUser(fixture.member)

        assertEquals(listOf("Zukunft"), list.activities.map { it.description })
    }

    @Test
    fun `only admins may update or delete activities`() {
        val fixture = activityFixture()
        val detail = fixture.createDefaultActivity()

        val updateException = assertFailsWith<ActivityException> {
            fixture.service.updateActivity(
                fixture.groupId,
                detail.id,
                UpdateActivityRequest(
                    description = "Update",
                    details = null,
                    location = "Studio",
                    scheduledAt = "2026-03-29T18:30:00Z"
                ),
                fixture.member
            )
        }
        assertEquals("FORBIDDEN_ACTIVITY_ADMIN", updateException.code)

        val deleteException = assertFailsWith<ActivityException> {
            fixture.service.deleteActivity(fixture.groupId, detail.id, fixture.member)
        }
        assertEquals("FORBIDDEN_ACTIVITY_ADMIN", deleteException.code)
    }

    private fun activityFixture(): ActivityFixture {
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
            ),
            membershipStore.save(
                GroupMembership(
                    groupId = group.id,
                    userId = "invited-1",
                    inviteEmail = "invited@example.org",
                    normalizedInviteEmail = "invited@example.org",
                    displayName = "invited",
                    status = GroupMembershipStatus.INVITED,
                    isAdmin = false,
                    createdAt = Instant.parse("2026-03-20T10:00:00Z"),
                    updatedAt = Instant.parse("2026-03-20T10:00:00Z")
                )
            )
        )

        return ActivityFixture(
            service = ActivityService(activityStore, participantStore, groupStore, membershipStore, clock),
            groupId = group.id,
            memberships = memberships,
            owner = CurrentUser("owner-1", "owner", "owner@example.org"),
            member = CurrentUser("member-1", "member", "member@example.org"),
            guest = CurrentUser("guest-1", "guest", "guest@example.org")
        )
    }
}

private data class ActivityFixture(
    val service: ActivityService,
    val groupId: Long,
    val memberships: List<GroupMembership>,
    val owner: CurrentUser,
    val member: CurrentUser,
    val guest: CurrentUser
) {
    fun createDefaultActivity() =
        service.createActivity(
            groupId,
            CreateActivityRequest(
                description = "Probeabend",
                details = null,
                location = "Studio",
                scheduledAt = "2026-03-26T18:30:00Z"
            ),
            owner
        )
}

internal class InMemoryActivityStore : ActivityStore {
    private val records = linkedMapOf<Long, Activity>()
    private var nextId = 1L

    override fun save(activity: Activity): Activity {
        val persisted = if (activity.id == null) activity.copy(id = nextId++) else activity
        records[persisted.id!!] = persisted
        return persisted
    }

    override fun findActiveById(id: Long): Activity? =
        records[id]?.takeIf { it.deletedAt == null }

    override fun findActiveByIds(ids: Collection<Long>): List<Activity> =
        ids.mapNotNull { findActiveById(it) }

    override fun findAllActiveByGroupId(groupId: Long): List<Activity> =
        records.values.filter { it.groupId == groupId && it.deletedAt == null }.sortedBy { it.scheduledAt }
}

internal class InMemoryActivityParticipantStore : ActivityParticipantStore {
    private val records = linkedMapOf<Long, ActivityParticipant>()
    private var nextId = 1L

    override fun save(participant: ActivityParticipant): ActivityParticipant {
        val persisted = if (participant.id == null) participant.copy(id = nextId++) else participant
        records[persisted.id!!] = persisted
        return persisted
    }

    override fun findById(id: Long): ActivityParticipant? = records[id]

    override fun findAllByActivityId(activityId: Long): List<ActivityParticipant> =
        records.values.filter { it.activityId == activityId }.sortedBy { it.createdAt }

    override fun findAllActiveByActivityIds(activityIds: Collection<Long>): List<ActivityParticipant> =
        records.values.filter { it.activityId in activityIds && it.removedAt == null }.sortedBy { it.createdAt }

    override fun findAllActiveByGroupMembershipIds(groupMembershipIds: Collection<Long>): List<ActivityParticipant> =
        records.values.filter { it.groupMembershipId in groupMembershipIds && it.removedAt == null }.sortedBy { it.createdAt }

    override fun findActiveByActivityIdAndGroupMembershipId(activityId: Long, groupMembershipId: Long): ActivityParticipant? =
        records.values.firstOrNull {
            it.activityId == activityId && it.groupMembershipId == groupMembershipId && it.removedAt == null
        }
}

internal class ActivityTestGroupStore : GroupStore {
    private val records = linkedMapOf<Long, Group>()
    private var nextId = 1L

    override fun save(group: Group): Group {
        val persisted = if (group.id == null) group.copy(id = nextId++) else group
        records[persisted.id!!] = persisted
        return persisted
    }

    override fun findActiveById(id: Long): Group? =
        records[id]?.takeIf { it.deletedAt == null }

    override fun findActiveByNormalizedName(normalizedName: String): Group? =
        records.values.firstOrNull { it.normalizedName == normalizedName && it.deletedAt == null }

    override fun findAllActive(): List<Group> =
        records.values.filter { it.deletedAt == null }.sortedBy { it.name }
}

internal class ActivityTestGroupMembershipStore : GroupMembershipStore {
    private val records = linkedMapOf<Long, GroupMembership>()
    private var nextId = 1L

    override fun save(membership: GroupMembership): GroupMembership {
        val persisted = if (membership.id == null) membership.copy(id = nextId++) else membership
        records[persisted.id!!] = persisted
        return persisted
    }

    override fun findById(id: Long): GroupMembership? = records[id]

    override fun findByGroupId(groupId: Long): List<GroupMembership> =
        records.values.filter { it.groupId == groupId }

    override fun findByGroupIdAndUserId(groupId: Long, userId: String): GroupMembership? =
        records.values.firstOrNull { it.groupId == groupId && it.userId == userId }

    override fun findByUserIdAndStatuses(userId: String, statuses: Set<GroupMembershipStatus>): List<GroupMembership> =
        records.values.filter { it.userId == userId && it.status in statuses }

    override fun findInvitedByEmailWithoutUser(normalizedEmail: String): List<GroupMembership> =
        records.values.filter {
            it.userId == null && it.normalizedInviteEmail == normalizedEmail && it.status == GroupMembershipStatus.INVITED
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
