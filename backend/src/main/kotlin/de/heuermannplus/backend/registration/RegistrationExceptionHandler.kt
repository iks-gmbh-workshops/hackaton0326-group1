package de.heuermannplus.backend.registration

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice
class RegistrationExceptionHandler {

    @ExceptionHandler(RegistrationException::class)
    fun handleRegistrationException(exception: RegistrationException): ResponseEntity<ApiErrorResponse> =
        ResponseEntity.status(exception.status)
            .body(
                ApiErrorResponse(
                    code = exception.code,
                    message = exception.message,
                    field = exception.field,
                    suggestedNickname = exception.suggestedNickname
                )
            )

    @ExceptionHandler(IllegalArgumentException::class)
    fun handleIllegalArgumentException(exception: IllegalArgumentException): ResponseEntity<ApiErrorResponse> =
        ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(
                ApiErrorResponse(
                    code = "BAD_REQUEST",
                    message = exception.message ?: "Ungültige Anfrage"
                )
            )
}
