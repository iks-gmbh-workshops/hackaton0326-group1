package de.heuermannplus.backend.registration

import org.springframework.mail.SimpleMailMessage
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.stereotype.Service

interface RegistrationMailService {
    fun sendVerificationEmail(email: String, nickname: String, token: String)
}

@Service
class SmtpRegistrationMailService(
    private val mailSender: JavaMailSender,
    private val registrationProperties: RegistrationProperties
) : RegistrationMailService {

    override fun sendVerificationEmail(email: String, nickname: String, token: String) {
        val verifyUrl = "${registrationProperties.frontendBaseUrl.trimEnd('/')}/register/verify?token=$token"

        val message = SimpleMailMessage().apply {
            from = registrationProperties.emailFrom
            setTo(email)
            subject = "Bitte bestätige deine Registrierung"
            text =
                """
                Hallo $nickname,

                bitte bestätige deine Registrierung für drumdibum über folgenden Link:
                $verifyUrl

                Der Link ist 24 Stunden gültig.
                """.trimIndent()
        }

        mailSender.send(message)
    }
}
