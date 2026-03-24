package de.heuermannplus.backend.registration

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties("app.keycloak")
data class KeycloakAdminProperties(
    val serverUrl: String = "http://keycloak:8080",
    val realm: String = "heuermannplus",
    val adminClientId: String = "heuermannplus-registration-service",
    val adminClientSecret: String = "heuermannplus-registration-secret"
)
