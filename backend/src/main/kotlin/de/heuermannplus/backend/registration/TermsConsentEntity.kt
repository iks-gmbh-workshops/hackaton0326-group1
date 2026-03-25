package de.heuermannplus.backend.registration

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant

@Entity
@Table(name = "terms_consent")
class TermsConsentEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,
    @Column(name = "keycloak_user_id", nullable = false, length = 64)
    var keycloakUserId: String = "",
    @Column(name = "terms_version_id", nullable = false)
    var termsVersionId: Long = 0,
    @Enumerated(EnumType.STRING)
    @Column(name = "consent_type", nullable = false, length = 32)
    var consentType: TermsConsentType = TermsConsentType.EXPLICIT_YES,
    @Column(name = "consented_at", nullable = false)
    var consentedAt: Instant = Instant.EPOCH
)
