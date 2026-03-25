package de.heuermannplus.backend.registration

import java.time.Instant
import org.springframework.stereotype.Component

interface RegistrationVerificationStore {
    fun save(verification: RegistrationVerification): RegistrationVerification

    fun findByTokenHash(tokenHash: String): RegistrationVerification?

    fun findExpiredPending(now: Instant): List<RegistrationVerification>
}

@Component
class JpaRegistrationVerificationStore(
    private val repository: RegistrationVerificationJpaRepository
) : RegistrationVerificationStore {

    override fun save(verification: RegistrationVerification): RegistrationVerification =
        repository.save(verification.toEntity()).toDomain()

    override fun findByTokenHash(tokenHash: String): RegistrationVerification? =
        repository.findByTokenHash(tokenHash)?.toDomain()

    override fun findExpiredPending(now: Instant): List<RegistrationVerification> =
        repository.findAllByStatusAndExpiresAtBefore(RegistrationVerificationStatus.PENDING, now)
            .map { entity -> entity.toDomain() }

    private fun RegistrationVerification.toEntity(): RegistrationVerificationEntity =
        RegistrationVerificationEntity(
            id = id,
            keycloakUserId = keycloakUserId,
            nickname = nickname,
            email = email,
            tokenHash = tokenHash,
            status = status,
            expiresAt = expiresAt,
            verifiedAt = verifiedAt,
            createdAt = createdAt ?: Instant.now()
        )

    private fun RegistrationVerificationEntity.toDomain(): RegistrationVerification =
        RegistrationVerification(
            id = id,
            keycloakUserId = keycloakUserId,
            nickname = nickname,
            email = email,
            tokenHash = tokenHash,
            status = status,
            expiresAt = expiresAt,
            verifiedAt = verifiedAt,
            createdAt = createdAt
        )
}
