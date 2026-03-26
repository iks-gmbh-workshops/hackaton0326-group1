package de.heuermannplus.backend

import de.heuermannplus.backend.config.SecurityConfig
import kotlin.test.Test
import kotlin.test.BeforeTest
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import org.hamcrest.Matchers.containsString
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.springframework.http.HttpHeaders
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.oauth2.jwt.JwtDecoder
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.TestPropertySource
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig
import org.springframework.test.context.web.WebAppConfiguration
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.test.web.servlet.setup.DefaultMockMvcBuilder
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.header
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.web.context.WebApplicationContext
import org.springframework.web.servlet.config.annotation.EnableWebMvc
import jakarta.servlet.Filter
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@SpringJUnitConfig
@WebAppConfiguration
@ContextConfiguration(classes = [SecurityConfigTestConfiguration::class])
@TestPropertySource(properties = ["app.frontend-base-url=http://frontend.example"])
class SecurityConfigTest(
    @Autowired private val webApplicationContext: WebApplicationContext,
    @Autowired private val securityConfig: SecurityConfig,
    @Autowired private val springSecurityFilterChain: Filter
) {
    private lateinit var mockMvc: MockMvc

    @BeforeTest
    fun setUp() {
        val builder: DefaultMockMvcBuilder = MockMvcBuilders.webAppContextSetup(webApplicationContext)
        builder.addFilters<DefaultMockMvcBuilder>(springSecurityFilterChain)
        mockMvc = builder.build()
    }

    @Test
    fun `cors configuration source uses configured frontend origin and expected headers`() {
        val source = securityConfig.corsConfigurationSource()
        val configuration = source.getCorsConfiguration(
            org.springframework.mock.web.MockHttpServletRequest("GET", "/api/private/ping").apply {
                addHeader(HttpHeaders.ORIGIN, "http://frontend.example")
            }
        )

        assertNotNull(configuration)
        assertEquals(listOf("http://frontend.example"), configuration.allowedOrigins)
        assertTrue(configuration.allowedMethods!!.containsAll(listOf("GET", "POST", "PUT", "PATCH", "DELETE", "HEAD", "OPTIONS")))
        assertEquals(listOf("Authorization", "Content-Type", "Accept"), configuration.allowedHeaders)
        assertEquals(true, configuration.allowCredentials)
    }

    @Test
    fun `public endpoints are accessible without authentication`() {
        mockMvc.perform(get("/api/public/ping"))
            .andExpect(status().isOk)
    }

    @Test
    fun `private endpoints require authentication and accept jwt authenticated requests`() {
        mockMvc.perform(get("/api/private/ping"))
            .andExpect(status().isUnauthorized)

        mockMvc.perform(
            get("/api/private/ping")
                .with(jwt().jwt { it.subject("user-123") })
        )
            .andExpect(status().isOk)
    }

    @Test
    fun `preflight requests use cors configuration`() {
        mockMvc.perform(
            options("/api/private/ping")
                .header(HttpHeaders.ORIGIN, "http://frontend.example")
                .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET")
        )
            .andExpect(status().isOk)
            .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "http://frontend.example"))
            .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS, containsString("GET")))
    }
}

@Configuration
@EnableWebMvc
@EnableWebSecurity
@Import(SecurityConfig::class, SecurityConfigTestController::class)
private class SecurityConfigTestConfiguration {
    @Bean
    fun jwtDecoder(): JwtDecoder = JwtDecoder {
        throw UnsupportedOperationException("JWT decoding is not required in this test")
    }
}

@RestController
private class SecurityConfigTestController {
    @GetMapping("/api/public/ping")
    fun publicPing(): String = "public"

    @GetMapping("/api/private/ping")
    fun privatePing(): String = "private"
}
