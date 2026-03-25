package de.heuermannplus.backend.registration

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant

@Entity
@Table(name = "app_user")
class AppUserEntity(
    @Id
    @Column(name = "keycloak_user_id", nullable = false, length = 64)
    var keycloakUserId: String = "",
    @Column(nullable = false)
    var nickname: String = "",
    @Column(nullable = false)
    var email: String = "",
    @Column(name = "first_name")
    var firstName: String? = null,
    @Column(name = "last_name")
    var lastName: String? = null,
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var status: AppUserStatus = AppUserStatus.PENDING,
    @Column(nullable = false)
    var enabled: Boolean = false,
    @Column(name = "email_verified", nullable = false)
    var emailVerified: Boolean = false,
    @Column(name = "keycloak_role", nullable = false, length = 64)
    var keycloakRole: String = "",
    @Column(name = "created_at", nullable = false)
    var createdAt: Instant = Instant.EPOCH,
    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.EPOCH,
    @Column(name = "verified_at")
    var verifiedAt: Instant? = null,
    @Column(name = "deleted_at")
    var deletedAt: Instant? = null
)
