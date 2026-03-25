package de.heuermannplus.backend.registration

import java.time.Duration
import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties("app.registration")
data class RegistrationProperties(
    val frontendBaseUrl: String = "http://localhost:3000",
    val verificationTtl: Duration = Duration.ofHours(24),
    val cleanupCron: String = "0 0 * * * *",
    val emailFrom: String = "no-reply@heuermannplus.local",
    val password: PasswordPolicyProperties = PasswordPolicyProperties(),
    val captcha: CaptchaProperties = CaptchaProperties()
)

data class PasswordPolicyProperties(
    val minLength: Int = 8,
    val minUpperCase: Int = 1,
    val minLowerCase: Int = 1,
    val minDigits: Int = 1,
    val minSpecialChars: Int = 1
)

enum class CaptchaMode {
    MOCK,
    TURNSTILE
}

data class CaptchaProperties(
    val mode: CaptchaMode = CaptchaMode.MOCK,
    val mockPassToken: String = "test-pass",
    val turnstileSiteKey: String = "",
    val turnstileSecret: String = ""
)
