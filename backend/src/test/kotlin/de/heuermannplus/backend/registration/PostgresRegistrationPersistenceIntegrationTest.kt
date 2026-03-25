package de.heuermannplus.backend.registration

import de.heuermannplus.backend.config.EntityManagerFactoryDependsOnFlywayPostProcessor
import de.heuermannplus.backend.config.FlywayConfig
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.SpringBootConfiguration
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers

@Testcontainers
@SpringBootTest(
    classes = [PostgresRegistrationPersistenceIntegrationTest.TestApplication::class],
    webEnvironment = SpringBootTest.WebEnvironment.NONE
)
class PostgresRegistrationPersistenceIntegrationTest {

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @Import(
        FlywayConfig::class,
        EntityManagerFactoryDependsOnFlywayPostProcessor::class,
        JpaAppUserStore::class,
        JpaTermsVersionStore::class,
        JpaTermsConsentStore::class
    )
    class TestApplication

    @Autowired
    lateinit var appUserStore: AppUserStore

    @Autowired
    lateinit var termsVersionStore: TermsVersionStore

    @Autowired
    lateinit var termsConsentStore: TermsConsentStore

    @Test
    fun `flyway initializes the active terms version on PostgreSQL`() {
        val currentTerms = termsVersionStore.findCurrent()

        assertNotNull(currentTerms)
        assertEquals("2026-03", currentTerms.version)
        assertEquals("drumdibum-agb-2026-03", currentTerms.contentSlug)
    }

    @Test
    fun `postgres integration persists app users and terms consents together`() {
        val currentTerms = termsVersionStore.findCurrent()
        assertNotNull(currentTerms)

        appUserStore.save(
            AppUser(
                keycloakUserId = "postgres-user",
                nickname = "postgres-drummer",
                email = "postgres@example.org",
                firstName = "Postgres",
                lastName = "Tester",
                status = AppUserStatus.PENDING,
                enabled = false,
                emailVerified = false,
                keycloakRole = "registration-pending",
                createdAt = Instant.parse("2026-03-24T12:00:00Z"),
                updatedAt = Instant.parse("2026-03-24T12:00:00Z")
            )
        )

        termsConsentStore.save(
            TermsConsent(
                keycloakUserId = "postgres-user",
                termsVersionId = currentTerms.id ?: error("Expected seeded terms version id"),
                consentType = TermsConsentType.EXPLICIT_YES,
                consentedAt = Instant.parse("2026-03-24T12:00:00Z")
            )
        )

        val appUser = appUserStore.findById("postgres-user")
        val consents = termsConsentStore.findByKeycloakUserId("postgres-user")

        assertNotNull(appUser)
        assertEquals("postgres-drummer", appUser.nickname)
        assertEquals(1, consents.size)
        assertEquals(TermsConsentType.EXPLICIT_YES, consents.single().consentType)
    }

    companion object {
        @Container
        @JvmStatic
        val postgres = PostgreSQLContainer("postgres:18.3")

        @JvmStatic
        @DynamicPropertySource
        fun overrideDatasourceProperties(registry: DynamicPropertyRegistry) {
            registry.add("spring.datasource.url", postgres::getJdbcUrl)
            registry.add("spring.datasource.username", postgres::getUsername)
            registry.add("spring.datasource.password", postgres::getPassword)
            registry.add("spring.flyway.enabled") { true }
            registry.add("spring.jpa.hibernate.ddl-auto") { "validate" }
        }
    }
}
