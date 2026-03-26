package de.heuermannplus.backend

import de.heuermannplus.backend.api.RegistrationController
import de.heuermannplus.backend.registration.AppUser
import de.heuermannplus.backend.registration.AppUserStatus
import de.heuermannplus.backend.registration.AppUserStore
import de.heuermannplus.backend.registration.CaptchaMode
import de.heuermannplus.backend.registration.CaptchaProperties
import de.heuermannplus.backend.registration.CaptchaVerifier
import de.heuermannplus.backend.registration.KeycloakAdminClient
import de.heuermannplus.backend.registration.KeycloakUserRepresentation
import de.heuermannplus.backend.registration.KeycloakUserSummary
import de.heuermannplus.backend.registration.PasswordPolicyEvaluator
import de.heuermannplus.backend.registration.PasswordPolicyProperties
import de.heuermannplus.backend.registration.RegistrationMailService
import de.heuermannplus.backend.registration.RegistrationProperties
import de.heuermannplus.backend.registration.RegistrationRequest
import de.heuermannplus.backend.registration.RegistrationService
import de.heuermannplus.backend.registration.RegistrationUserDraft
import de.heuermannplus.backend.registration.RegistrationVerification
import de.heuermannplus.backend.registration.RegistrationVerificationStatus
import de.heuermannplus.backend.registration.RegistrationVerificationStore
import de.heuermannplus.backend.registration.RegistrationVerifyRequest
import de.heuermannplus.backend.registration.TermsConsent
import de.heuermannplus.backend.registration.TermsConsentStore
import de.heuermannplus.backend.registration.TermsConsentType
import de.heuermannplus.backend.registration.TermsVersion
import de.heuermannplus.backend.registration.TermsVersionStore
import de.heuermannplus.backend.registration.VerificationTokenService
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class RegistrationControllerTest {

    @Test
    fun `policy returns current password captcha and terms requirements`() {
        val fixture = registrationControllerFixture()
        val controller = RegistrationController(fixture.service)

        val response = controller.policy()

        assertEquals(3, response.nickname.minLength)
        assertEquals("mock", response.captcha.mode)
        assertEquals("test-pass", response.captcha.mockPassToken)
        assertEquals("/terms/drumdibum-agb-2026-03", response.terms.url)
    }

    @Test
    fun `register and verify delegate through controller to service`() {
        val fixture = registrationControllerFixture()
        val controller = RegistrationController(fixture.service)

        val accepted = controller.register(
            RegistrationRequest(
                nickname = "drummer",
                password = "Drum123!",
                passwordRepeat = "Drum123!",
                email = "drummer@example.org",
                captchaToken = "test-pass",
                firstName = "Max",
                lastName = "Mustermann",
                acceptTerms = true
            )
        )

        assertEquals("Die Verifizierungs-E-Mail wurde versendet", accepted.message)
        val pendingUser = fixture.keycloakAdminClient.findUserByUsername("drummer")
        assertNotNull(pendingUser)
        assertTrue("registration-pending" in fixture.keycloakAdminClient.rolesFor(pendingUser.id))

        val verified = controller.verify(
            RegistrationVerifyRequest(token = fixture.mailService.lastToken())
        )

        assertEquals("Die Registrierung wurde erfolgreich bestätigt", verified.message)
        val activeUser = fixture.appUserStore.findById(pendingUser.id)
        assertNotNull(activeUser)
        assertEquals(AppUserStatus.ACTIVE, activeUser.status)
        assertTrue(activeUser.enabled)
        assertTrue(activeUser.emailVerified)
    }
}

private data class RegistrationControllerFixture(
    val service: RegistrationService,
    val keycloakAdminClient: RegistrationControllerFakeKeycloakAdminClient,
    val appUserStore: RegistrationControllerInMemoryAppUserStore,
    val mailService: RegistrationControllerFakeMailService
)

