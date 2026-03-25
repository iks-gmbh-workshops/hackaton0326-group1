package de.heuermannplus.backend.registration

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface AppUserJpaRepository : JpaRepository<AppUserEntity, String> {
    fun findByNickname(nickname: String): AppUserEntity?

    fun findByEmail(email: String): AppUserEntity?

    @Query(
        """
        select user
        from AppUserEntity user
        where user.status = de.heuermannplus.backend.registration.AppUserStatus.ACTIVE
          and user.deletedAt is null
          and lower(user.keycloakUserId) <> lower(:excludedUserId)
          and (
            :query = ''
            or lower(user.nickname) like lower(concat('%', :query, '%'))
            or lower(user.email) like lower(concat('%', :query, '%'))
          )
        order by lower(user.nickname) asc, lower(user.email) asc
        """
    )
    fun searchInviteSuggestions(
        @Param("query") query: String,
        @Param("excludedUserId") excludedUserId: String
    ): List<AppUserEntity>
}
