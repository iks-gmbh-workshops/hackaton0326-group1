package de.heuermannplus.backend.registration

import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
class RegistrationCleanupJob(
    private val registrationService: RegistrationService
) {

    @Scheduled(cron = "\${app.registration.cleanup-cron}")
    fun cleanupExpiredRegistrations() {
        registrationService.cleanupExpiredPendingRegistrations()
    }
}
