package de.heuermannplus.backend.group

import org.springframework.mail.SimpleMailMessage
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.stereotype.Service

interface GroupMailService {
    fun sendKnownUserInvitation(email: String, inviteeName: String, groupName: String, inviterName: String)

    fun sendUnknownEmailInvitation(email: String, groupName: String, inviterName: String)
}

@Service
class SmtpGroupMailService(
    private val mailSender: JavaMailSender,
    private val groupProperties: GroupProperties
) : GroupMailService {

    override fun sendKnownUserInvitation(email: String, inviteeName: String, groupName: String, inviterName: String) {
        val groupsUrl = "${groupProperties.frontendBaseUrl.trimEnd('/')}/groups"
        val message = SimpleMailMessage().apply {
            from = groupProperties.emailFrom
            setTo(email)
            subject = "Neue Gruppeneinladung für $groupName"
            text =
                """
                Hallo $inviteeName,

                $inviterName hat Dich zur Gruppe "$groupName" eingeladen.

                Melde Dich bei drumdibum an und öffne den Gruppenbereich:
                $groupsUrl
                """.trimIndent()
        }

        mailSender.send(message)
    }

    override fun sendUnknownEmailInvitation(email: String, groupName: String, inviterName: String) {
        val registerUrl = "${groupProperties.frontendBaseUrl.trimEnd('/')}/register"
        val message = SimpleMailMessage().apply {
            from = groupProperties.emailFrom
            setTo(email)
            subject = "Einladung zu drumdibum und zur Gruppe $groupName"
            text =
                """
                Hallo,

                $inviterName hat Dich zur Gruppe "$groupName" bei drumdibum eingeladen.

                Falls Du noch kein Konto hast, registriere Dich bitte hier:
                $registerUrl

                Nach der Anmeldung wird die Einladung Deinem Konto automatisch zugeordnet.
                """.trimIndent()
        }

        mailSender.send(message)
    }
}
