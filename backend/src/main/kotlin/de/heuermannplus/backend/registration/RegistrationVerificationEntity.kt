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
@Table(name = "registration_verification")
class RegistrationVerificationEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,
    @Column(name = "keycloak_user_id", nullable = false)
    var keycloakUserId: String = "",
    @Column(nullable = false)
    var nickname: String = "",
    @Column(nullable = false)
    var email: String = "",
    @Column(name = "token_hash", nullable = false, unique = true, length = 64)
    var tokenHash: String = "",
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var status: RegistrationVerificationStatus = RegistrationVerificationStatus.PENDING,
    @Column(name = "expires_at", nullable = false)
    var expiresAt: Instant = Instant.EPOCH,
    @Column(name = "verified_at")
    var verifiedAt: Instant? = null,
    @Column(name = "created_at", nullable = false)
    var createdAt: Instant = Instant.EPOCH
)
