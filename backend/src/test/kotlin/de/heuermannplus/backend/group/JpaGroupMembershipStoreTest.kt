package de.heuermannplus.backend.group

import de.heuermannplus.backend.config.EntityManagerFactoryDependsOnFlywayPostProcessor
import de.heuermannplus.backend.config.FlywayConfig
import java.time.Instant
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.SpringBootConfiguration
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.dao.DataIntegrityViolationException

@SpringBootTest(
    classes = [JpaGroupMembershipStoreTest.TestApplication::class],
    webEnvironment = SpringBootTest.WebEnvironment.NONE,
    properties = [
        "spring.datasource.url=jdbc:h2:mem:group-membership-store;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.datasource.hikari.connection-init-sql=CREATE DOMAIN IF NOT EXISTS TIMESTAMPTZ AS TIMESTAMP WITH TIME ZONE",
        "spring.jpa.hibernate.ddl-auto=validate",
        "spring.flyway.enabled=true"
    ]
)
class JpaGroupMembershipStoreTest {

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @Import(
        FlywayConfig::class,
        EntityManagerFactoryDependsOnFlywayPostProcessor::class,
        JpaGroupStore::class,
        JpaGroupMembershipStore::class
    )
    class TestApplication

    @Autowired
    lateinit var groupStore: GroupStore

    @Autowired
    lateinit var membershipStore: GroupMembershipStore

    @Autowired
    lateinit var membershipRepository: GroupMembershipJpaRepository

    @Test
    fun `updating invited membership to removed succeeds when removed history exists`() {
        val groupId = createGroup()
        membershipStore.save(
            GroupMembership(
                groupId = groupId,
                userId = "user-1",
                inviteEmail = "user@example.org",
                normalizedInviteEmail = "user@example.org",
                displayName = "user",
                status = GroupMembershipStatus.REMOVED,
                isAdmin = false,
                createdAt = Instant.parse("2026-03-25T08:00:00Z"),
                updatedAt = Instant.parse("2026-03-25T08:00:00Z"),
                removedAt = Instant.parse("2026-03-25T08:00:00Z")
            )
        )
        val invited = membershipStore.save(
            GroupMembership(
                groupId = groupId,
                userId = "user-1",
                inviteEmail = "user@example.org",
                normalizedInviteEmail = "user@example.org",
                displayName = "user",
                status = GroupMembershipStatus.INVITED,
                isAdmin = false,
                createdAt = Instant.parse("2026-03-26T08:00:00Z"),
                updatedAt = Instant.parse("2026-03-26T08:00:00Z")
            )
        )
        membershipRepository.flush()

        val updated = membershipStore.save(
            invited.copy(
                status = GroupMembershipStatus.REMOVED,
                updatedAt = Instant.parse("2026-03-26T09:00:00Z"),
                removedAt = Instant.parse("2026-03-26T09:00:00Z")
            )
        )
        membershipRepository.flush()

        assertNotNull(updated.id)
    }

    @Test
    fun `open memberships stay unique for the same user`() {
        val groupId = createGroup()
        membershipStore.save(
            GroupMembership(
                groupId = groupId,
                userId = "user-2",
                inviteEmail = "user2@example.org",
                normalizedInviteEmail = "user2@example.org",
                displayName = "user2",
                status = GroupMembershipStatus.ACTIVE,
                isAdmin = false,
                createdAt = Instant.parse("2026-03-26T08:00:00Z"),
                updatedAt = Instant.parse("2026-03-26T08:00:00Z"),
                joinedAt = Instant.parse("2026-03-26T08:00:00Z")
            )
        )
        membershipRepository.flush()

        assertFailsWith<DataIntegrityViolationException> {
            membershipStore.save(
                GroupMembership(
                    groupId = groupId,
                    userId = "user-2",
                    inviteEmail = "user2@example.org",
                    normalizedInviteEmail = "user2@example.org",
                    displayName = "user2",
                    status = GroupMembershipStatus.INVITED,
                    isAdmin = false,
                    createdAt = Instant.parse("2026-03-26T09:00:00Z"),
                    updatedAt = Instant.parse("2026-03-26T09:00:00Z")
                )
            )
            membershipRepository.flush()
        }
    }

    @Test
    fun `open email invites stay unique for the same group`() {
        val groupId = createGroup()
        membershipStore.save(
            GroupMembership(
                groupId = groupId,
                userId = null,
                inviteEmail = "guest@example.org",
                normalizedInviteEmail = "guest@example.org",
                displayName = "guest@example.org",
                status = GroupMembershipStatus.INVITED,
                isAdmin = false,
                createdAt = Instant.parse("2026-03-26T08:00:00Z"),
                updatedAt = Instant.parse("2026-03-26T08:00:00Z")
            )
        )
        membershipRepository.flush()

        assertFailsWith<DataIntegrityViolationException> {
            membershipStore.save(
                GroupMembership(
                    groupId = groupId,
                    userId = null,
                    inviteEmail = "guest@example.org",
                    normalizedInviteEmail = "guest@example.org",
                    displayName = "guest@example.org",
                    status = GroupMembershipStatus.INVITED,
                    isAdmin = false,
                    createdAt = Instant.parse("2026-03-26T09:00:00Z"),
                    updatedAt = Instant.parse("2026-03-26T09:00:00Z")
                )
            )
            membershipRepository.flush()
        }
    }

    private fun createGroup(): Long =
        createGroup("band-${UUID.randomUUID()}")

    private fun createGroup(normalizedName: String): Long =
        groupStore.save(
            Group(
                name = normalizedName.replaceFirstChar { it.uppercase() },
                normalizedName = normalizedName,
                description = null,
                createdByUserId = "owner-1",
                createdAt = Instant.parse("2026-03-25T07:00:00Z"),
                updatedAt = Instant.parse("2026-03-25T07:00:00Z")
            )
        ).id!!
}
