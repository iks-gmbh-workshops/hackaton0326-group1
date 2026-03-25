package de.heuermannplus.backend.registration

import org.springframework.data.jpa.repository.JpaRepository

interface TermsVersionJpaRepository : JpaRepository<TermsVersionEntity, Long> {
    fun findAllByIsActiveTrue(): List<TermsVersionEntity>
}
