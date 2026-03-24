package de.heuermannplus.backend.api

import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/private")
class PrivateController {

    @GetMapping("/me")
    fun me(authentication: JwtAuthenticationToken): Map<String, Any?> {
        val jwt = authentication.token

        return mapOf(
            "subject" to jwt.subject,
            "issuer" to jwt.issuer?.toString(),
            "email" to jwt.claims["email"],
            "name" to jwt.claims["name"],
            "preferredUsername" to jwt.claims["preferred_username"],
            "roles" to jwt.claims["realm_access"],
            "issuedAt" to jwt.issuedAt?.toString()
        )
    }
}
