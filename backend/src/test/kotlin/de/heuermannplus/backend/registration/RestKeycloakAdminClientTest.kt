package de.heuermannplus.backend.registration

import java.net.URI
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue
import org.hamcrest.Matchers.containsString
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.test.web.client.MockRestServiceServer
import org.springframework.test.web.client.ExpectedCount.once
import org.springframework.test.web.client.match.MockRestRequestMatchers.content
import org.springframework.test.web.client.match.MockRestRequestMatchers.header
import org.springframework.test.web.client.match.MockRestRequestMatchers.method
import org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo
import org.springframework.test.web.client.response.MockRestResponseCreators.withNoContent
import org.springframework.test.web.client.response.MockRestResponseCreators.withStatus
import org.springframework.web.client.RestClient

class RestKeycloakAdminClientTest {

    @Test
    fun `findUserByUsername returns first matching summary`() {
        val fixture = testFixture()
        fixture.expectAdminToken()
        fixture.server.expect(requestTo("${fixture.adminBaseUrl}/users?username=drummer&exact=true"))
            .andExpect(method(HttpMethod.GET))
            .andExpect(header("Authorization", "Bearer token"))
            .andRespond(
                withStatus(HttpStatus.OK)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(
                        """
                        [
                          {"id":"user-1","username":"drummer","email":"drummer@example.org","enabled":true,"emailVerified":false},
                          {"id":"user-2","username":"ignored","email":"ignored@example.org","enabled":false,"emailVerified":false}
                        ]
                        """.trimIndent()
                    )
            )

        val user = fixture.client.findUserByUsername("drummer")

        assertEquals("user-1", user?.id)
        assertEquals("drummer", user?.username)
        assertEquals("drummer@example.org", user?.email)
        assertTrue(user?.enabled == true)
        assertFalse(user?.emailVerified == true)
        fixture.server.verify()
    }

    @Test
    fun `findUserByUsername returns null for incomplete representation`() {
        val fixture = testFixture()
        fixture.expectAdminToken()
        fixture.server.expect(requestTo("${fixture.adminBaseUrl}/users?username=drummer&exact=true"))
            .andExpect(method(HttpMethod.GET))
            .andRespond(
                withStatus(HttpStatus.OK)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body("""[{"email":"drummer@example.org","enabled":true,"emailVerified":true}]""")
            )

        val user = fixture.client.findUserByUsername("drummer")

        assertNull(user)
        fixture.server.verify()
    }

    @Test
    fun `findUserByEmail returns first matching summary`() {
        val fixture = testFixture()
        fixture.expectAdminToken()
        fixture.server.expect(requestTo("${fixture.adminBaseUrl}/users?email=drummer%40example.org&exact=true"))
            .andExpect(method(HttpMethod.GET))
            .andExpect(header("Authorization", "Bearer token"))
            .andRespond(
                withStatus(HttpStatus.OK)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body("""[{"id":"user-1","username":"drummer","email":"drummer@example.org","enabled":true,"emailVerified":true}]""")
            )

        val user = fixture.client.findUserByEmail("drummer@example.org")

        assertEquals("user-1", user?.id)
        assertEquals("drummer", user?.username)
        assertTrue(user?.emailVerified == true)
        fixture.server.verify()
    }

    @Test
    fun `findUserById returns representation when present`() {
        val fixture = testFixture()
        fixture.expectAdminToken()
        fixture.server.expect(requestTo("${fixture.adminBaseUrl}/users/user-1"))
            .andExpect(method(HttpMethod.GET))
            .andExpect(header("Authorization", "Bearer token"))
            .andRespond(
                withStatus(HttpStatus.OK)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body("""{"id":"user-1","username":"drummer","email":"drummer@example.org","enabled":false,"emailVerified":false,"firstName":"Nick","lastName":"Mason"}""")
            )

        val user = fixture.client.findUserById("user-1")

        assertEquals("user-1", user?.id)
        assertEquals("drummer", user?.username)
        assertEquals("Nick", user?.firstName)
        assertEquals("Mason", user?.lastName)
        fixture.server.verify()
    }

