package de.heuermannplus.backend

import de.heuermannplus.backend.api.PrivateController
import de.heuermannplus.backend.api.PrivateUserProfileResponse
import de.heuermannplus.backend.registration.ApiErrorResponse
import de.heuermannplus.backend.registration.AppUser
import de.heuermannplus.backend.registration.AppUserStatus
import de.heuermannplus.backend.registration.AppUserStore
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import org.springframework.http.HttpStatus
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken
import org.springframework.web.server.ResponseStatusException

class PrivateControllerTest {

    @Test
    fun `me reads subject from jwt and resolves remaining profile data from app user store`() {
        val controller = PrivateController(
            appUserStore = FakeAppUserStore(
                AppUser(
                    keycloakUserId = "user-123",
                    nickname = "drummer",
                    email = "drummer@example.org",
                    firstName = "Max",
                    lastName = "Mustermann",
                    status = AppUserStatus.ACTIVE,
                    enabled = true,
                    emailVerified = true,
                    keycloakRole = "app-user",
                    createdAt = Instant.parse("2026-03-24T12:00:00Z"),
                    updatedAt = Instant.parse("2026-03-24T12:05:00Z")
                )
            )
        )

        val response = controller.me(
            authentication = authenticationToken(
                subject = "user-123",
                email = "token@example.org",
                name = "Token Name",
                preferredUsername = "token-user"
            )
        )

        assertEquals(HttpStatus.OK, response.statusCode)
        assertEquals(
            PrivateUserProfileResponse(
                subject = "user-123",
                nickname = "drummer",
                email = "drummer@example.org",
                firstName = "Max",
                lastName = "Mustermann",
                status = AppUserStatus.ACTIVE,
                enabled = true,
                emailVerified = true,
                keycloakRole = "app-user"
            ),
            response.body
        )
    }

    @Test
    fun `me returns 404 when local user profile does not exist`() {
        val controller = PrivateController(
            appUserStore = FakeAppUserStore()
        )

        val response = controller.me(authenticationToken(subject = "missing-user"))

        assertEquals(HttpStatus.NOT_FOUND, response.statusCode)
        assertEquals(
            ApiErrorResponse(
                code = "USER_PROFILE_NOT_FOUND",
                message = "Lokales Benutzerprofil nicht gefunden"
            ),
            response.body
        )
    }

    @Test
    fun `me returns unauthorized when jwt subject is missing`() {
        val controller = PrivateController(
            appUserStore = FakeAppUserStore()
        )

        val exception = assertFailsWith<ResponseStatusException> {
            controller.me(authenticationToken(subject = null))
        }

        assertEquals(HttpStatus.UNAUTHORIZED, exception.statusCode)
        assertEquals("401 UNAUTHORIZED \"JWT enthaelt keinen Subject-Claim\"", exception.message)
    }

    private fun authenticationToken(
        subject: String?,
        email: String? = null,
        name: String? = null,
        preferredUsername: String? = null
    ): JwtAuthenticationToken {
        val claims = buildMap<String, Any> {
            email?.let { put("email", it) }
            name?.let { put("name", it) }
            preferredUsername?.let { put("preferred_username", it) }
        }

        return JwtAuthenticationToken(
            Jwt(
                "token",
                Instant.parse("2026-03-24T12:00:00Z"),
                Instant.parse("2026-03-24T13:00:00Z"),
                mapOf("alg" to "none"),
                buildMap {
                    put("iss", "http://localhost:8081/realms/heuermannplus")
                    subject?.let { value -> put("sub", value) }
                    putAll(claims)
                }
            )
        )
    }
}

private class FakeAppUserStore(
    private vararg val users: AppUser
) : AppUserStore {

    override fun save(user: AppUser): AppUser = user

    override fun findById(keycloakUserId: String): AppUser? =
        users.firstOrNull { it.keycloakUserId == keycloakUserId }

    override fun findByNickname(nickname: String): AppUser? =
        users.firstOrNull { it.nickname == nickname }

    override fun findByEmail(email: String): AppUser? =
        users.firstOrNull { it.email == email }
}
