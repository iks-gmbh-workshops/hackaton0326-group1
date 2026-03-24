package de.heuermannplus.backend

import de.heuermannplus.backend.registration.CaptchaMode
import de.heuermannplus.backend.registration.CaptchaProperties
import de.heuermannplus.backend.registration.CaptchaVerifier
import de.heuermannplus.backend.registration.KeycloakAdminClient
import de.heuermannplus.backend.registration.KeycloakUserSummary
import de.heuermannplus.backend.registration.PasswordPolicyEvaluator
import de.heuermannplus.backend.registration.PasswordPolicyProperties
import de.heuermannplus.backend.registration.RegistrationAcceptedResponse
import de.heuermannplus.backend.registration.RegistrationException
import de.heuermannplus.backend.registration.RegistrationMailService
import de.heuermannplus.backend.registration.RegistrationProperties
import de.heuermannplus.backend.registration.RegistrationRequest
import de.heuermannplus.backend.registration.RegistrationService
import de.heuermannplus.backend.registration.RegistrationUserDraft
import de.heuermannplus.backend.registration.RegistrationVerification
import de.heuermannplus.backend.registration.RegistrationVerificationStatus
import de.heuermannplus.backend.registration.RegistrationVerificationStore
import de.heuermannplus.backend.registration.RegistrationVerifyRequest
import de.heuermannplus.backend.registration.VerificationTokenService
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class RegistrationServiceTest {

    @Test
    fun `register and verify activates user and assigns app role`() {
        val fixture = registrationFixture()

        val response = fixture.service.register(
            RegistrationRequest(
                nickname = "drummer",
                password = "Drum123!",
                passwordRepeat = "Drum123!",
                email = "drummer@example.org",
                captchaToken = "test-pass",
                firstName = "Max",
                lastName = "Mustermann"
            )
        )

        assertEquals("Die Verifizierungs-E-Mail wurde versendet", response.message)

        val pendingUser = fixture.keycloakClient.findUserByUsername("drummer")
        assertNotNull(pendingUser)
        assertFalse(pendingUser.enabled)
        assertTrue("registration-pending" in fixture.keycloakClient.rolesFor(pendingUser.id))

        val token = fixture.mailService.lastToken()
        val verifyResponse = fixture.service.verify(RegistrationVerifyRequest(token = token))

        assertEquals("Die Registrierung wurde erfolgreich bestaetigt", verifyResponse.message)

        val activatedUser = fixture.keycloakClient.findUserByUsername("drummer")
        assertNotNull(activatedUser)
        assertTrue(activatedUser.enabled)
        assertTrue(activatedUser.emailVerified)
        assertTrue("app-user" in fixture.keycloakClient.rolesFor(activatedUser.id))
        assertFalse("registration-pending" in fixture.keycloakClient.rolesFor(activatedUser.id))
    }

    @Test
    fun `register rejects missing nickname`() {
        val fixture = registrationFixture()

        val exception = assertFailsWith<RegistrationException> {
            fixture.service.register(
                RegistrationRequest(
                    nickname = " ",
                    password = "Drum123!",
                    passwordRepeat = "Drum123!",
                    email = "drummer@example.org",
                    captchaToken = "test-pass"
                )
            )
        }

        assertEquals("Bitte Nickname eingeben", exception.message)
    }

    @Test
    fun `register rejects invalid email`() {
        val fixture = registrationFixture()

        val exception = assertFailsWith<RegistrationException> {
            fixture.service.register(
                RegistrationRequest(
                    nickname = "drummer",
                    password = "Drum123!",
                    passwordRepeat = "Drum123!",
                    email = "sfsd",
                    captchaToken = "test-pass"
                )
            )
        }

        assertEquals("Ungueltiges Format der Email Adresse", exception.message)
    }

    @Test
    fun `register suggests next free nickname on conflict`() {
        val fixture = registrationFixture().also {
            it.keycloakClient.createPendingUser(
                RegistrationUserDraft(
                    username = "drummer",
                    email = "existing@example.org",
                    firstName = null,
                    lastName = null,
                    password = "Drum123!"
                )
            )
            it.keycloakClient.createPendingUser(
                RegistrationUserDraft(
                    username = "drummer1",
                    email = "existing2@example.org",
                    firstName = null,
                    lastName = null,
                    password = "Drum123!"
                )
            )
        }

        val exception = assertFailsWith<RegistrationException> {
            fixture.service.register(
                RegistrationRequest(
                    nickname = "drummer",
                    password = "Drum123!",
                    passwordRepeat = "Drum123!",
                    email = "new@example.org",
                    captchaToken = "test-pass"
                )
            )
        }

        assertEquals("Nickname existiert bereits", exception.message)
        assertEquals("drummer2", exception.suggestedNickname)
    }

    @Test
    fun `register rejects password mismatch`() {
        val fixture = registrationFixture()

        val exception = assertFailsWith<RegistrationException> {
            fixture.service.register(
                RegistrationRequest(
                    nickname = "drummer",
                    password = "Drum123!",
                    passwordRepeat = "Drum124!",
                    email = "drummer@example.org",
                    captchaToken = "test-pass"
                )
            )
        }

        assertEquals("Passwort-Wiederholung falsch", exception.message)
    }

    @Test
    fun `register rejects weak password`() {
        val fixture = registrationFixture()

        val exception = assertFailsWith<RegistrationException> {
            fixture.service.register(
                RegistrationRequest(
                    nickname = "drummer",
                    password = "weak",
                    passwordRepeat = "weak",
                    email = "drummer@example.org",
                    captchaToken = "test-pass"
                )
            )
        }

        assertEquals("Passwort entspricht nicht den Mindestanforderungen", exception.message)
    }

    @Test
    fun `register rejects invalid captcha`() {
        val fixture = registrationFixture()

        val exception = assertFailsWith<RegistrationException> {
            fixture.service.register(
                RegistrationRequest(
                    nickname = "drummer",
                    password = "Drum123!",
                    passwordRepeat = "Drum123!",
                    email = "drummer@example.org",
                    captchaToken = "wrong"
                )
            )
        }

        assertEquals("Das Captcha wurde falsch eingegeben", exception.message)
    }

    @Test
    fun `verify rejects unknown token`() {
        val fixture = registrationFixture()

        val exception = assertFailsWith<RegistrationException> {
            fixture.service.verify(RegistrationVerifyRequest(token = "unknown"))
        }

        assertEquals("Der Verifizierungslink ist ungueltig", exception.message)
    }

    @Test
    fun `verify rejects expired token`() {
        val fixture = registrationFixture()

        fixture.service.register(
            RegistrationRequest(
                nickname = "drummer",
                password = "Drum123!",
                passwordRepeat = "Drum123!",
                email = "drummer@example.org",
                captchaToken = "test-pass"
            )
        )

        fixture.clock.currentInstant = fixture.clock.currentInstant.plusSeconds(25 * 60 * 60)

        val exception = assertFailsWith<RegistrationException> {
            fixture.service.verify(RegistrationVerifyRequest(token = fixture.mailService.lastToken()))
        }

        assertEquals("Der Verifizierungslink ist abgelaufen", exception.message)
    }

    private fun registrationFixture(): RegistrationFixture {
        val properties = RegistrationProperties(
            frontendBaseUrl = "http://localhost:3000",
            verificationTtl = java.time.Duration.ofHours(24),
            cleanupCron = "0 0 * * * *",
            emailFrom = "no-reply@heuermannplus.local",
            password = PasswordPolicyProperties(),
            captcha = CaptchaProperties(
                mode = CaptchaMode.MOCK,
                mockPassToken = "test-pass"
            )
        )
        val clock = MutableClock(Instant.parse("2026-03-24T12:00:00Z"))
        val keycloakClient = FakeKeycloakAdminClient()
        val verificationStore = InMemoryRegistrationVerificationStore(clock)
        val mailService = FakeRegistrationMailService()

        val service = RegistrationService(
            registrationProperties = properties,
            passwordPolicyEvaluator = PasswordPolicyEvaluator(properties),
            captchaVerifier = CaptchaVerifier { token -> token == "test-pass" },
            keycloakAdminClient = keycloakClient,
            verificationStore = verificationStore,
            verificationTokenService = VerificationTokenService(),
            registrationMailService = mailService,
            clock = clock
        )

        return RegistrationFixture(
            service = service,
            keycloakClient = keycloakClient,
            mailService = mailService,
            clock = clock
        )
    }
}

