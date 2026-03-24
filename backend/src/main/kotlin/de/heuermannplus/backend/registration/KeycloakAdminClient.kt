package de.heuermannplus.backend.registration

import com.fasterxml.jackson.annotation.JsonProperty
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.util.LinkedMultiValueMap
import org.springframework.stereotype.Component
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.RestClientException
import org.springframework.web.client.RestClient

interface KeycloakAdminClient {
    fun findUserByUsername(username: String): KeycloakUserSummary?

    fun findUserByEmail(email: String): KeycloakUserSummary?

    fun createPendingUser(draft: RegistrationUserDraft): String

    fun assignRealmRoles(userId: String, roleNames: Set<String>)

    fun removeRealmRoles(userId: String, roleNames: Set<String>)

    fun enableUser(userId: String)

    fun deleteUser(userId: String)
}

@Component
class RestKeycloakAdminClient(
    private val keycloakAdminProperties: KeycloakAdminProperties,
    restClientBuilder: RestClient.Builder
) : KeycloakAdminClient {
    private val logger = LoggerFactory.getLogger(javaClass)

    private val restClient = restClientBuilder.build()

    override fun findUserByUsername(username: String): KeycloakUserSummary? {
        val token = accessToken()

        return restClient.get()
            .uri("${adminBaseUrl()}/users?username={username}&exact=true", username)
            .headers { headers -> headers.setBearerAuth(token) }
            .retrieve()
            .body(Array<KeycloakUserRepresentation>::class.java)
            ?.firstOrNull()
            ?.toSummary()
    }

    override fun findUserByEmail(email: String): KeycloakUserSummary? {
        val token = accessToken()

        return restClient.get()
            .uri("${adminBaseUrl()}/users?email={email}&exact=true", email)
            .headers { headers -> headers.setBearerAuth(token) }
            .retrieve()
            .body(Array<KeycloakUserRepresentation>::class.java)
            ?.firstOrNull()
            ?.toSummary()
    }

    override fun createPendingUser(draft: RegistrationUserDraft): String {
        val response = restClient.post()
            .uri("${adminBaseUrl()}/users")
            .headers { headers -> headers.setBearerAuth(accessToken()) }
            .contentType(MediaType.APPLICATION_JSON)
            .body(
                KeycloakCreateUserRequest(
                    username = draft.username,
                    email = draft.email,
                    firstName = draft.firstName,
                    lastName = draft.lastName,
                    enabled = false,
                    emailVerified = false,
                    credentials = listOf(
                        KeycloakCredentialRepresentation(
                            type = "password",
                            value = draft.password,
                            temporary = false
                        )
                    )
                )
            )
            .retrieve()
            .toBodilessEntity()

        val location = response.headers.location?.toString()
            ?: throw RegistrationException(
                status = HttpStatus.BAD_GATEWAY,
                code = "KEYCLOAK_CREATE_FAILED",
                message = "Benutzer konnte nicht angelegt werden"
            )

        return location.substringAfterLast("/")
    }

    override fun assignRealmRoles(userId: String, roleNames: Set<String>) {
        if (roleNames.isEmpty()) {
            return
        }

        val roles = resolveRoles(roleNames)
        restClient.post()
            .uri("${adminBaseUrl()}/users/{userId}/role-mappings/realm", userId)
            .headers { headers -> headers.setBearerAuth(accessToken()) }
            .contentType(MediaType.APPLICATION_JSON)
            .body(roles)
            .retrieve()
            .toBodilessEntity()
    }

    override fun removeRealmRoles(userId: String, roleNames: Set<String>) {
        if (roleNames.isEmpty()) {
            return
        }

        val roles = resolveRoles(roleNames)
        restClient.method(org.springframework.http.HttpMethod.DELETE)
            .uri("${adminBaseUrl()}/users/{userId}/role-mappings/realm", userId)
            .headers { headers -> headers.setBearerAuth(accessToken()) }
            .contentType(MediaType.APPLICATION_JSON)
            .body(roles)
            .retrieve()
            .toBodilessEntity()
    }

    override fun enableUser(userId: String) {
        val token = accessToken()
        val user = restClient.get()
            .uri("${adminBaseUrl()}/users/{userId}", userId)
            .headers { headers -> headers.setBearerAuth(token) }
            .retrieve()
            .body(KeycloakUserRepresentation::class.java)
            ?: throw RegistrationException(
                status = HttpStatus.BAD_REQUEST,
                code = "INVALID_TOKEN",
                message = "Der Verifizierungslink ist ungueltig"
            )

        restClient.put()
            .uri("${adminBaseUrl()}/users/{userId}", userId)
            .headers { headers -> headers.setBearerAuth(token) }
            .contentType(MediaType.APPLICATION_JSON)
            .body(user.copy(enabled = true, emailVerified = true))
            .retrieve()
            .toBodilessEntity()
    }

    override fun deleteUser(userId: String) {
        try {
            restClient.delete()
                .uri("${adminBaseUrl()}/users/{userId}", userId)
                .headers { headers -> headers.setBearerAuth(accessToken()) }
                .retrieve()
                .toBodilessEntity()
        } catch (_: HttpClientErrorException.NotFound) {
            // Ignore already deleted users during cleanup or rollback.
        }
    }

    private fun resolveRoles(roleNames: Set<String>): List<KeycloakRoleRepresentation> =
        roleNames.map { roleName ->
            restClient.get()
                .uri("${adminBaseUrl()}/roles/{roleName}", roleName)
                .headers { headers -> headers.setBearerAuth(accessToken()) }
                .retrieve()
                .body(KeycloakRoleRepresentation::class.java)
                ?: throw RegistrationException(
                    status = HttpStatus.BAD_GATEWAY,
                    code = "KEYCLOAK_ROLE_NOT_FOUND",
                    message = "Rolle $roleName konnte nicht geladen werden"
                )
        }

    private fun adminBaseUrl(): String =
        "${keycloakAdminProperties.serverUrl.trimEnd('/')}/admin/realms/${keycloakAdminProperties.realm}"

    private fun accessToken(): String {
        try {
            val response = restClient.post()
                .uri("${keycloakAdminProperties.serverUrl.trimEnd('/')}/realms/${keycloakAdminProperties.realm}/protocol/openid-connect/token")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(
                    LinkedMultiValueMap<String, String>().apply {
                        add("grant_type", "client_credentials")
                        add("client_id", keycloakAdminProperties.adminClientId)
                        add("client_secret", keycloakAdminProperties.adminClientSecret)
                    }
                )
                .retrieve()
                .body(KeycloakTokenResponse::class.java)

            return response?.accessToken ?: throw RegistrationException(
                status = HttpStatus.BAD_GATEWAY,
                code = "KEYCLOAK_TOKEN_FAILED",
                message = "Registrierung ist momentan nicht verfuegbar"
            )
        } catch (exception: HttpClientErrorException.Unauthorized) {
            logger.error(
                "Keycloak admin token request was rejected for client {} in realm {} at {}: {}",
                keycloakAdminProperties.adminClientId,
                keycloakAdminProperties.realm,
                keycloakAdminProperties.serverUrl,
                exception.responseBodyAsString
            )
            throw RegistrationException(
                status = HttpStatus.BAD_GATEWAY,
                code = "KEYCLOAK_ADMIN_AUTH_FAILED",
                message = "Registrierung ist momentan nicht verfuegbar"
            )
        } catch (exception: RestClientException) {
            logger.error(
                "Failed to request Keycloak admin token for client {} in realm {} at {}",
                keycloakAdminProperties.adminClientId,
                keycloakAdminProperties.realm,
                keycloakAdminProperties.serverUrl,
                exception
            )
            throw RegistrationException(
                status = HttpStatus.BAD_GATEWAY,
                code = "KEYCLOAK_TOKEN_FAILED",
                message = "Registrierung ist momentan nicht verfuegbar"
            )
        }
    }
}

data class KeycloakTokenResponse(
    @field:JsonProperty("access_token")
    val accessToken: String? = null
)

data class KeycloakCreateUserRequest(
    val username: String,
    val email: String,
    val firstName: String?,
    val lastName: String?,
    val enabled: Boolean,
    val emailVerified: Boolean,
    val credentials: List<KeycloakCredentialRepresentation>
)

data class KeycloakCredentialRepresentation(
    val type: String,
    val value: String,
    val temporary: Boolean
)

data class KeycloakRoleRepresentation(
    val id: String,
    val name: String,
    val description: String? = null
)

data class KeycloakUserRepresentation(
    val id: String? = null,
    val username: String? = null,
    val email: String? = null,
    val enabled: Boolean = false,
    val emailVerified: Boolean = false,
    val firstName: String? = null,
    val lastName: String? = null,
    val credentials: List<KeycloakCredentialRepresentation>? = null
) {
    fun toSummary(): KeycloakUserSummary? =
        if (id == null || username == null) {
            null
        } else {
            KeycloakUserSummary(
                id = id,
                username = username,
                email = email,
                enabled = enabled,
                emailVerified = emailVerified
            )
        }
}
