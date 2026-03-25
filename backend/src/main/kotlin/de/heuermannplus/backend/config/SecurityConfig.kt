package de.heuermannplus.backend.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.annotation.Order
import org.springframework.security.config.Customizer
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.web.SecurityFilterChain

@Configuration
class SecurityConfig {

    @Bean
    @Order(1)
    fun publicSecurityFilterChain(http: HttpSecurity): SecurityFilterChain =
        http
            .securityMatcher(
                "/actuator/health",
                "/api/public",
                "/api/public/",
                "/api/public/**",
                "/api/public/registration",
                "/api/public/registration/verify"
            )
            .csrf { it.disable() }
            .authorizeHttpRequests {
                it.anyRequest().permitAll()
            }
            .build()

    @Bean
    @Order(2)
    fun authenticatedSecurityFilterChain(http: HttpSecurity): SecurityFilterChain =
        http
            .csrf { it.disable() }
            .authorizeHttpRequests {
                it.anyRequest()
                    .authenticated()
            }
            .oauth2ResourceServer {
                it.jwt(Customizer.withDefaults())
            }
            .build()
}
