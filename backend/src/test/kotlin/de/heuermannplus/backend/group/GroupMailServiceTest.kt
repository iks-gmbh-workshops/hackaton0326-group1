package de.heuermannplus.backend.group

import jakarta.mail.Multipart
import jakarta.mail.Part
import jakarta.mail.Session
import jakarta.mail.internet.MimeMessage
import java.time.Duration
import java.time.Instant
import java.util.Properties
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.springframework.mail.SimpleMailMessage
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.mail.javamail.MimeMessagePreparator

class GroupMailServiceTest {

    @Test
    fun `known user invitation is sent as multipart mail with html buttons and text fallback`() {
        val mailSender = RecordingJavaMailSender()
        val service = SmtpGroupMailService(
            mailSender = mailSender,
            groupProperties = GroupProperties(
                frontendBaseUrl = "http://localhost:3000",
                invitationTtl = Duration.ofHours(24),
                emailFrom = "no-reply@heuermannplus.local"
            )
        )

        service.sendKnownUserInvitation(
            GroupKnownUserInvitationMail(
                email = "alice@example.org",
                inviteeName = "Alice",
                groupName = "Band 1",
                groupDescription = "Probegruppe",
                inviterName = "Bob",
                acceptUrl = "http://localhost:3000/group-invitations/respond?token=abc&decision=accept",
                declineUrl = "http://localhost:3000/group-invitations/respond?token=abc&decision=decline",
                nextActivity = GroupInvitationMailNextActivity(
                    description = "Naechste Probe",
                    location = "Studio",
                    scheduledAt = Instant.parse("2026-03-26T18:30:00Z")
                )
            )
        )

        val sentMessage = mailSender.mimeMessages.single()
        val multipart = sentMessage.content as Multipart
        val textBody = findBodyContent(multipart, "text/plain")
        val htmlBody = findBodyContent(multipart, "text/html")

        assertTrue(textBody.contains("Einladung direkt annehmen"))
        assertTrue(textBody.contains("Einladung direkt ablehnen"))
        assertTrue(htmlBody.contains("Einladung annehmen"))
        assertTrue(htmlBody.contains("Einladung ablehnen"))
        assertTrue(htmlBody.contains("href=\"http://localhost:3000/group-invitations/respond?token=abc&amp;decision=accept\""))
    }

    private fun findBodyContent(multipart: Multipart, mimeType: String): String {
        for (index in 0 until multipart.count) {
            val bodyPart = multipart.getBodyPart(index)
            if (bodyPart.isMimeType(mimeType)) {
                return bodyPart.content.toString()
            }
            if (bodyPart.isMimeType("multipart/*")) {
                return findBodyContent(bodyPart.content as Multipart, mimeType)
            }
        }
        error("No part found for mime type $mimeType")
    }
}

private class RecordingJavaMailSender : JavaMailSender {
    val mimeMessages = mutableListOf<MimeMessage>()
    val simpleMessages = mutableListOf<SimpleMailMessage>()

    override fun createMimeMessage(): MimeMessage = MimeMessage(Session.getInstance(Properties()))

    override fun createMimeMessage(contentStream: java.io.InputStream): MimeMessage =
        MimeMessage(Session.getInstance(Properties()), contentStream)

    override fun send(mimeMessage: MimeMessage) {
        mimeMessage.saveChanges()
        mimeMessages += mimeMessage
    }

    override fun send(vararg mimeMessages: MimeMessage) {
        mimeMessages.forEach(::send)
    }

    override fun send(mimeMessagePreparator: MimeMessagePreparator) {
        val mimeMessage = createMimeMessage()
        mimeMessagePreparator.prepare(mimeMessage)
        send(mimeMessage)
    }

    override fun send(vararg mimeMessagePreparators: MimeMessagePreparator) {
        mimeMessagePreparators.forEach(::send)
    }

    override fun send(simpleMessage: SimpleMailMessage) {
        simpleMessages += simpleMessage
    }

    override fun send(vararg simpleMessages: SimpleMailMessage) {
        this.simpleMessages += simpleMessages
    }
}
