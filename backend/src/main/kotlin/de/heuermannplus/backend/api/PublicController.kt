package de.heuermannplus.backend.api

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/public")
class PublicController {

    @GetMapping("/health")
    fun health(): Map<String, String> =
        mapOf(
            "status" to "UP",
            "application" to "heuermannplus-backend",
            "layer" to "public-api"
        )
}
