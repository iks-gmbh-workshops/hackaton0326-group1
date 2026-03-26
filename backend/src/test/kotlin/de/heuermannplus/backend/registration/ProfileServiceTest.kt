package de.heuermannplus.backend.registration

import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import org.springframework.http.HttpStatus
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken

class ProfileServiceTest {

    @Test
    fun `profile returns current user data`() {
        val appUser = appUser(
            keycloakUserId = "kc-rob",
            nickname = "rob",
            email = "r.schlottmann@example.com"
        )
        val service = profileService(appUsers = listOf(appUser))
        val authentication = authentication(subject = "kc-rob")

        val profile = service.profile(authentication)

        assertEquals("rob", profile.username)
        assertEquals("r.schlottmann@example.com", profile.email)
    }

    @Test
    fun `profile falls back to preferred username when subject is missing`() {
        val appUser = appUser(
            keycloakUserId = "kc-rob",
            nickname = "rob",
            email = "r.schlottmann@example.com"
        )
        val service = profileService(appUsers = listOf(appUser))
        val authentication = authentication(preferredUsername = "rob")

        val profile = service.profile(authentication)

        assertEquals("rob", profile.username)
        assertEquals("r.schlottmann@example.com", profile.email)
    }

    @Test
    fun `profile rejects tokens without any usable identity claim`() {
        val service = profileService()

        val exception = assertFailsWith<RegistrationException> {
            service.profile(authentication())
        }

        assertEquals(HttpStatus.UNAUTHORIZED, exception.status)
        assertEquals("INVALID_TOKEN", exception.code)
    }

    @Test
    fun `updateProfile rejects when username is empty`() {
        val appUser = appUser(keycloakUserId = "kc-rob", nickname = "rob")
        val service = profileService(appUsers = listOf(appUser))
        val authentication = authentication(subject = "kc-rob")

        val exception = assertFailsWith<RegistrationException> {
            service.updateProfile(authentication, UpdateProfileRequest(username = ""))
        }

        assertEquals(HttpStatus.BAD_REQUEST, exception.status)
        assertEquals("FIELD_REQUIRED", exception.code)
        assertEquals("username", exception.field)
    }

    @Test
    fun `updateProfile updates nickname when not conflicting`() {
        val appUser = appUser(keycloakUserId = "kc-rob", nickname = "rob")
        val appStore = InMemoryAppUserStore(listOf(appUser))
        val keycloakClient = RecordingKeycloakAdminClient()
        val service = ProfileService(
            passwordPolicyEvaluator = PasswordPolicyEvaluator(registrationProperties()),
            keycloakAdminClient = keycloakClient,
            appUserStore = appStore,
            clock = Clock.fixed(Instant.parse("2026-03-25T11:30:00Z"), ZoneId.of("UTC"))
        )
        val authentication = authentication(subject = "kc-rob")

        val updated = service.updateProfile(authentication, UpdateProfileRequest(username = "newrob"))

        assertEquals("newrob", updated.username)
        assertEquals("newrob", keycloakClient.updatedUsername)
    }

    @Test
    fun `updateProfile rejects username conflict from local store`() {
        val currentUser = appUser(keycloakUserId = "kc-rob", nickname = "rob")
        val conflictingUser = appUser(keycloakUserId = "kc-alice", nickname = "alice")
        val service = profileService(appUsers = listOf(currentUser, conflictingUser))

        val exception = assertFailsWith<RegistrationException> {
            service.updateProfile(authentication(subject = "kc-rob"), UpdateProfileRequest(username = "alice"))
        }

        assertEquals(HttpStatus.CONFLICT, exception.status)
        assertEquals("USERNAME_ALREADY_EXISTS", exception.code)
        assertEquals("username", exception.field)
    }

    @Test
    fun `updateProfile rejects username conflict from keycloak`() {
        val currentUser = appUser(keycloakUserId = "kc-rob", nickname = "rob")
        val keycloakClient = RecordingKeycloakAdminClient(
            usernameLookup = mapOf(
                "alice" to KeycloakUserSummary(
                    id = "kc-alice",
                    username = "alice",
                    email = "alice@example.com",
                    enabled = true,
                    emailVerified = true
                )
            )
        )
        val service = ProfileService(
            passwordPolicyEvaluator = PasswordPolicyEvaluator(registrationProperties()),
            keycloakAdminClient = keycloakClient,
            appUserStore = InMemoryAppUserStore(listOf(currentUser)),
            clock = Clock.fixed(Instant.parse("2026-03-25T11:30:00Z"), ZoneId.of("UTC"))
        )

        val exception = assertFailsWith<RegistrationException> {
            service.updateProfile(authentication(subject = "kc-rob"), UpdateProfileRequest(username = "alice"))
        }

        assertEquals(HttpStatus.CONFLICT, exception.status)
        assertEquals("USERNAME_ALREADY_EXISTS", exception.code)
    }

