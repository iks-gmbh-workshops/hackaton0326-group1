package de.heuermannplus.backend.activity

import de.heuermannplus.backend.registration.ApiErrorResponse
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

class ActivityException(
    val status: HttpStatus,
    val code: String,
    override val message: String,
    val field: String? = null
) : RuntimeException(message)

@RestControllerAdvice
class ActivityExceptionHandler {

    @ExceptionHandler(ActivityException::class)
    fun handleActivityException(exception: ActivityException): ResponseEntity<ApiErrorResponse> =
        ResponseEntity.status(exception.status)
            .body(
                ApiErrorResponse(
                    code = exception.code,
                    message = exception.message,
                    field = exception.field
                )
            )
}
