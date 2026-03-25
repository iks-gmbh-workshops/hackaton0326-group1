package de.heuermannplus.backend.registration

import java.time.Instant
import org.springframework.stereotype.Component

interface TermsVersionStore {
    fun findCurrent(): TermsVersion?

    fun findActive(): List<TermsVersion>
}

@Component
class JpaTermsVersionStore(
    private val repository: TermsVersionJpaRepository
) : TermsVersionStore {

    override fun findCurrent(): TermsVersion? =
        repository.findAllByIsActiveTrue().singleOrNull()?.toDomain()

    override fun findActive(): List<TermsVersion> =
        repository.findAllByIsActiveTrue().map { it.toDomain() }

    private fun TermsVersionEntity.toDomain(): TermsVersion =
        TermsVersion(
            id = id,
            version = version,
            contentSlug = contentSlug,
            isActive = isActive,
            createdAt = createdAt
        )
}
