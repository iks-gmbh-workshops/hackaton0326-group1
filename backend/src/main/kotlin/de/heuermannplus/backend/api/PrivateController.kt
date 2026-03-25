package de.heuermannplus.backend.api

import de.heuermannplus.backend.registration.ApiErrorResponse
import de.heuermannplus.backend.registration.AppUserStore
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/private")
class PrivateController(
    private val appUserStore: AppUserStore
) {

    @GetMapping("/me")
    fun me(authentication: JwtAuthenticationToken): ResponseEntity<Any> {
        val subject = authentication.requiredSubject()
        val user = appUserStore.findById(subject)
            ?: return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(
                    ApiErrorResponse(
                        code = "USER_PROFILE_NOT_FOUND",
                        message = "Lokales Benutzerprofil nicht gefunden"
                    )
                )

        return ResponseEntity.ok(
            PrivateUserProfileResponse(
                subject = subject,
                nickname = user.nickname,
                email = user.email,
                firstName = user.firstName,
                lastName = user.lastName,
                status = user.status,
                enabled = user.enabled,
                emailVerified = user.emailVerified,
                keycloakRole = user.keycloakRole
            )
        )
    }
}
