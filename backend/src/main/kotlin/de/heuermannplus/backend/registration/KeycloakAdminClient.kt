package de.heuermannplus.backend.registration

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.util.LinkedMultiValueMap
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.RestClientException
import org.springframework.web.client.RestClient

interface KeycloakAdminClient {
    fun findUserByUsername(username: String): KeycloakUserSummary?

    fun findUserByEmail(email: String): KeycloakUserSummary?

    fun findUserById(userId: String): KeycloakUserRepresentation?

    fun createPendingUser(draft: RegistrationUserDraft): String

    fun updateUser(
        userId: String,
        username: String,
        email: String,
        firstName: String?,
        lastName: String?,
        enabled: Boolean,
        emailVerified: Boolean
    )

    fun changePassword(userId: String, newPassword: String)

    fun validateUserCredentials(username: String, password: String): Boolean

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
    private val objectMapper = jacksonObjectMapper()

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

    override fun findUserById(userId: String): KeycloakUserRepresentation? =
        try {
            restClient.get()
                .uri("${adminBaseUrl()}/users/{userId}", userId)
                .headers { headers -> headers.setBearerAuth(accessToken()) }
                .retrieve()
                .body(KeycloakUserRepresentation::class.java)
        } catch (_: HttpClientErrorException.NotFound) {
            null
        }

    override fun createPendingUser(draft: RegistrationUserDraft): String {
        try {
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
        } catch (exception: HttpClientErrorException.BadRequest) {
            throw mapCreateUserBadRequest(exception)
        }
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
        try {
            restClient.put()
                .uri("${adminBaseUrl()}/users/{userId}", userId)
                .headers { headers -> headers.setBearerAuth(accessToken()) }
                .contentType(MediaType.APPLICATION_JSON)
                .body(
                    KeycloakUpdateUserRequest(
                        username = username,
                        email = email,
                        firstName = firstName,
                        lastName = lastName,
                        enabled = enabled,
                        emailVerified = emailVerified
                    )
                )
                .retrieve()
                .toBodilessEntity()
        } catch (exception: HttpClientErrorException.BadRequest) {
            throw mapUpdateUserBadRequest(exception)
        }
    }

    override fun changePassword(userId: String, newPassword: String) {
        restClient.put()
            .uri("${adminBaseUrl()}/users/{userId}/reset-password", userId)
            .headers { headers -> headers.setBearerAuth(accessToken()) }
            .contentType(MediaType.APPLICATION_JSON)
            .body(
                KeycloakCredentialRepresentation(
                    type = "password",
                    value = newPassword,
                    temporary = false
                )
            )
            .retrieve()
            .toBodilessEntity()
    }

    override fun validateUserCredentials(username: String, password: String): Boolean =
        try {
            val response = restClient.post()
                .uri("${tokenUrl()}")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(
                    LinkedMultiValueMap<String, String>().apply {
                        add("grant_type", "password")
                        add("client_id", keycloakAdminProperties.adminClientId)
                        add("client_secret", keycloakAdminProperties.adminClientSecret)
                        add("username", username)
                        add("password", password)
                    }
                )
                .retrieve()
                .body(KeycloakTokenResponse::class.java)

            response?.accessToken.isNullOrBlank().not()
        } catch (_: HttpClientErrorException.BadRequest) {
            false
        } catch (_: HttpClientErrorException.Unauthorized) {
            false
        }

    override fun assignRealmRoles(userId: String, roleNames: Set<String>) {
        if (roleNames.isEmpty()) {
            return
        }

        val roles = resolveRoles(roleNames)
        try {
            restClient.post()
                .uri("${adminBaseUrl()}/users/{userId}/role-mappings/realm", userId)
                .headers { headers -> headers.setBearerAuth(accessToken()) }
                .contentType(MediaType.APPLICATION_JSON)
                .body(roles)
                .retrieve()
                .toBodilessEntity()
        } catch (exception: HttpClientErrorException.Forbidden) {
            throw mapRoleOperationForbidden("assign", userId, roleNames, exception)
        }
    }

