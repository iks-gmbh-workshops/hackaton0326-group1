package de.heuermannplus.backend.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.annotation.Order
import org.springframework.beans.factory.annotation.Value
import org.springframework.security.config.Customizer
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.web.SecurityFilterChain
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.CorsConfigurationSource
import org.springframework.web.cors.UrlBasedCorsConfigurationSource

@Configuration
class SecurityConfig(
    @Value("\${app.frontend-base-url:http://localhost:3000}")
    private val frontendBaseUrl: String
) {

    @Bean
    fun corsConfigurationSource(): CorsConfigurationSource {
        val configuration = CorsConfiguration()
        configuration.allowedOrigins = listOf(frontendBaseUrl)
        configuration.allowedMethods = listOf("GET", "POST", "PUT", "PATCH", "DELETE", "HEAD", "OPTIONS")
        configuration.allowedHeaders = listOf("Authorization", "Content-Type", "Accept")
        configuration.allowCredentials = true

        return UrlBasedCorsConfigurationSource().apply {
            registerCorsConfiguration("/**", configuration)
        }
    }

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
            .cors(Customizer.withDefaults())
            .authorizeHttpRequests {
                it.anyRequest().permitAll()
            }
            .build()

    @Bean
    @Order(2)
    fun authenticatedSecurityFilterChain(http: HttpSecurity): SecurityFilterChain =
        http
            .csrf { it.disable() }
            .cors(Customizer.withDefaults())
            .authorizeHttpRequests {
                it.anyRequest()
                    .authenticated()
            }
            .oauth2ResourceServer {
                it.jwt(Customizer.withDefaults())
            }
            .build()
}
