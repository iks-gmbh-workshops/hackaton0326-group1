package de.heuermannplus.backend.api

import org.springframework.http.HttpStatus
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken
import org.springframework.web.server.ResponseStatusException

fun JwtAuthenticationToken.requiredSubject(): String =
    token.getClaimAsString("sub")
        ?: throw ResponseStatusException(
            HttpStatus.UNAUTHORIZED,
            "JWT enthaelt keinen Subject-Claim"
        )
