package de.heuermannplus.backend.registration

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.test.web.client.MockRestServiceServer
import org.springframework.test.web.client.match.MockRestRequestMatchers.content
import org.springframework.test.web.client.match.MockRestRequestMatchers.method
import org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo
import org.springframework.test.web.client.response.MockRestResponseCreators.withStatus
import org.springframework.web.client.RestClient

class RestKeycloakAdminClientTest {

    @Test
    fun `maps unauthorized admin token response to registration exception`() {
        val restClientBuilder = RestClient.builder()
        val server = MockRestServiceServer.bindTo(restClientBuilder).build()
        val client = RestKeycloakAdminClient(
            keycloakAdminProperties = KeycloakAdminProperties(
                serverUrl = "http://keycloak:8080",
                realm = "heuermannplus",
                adminClientId = "heuermannplus-registration-service",
                adminClientSecret = "wrong-secret"
            ),
            restClientBuilder = restClientBuilder
        )

        server.expect(requestTo("http://keycloak:8080/realms/heuermannplus/protocol/openid-connect/token"))
            .andExpect(method(HttpMethod.POST))
            .andExpect(content().contentType(MediaType.APPLICATION_FORM_URLENCODED))
            .andRespond(
                withStatus(HttpStatus.UNAUTHORIZED)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body("""{"error":"invalid_client","error_description":"Invalid client or Invalid client credentials"}""")
            )

        val exception = assertFailsWith<RegistrationException> {
            client.findUserByUsername("drummer")
        }

        assertEquals(HttpStatus.BAD_GATEWAY, exception.status)
        assertEquals("KEYCLOAK_ADMIN_AUTH_FAILED", exception.code)
        assertEquals("Registrierung ist momentan nicht verfuegbar", exception.message)

        server.verify()
    }
}
