package de.heuermannplus.backend.registration

import java.time.Instant
import org.springframework.data.jpa.repository.JpaRepository

interface RegistrationVerificationJpaRepository : JpaRepository<RegistrationVerificationEntity, Long> {
    fun findByTokenHash(tokenHash: String): RegistrationVerificationEntity?

    fun findAllByStatusAndExpiresAtBefore(
        status: RegistrationVerificationStatus,
        expiresAt: Instant
    ): List<RegistrationVerificationEntity>
}

