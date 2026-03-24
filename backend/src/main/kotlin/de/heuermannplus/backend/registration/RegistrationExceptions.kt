package de.heuermannplus.backend.registration

import org.springframework.http.HttpStatus

class RegistrationException(
    val status: HttpStatus,
    val code: String,
    override val message: String,
    val field: String? = null,
    val suggestedNickname: String? = null
) : RuntimeException(message)

