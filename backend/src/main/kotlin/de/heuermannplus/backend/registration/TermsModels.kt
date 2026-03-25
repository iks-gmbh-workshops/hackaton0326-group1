package de.heuermannplus.backend.registration

import java.time.Instant

enum class TermsConsentType {
    EXPLICIT_YES,
    EXPLICIT_NO,
    IMPLICIT_YES
}

data class TermsVersion(
    val id: Long? = null,
    val version: String,
    val contentSlug: String,
    val isActive: Boolean,
    val createdAt: Instant? = null
)

data class TermsConsent(
    val id: Long? = null,
    val keycloakUserId: String,
    val termsVersionId: Long,
    val consentType: TermsConsentType,
    val consentedAt: Instant
)