private data class RegistrationFixture(
    val service: RegistrationService,
    val keycloakClient: FakeKeycloakAdminClient,
    val mailService: FakeRegistrationMailService,
    val clock: MutableClock
)

private class MutableClock(
    var currentInstant: Instant
) : Clock() {
    override fun getZone(): ZoneId = ZoneId.of("UTC")

    override fun withZone(zone: ZoneId?): Clock = this

    override fun instant(): Instant = currentInstant
}

private class FakeRegistrationMailService : RegistrationMailService {
    private val sentMessages = mutableListOf<Triple<String, String, String>>()

    override fun sendVerificationEmail(email: String, nickname: String, token: String) {
        sentMessages += Triple(email, nickname, token)
    }

    fun lastToken(): String = sentMessages.last().third
}

private class InMemoryRegistrationVerificationStore(
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

private class FakeKeycloakAdminClient : KeycloakAdminClient {
    private val users = linkedMapOf<String, FakeKeycloakUser>()
    private var sequence = 1

    override fun findUserByUsername(username: String): KeycloakUserSummary? =
        users.values.firstOrNull { it.username == username }?.toSummary()

    override fun findUserByEmail(email: String): KeycloakUserSummary? =
        users.values.firstOrNull { it.email == email }?.toSummary()

    override fun createPendingUser(draft: RegistrationUserDraft): String {
        val id = "user-${sequence++}"
        users[id] = FakeKeycloakUser(
            id = id,
            username = draft.username,
            email = draft.email,
            enabled = false,
            emailVerified = false,
            roles = linkedSetOf()
        )
        return id
    }

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

private data class FakeKeycloakUser(
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
}
