package com.campusprint.security

import com.campusprint.service.ApiException
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

/**
 * Limits how many requests one IP address can make per minute.
 * Stricter for login and for the endpoints that send email codes, so password guessing
 * and email-bombing get stopped early. Counters live in memory (fine for one server).
 */
@Component
class RateLimitFilter : OncePerRequestFilter() {

    private class Window(var start: Long, var count: Int)

    private val windows = HashMap<String, Window>()
    private val windowMs = 60_000L

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        chain: FilterChain,
    ) {
        // Browser pre-flight checks and non-API paths are not limited.
        if (request.method == "OPTIONS" || !request.requestURI.startsWith("/api/")) {
            chain.doFilter(request, response)
            return
        }
        val (group, limit) = rule(request)
        if (!allow("$group|${clientIp(request)}", limit)) {
            response.status = HttpStatus.TOO_MANY_REQUESTS.value()
            response.setHeader("Retry-After", "60")
            response.contentType = MediaType.APPLICATION_JSON_VALUE
            response.writer.write("""{"message":"Too many requests. Please wait a minute and try again."}""")
            return
        }
        chain.doFilter(request, response)
    }

    private fun rule(request: HttpServletRequest): Pair<String, Int> {
        val path = request.requestURI
        return when {
            path == "/api/auth/login" -> "login" to 10
            path.startsWith("/api/auth/") && path != "/api/auth/me" -> "codes" to 8
            request.method == "POST" && path == "/api/requests" -> "upload" to 10
            else -> "general" to 180
        }
    }

    private fun clientIp(request: HttpServletRequest): String =
        request.getHeader("X-Forwarded-For")?.substringBefore(',')?.trim()?.takeIf { it.isNotEmpty() }
            ?: request.remoteAddr

    @Synchronized
    private fun allow(key: String, limit: Int): Boolean {
        val now = System.currentTimeMillis()
        if (windows.size > 20_000) windows.entries.removeIf { now - it.value.start > windowMs }
        val w = windows[key]
        if (w == null || now - w.start >= windowMs) {
            windows[key] = Window(now, 1)
            return true
        }
        w.count += 1
        return w.count <= limit
    }
}

/**
 * Locks an account for 15 minutes after 5 wrong passwords, no matter which IP the guesses
 * come from. Success clears the counter.
 */
@Component
class LoginThrottle {
    private class Entry(var fails: Int, var first: Long)

    private val entries = HashMap<String, Entry>()
    private val maxFails = 5
    private val windowMs = 15 * 60 * 1000L

    @Synchronized
    fun check(email: String) {
        val e = entries[email] ?: return
        if (System.currentTimeMillis() - e.first > windowMs) {
            entries.remove(email)
        } else if (e.fails >= maxFails) {
            throw ApiException(HttpStatus.TOO_MANY_REQUESTS, "Too many wrong attempts. Try again in 15 minutes.")
        }
    }

    @Synchronized
    fun failed(email: String) {
        val now = System.currentTimeMillis()
        val e = entries[email]
        if (e == null || now - e.first > windowMs) entries[email] = Entry(1, now) else e.fails += 1
    }

    @Synchronized
    fun succeeded(email: String) {
        entries.remove(email)
    }
}
