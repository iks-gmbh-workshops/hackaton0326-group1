package de.heuermannplus.backend.group

import de.heuermannplus.backend.registration.AppUserStore
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken

fun JwtAuthenticationToken.toCurrentUser(appUserStore: AppUserStore): CurrentUser {
    val appUser = appUserStore.findById(token.subject)
    val nickname = appUser?.nickname
        ?: (token.claims["preferred_username"] as? String)
        ?: (token.claims["name"] as? String)
        ?: token.subject

    return CurrentUser(
        userId = token.subject,
        nickname = nickname,
        email = appUser?.email ?: token.claims["email"] as? String
    )
}
