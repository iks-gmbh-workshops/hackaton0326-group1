package de.heuermannplus.backend.registration

import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.SpringBootConfiguration
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.dao.DataIntegrityViolationException
import de.heuermannplus.backend.config.FlywayConfig
import de.heuermannplus.backend.config.EntityManagerFactoryDependsOnFlywayPostProcessor

@SpringBootTest(
    classes = [JpaAppUserStoreTest.TestApplication::class],
    webEnvironment = SpringBootTest.WebEnvironment.NONE,
    properties = [
        "spring.datasource.url=jdbc:h2:mem:app-user-store;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.datasource.hikari.connection-init-sql=CREATE DOMAIN IF NOT EXISTS TIMESTAMPTZ AS TIMESTAMP WITH TIME ZONE",
        "spring.jpa.hibernate.ddl-auto=validate",
        "spring.flyway.enabled=true"
    ]
)
class JpaAppUserStoreTest {

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @Import(FlywayConfig::class, EntityManagerFactoryDependsOnFlywayPostProcessor::class, JpaAppUserStore::class)
    class TestApplication

    @Autowired
    lateinit var store: AppUserStore

    @Autowired
    lateinit var repository: AppUserJpaRepository

    @Test
    fun `save and read app user by keycloak primary key`() {
        val saved = store.save(
            AppUser(
                keycloakUserId = "user-1",
                nickname = "drummer",
                email = "drummer@example.org",
                firstName = "Max",
                lastName = "Mustermann",
                status = AppUserStatus.PENDING,
                enabled = false,
                emailVerified = false,
                keycloakRole = "registration-pending"
            )
        )

        val loaded = store.findById("user-1")
        assertNotNull(loaded)
        assertEquals(saved.keycloakUserId, loaded.keycloakUserId)
        assertEquals("drummer", loaded.nickname)
        assertEquals("drummer@example.org", loaded.email)
        assertEquals("registration-pending", loaded.keycloakRole)
        assertNotNull(loaded.createdAt)
        assertNotNull(loaded.updatedAt)
    }

    @Test
    fun `save updates existing row while preserving created timestamp`() {
        val initial = store.save(
            AppUser(
                keycloakUserId = "user-2",
                nickname = "guitarist",
                email = "guitarist@example.org",
                firstName = null,
                lastName = null,
                status = AppUserStatus.PENDING,
                enabled = false,
                emailVerified = false,
                keycloakRole = "registration-pending",
                createdAt = Instant.parse("2026-03-24T12:00:00Z"),
                updatedAt = Instant.parse("2026-03-24T12:00:00Z")
            )
        )

        val updated = store.save(
            initial.copy(
                status = AppUserStatus.ACTIVE,
                enabled = true,
                emailVerified = true,
                keycloakRole = "app-user",
                updatedAt = Instant.parse("2026-03-24T13:00:00Z"),
                verifiedAt = Instant.parse("2026-03-24T13:00:00Z")
            )
        )

        assertEquals(initial.createdAt, updated.createdAt)
        assertEquals(AppUserStatus.ACTIVE, updated.status)
        assertTrue(updated.enabled)
        assertTrue(updated.emailVerified)
        assertEquals("app-user", updated.keycloakRole)
        assertEquals(Instant.parse("2026-03-24T13:00:00Z"), updated.updatedAt)
    }

    @Test
    fun `unique constraints reject duplicate nickname and email`() {
        store.save(
            AppUser(
                keycloakUserId = "user-3",
                nickname = "bassist",
                email = "bassist@example.org",
                firstName = null,
                lastName = null,
                status = AppUserStatus.PENDING,
                enabled = false,
                emailVerified = false,
                keycloakRole = "registration-pending"
            )
        )

        assertFailsWith<DataIntegrityViolationException> {
            store.save(
                AppUser(
                    keycloakUserId = "user-4",
                    nickname = "bassist",
                    email = "bassist-2@example.org",
                    firstName = null,
                    lastName = null,
                    status = AppUserStatus.PENDING,
                    enabled = false,
                    emailVerified = false,
                    keycloakRole = "registration-pending"
                )
            )
            repository.flush()
        }

        assertFailsWith<DataIntegrityViolationException> {
            store.save(
                AppUser(
                    keycloakUserId = "user-5",
                    nickname = "bassist-2",
                    email = "bassist@example.org",
                    firstName = null,
                    lastName = null,
                    status = AppUserStatus.PENDING,
                    enabled = false,
                    emailVerified = false,
                    keycloakRole = "registration-pending"
                )
            )
            repository.flush()
        }
    }
}