private fun registrationControllerFixture(): RegistrationControllerFixture {
    val clock = RegistrationControllerMutableClock(Instant.parse("2026-03-24T12:00:00Z"))
    val keycloakAdminClient = RegistrationControllerFakeKeycloakAdminClient()
    val appUserStore = RegistrationControllerInMemoryAppUserStore(clock)
    val mailService = RegistrationControllerFakeMailService()
    val service = RegistrationService(
        registrationProperties = RegistrationProperties(
            frontendBaseUrl = "http://localhost:3000",
            verificationTtl = java.time.Duration.ofHours(24),
            cleanupCron = "0 0 * * * *",
            emailFrom = "no-reply@example.com",
            password = PasswordPolicyProperties(
                minLength = 8,
                minUpperCase = 1,
                minLowerCase = 1,
                minDigits = 1,
                minSpecialChars = 1
            ),
            captcha = CaptchaProperties(
                mode = CaptchaMode.MOCK,
                mockPassToken = "test-pass"
            )
        ),
        passwordPolicyEvaluator = PasswordPolicyEvaluator(
            RegistrationProperties(
                frontendBaseUrl = "http://localhost:3000",
                verificationTtl = java.time.Duration.ofHours(24),
                cleanupCron = "0 0 * * * *",
                emailFrom = "no-reply@example.com",
                password = PasswordPolicyProperties(
                    minLength = 8,
                    minUpperCase = 1,
                    minLowerCase = 1,
                    minDigits = 1,
                    minSpecialChars = 1
                ),
                captcha = CaptchaProperties(
                    mode = CaptchaMode.MOCK,
                    mockPassToken = "test-pass"
                )
            )
        ),
        captchaVerifier = CaptchaVerifier { token -> token == "test-pass" },
        keycloakAdminClient = keycloakAdminClient,
        appUserStore = appUserStore,
        termsVersionStore = RegistrationControllerTermsVersionStore(
            listOf(
                TermsVersion(id = 1, version = "2026-03", contentSlug = "drumdibum-agb-2026-03", isActive = true)
            )
        ),
        termsConsentStore = RegistrationControllerTermsConsentStore(),
        verificationStore = RegistrationControllerVerificationStore(clock),
        verificationTokenService = VerificationTokenService(),
        registrationMailService = mailService,
        clock = clock
    )

    return RegistrationControllerFixture(
        service = service,
        keycloakAdminClient = keycloakAdminClient,
        appUserStore = appUserStore,
        mailService = mailService
    )
}

private class RegistrationControllerMutableClock(
    var currentInstant: Instant
) : Clock() {
    override fun getZone(): ZoneId = ZoneId.of("UTC")

    override fun withZone(zone: ZoneId?): Clock = this

    override fun instant(): Instant = currentInstant
}

private class RegistrationControllerFakeMailService : RegistrationMailService {
    private val sentTokens = mutableListOf<String>()

    override fun sendVerificationEmail(email: String, nickname: String, token: String) {
        sentTokens += token
    }

    fun lastToken(): String = sentTokens.last()
}

private class RegistrationControllerVerificationStore(
    private val clock: Clock
) : RegistrationVerificationStore {
    private val records = linkedMapOf<String, RegistrationVerification>()
    private var sequence = 1L

    override fun save(verification: RegistrationVerification): RegistrationVerification {
        val persisted = verification.copy(
            id = verification.id ?: sequence++,
            createdAt = verification.createdAt ?: Instant.now(clock)
        )
        records[persisted.tokenHash] = persisted
        return persisted
    }

    override fun findByTokenHash(tokenHash: String): RegistrationVerification? = records[tokenHash]

    override fun findExpiredPending(now: Instant): List<RegistrationVerification> =
        records.values.filter { it.status == RegistrationVerificationStatus.PENDING && it.expiresAt.isBefore(now) }
}

private class RegistrationControllerTermsVersionStore(
    private val activeVersions: List<TermsVersion>
) : TermsVersionStore {
    override fun findCurrent(): TermsVersion? = activeVersions.singleOrNull()

    override fun findActive(): List<TermsVersion> = activeVersions.toList()
}

private class RegistrationControllerTermsConsentStore : TermsConsentStore {
    private val records = mutableListOf<TermsConsent>()
    private var sequence = 1L

