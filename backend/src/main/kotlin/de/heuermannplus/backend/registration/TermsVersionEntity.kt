package de.heuermannplus.backend.registration

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant

@Entity
@Table(name = "terms_version")
class TermsVersionEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,
    @Column(nullable = false, unique = true, length = 64)
    var version: String = "",
    @Column(name = "content_slug", nullable = false, unique = true, length = 128)
    var contentSlug: String = "",
    @Column(name = "is_active", nullable = false)
    var isActive: Boolean = false,
    @Column(name = "created_at", nullable = false)
    var createdAt: Instant = Instant.EPOCH
)
