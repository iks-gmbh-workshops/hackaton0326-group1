package de.heuermannplus.backend.api

import de.heuermannplus.backend.registration.ApiErrorResponse
import de.heuermannplus.backend.registration.AppUserStore
import de.heuermannplus.backend.registration.ChangePasswordRequest
import de.heuermannplus.backend.registration.DeleteAccountRequest
import de.heuermannplus.backend.registration.MessageResponse
import de.heuermannplus.backend.registration.ProfileResponse
import de.heuermannplus.backend.registration.ProfileService
import de.heuermannplus.backend.registration.UpdateProfileRequest
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/private")
class PrivateController(
    private val appUserStore: AppUserStore,
    private val profileService: ProfileService
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

    @GetMapping("/profile")
    fun profile(authentication: JwtAuthenticationToken): ProfileResponse =
        profileService.profile(authentication)

    @PutMapping("/profile")
    fun updateProfile(
        authentication: JwtAuthenticationToken,
        @RequestBody request: UpdateProfileRequest
    ): ProfileResponse =
        profileService.updateProfile(authentication, request)

    @PostMapping("/profile/password")
    fun changePassword(
        authentication: JwtAuthenticationToken,
        @RequestBody request: ChangePasswordRequest
    ): MessageResponse =
        profileService.changePassword(authentication, request)

    @DeleteMapping("/profile")
    fun deleteProfile(
        authentication: JwtAuthenticationToken,
        @RequestBody request: DeleteAccountRequest
    ): MessageResponse =
        profileService.deleteAccount(authentication, request)
}
