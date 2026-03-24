package de.heuermannplus.backend.registration

import java.time.Instant

data class RegistrationPolicyResponse(
    val password: PasswordPolicyResponse,
    val captcha: CaptchaPolicyResponse
)

data class PasswordPolicyResponse(
    val minLength: Int,
    val minUpperCase: Int,
    val minLowerCase: Int,
    val minDigits: Int,
    val minSpecialChars: Int
)

data class CaptchaPolicyResponse(
    val mode: String,
    val mockPassToken: String?,
    val turnstileSiteKey: String?
)

data class RegistrationRequest(
    val nickname: String?,
    val password: String?,
    val passwordRepeat: String?,
    val email: String?,
    val captchaToken: String?,
    val firstName: String? = null,
    val lastName: String? = null
)

data class RegistrationAcceptedResponse(
    val message: String
)

data class RegistrationVerifyRequest(
    val token: String?
)

data class RegistrationVerifyResponse(
    val message: String
)

data class ApiErrorResponse(
    val code: String,
    val message: String,
    val field: String? = null,
    val suggestedNickname: String? = null
)

data class PasswordRequirementStatus(
    val code: String,
    val label: String,
    val satisfied: Boolean
)

data class PasswordEvaluation(
    val requirements: List<PasswordRequirementStatus>
)

enum class RegistrationVerificationStatus {
    PENDING,
    VERIFIED,
    EXPIRED
}

data class RegistrationVerification(
    val id: Long? = null,
    val keycloakUserId: String,
    val nickname: String,
    val email: String,
    val tokenHash: String,
    val status: RegistrationVerificationStatus,
    val expiresAt: Instant,
    val verifiedAt: Instant? = null,
    val createdAt: Instant? = null
)

data class RegistrationUserDraft(
    val username: String,
    val email: String,
    val firstName: String?,
    val lastName: String?,
    val password: String
)

data class KeycloakUserSummary(
    val id: String,
    val username: String,
    val email: String?,
    val enabled: Boolean,
    val emailVerified: Boolean
)