    @Test
    fun `changePassword rejects when passwords don't match`() {
        val appUser = appUser(keycloakUserId = "kc-rob", nickname = "rob")
        val service = profileService(appUsers = listOf(appUser))
        val authentication = authentication(subject = "kc-rob")

        val exception = assertFailsWith<RegistrationException> {
            service.changePassword(
                authentication,
                ChangePasswordRequest(
                    newPassword = "NewSecurePass123!",
                    newPasswordRepeat = "DifferentPass123!"
                )
            )
        }

        assertEquals(HttpStatus.BAD_REQUEST, exception.status)
        assertEquals("PASSWORD_REPEAT_MISMATCH", exception.code)
    }

    @Test
    fun `changePassword updates keycloak when password matches policy`() {
        val appUser = appUser(keycloakUserId = "kc-rob", nickname = "rob")
        val keycloakClient = RecordingKeycloakAdminClient()
        val service = ProfileService(
            passwordPolicyEvaluator = PasswordPolicyEvaluator(registrationProperties()),
            keycloakAdminClient = keycloakClient,
            appUserStore = InMemoryAppUserStore(listOf(appUser)),
            clock = Clock.fixed(Instant.parse("2026-03-25T11:30:00Z"), ZoneId.of("UTC"))
        )

        val response = service.changePassword(
            authentication(subject = "kc-rob"),
            ChangePasswordRequest(
                newPassword = "NewSecurePass123!",
                newPasswordRepeat = "NewSecurePass123!"
            )
        )

        assertEquals("Passwort wurde geändert", response.message)
        assertEquals("kc-rob", keycloakClient.changedPasswordUserId)
        assertEquals("NewSecurePass123!", keycloakClient.changedPassword)
    }

    @Test
    fun `changePassword rejects passwords that fail policy`() {
        val appUser = appUser(keycloakUserId = "kc-rob", nickname = "rob")
        val service = profileService(appUsers = listOf(appUser))

        val exception = assertFailsWith<RegistrationException> {
            service.changePassword(
                authentication(subject = "kc-rob"),
                ChangePasswordRequest(
                    newPassword = "weak",
                    newPasswordRepeat = "weak"
                )
            )
        }

        assertEquals(HttpStatus.BAD_REQUEST, exception.status)
        assertEquals("PASSWORD_POLICY_FAILED", exception.code)
        assertEquals("newPassword", exception.field)
    }

    @Test
    fun `deleteAccount rejects when confirmation is wrong`() {
        val appUser = appUser(keycloakUserId = "kc-rob", nickname = "rob")
        val service = profileService(appUsers = listOf(appUser))
        val authentication = authentication(subject = "kc-rob")

        val exception = assertFailsWith<RegistrationException> {
            service.deleteAccount(authentication, DeleteAccountRequest(confirmation = "wrong"))
        }

        assertEquals(HttpStatus.BAD_REQUEST, exception.status)
        assertEquals("DELETE_CONFIRMATION_MISMATCH", exception.code)
    }

    @Test
    fun `deleteAccount removes local and keycloak user when confirmed`() {
        val appUser = appUser(keycloakUserId = "kc-rob", nickname = "rob")
        val appStore = InMemoryAppUserStore(listOf(appUser))
        val keycloakClient = RecordingKeycloakAdminClient()
        val service = ProfileService(
            passwordPolicyEvaluator = PasswordPolicyEvaluator(registrationProperties()),
            keycloakAdminClient = keycloakClient,
            appUserStore = appStore,
            clock = Clock.fixed(Instant.parse("2026-03-25T11:30:00Z"), ZoneId.of("UTC"))
        )

        val response = service.deleteAccount(authentication(subject = "kc-rob"), DeleteAccountRequest(confirmation = "rob"))

        assertEquals("Konto wurde gelöscht", response.message)
        assertEquals("kc-rob", keycloakClient.deletedUserId)
        assertNull(appStore.findById("kc-rob"))
    }

    @Test
    fun `profile falls back to email when subject lookup misses`() {
        val appUser = appUser(
            keycloakUserId = "kc-rob",
            nickname = "rob",
            email = "r.schlottmann@example.com"
        )
        val service = profileService(appUsers = listOf(appUser))
        val authentication = authentication(subject = "missing-id", email = "r.schlottmann@example.com")

        val profile = service.profile(authentication)

        assertEquals("rob", profile.username)
        assertEquals("r.schlottmann@example.com", profile.email)
    }