    override fun save(consent: TermsConsent): TermsConsent {
        val persisted = consent.copy(id = consent.id ?: sequence++)
        records += persisted
        return persisted
    }

    override fun findByKeycloakUserId(keycloakUserId: String): List<TermsConsent> =
        records.filter { it.keycloakUserId == keycloakUserId }

    override fun deleteByKeycloakUserId(keycloakUserId: String) {
        records.removeAll { it.keycloakUserId == keycloakUserId }
    }
}

private class RegistrationControllerInMemoryAppUserStore(
    private val clock: Clock
) : AppUserStore {
    private val records = linkedMapOf<String, AppUser>()

    override fun save(user: AppUser): AppUser {
        val existing = records[user.keycloakUserId]
        val persisted = user.copy(
            createdAt = user.createdAt ?: existing?.createdAt ?: Instant.now(clock),
            updatedAt = user.updatedAt ?: Instant.now(clock)
        )
        records[persisted.keycloakUserId] = persisted
        return persisted
    }

    override fun findById(keycloakUserId: String): AppUser? = records[keycloakUserId]

    override fun findByNickname(nickname: String): AppUser? =
        records.values.firstOrNull { it.nickname == nickname }

    override fun findByEmail(email: String): AppUser? =
        records.values.firstOrNull { it.email == email }

    override fun deleteById(keycloakUserId: String) {
        records.remove(keycloakUserId)
    }

    override fun searchInviteSuggestions(query: String, excludedUserId: String, limit: Int): List<AppUser> = emptyList()
}

private class RegistrationControllerFakeKeycloakAdminClient : KeycloakAdminClient {
    private val users = linkedMapOf<String, RegistrationControllerFakeKeycloakUser>()
    private var sequence = 1

    override fun findUserByUsername(username: String): KeycloakUserSummary? =
        users.values.firstOrNull { it.username == username }?.toSummary()

    override fun findUserByEmail(email: String): KeycloakUserSummary? =
        users.values.firstOrNull { it.email == email }?.toSummary()

    override fun findUserById(userId: String): KeycloakUserRepresentation? =
        users[userId]?.toRepresentation()

    override fun createPendingUser(draft: RegistrationUserDraft): String {
        val id = "user-${sequence++}"
        users[id] = RegistrationControllerFakeKeycloakUser(
            id = id,
            username = draft.username,
            email = draft.email,
            enabled = false,
            emailVerified = false,
            roles = linkedSetOf()
        )
        return id
    }

    override fun updateUser(
        userId: String,
        username: String,
        email: String,
        firstName: String?,
        lastName: String?,
        enabled: Boolean,
        emailVerified: Boolean
    ) {
    }

    override fun changePassword(userId: String, newPassword: String) {
    }

    override fun validateUserCredentials(username: String, password: String): Boolean = true

    override fun assignRealmRoles(userId: String, roleNames: Set<String>) {
        users.getValue(userId).roles += roleNames
    }

    override fun removeRealmRoles(userId: String, roleNames: Set<String>) {
        users.getValue(userId).roles -= roleNames
    }

    override fun enableUser(userId: String) {
        val current = users.getValue(userId)
        users[userId] = current.copy(enabled = true, emailVerified = true, roles = current.roles)
    }

    override fun deleteUser(userId: String) {
        users.remove(userId)
    }

    fun rolesFor(userId: String): Set<String> = users.getValue(userId).roles.toSet()
}

private data class RegistrationControllerFakeKeycloakUser(
    val id: String,
    val username: String,
    val email: String,
    val enabled: Boolean,
    val emailVerified: Boolean,
    val roles: LinkedHashSet<String>
) {
    fun toSummary(): KeycloakUserSummary =
        KeycloakUserSummary(
            id = id,
            username = username,
            email = email,
            enabled = enabled,
            emailVerified = emailVerified
        )

    fun toRepresentation(): KeycloakUserRepresentation =
        KeycloakUserRepresentation(
            id = id,
            username = username,
            email = email,
            enabled = enabled,
            emailVerified = emailVerified
        )
}
