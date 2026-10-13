package com.campusprint.service

import com.campusprint.domain.AppUser
import com.campusprint.domain.Role
import com.campusprint.dto.AuthResponse
import com.campusprint.dto.LoginRequest
import com.campusprint.dto.RegisterRequest
import com.campusprint.dto.toDto
import com.campusprint.repository.UserRepository
import com.campusprint.security.JwtService
import org.springframework.http.HttpStatus
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service

@Service
class AuthService(
    private val users: UserRepository,
    private val encoder: PasswordEncoder,
    private val jwt: JwtService,
) {
    /** Public sign-up always creates a STUDENT. Admins can never be created through the API. */
    fun register(req: RegisterRequest): AuthResponse {
        val email = req.email.trim().lowercase()
        if (users.existsByEmailIgnoreCase(email)) {
            throw ApiException(HttpStatus.CONFLICT, "An account with this email already exists.")
        }
        val user = users.save(
            AppUser(
                name = req.name.trim(),
                email = email,
                passwordHash = requireNotNull(encoder.encode(req.password)),
                role = Role.STUDENT,
                rollNo = req.rollNo?.trim()?.ifBlank { null },
            ),
        )
        return AuthResponse(jwt.generate(user), user.toDto())
    }

    fun login(req: LoginRequest): AuthResponse {
        val user = users.findByEmailIgnoreCase(req.email.trim())
        // Same message for "no such user" and "wrong password" so attackers cannot probe for emails.
        if (user == null || !encoder.matches(req.password, user.passwordHash)) {
            throw ApiException(HttpStatus.UNAUTHORIZED, "Email or password is incorrect.")
        }
        return AuthResponse(jwt.generate(user), user.toDto())
    }
}