    override fun removeRealmRoles(userId: String, roleNames: Set<String>) {
        if (roleNames.isEmpty()) {
            return
        }

        val roles = resolveRoles(roleNames)
        try {
            restClient.method(org.springframework.http.HttpMethod.DELETE)
                .uri("${adminBaseUrl()}/users/{userId}/role-mappings/realm", userId)
                .headers { headers -> headers.setBearerAuth(accessToken()) }
                .contentType(MediaType.APPLICATION_JSON)
                .body(roles)
                .retrieve()
                .toBodilessEntity()
        } catch (exception: HttpClientErrorException.Forbidden) {
            throw mapRoleOperationForbidden("remove", userId, roleNames, exception)
        }
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
        roleNames.map(::resolveRole)

    private fun adminBaseUrl(): String =
        "${keycloakAdminProperties.serverUrl.trimEnd('/')}/admin/realms/${keycloakAdminProperties.realm}"

    private fun tokenUrl(): String =
        "${keycloakAdminProperties.serverUrl.trimEnd('/')}/realms/${keycloakAdminProperties.realm}/protocol/openid-connect/token"

    private fun resolveRole(roleName: String): KeycloakRoleRepresentation {
        try {
            return restClient.get()
                .uri("${adminBaseUrl()}/roles/{roleName}", roleName)
                .headers { headers -> headers.setBearerAuth(accessToken()) }
                .retrieve()
                .body(KeycloakRoleRepresentation::class.java)
                ?: throw RegistrationException(
                    status = HttpStatus.BAD_GATEWAY,
                    code = "KEYCLOAK_ROLE_NOT_FOUND",
                    message = "Rolle $roleName konnte nicht geladen werden"
                )
        } catch (_: HttpClientErrorException.NotFound) {
            throw RegistrationException(
                status = HttpStatus.BAD_GATEWAY,
                code = "KEYCLOAK_ROLE_NOT_FOUND",
                message = "Rolle $roleName konnte nicht geladen werden"
            )
        } catch (exception: HttpClientErrorException.Forbidden) {
            logger.error(
                "Keycloak denied reading realm role {} in realm {} for admin client {}. The service account likely needs realm-management role view-realm. Response: {}",
                roleName,
                keycloakAdminProperties.realm,
                keycloakAdminProperties.adminClientId,
                exception.responseBodyAsString
            )
            throw RegistrationException(
                status = HttpStatus.BAD_GATEWAY,
                code = "KEYCLOAK_ADMIN_FORBIDDEN",
                message = "Registrierung ist momentan nicht verfuegbar"
            )
        }
    }

    private fun mapRoleOperationForbidden(
        action: String,
        userId: String,
        roleNames: Set<String>,
        exception: HttpClientErrorException.Forbidden
    ): RegistrationException {
        logger.error(
            "Keycloak denied {} realm roles {} for user {} in realm {} for admin client {}. The service account likely needs realm-management permissions such as manage-users. Response: {}",
            action,
            roleNames,
            userId,
            keycloakAdminProperties.realm,
            keycloakAdminProperties.adminClientId,
            exception.responseBodyAsString
        )
        return RegistrationException(
            status = HttpStatus.BAD_GATEWAY,
            code = "KEYCLOAK_ADMIN_FORBIDDEN",
            message = "Registrierung ist momentan nicht verfuegbar"
        )
    }

    private fun mapCreateUserBadRequest(exception: HttpClientErrorException.BadRequest): RegistrationException {
        val validationError = runCatching {
            objectMapper.readValue(exception.responseBodyAsByteArray, KeycloakValidationErrorResponse::class.java)
        }.getOrNull()

        if (validationError?.field == "username" && validationError.errorMessage == "error-invalid-length") {
            val minLength = validationError.params.getOrNull(1)
            val maxLength = validationError.params.getOrNull(2)

            return RegistrationException(
                status = HttpStatus.BAD_REQUEST,
                code = "INVALID_NICKNAME",
                message = if (minLength != null && maxLength != null) {
                    "Nickname muss zwischen $minLength und $maxLength Zeichen lang sein"
                } else {
                    "Nickname ist ungueltig"
                },
                field = "nickname"
            )
        }

        if (validationError?.field == "username") {
            return RegistrationException(
                status = HttpStatus.BAD_REQUEST,
                code = "INVALID_NICKNAME",
                message = "Nickname ist ungueltig",
                field = "nickname"
            )
        }

        if (validationError?.field == "email") {
            return RegistrationException(
                status = HttpStatus.BAD_REQUEST,
                code = "INVALID_EMAIL",
                message = "Email-Adresse ist ungueltig",
                field = "email"
            )
        }

        logger.warn(
            "Keycloak rejected user creation in realm {}: {}",
            keycloakAdminProperties.realm,
            exception.responseBodyAsString
        )
        return RegistrationException(
            status = HttpStatus.BAD_REQUEST,
            code = "KEYCLOAK_CREATE_FAILED",
            message = "Die Registrierungsdaten sind ungueltig"
        )
    }

    private fun mapUpdateUserBadRequest(exception: HttpClientErrorException.BadRequest): RegistrationException {
        val validationError = runCatching {
            objectMapper.readValue(exception.responseBodyAsByteArray, KeycloakValidationErrorResponse::class.java)
        }.getOrNull()

        if (validationError?.field == "username") {
            return RegistrationException(
                status = HttpStatus.BAD_REQUEST,
                code = "INVALID_USERNAME",
                message = "Username ist ungueltig",
                field = "username"
            )
        }

        logger.warn(
            "Keycloak rejected user update in realm {}: {}",
            keycloakAdminProperties.realm,
            exception.responseBodyAsString
        )
        return RegistrationException(
            status = HttpStatus.BAD_GATEWAY,
            code = "KEYCLOAK_UPDATE_FAILED",
            message = "Profil konnte nicht aktualisiert werden"
        )
    }

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

data class KeycloakValidationErrorResponse(
    val field: String? = null,
    val errorMessage: String? = null,
    val params: List<String> = emptyList()
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

data class KeycloakUpdateUserRequest(
    val username: String,
    val email: String,
    val firstName: String?,
    val lastName: String?,
    val enabled: Boolean,
    val emailVerified: Boolean
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
