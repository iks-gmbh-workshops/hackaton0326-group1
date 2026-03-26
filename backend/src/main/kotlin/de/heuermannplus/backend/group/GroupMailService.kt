package de.heuermannplus.backend.group

import org.springframework.mail.SimpleMailMessage
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.mail.javamail.MimeMessageHelper
import org.springframework.stereotype.Service

interface GroupMailService {
    fun sendKnownUserInvitation(invitation: GroupKnownUserInvitationMail)

    fun sendUnknownEmailInvitation(email: String, groupName: String, inviterName: String)
}

@Service
class SmtpGroupMailService(
    private val mailSender: JavaMailSender,
    private val groupProperties: GroupProperties
) : GroupMailService {

    override fun sendKnownUserInvitation(invitation: GroupKnownUserInvitationMail) {
        val mimeMessage = mailSender.createMimeMessage()
        val helper = MimeMessageHelper(mimeMessage, true, "UTF-8")
        helper.setFrom(groupProperties.emailFrom)
        helper.setTo(invitation.email)
        helper.setSubject("Neue Gruppeneinladung fuer ${invitation.groupName}")
        helper.setText(buildKnownUserInvitationText(invitation), buildKnownUserInvitationHtml(invitation))

        mailSender.send(mimeMessage)
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

    internal fun buildKnownUserInvitationText(invitation: GroupKnownUserInvitationMail): String {
        val groupDescription = invitation.groupDescription?.takeIf { it.isNotBlank() } ?: "Keine Beschreibung hinterlegt."
        val nextActivityBlock = invitation.nextActivity?.let { activity ->
            """
            Naechste Aktivitaet:
            ${activity.description}
            Ort: ${activity.location}
            Zeitpunkt: ${activity.scheduledAt}
            """.trimIndent()
        } ?: "Naechste Aktivitaet:\nAktuell ist keine anstehende Aktivitaet hinterlegt."

        return """
            Hallo ${invitation.inviteeName},

            ${invitation.inviterName} hat Dich zur Gruppe "${invitation.groupName}" eingeladen.

            Gruppenbeschreibung:
            $groupDescription

            $nextActivityBlock

            Einladung direkt annehmen:
            ${invitation.acceptUrl}

            Einladung direkt ablehnen:
            ${invitation.declineUrl}
            """.trimIndent()
    }

    internal fun buildKnownUserInvitationHtml(invitation: GroupKnownUserInvitationMail): String {
        val groupDescription = escapeHtml(invitation.groupDescription?.takeIf { it.isNotBlank() } ?: "Keine Beschreibung hinterlegt.")
        val nextActivityBlock = invitation.nextActivity?.let { activity ->
            """
            <div style="margin-top: 8px;">
              <p style="margin: 0 0 6px; color: #575756;">${escapeHtml(activity.description)}</p>
              <p style="margin: 0 0 4px; color: #575756;">Ort: ${escapeHtml(activity.location)}</p>
              <p style="margin: 0; color: #575756;">Zeitpunkt: ${escapeHtml(activity.scheduledAt.toString())}</p>
            </div>
            """.trimIndent()
        } ?: """
            <p style="margin: 8px 0 0; color: #575756;">
              Aktuell ist keine anstehende Aktivitaet hinterlegt.
            </p>
            """.trimIndent()

        return """
            <!DOCTYPE html>
            <html lang="de">
              <body style="margin: 0; background: #f6f2eb; font-family: 'Segoe UI', Roboto, Arial, sans-serif; color: #575756;">
                <div style="max-width: 640px; margin: 0 auto; padding: 32px 20px;">
                  <div style="background: #ffffff; border: 1px solid #d9d4cb; border-radius: 24px; padding: 32px;">
                    <p style="margin: 0 0 12px; color: #005578; font-size: 12px; font-weight: 700; letter-spacing: 0.08em; text-transform: uppercase;">
                      Gruppeneinladung
                    </p>
                    <h1 style="margin: 0 0 16px; color: #005578; font-family: 'Merriweather', Georgia, serif; font-size: 28px; font-style: italic; font-weight: 300;">
                      Einladung fuer ${escapeHtml(invitation.groupName)}
                    </h1>
                    <p style="margin: 0 0 20px; line-height: 1.6;">
                      Hallo ${escapeHtml(invitation.inviteeName)},
                    </p>
                    <p style="margin: 0 0 24px; line-height: 1.6;">
                      ${escapeHtml(invitation.inviterName)} hat Dich zur Gruppe "${escapeHtml(invitation.groupName)}" eingeladen.
                    </p>

                    <div style="margin-bottom: 20px; border: 1px solid #d9d4cb; border-radius: 18px; padding: 18px 20px; background: #fcfaf7;">
                      <p style="margin: 0 0 8px; font-size: 12px; font-weight: 700; color: #575756; text-transform: uppercase; letter-spacing: 0.04em;">
                        Gruppenbeschreibung
                      </p>
                      <p style="margin: 0; line-height: 1.6;">$groupDescription</p>
                    </div>

                    <div style="margin-bottom: 28px; border: 1px solid #d9d4cb; border-radius: 18px; padding: 18px 20px; background: #fcfaf7;">
                      <p style="margin: 0; font-size: 12px; font-weight: 700; color: #575756; text-transform: uppercase; letter-spacing: 0.04em;">
                        Naechste Aktivitaet
                      </p>
                      $nextActivityBlock
                    </div>

                    <table role="presentation" cellpadding="0" cellspacing="0" border="0" style="margin: 0 0 16px;">
                      <tr>
                        <td style="padding: 0 12px 12px 0;">
                          <a
                            href="${escapeHtml(invitation.acceptUrl)}"
                            style="display: inline-block; padding: 14px 22px; border-radius: 999px; background: #005578; color: #ffffff; font-weight: 600; text-decoration: none;"
                          >
                            Einladung annehmen
                          </a>
                        </td>
                        <td style="padding: 0 0 12px;">
                          <a
                            href="${escapeHtml(invitation.declineUrl)}"
                            style="display: inline-block; padding: 14px 22px; border-radius: 999px; background: #ffffff; border: 1px solid #005578; color: #005578; font-weight: 600; text-decoration: none;"
                          >
                            Einladung ablehnen
                          </a>
                        </td>
                      </tr>
                    </table>

                    <p style="margin: 0; color: #575756; font-size: 14px; line-height: 1.6;">
                      Falls Dein Mail-Client keine Buttons anzeigt, kannst Du auch diese Links verwenden:
                    </p>
                    <p style="margin: 10px 0 0; font-size: 14px; line-height: 1.7;">
                      <a href="${escapeHtml(invitation.acceptUrl)}" style="color: #005578;">Einladung annehmen</a><br />
                      <a href="${escapeHtml(invitation.declineUrl)}" style="color: #005578;">Einladung ablehnen</a>
                    </p>
                  </div>
                </div>
              </body>
            </html>
            """.trimIndent()
    }

    private fun escapeHtml(value: String): String =
        value
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&#39;")
}
