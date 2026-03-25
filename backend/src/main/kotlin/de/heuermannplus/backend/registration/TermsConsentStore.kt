package de.heuermannplus.backend.registration

import org.springframework.stereotype.Component

interface TermsConsentStore {
    fun save(consent: TermsConsent): TermsConsent

    fun findByKeycloakUserId(keycloakUserId: String): List<TermsConsent>

    fun deleteByKeycloakUserId(keycloakUserId: String)
}

@Component
class JpaTermsConsentStore(
    private val repository: TermsConsentJpaRepository
) : TermsConsentStore {

    override fun save(consent: TermsConsent): TermsConsent =
        repository.save(consent.toEntity()).toDomain()

    override fun findByKeycloakUserId(keycloakUserId: String): List<TermsConsent> =
        repository.findAllByKeycloakUserIdOrderByConsentedAtAsc(keycloakUserId)
            .map { it.toDomain() }

    override fun deleteByKeycloakUserId(keycloakUserId: String) {
        repository.deleteAllByKeycloakUserId(keycloakUserId)
    }

    private fun TermsConsent.toEntity(): TermsConsentEntity =
        TermsConsentEntity(
            id = id,
            keycloakUserId = keycloakUserId,
            termsVersionId = termsVersionId,
            consentType = consentType,
            consentedAt = consentedAt
        )

    private fun TermsConsentEntity.toDomain(): TermsConsent =
        TermsConsent(
            id = id,
            keycloakUserId = keycloakUserId,
            termsVersionId = termsVersionId,
            consentType = consentType,
            consentedAt = consentedAt
        )
}