    @Test
    fun `findUserById returns null on not found`() {
        val fixture = testFixture()
        fixture.expectAdminToken()
        fixture.server.expect(requestTo("${fixture.adminBaseUrl}/users/missing-user"))
            .andExpect(method(HttpMethod.GET))
            .andRespond(withStatus(HttpStatus.NOT_FOUND))

        val user = fixture.client.findUserById("missing-user")

        assertNull(user)
        fixture.server.verify()
    }

    @Test
    fun `maps unauthorized admin token response to registration exception`() {
        val fixture = testFixture(adminClientSecret = "wrong-secret")
        fixture.server.expect(requestTo(fixture.tokenUrl))
            .andExpect(method(HttpMethod.POST))
            .andExpect(content().contentType(MediaType.APPLICATION_FORM_URLENCODED))
            .andRespond(
                withStatus(HttpStatus.UNAUTHORIZED)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body("""{"error":"invalid_client","error_description":"Invalid client or Invalid client credentials"}""")
            )

        val exception = assertFailsWith<RegistrationException> {
            fixture.client.findUserByUsername("drummer")
        }

        assertEquals(HttpStatus.BAD_GATEWAY, exception.status)
        assertEquals("KEYCLOAK_ADMIN_AUTH_FAILED", exception.code)
        assertEquals("Registrierung ist momentan nicht verfuegbar", exception.message)
        fixture.server.verify()
    }

    @Test
    fun `maps missing access token in admin token response to registration exception`() {
        val fixture = testFixture()
        fixture.server.expect(requestTo(fixture.tokenUrl))
            .andExpect(method(HttpMethod.POST))
            .andRespond(
                withStatus(HttpStatus.OK)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body("""{}""")
            )

        val exception = assertFailsWith<RegistrationException> {
            fixture.client.findUserByUsername("drummer")
        }

        assertEquals(HttpStatus.BAD_GATEWAY, exception.status)
        assertEquals("KEYCLOAK_TOKEN_FAILED", exception.code)
        fixture.server.verify()
    }

