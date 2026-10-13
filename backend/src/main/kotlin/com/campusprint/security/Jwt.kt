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
import java.security.MessageDigest
import java.util.Date
import javax.crypto.SecretKey

data class TokenData(val email: String, val passwordFingerprint: String?)

/**
 * Creates and checks JSON Web Tokens (JWT): header.payload.signature.
 * The token carries the user's email, role, and a "password fingerprint". When a user's
 * password changes, the fingerprint changes, so every older token stops working immediately.
 */
@Service
class JwtService(
    @Value("\${app.jwt.secret}") secret: String,
    @Value("\${app.jwt.expiry-hours}") private val expiryHours: Long,
) {
    private val key: SecretKey

    init {
        val bytes = secret.toByteArray(Charsets.UTF_8)
        // Fail fast: a short secret would make tokens easy to forge.
        require(bytes.size >= 32) { "JWT_SECRET must be at least 32 characters long." }
        key = Keys.hmacShaKeyFor(bytes)
    }

    fun fingerprint(user: AppUser): String =
        MessageDigest.getInstance("SHA-256")
            .digest(user.passwordHash.toByteArray(Charsets.UTF_8))
            .take(8)
            .joinToString("") { "%02x".format(it) }

    fun generate(user: AppUser): String {
        val now = Date()
        return Jwts.builder()
            .subject(user.email)
            .claim("role", user.role.name)
            .claim("pv", fingerprint(user))
            .issuedAt(now)
            .expiration(Date(now.time + expiryHours * 3_600_000))
            .signWith(key)
            .compact()
    }

    /** Returns the data inside a valid, unexpired token, or null if the token is bad. */
    fun parse(token: String): TokenData? =
        try {
            val claims = Jwts.parser().verifyWith(key).build().parseSignedClaims(token).payload
            TokenData(claims.subject, claims.get("pv", String::class.java))
        } catch (e: JwtException) {
            null
        } catch (e: IllegalArgumentException) {
            null
        }
}

/**
 * Runs once per request BEFORE the controllers. Reads "Authorization: Bearer <token>",
 * validates it, loads the user, and puts them in Spring Security's context.
 * If anything is wrong the request stays anonymous and gets a 401 from SecurityConfig.
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
            val data = jwt.parse(header.substring(7))
            val user = data?.let { users.findByEmailIgnoreCase(it.email) }
            if (data != null && user != null && user.emailVerified &&
                data.passwordFingerprint == jwt.fingerprint(user)
            ) {
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
