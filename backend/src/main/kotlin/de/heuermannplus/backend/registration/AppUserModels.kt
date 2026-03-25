package de.heuermannplus.backend.registration

import java.time.Instant

enum class AppUserStatus {
    PENDING,
    ACTIVE,
    EXPIRED,
    DELETED
}

data class AppUser(
    val keycloakUserId: String,
    val nickname: String,
    val email: String,
    val firstName: String?,
    val lastName: String?,
    val status: AppUserStatus,
    val enabled: Boolean,
    val emailVerified: Boolean,
    val keycloakRole: String,
    val createdAt: Instant? = null,
    val updatedAt: Instant? = null,
    val verifiedAt: Instant? = null,
    val deletedAt: Instant? = null
)
