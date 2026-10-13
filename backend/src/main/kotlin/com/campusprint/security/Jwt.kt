package com.campusprint.security

import com.campusprint.domain.AppUser
import com.campusprint.repository.UserRepository
import io.jsonwebtoken.JwtException
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.beans.factory.annotation.Value
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource
import org.springframework.stereotype.Component
import org.springframework.stereotype.Service
import org.springframework.web.filter.OncePerRequestFilter
import java.util.Date
import javax.crypto.SecretKey

/**
 * Creates and checks JSON Web Tokens (JWT).
 * A JWT is a signed string: header.payload.signature. The payload carries the user's email and role,
 * and the signature (HMAC-SHA256 with our secret) proves the server issued it and nobody edited it.
 */
@Service
class JwtService(
    @Value("\${app.jwt.secret}") secret: String,
    @Value("\${app.jwt.expiry-hours}") private val expiryHours: Long,
) {
    private val key: SecretKey = Keys.hmacShaKeyFor(secret.toByteArray(Charsets.UTF_8))

    fun generate(user: AppUser): String {
        val now = Date()
        return Jwts.builder()
            .subject(user.email)
            .claim("role", user.role.name)
            .issuedAt(now)
            .expiration(Date(now.time + expiryHours * 3_600_000))
            .signWith(key)
            .compact()
    }

    /** Returns the email inside a valid, unexpired token, or null if the token is bad. */
    fun parseEmail(token: String): String? =
        try {
            Jwts.parser().verifyWith(key).build().parseSignedClaims(token).payload.subject
        } catch (e: JwtException) {
            null
        } catch (e: IllegalArgumentException) {
            null
        }
}

/**
 * Runs once per request BEFORE the controllers.
 * It reads "Authorization: Bearer <token>", validates it, loads the user from the database
 * and puts them in Spring Security's context. If anything is wrong the request simply stays
 * anonymous and the rules in SecurityConfig reject it with 401.
 */
@Component
class JwtAuthFilter(
    private val jwt: JwtService,
    private val users: UserRepository,
) : OncePerRequestFilter() {

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        chain: FilterChain,
    ) {
        val header = request.getHeader("Authorization")
        if (header != null && header.startsWith("Bearer ") &&
            SecurityContextHolder.getContext().authentication == null
        ) {
            val email = jwt.parseEmail(header.substring(7))
            val user = email?.let { users.findByEmailIgnoreCase(it) }
            if (user != null) {
                val auth = UsernamePasswordAuthenticationToken(
                    user, // the principal: controllers receive this with @AuthenticationPrincipal
                    null,
                    listOf(SimpleGrantedAuthority("ROLE_${user.role.name}")),
                )
                auth.details = WebAuthenticationDetailsSource().buildDetails(request)
                SecurityContextHolder.getContext().authentication = auth
            }
        }
        chain.doFilter(request, response)
    }
}
