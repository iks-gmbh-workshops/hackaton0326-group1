package de.heuermannplus.backend

import de.heuermannplus.backend.api.PrivateUserProfileResponse
import de.heuermannplus.backend.config.AppBeansConfig
import de.heuermannplus.backend.registration.AppUserStatus
import java.time.ZoneOffset
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class AppBeansConfigTest {

    @Test
    fun `app beans config exposes utc clock and rest client builder`() {
        val config = AppBeansConfig()

        val clock = config.clock()
        val restClientBuilder = config.restClientBuilder()

        assertEquals(ZoneOffset.UTC, clock.zone)
        assertNotNull(restClientBuilder.build())
    }

    @Test
    fun `private user profile response exposes all constructor properties`() {
        val response = PrivateUserProfileResponse(
            subject = "user-123",
            nickname = "drummer",
            email = "drummer@example.org",
            firstName = "Max",
            lastName = "Mustermann",
            status = AppUserStatus.ACTIVE,
            enabled = true,
            emailVerified = true,
            keycloakRole = "app-user"
        )

        assertEquals("user-123", response.subject)
        assertEquals("drummer", response.nickname)
        assertEquals("drummer@example.org", response.email)
        assertEquals("Max", response.firstName)
        assertEquals("Mustermann", response.lastName)
        assertEquals(AppUserStatus.ACTIVE, response.status)
        assertEquals(true, response.enabled)
        assertEquals(true, response.emailVerified)
        assertEquals("app-user", response.keycloakRole)
    }
}
