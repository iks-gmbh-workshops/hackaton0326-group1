package de.heuermannplus.backend.api

import de.heuermannplus.backend.registration.AppUserStatus

data class PrivateUserProfileResponse(
    val subject: String,
    val nickname: String,
    val email: String,
    val firstName: String?,
    val lastName: String?,
    val status: AppUserStatus,
    val enabled: Boolean,
    val emailVerified: Boolean,
    val keycloakRole: String
)
