package de.heuermannplus.backend.registration

import org.springframework.data.jpa.repository.JpaRepository

interface TermsConsentJpaRepository : JpaRepository<TermsConsentEntity, Long> {
    fun findAllByKeycloakUserIdOrderByConsentedAtAsc(keycloakUserId: String): List<TermsConsentEntity>

    fun deleteAllByKeycloakUserId(keycloakUserId: String)
}