    @Test
    fun `createPendingUser returns created user id from location header`() {
        val fixture = testFixture()
        fixture.expectAdminToken()
        fixture.server.expect(requestTo("${fixture.adminBaseUrl}/users"))
            .andExpect(method(HttpMethod.POST))
            .andExpect(header("Authorization", "Bearer token"))
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(content().string(containsString(""""username":"drummer"""")))
            .andExpect(content().string(containsString(""""email":"drummer@example.org"""")))
            .andExpect(content().string(containsString(""""enabled":false""")))
            .andExpect(content().string(containsString(""""emailVerified":false""")))
            .andExpect(content().string(containsString(""""value":"Drum123!"""")))
            .andRespond(
                withStatus(HttpStatus.CREATED)
                    .location(URI.create("${fixture.adminBaseUrl}/users/generated-user"))
            )

        val userId = fixture.client.createPendingUser(
            RegistrationUserDraft(
                username = "drummer",
                email = "drummer@example.org",
                firstName = "Nick",
                lastName = "Mason",
                password = "Drum123!"
            )
        )

        assertEquals("generated-user", userId)
        fixture.server.verify()
    }

    @Test
    fun `createPendingUser maps missing location header to registration exception`() {
        val fixture = testFixture()
        fixture.expectAdminToken()
        fixture.server.expect(requestTo("${fixture.adminBaseUrl}/users"))
            .andExpect(method(HttpMethod.POST))
            .andRespond(withStatus(HttpStatus.CREATED))

        val exception = assertFailsWith<RegistrationException> {
            fixture.client.createPendingUser(
                RegistrationUserDraft(
                    username = "drummer",
                    email = "drummer@example.org",
                    firstName = null,
                    lastName = null,
                    password = "Drum123!"
                )
            )
        }

        assertEquals(HttpStatus.BAD_GATEWAY, exception.status)
        assertEquals("KEYCLOAK_CREATE_FAILED", exception.code)
        fixture.server.verify()
    }

    @Test
    fun `maps keycloak username length validation to nickname field error`() {
        val fixture = testFixture()
        fixture.expectAdminToken()
        fixture.server.expect(requestTo("${fixture.adminBaseUrl}/users"))
            .andExpect(method(HttpMethod.POST))
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andRespond(
                withStatus(HttpStatus.BAD_REQUEST)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body("""{"field":"username","errorMessage":"error-invalid-length","params":["username","3","255"]}""")
            )

        val exception = assertFailsWith<RegistrationException> {
            fixture.client.createPendingUser(
                RegistrationUserDraft(
                    username = "ab",
                    email = "ab@example.org",
                    firstName = null,
                    lastName = null,
                    password = "Drum123!"
                )
            )
        }

        assertEquals(HttpStatus.BAD_REQUEST, exception.status)
        assertEquals("INVALID_NICKNAME", exception.code)
        assertEquals("nickname", exception.field)
        assertEquals("Nickname muss zwischen 3 und 255 Zeichen lang sein", exception.message)
        fixture.server.verify()
    }

    @Test
    fun `createPendingUser maps email validation to email field error`() {
        val fixture = testFixture()
        fixture.expectAdminToken()
        fixture.server.expect(requestTo("${fixture.adminBaseUrl}/users"))
            .andExpect(method(HttpMethod.POST))
            .andRespond(
                withStatus(HttpStatus.BAD_REQUEST)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body("""{"field":"email","errorMessage":"error-invalid-email"}""")
            )

        val exception = assertFailsWith<RegistrationException> {
            fixture.client.createPendingUser(
                RegistrationUserDraft(
                    username = "drummer",
                    email = "bad-mail",
                    firstName = null,
                    lastName = null,
                    password = "Drum123!"
                )
            )
        }

        assertEquals(HttpStatus.BAD_REQUEST, exception.status)
        assertEquals("INVALID_EMAIL", exception.code)
        assertEquals("email", exception.field)
        fixture.server.verify()
    }

    @Test
    fun `updateUser sends payload to keycloak`() {
        val fixture = testFixture()
        fixture.expectAdminToken()
        fixture.server.expect(requestTo("${fixture.adminBaseUrl}/users/user-1"))
            .andExpect(method(HttpMethod.PUT))
            .andExpect(header("Authorization", "Bearer token"))
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(content().string(containsString(""""username":"newdrummer"""")))
            .andExpect(content().string(containsString(""""email":"new@example.org"""")))
            .andExpect(content().string(containsString(""""enabled":true""")))
            .andExpect(content().string(containsString(""""emailVerified":true""")))
            .andRespond(withNoContent())

        fixture.client.updateUser(
            userId = "user-1",
            username = "newdrummer",
            email = "new@example.org",
            firstName = "Nick",
            lastName = "Mason",
            enabled = true,
            emailVerified = true
        )

        fixture.server.verify()
    }

    @Test
    fun `updateUser maps username validation to username field error`() {
        val fixture = testFixture()
        fixture.expectAdminToken()
        fixture.server.expect(requestTo("${fixture.adminBaseUrl}/users/user-1"))
            .andExpect(method(HttpMethod.PUT))
            .andRespond(
                withStatus(HttpStatus.BAD_REQUEST)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body("""{"field":"username","errorMessage":"error-invalid-length"}""")
            )

        val exception = assertFailsWith<RegistrationException> {
            fixture.client.updateUser(
                userId = "user-1",
                username = "bad",
                email = "drummer@example.org",
                firstName = null,
                lastName = null,
                enabled = true,
                emailVerified = true
            )
        }

        assertEquals(HttpStatus.BAD_REQUEST, exception.status)
        assertEquals("INVALID_USERNAME", exception.code)
        assertEquals("username", exception.field)
        fixture.server.verify()
    }

    @Test
    fun `updateUser maps unknown bad request to update failed`() {
        val fixture = testFixture()
        fixture.expectAdminToken()
        fixture.server.expect(requestTo("${fixture.adminBaseUrl}/users/user-1"))
            .andExpect(method(HttpMethod.PUT))
            .andRespond(
                withStatus(HttpStatus.BAD_REQUEST)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body("""{"field":"email","errorMessage":"unknown"}""")
            )

        val exception = assertFailsWith<RegistrationException> {
            fixture.client.updateUser(
                userId = "user-1",
                username = "drummer",
                email = "bad@example.org",
                firstName = null,
                lastName = null,
                enabled = true,
                emailVerified = true
            )
        }

        assertEquals(HttpStatus.BAD_GATEWAY, exception.status)
        assertEquals("KEYCLOAK_UPDATE_FAILED", exception.code)
        fixture.server.verify()
    }

    @Test
    fun `changePassword posts reset request`() {
        val fixture = testFixture()
        fixture.expectAdminToken()
        fixture.server.expect(requestTo("${fixture.adminBaseUrl}/users/user-1/reset-password"))
            .andExpect(method(HttpMethod.PUT))
            .andExpect(header("Authorization", "Bearer token"))
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(content().string(containsString(""""type":"password"""")))
            .andExpect(content().string(containsString(""""value":"NewDrum123!"""")))
            .andExpect(content().string(containsString(""""temporary":false""")))
            .andRespond(withNoContent())

        fixture.client.changePassword("user-1", "NewDrum123!")

        fixture.server.verify()
    }

    @Test
    fun `validateUserCredentials returns true for access token response`() {
        val fixture = testFixture()
        fixture.server.expect(requestTo(fixture.tokenUrl))
            .andExpect(method(HttpMethod.POST))
            .andExpect(content().contentType(MediaType.APPLICATION_FORM_URLENCODED))
            .andExpect(content().string(containsString("grant_type=password")))
            .andExpect(content().string(containsString("client_id=heuermannplus-registration-service")))
            .andExpect(content().string(containsString("client_secret=heuermannplus-registration-secret")))
            .andExpect(content().string(containsString("username=drummer")))
            .andExpect(content().string(containsString("password=Secret123%21")))
            .andRespond(
                withStatus(HttpStatus.OK)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body("""{"access_token":"user-token"}""")
            )

        val valid = fixture.client.validateUserCredentials("drummer", "Secret123!")

        assertTrue(valid)
        fixture.server.verify()
    }

    @Test
    fun `validateUserCredentials returns false for bad request`() {
        val fixture = testFixture()
        fixture.server.expect(requestTo(fixture.tokenUrl))
            .andExpect(method(HttpMethod.POST))
            .andRespond(withStatus(HttpStatus.BAD_REQUEST))

        val valid = fixture.client.validateUserCredentials("drummer", "wrong")

        assertFalse(valid)
        fixture.server.verify()
    }

    @Test
    fun `assignRealmRoles does nothing when no roles are requested`() {
        val fixture = testFixture()

        fixture.client.assignRealmRoles("user-1", emptySet())

        fixture.server.verify()
    }

    @Test
    fun `assignRealmRoles resolves roles and posts them`() {
        val fixture = testFixture()
        fixture.expectAdminToken()
        fixture.server.expect(requestTo("${fixture.adminBaseUrl}/roles/registration-pending"))
            .andExpect(method(HttpMethod.GET))
            .andExpect(header("Authorization", "Bearer token"))
            .andRespond(
                withStatus(HttpStatus.OK)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body("""{"id":"role-1","name":"registration-pending","description":"Pending registration"}""")
            )
        fixture.expectAdminToken()
        fixture.server.expect(requestTo("${fixture.adminBaseUrl}/users/user-1/role-mappings/realm"))
            .andExpect(method(HttpMethod.POST))
            .andExpect(header("Authorization", "Bearer token"))
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(content().string(containsString(""""id":"role-1"""")))
            .andExpect(content().string(containsString(""""name":"registration-pending"""")))
            .andRespond(withNoContent())

        fixture.client.assignRealmRoles("user-1", setOf("registration-pending"))

        fixture.server.verify()
    }

    @Test
    fun `maps forbidden realm role lookup to registration exception`() {
        val fixture = testFixture()
        fixture.expectAdminToken()
        fixture.server.expect(requestTo("${fixture.adminBaseUrl}/roles/registration-pending"))
            .andExpect(method(HttpMethod.GET))
            .andRespond(
                withStatus(HttpStatus.FORBIDDEN)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body("""{"error":"HTTP 403 Forbidden"}""")
            )

        val exception = assertFailsWith<RegistrationException> {
            fixture.client.assignRealmRoles("user-1", setOf("registration-pending"))
        }

        assertEquals(HttpStatus.BAD_GATEWAY, exception.status)
        assertEquals("KEYCLOAK_ADMIN_FORBIDDEN", exception.code)
        assertEquals("Registrierung ist momentan nicht verfuegbar", exception.message)
        fixture.server.verify()
    }

    @Test
    fun `assignRealmRoles maps forbidden role assignment to registration exception`() {
        val fixture = testFixture()
        fixture.expectAdminToken()
        fixture.server.expect(requestTo("${fixture.adminBaseUrl}/roles/registration-pending"))
            .andExpect(method(HttpMethod.GET))
            .andRespond(
                withStatus(HttpStatus.OK)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body("""{"id":"role-1","name":"registration-pending"}""")
            )
        fixture.expectAdminToken()
        fixture.server.expect(requestTo("${fixture.adminBaseUrl}/users/user-1/role-mappings/realm"))
            .andExpect(method(HttpMethod.POST))
            .andRespond(withStatus(HttpStatus.FORBIDDEN))

        val exception = assertFailsWith<RegistrationException> {
            fixture.client.assignRealmRoles("user-1", setOf("registration-pending"))
        }

        assertEquals(HttpStatus.BAD_GATEWAY, exception.status)
        assertEquals("KEYCLOAK_ADMIN_FORBIDDEN", exception.code)
        fixture.server.verify()
    }

    @Test
    fun `removeRealmRoles does nothing when no roles are requested`() {
        val fixture = testFixture()

        fixture.client.removeRealmRoles("user-1", emptySet())

        fixture.server.verify()
    }

    @Test
    fun `removeRealmRoles resolves roles and deletes them`() {
        val fixture = testFixture()
        fixture.expectAdminToken()
        fixture.server.expect(requestTo("${fixture.adminBaseUrl}/roles/member"))
            .andExpect(method(HttpMethod.GET))
            .andRespond(
                withStatus(HttpStatus.OK)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body("""{"id":"role-2","name":"member"}""")
            )
        fixture.expectAdminToken()
        fixture.server.expect(requestTo("${fixture.adminBaseUrl}/users/user-1/role-mappings/realm"))
            .andExpect(method(HttpMethod.DELETE))
            .andExpect(header("Authorization", "Bearer token"))
            .andExpect(content().string(containsString(""""id":"role-2"""")))
            .andExpect(content().string(containsString(""""name":"member"""")))
            .andRespond(withNoContent())

        fixture.client.removeRealmRoles("user-1", setOf("member"))

        fixture.server.verify()
    }

    @Test
    fun `removeRealmRoles maps forbidden delete to registration exception`() {
        val fixture = testFixture()
        fixture.expectAdminToken()
        fixture.server.expect(requestTo("${fixture.adminBaseUrl}/roles/member"))
            .andExpect(method(HttpMethod.GET))
            .andRespond(
                withStatus(HttpStatus.OK)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body("""{"id":"role-2","name":"member"}""")
            )
        fixture.expectAdminToken()
        fixture.server.expect(requestTo("${fixture.adminBaseUrl}/users/user-1/role-mappings/realm"))
            .andExpect(method(HttpMethod.DELETE))
            .andRespond(withStatus(HttpStatus.FORBIDDEN))

        val exception = assertFailsWith<RegistrationException> {
            fixture.client.removeRealmRoles("user-1", setOf("member"))
        }

        assertEquals(HttpStatus.BAD_GATEWAY, exception.status)
        assertEquals("KEYCLOAK_ADMIN_FORBIDDEN", exception.code)
        fixture.server.verify()
    }

    @Test
    fun `enableUser loads current representation and enables it`() {
        val fixture = testFixture()
        fixture.expectAdminToken()
        fixture.server.expect(requestTo("${fixture.adminBaseUrl}/users/user-1"))
            .andExpect(method(HttpMethod.GET))
            .andExpect(header("Authorization", "Bearer token"))
            .andRespond(
                withStatus(HttpStatus.OK)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body("""{"id":"user-1","username":"drummer","email":"drummer@example.org","enabled":false,"emailVerified":false,"firstName":"Nick","lastName":"Mason"}""")
            )
        fixture.server.expect(requestTo("${fixture.adminBaseUrl}/users/user-1"))
            .andExpect(method(HttpMethod.PUT))
            .andExpect(header("Authorization", "Bearer token"))
            .andExpect(content().string(containsString(""""enabled":true""")))
            .andExpect(content().string(containsString(""""emailVerified":true""")))
            .andExpect(content().string(containsString(""""username":"drummer"""")))
            .andRespond(withNoContent())

        fixture.client.enableUser("user-1")

        fixture.server.verify()
    }

    @Test
    fun `enableUser maps missing keycloak user to invalid token`() {
        val fixture = testFixture()
        fixture.expectAdminToken()
        fixture.server.expect(requestTo("${fixture.adminBaseUrl}/users/user-1"))
            .andExpect(method(HttpMethod.GET))
            .andRespond(
                withStatus(HttpStatus.OK)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body("")
            )

        val exception = assertFailsWith<RegistrationException> {
            fixture.client.enableUser("user-1")
        }

        assertEquals(HttpStatus.BAD_REQUEST, exception.status)
        assertEquals("INVALID_TOKEN", exception.code)
        fixture.server.verify()
    }

    @Test
    fun `deleteUser deletes existing user`() {
        val fixture = testFixture()
        fixture.expectAdminToken()
        fixture.server.expect(requestTo("${fixture.adminBaseUrl}/users/user-1"))
            .andExpect(method(HttpMethod.DELETE))
            .andExpect(header("Authorization", "Bearer token"))
            .andRespond(withNoContent())

        fixture.client.deleteUser("user-1")

        fixture.server.verify()
    }

    @Test
    fun `deleteUser ignores missing users`() {
        val fixture = testFixture()
        fixture.expectAdminToken()
        fixture.server.expect(requestTo("${fixture.adminBaseUrl}/users/missing-user"))
            .andExpect(method(HttpMethod.DELETE))
            .andRespond(withStatus(HttpStatus.NOT_FOUND))

        fixture.client.deleteUser("missing-user")

        fixture.server.verify()
    }

    private fun testFixture(
        serverUrl: String = "http://keycloak:8080/",
        realm: String = "heuermannplus",
        adminClientId: String = "heuermannplus-registration-service",
        adminClientSecret: String = "heuermannplus-registration-secret"
    ): TestFixture {
        val restClientBuilder = RestClient.builder()
        val server = MockRestServiceServer.bindTo(restClientBuilder).build()
        val properties = KeycloakAdminProperties(
            serverUrl = serverUrl,
            realm = realm,
            adminClientId = adminClientId,
            adminClientSecret = adminClientSecret
        )

        return TestFixture(
            client = RestKeycloakAdminClient(
                keycloakAdminProperties = properties,
                restClientBuilder = restClientBuilder
            ),
            server = server,
            tokenUrl = "${serverUrl.trimEnd('/')}/realms/$realm/protocol/openid-connect/token",
            adminBaseUrl = "${serverUrl.trimEnd('/')}/admin/realms/$realm"
        )
    }

    private fun TestFixture.expectAdminToken(token: String = "token") {
        server.expect(once(), requestTo(tokenUrl))
            .andExpect(method(HttpMethod.POST))
            .andExpect(content().contentType(MediaType.APPLICATION_FORM_URLENCODED))
            .andExpect(content().string(containsString("grant_type=client_credentials")))
            .andExpect(content().string(containsString("client_id=heuermannplus-registration-service")))
            .andExpect(content().string(containsString("client_secret=")))
            .andRespond(
                withStatus(HttpStatus.OK)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body("""{"access_token":"$token"}""")
            )
    }

    private data class TestFixture(
        val client: RestKeycloakAdminClient,
        val server: MockRestServiceServer,
        val tokenUrl: String,
        val adminBaseUrl: String
    )
}
