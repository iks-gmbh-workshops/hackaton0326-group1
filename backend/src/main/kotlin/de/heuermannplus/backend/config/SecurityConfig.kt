package de.heuermannplus.backend.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.Customizer
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.web.SecurityFilterChain

@Configuration
class SecurityConfig {

    @Bean
    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain =
        http
            .csrf { it.disable() }
            .authorizeHttpRequests {
                it.requestMatchers("/actuator/health", "/api/public/**").permitAll()
                    .anyRequest()
                    .authenticated()
            }
            .oauth2ResourceServer {
                it.jwt(Customizer.withDefaults())
            }
            .build()
}
