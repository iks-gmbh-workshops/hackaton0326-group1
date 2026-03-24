package de.heuermannplus.backend.api

import de.heuermannplus.backend.registration.RegistrationAcceptedResponse
import de.heuermannplus.backend.registration.RegistrationPolicyResponse
import de.heuermannplus.backend.registration.RegistrationRequest
import de.heuermannplus.backend.registration.RegistrationService
import de.heuermannplus.backend.registration.RegistrationVerifyRequest
import de.heuermannplus.backend.registration.RegistrationVerifyResponse
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/public/registration")
class RegistrationController(
    private val registrationService: RegistrationService
) {

    @GetMapping("/policy")
    fun policy(): RegistrationPolicyResponse = registrationService.policy()

    @PostMapping
    @ResponseStatus(HttpStatus.ACCEPTED)
    fun register(@RequestBody request: RegistrationRequest): RegistrationAcceptedResponse =
        registrationService.register(request)

    @PostMapping("/verify")
    fun verify(@RequestBody request: RegistrationVerifyRequest): RegistrationVerifyResponse =
        registrationService.verify(request)
}
