package de.heuermannplus.backend.registration

import org.springframework.util.LinkedMultiValueMap
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient

fun interface CaptchaVerifier {
    fun verify(token: String): Boolean
}

@Component
@ConditionalOnProperty(
    prefix = "app.registration.captcha",
    name = ["mode"],
    havingValue = "mock",
    matchIfMissing = true
)
class MockCaptchaVerifier(
    private val registrationProperties: RegistrationProperties
) : CaptchaVerifier {

    override fun verify(token: String): Boolean = token == registrationProperties.captcha.mockPassToken
}

@Component
@ConditionalOnProperty(
    prefix = "app.registration.captcha",
    name = ["mode"],
    havingValue = "turnstile"
)
class TurnstileCaptchaVerifier(
    private val registrationProperties: RegistrationProperties,
    restClientBuilder: RestClient.Builder
) : CaptchaVerifier {
    private val restClient = restClientBuilder.baseUrl("https://challenges.cloudflare.com").build()

    override fun verify(token: String): Boolean {
        if (registrationProperties.captcha.turnstileSecret.isBlank()) {
            return false
        }

        val response = restClient.post()
            .uri("/turnstile/v0/siteverify")
            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
            .body(
                LinkedMultiValueMap<String, String>().apply {
                    add("secret", registrationProperties.captcha.turnstileSecret)
                    add("response", token)
                }
            )
            .retrieve()
            .body(TurnstileVerificationResponse::class.java)

        return response?.success == true
    }
}

data class TurnstileVerificationResponse(
    val success: Boolean = false
)
