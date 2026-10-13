package com.campusprint.config

import com.campusprint.security.JwtAuthFilter
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.security.config.Customizer
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.CorsConfigurationSource
import org.springframework.web.cors.UrlBasedCorsConfigurationSource

@Configuration
@EnableWebSecurity
class SecurityConfig(
    private val jwtFilter: JwtAuthFilter,
    @Value("\${app.cors-origins}") private val corsOrigins: String,
) {

    /** The rulebook: which URL needs which role. First matching rule wins. */
    @Bean
    fun filterChain(http: HttpSecurity): SecurityFilterChain {
        http
            .csrf { it.disable() } // not needed: we use tokens in a header, not cookies
            .cors(Customizer.withDefaults())
            .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
            .headers { h ->
                h.contentSecurityPolicy { it.policyDirectives("default-src 'none'; frame-ancestors 'none'") }
                h.referrerPolicy { it.policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.NO_REFERRER) }
            }
            .authorizeHttpRequests {
                it
                    .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                    .requestMatchers(
                        "/api/auth/register",
                        "/api/auth/login",
                        "/api/auth/verify-email",
                        "/api/auth/resend-code",
                        "/api/auth/forgot-password",
                        "/api/auth/reset-password",
                    ).permitAll()
                    .requestMatchers(HttpMethod.GET, "/api/services").permitAll()
                    .requestMatchers("/api/services/**").hasRole("ADMIN")
                    .requestMatchers("/api/admin/**").hasRole("ADMIN")
                    .requestMatchers(HttpMethod.PUT, "/api/settings/payment").hasRole("ADMIN")
                    .requestMatchers(HttpMethod.POST, "/api/requests").hasRole("STUDENT")
                    .requestMatchers(HttpMethod.GET, "/api/requests").hasRole("ADMIN")
                    .requestMatchers(HttpMethod.PATCH, "/api/requests/*/status").hasRole("ADMIN")
                    .requestMatchers(HttpMethod.PATCH, "/api/requests/*/payment").hasRole("ADMIN")
                    .anyRequest().authenticated() // everything else needs a valid login
            }
            .exceptionHandling {
                // 401 = not signed in (or token invalid). 403 = signed in but wrong role.
                it.authenticationEntryPoint { _, response, _ ->
                    response.status = 401
                    response.contentType = MediaType.APPLICATION_JSON_VALUE
                    response.writer.write("""{"message":"Please sign in to continue."}""")
                }
                it.accessDeniedHandler { _, response, _ ->
                    response.status = 403
                    response.contentType = MediaType.APPLICATION_JSON_VALUE
                    response.writer.write("""{"message":"You do not have permission to do this."}""")
                }
            }
            .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter::class.java)
        return http.build()
    }

    /** BCrypt: a slow, salted one-way hash designed for passwords. */
    @Bean
    fun passwordEncoder(): PasswordEncoder = BCryptPasswordEncoder()

    /** Lets only OUR website (a different origin) call this API from the browser. */
    @Bean
    fun corsConfigurationSource(): CorsConfigurationSource {
        val config = CorsConfiguration()
        config.allowedOriginPatterns = corsOrigins.split(",").map { it.trim() }.filter { it.isNotEmpty() }
        config.allowedMethods = listOf("GET", "POST", "PUT", "PATCH", "OPTIONS")
        config.allowedHeaders = listOf("Authorization", "Content-Type")
        config.maxAge = 3600
        val source = UrlBasedCorsConfigurationSource()
        source.registerCorsConfiguration("/**", config)
        return source
    }
}
