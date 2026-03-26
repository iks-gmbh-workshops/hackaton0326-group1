package de.heuermannplus.backend.registration

import java.time.Clock
import java.time.Instant
import org.springframework.http.HttpStatus
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.util.StringUtils

@Service
class ProfileService(
    private val passwordPolicyEvaluator: PasswordPolicyEvaluator,
    private val keycloakAdminClient: KeycloakAdminClient,
    private val appUserStore: AppUserStore,
    private val clock: Clock
) {

    fun profile(authentication: JwtAuthenticationToken): ProfileResponse =
        currentUser(authentication).toProfileResponse()

    @Transactional
    fun updateProfile(authentication: JwtAuthenticationToken, request: UpdateProfileRequest): ProfileResponse {
        val currentUser = currentUser(authentication)
        val username = request.username.requireField("username", "Bitte Username eingeben")
        val firstName = request.firstName.normalizeOptional()
        val lastName = request.lastName.normalizeOptional()
        val now = Instant.now(clock)

        if (username.length < NICKNAME_MIN_LENGTH || username.length > NICKNAME_MAX_LENGTH) {
            throw RegistrationException(
                status = HttpStatus.BAD_REQUEST,
                code = "INVALID_USERNAME",
                message = "Username muss zwischen $NICKNAME_MIN_LENGTH und $NICKNAME_MAX_LENGTH Zeichen lang sein",
                field = "username"
            )
        }

        val conflictingAppUser = appUserStore.findByNickname(username)
        if (conflictingAppUser != null && conflictingAppUser.keycloakUserId != currentUser.keycloakUserId) {
            throw usernameConflict()
        }

        val conflictingKeycloakUser = keycloakAdminClient.findUserByUsername(username)
        if (conflictingKeycloakUser != null && conflictingKeycloakUser.id != currentUser.keycloakUserId) {
            throw usernameConflict()
        }

        keycloakAdminClient.updateUser(
            userId = currentUser.keycloakUserId,
            username = username,
            email = currentUser.email,
            firstName = firstName,
            lastName = lastName,
            enabled = currentUser.enabled,
            emailVerified = currentUser.emailVerified
        )

        val updatedUser = appUserStore.save(
            currentUser.copy(
                nickname = username,
                firstName = firstName,
                lastName = lastName,
                updatedAt = now
            )
        )

        return updatedUser.toProfileResponse()
    }

    fun changePassword(authentication: JwtAuthenticationToken, request: ChangePasswordRequest): MessageResponse {
        val currentUser = currentUser(authentication)
        val newPassword = request.newPassword.requireField("newPassword", "Bitte neues Passwort eingeben")
        val newPasswordRepeat = request.newPasswordRepeat.requireField("newPasswordRepeat", "Bitte Passwort-Wiederholung eingeben")

        if (newPassword != newPasswordRepeat) {
            throw RegistrationException(
                status = HttpStatus.BAD_REQUEST,
                code = "PASSWORD_REPEAT_MISMATCH",
                message = "Passwort-Wiederholung falsch",
                field = "newPasswordRepeat"
            )
        }

        if (!passwordPolicyEvaluator.isSatisfied(newPassword)) {
            throw RegistrationException(
                status = HttpStatus.BAD_REQUEST,
                code = "PASSWORD_POLICY_FAILED",
                message = "Passwort entspricht nicht den Mindestanforderungen",
                field = "newPassword"
            )
        }

        keycloakAdminClient.changePassword(currentUser.keycloakUserId, newPassword)
        return MessageResponse("Passwort wurde geändert")
    }

    @Transactional
    fun deleteAccount(authentication: JwtAuthenticationToken, request: DeleteAccountRequest): MessageResponse {
        val currentUser = currentUser(authentication)
        val confirmation = request.confirmation.requireField("confirmation", "Bitte Username zur Bestätigung eingeben")

        if (confirmation != currentUser.nickname) {
            throw RegistrationException(
                status = HttpStatus.BAD_REQUEST,
                code = "DELETE_CONFIRMATION_MISMATCH",
                message = "Die Bestätigung stimmt nicht mit dem Username überein",
                field = "confirmation"
            )
        }

        keycloakAdminClient.deleteUser(currentUser.keycloakUserId)
        appUserStore.deleteById(currentUser.keycloakUserId)
        return MessageResponse("Konto wurde gelöscht")
    }

    private fun currentUser(authentication: JwtAuthenticationToken): AppUser {
        val claims = authentication.token.claims
        val keycloakUserId = authentication.token.subject
            ?: authentication.name.takeIf(StringUtils::hasText)
            ?: (claims["sub"] as? String)?.takeIf(StringUtils::hasText)

        if (StringUtils.hasText(keycloakUserId)) {
            val appUser = appUserStore.findById(keycloakUserId!!)
            if (appUser != null) {
                return appUser
            }
        }

        val preferredUsername = (claims["preferred_username"] as? String)?.takeIf(StringUtils::hasText)
        if (preferredUsername != null) {
            val appUser = appUserStore.findByNickname(preferredUsername)
            if (appUser != null) {
                return appUser
            }
        }

        val email = (claims["email"] as? String)?.takeIf(StringUtils::hasText)
        if (email != null) {
            val appUser = appUserStore.findByEmail(email)
            if (appUser != null) {
                return appUser
            }
        }

        if (keycloakUserId == null && preferredUsername == null && email == null) {
            throw RegistrationException(
                status = HttpStatus.UNAUTHORIZED,
                code = "INVALID_TOKEN",
                message = "Die Anmeldung enthält keine gültige Benutzerkennung"
            )
        }

        throw RegistrationException(
            status = HttpStatus.NOT_FOUND,
            code = "PROFILE_NOT_FOUND",
            message = "Profil konnte nicht geladen werden"
        )
    }

    private fun AppUser.toProfileResponse(): ProfileResponse =
        ProfileResponse(
            username = nickname,
            email = email,
            firstName = firstName,
            lastName = lastName
        )

    private fun usernameConflict(): RegistrationException =
        RegistrationException(
            status = HttpStatus.CONFLICT,
            code = "USERNAME_ALREADY_EXISTS",
            message = "Username existiert bereits",
            field = "username"
        )

    private fun String?.requireField(field: String, message: String): String {
        val value = this.normalizeOptional()
        if (!StringUtils.hasText(value)) {
            throw RegistrationException(
                status = HttpStatus.BAD_REQUEST,
                code = "FIELD_REQUIRED",
                message = message,
                field = field
            )
        }

        return value!!
    }

    private fun String?.normalizeOptional(): String? =
        this?.trim()?.takeIf(StringUtils::hasText)

    companion object {
        private const val NICKNAME_MIN_LENGTH = 3
        private const val NICKNAME_MAX_LENGTH = 255
    }
}
