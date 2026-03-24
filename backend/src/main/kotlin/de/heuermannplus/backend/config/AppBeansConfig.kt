package de.heuermannplus.backend.config

import java.time.Clock
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.client.RestClient

@Configuration
class AppBeansConfig {

    @Bean
    fun clock(): Clock = Clock.systemUTC()

    @Bean
    fun restClientBuilder(): RestClient.Builder = RestClient.builder()
}