    @Test
    fun `profile returns not found when token identifies missing user`() {
        val service = profileService()

        val exception = assertFailsWith<RegistrationException> {
            service.profile(authentication(subject = "missing-id"))
        }

        assertEquals(HttpStatus.NOT_FOUND, exception.status)
        assertEquals("PROFILE_NOT_FOUND", exception.code)
    }

    private fun profileService(appUsers: List<AppUser> = emptyList()): ProfileService =
        ProfileService(
            passwordPolicyEvaluator = PasswordPolicyEvaluator(registrationProperties()),
            keycloakAdminClient = RecordingKeycloakAdminClient(),
            appUserStore = InMemoryAppUserStore(appUsers),
            clock = Clock.fixed(Instant.parse("2026-03-25T11:30:00Z"), ZoneId.of("UTC"))
        )

    private fun registrationProperties(): RegistrationProperties =
        RegistrationProperties(
            frontendBaseUrl = "http://localhost:3000",
            verificationTtl = java.time.Duration.ofHours(24),
            cleanupCron = "0 0 * * * *",
            emailFrom = "no-reply@example.com",
            password = PasswordPolicyProperties(
                minLength = 12,
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

    private fun appUser(
        keycloakUserId: String,
        nickname: String,
        email: String = "test@example.com"
    ): AppUser =
        AppUser(
            keycloakUserId = keycloakUserId,
            nickname = nickname,
            email = email,
            firstName = "Test",
            lastName = "User",
            status = AppUserStatus.ACTIVE,
            enabled = true,
            emailVerified = true,
            keycloakRole = "app-user"
        )

    private fun authentication(
        subject: String? = null,
        preferredUsername: String? = null,
        email: String? = null
    ): JwtAuthenticationToken {
        val builder = Jwt.withTokenValue("token")
            .header("alg", "none")
            .claim("iss", "http://localhost:8081/realms/heuermannplus")

        if (subject != null) {
            builder.claim("sub", subject)
        }
        if (preferredUsername != null) {
            builder.claim("preferred_username", preferredUsername)
        }
        if (email != null) {
            builder.claim("email", email)
        }

        return JwtAuthenticationToken(builder.build())
    }

    private class InMemoryAppUserStore(
        users: List<AppUser>
    ) : AppUserStore {
        private val usersById = users.associateBy(AppUser::keycloakUserId).toMutableMap()

        override fun save(user: AppUser): AppUser {
            usersById[user.keycloakUserId] = user
            return user
        }

        override fun findById(keycloakUserId: String): AppUser? =
            usersById[keycloakUserId]

        override fun findByNickname(nickname: String): AppUser? =
            usersById.values.firstOrNull { it.nickname == nickname }

        override fun findByEmail(email: String): AppUser? =
            usersById.values.firstOrNull { it.email == email }

        override fun deleteById(keycloakUserId: String) {
            usersById.remove(keycloakUserId)
        }

        override fun searchInviteSuggestions(query: String, excludedUserId: String, limit: Int): List<AppUser> =
            usersById.values
                .filterNot { it.keycloakUserId == excludedUserId }
                .filter {
                    query.isBlank() ||
                        it.nickname.contains(query, ignoreCase = true) ||
                        it.email.contains(query, ignoreCase = true)
                }
                .take(limit)
    }

    private class RecordingKeycloakAdminClient(
        private val usernameLookup: Map<String, KeycloakUserSummary> = emptyMap()
    ) : KeycloakAdminClient {
        var updatedUsername: String? = null
            private set
        var changedPasswordUserId: String? = null
            private set
        var changedPassword: String? = null
            private set
        var deletedUserId: String? = null
            private set

        override fun findUserByUsername(username: String): KeycloakUserSummary? =
            usernameLookup[username]

        override fun findUserByEmail(email: String): KeycloakUserSummary? = null

        override fun findUserById(userId: String): KeycloakUserRepresentation? = null

        override fun createPendingUser(draft: RegistrationUserDraft): String = "kc-user"

        override fun updateUser(
            userId: String,
            username: String,
            email: String,
            firstName: String?,
            lastName: String?,
            enabled: Boolean,
            emailVerified: Boolean
        ) {
            updatedUsername = username
        }

        override fun changePassword(userId: String, newPassword: String) {
            changedPasswordUserId = userId
            changedPassword = newPassword
        }

        override fun validateUserCredentials(username: String, password: String): Boolean = true

        override fun assignRealmRoles(userId: String, roleNames: Set<String>) = Unit

        override fun removeRealmRoles(userId: String, roleNames: Set<String>) = Unit

        override fun enableUser(userId: String) = Unit

        override fun deleteUser(userId: String) {
            deletedUserId = userId
        }
    }
}
