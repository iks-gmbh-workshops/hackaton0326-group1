package de.heuermannplus.backend.registration

import java.time.Instant
import org.springframework.stereotype.Component

interface AppUserStore {
    fun save(user: AppUser): AppUser

    fun findById(keycloakUserId: String): AppUser?

    fun findByNickname(nickname: String): AppUser?

    fun findByEmail(email: String): AppUser?

    fun searchInviteSuggestions(query: String, excludedUserId: String, limit: Int): List<AppUser>
}

@Component
class JpaAppUserStore(
    private val repository: AppUserJpaRepository
) : AppUserStore {

    override fun save(user: AppUser): AppUser {
        val existing = repository.findById(user.keycloakUserId).orElse(null)
        val now = Instant.now()

        return repository.save(
            user.toEntity(
                createdAt = user.createdAt ?: existing?.createdAt ?: now,
                updatedAt = user.updatedAt ?: now
            )
        ).toDomain()
    }

    override fun findById(keycloakUserId: String): AppUser? =
        repository.findById(keycloakUserId).orElse(null)?.toDomain()

    override fun findByNickname(nickname: String): AppUser? =
        repository.findByNickname(nickname)?.toDomain()

    override fun findByEmail(email: String): AppUser? =
        repository.findByEmail(email)?.toDomain()

    override fun searchInviteSuggestions(query: String, excludedUserId: String, limit: Int): List<AppUser> =
        repository.searchInviteSuggestions(query.trim(), excludedUserId)
            .asSequence()
            .map { it.toDomain() }
            .take(limit)
            .toList()

    private fun AppUser.toEntity(createdAt: Instant, updatedAt: Instant): AppUserEntity =
        AppUserEntity(
            keycloakUserId = keycloakUserId,
            nickname = nickname,
            email = email,
            firstName = firstName,
            lastName = lastName,
            status = status,
            enabled = enabled,
            emailVerified = emailVerified,
            keycloakRole = keycloakRole,
            createdAt = createdAt,
            updatedAt = updatedAt,
            verifiedAt = verifiedAt,
            deletedAt = deletedAt
        )

    private fun AppUserEntity.toDomain(): AppUser =
        AppUser(
            keycloakUserId = keycloakUserId,
            nickname = nickname,
            email = email,
            firstName = firstName,
            lastName = lastName,
            status = status,
            enabled = enabled,
            emailVerified = emailVerified,
            keycloakRole = keycloakRole,
            createdAt = createdAt,
            updatedAt = updatedAt,
            verifiedAt = verifiedAt,
            deletedAt = deletedAt
        )
}
