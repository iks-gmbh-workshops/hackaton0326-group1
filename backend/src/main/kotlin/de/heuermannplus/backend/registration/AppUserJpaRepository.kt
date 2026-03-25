package de.heuermannplus.backend.registration

import org.springframework.data.jpa.repository.JpaRepository

interface AppUserJpaRepository : JpaRepository<AppUserEntity, String> {
    fun findByNickname(nickname: String): AppUserEntity?

    fun findByEmail(email: String): AppUserEntity?
}
