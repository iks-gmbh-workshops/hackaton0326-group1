package de.heuermannplus.backend.group

import java.time.Duration
import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties("app.group")
data class GroupProperties(
    val frontendBaseUrl: String = "http://localhost:3000",
    val invitationTtl: Duration = Duration.ofHours(24),
    val emailFrom: String = "no-reply@heuermannplus.local"
)
