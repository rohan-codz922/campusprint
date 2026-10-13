package com.campusprint.service

import com.campusprint.domain.AppUser
import com.campusprint.domain.OtpPurpose
import com.campusprint.domain.Role
import com.campusprint.dto.AuthResponse
import com.campusprint.dto.LoginRequest
import com.campusprint.dto.RegisterRequest
import com.campusprint.dto.VerifyEmailRequest
import com.campusprint.dto.toDto
import com.campusprint.repository.UserRepository
import com.campusprint.security.JwtService
import com.campusprint.security.LoginThrottle
import org.springframework.http.HttpStatus
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class AuthService(
    private val users: UserRepository,
    private val encoder: PasswordEncoder,
    private val jwt: JwtService,
    private val otp: OtpService,
    private val throttle: LoginThrottle,
) {
    // Used to burn the same time as a real password check when the email does not exist,
    // so response time does not reveal which emails have accounts.
    private val dummyHash: String by lazy { requireNotNull(encoder.encode("not-a-real-password-1")) }

    /**
     * Public sign-up always creates a STUDENT, and the account stays locked until the email
     * is verified. Re-registering an UNVERIFIED email just replaces it and sends a new code,
     * so a mistyped address never blocks the real owner.
     */
    @Transactional
    fun register(req: RegisterRequest) {
        val email = req.email.trim().lowercase()
        val existing = users.findByEmailIgnoreCase(email)
        if (existing != null && existing.emailVerified) {
            throw ApiException(HttpStatus.CONFLICT, "An account with this email already exists.")
        }
        val hash = requireNotNull(encoder.encode(req.password))
        val user = existing?.also {
            it.name = req.name.trim()
            it.passwordHash = hash
            it.rollNo = req.rollNo?.trim()?.ifBlank { null }
        } ?: AppUser(
            name = req.name.trim(),
            email = email,
            passwordHash = hash,
            role = Role.STUDENT,
            emailVerified = false,
            rollNo = req.rollNo?.trim()?.ifBlank { null },
        )
        users.save(user)
        otp.issue(user, OtpPurpose.VERIFY_EMAIL)
    }

    @Transactional(noRollbackFor = [ApiException::class])
    fun verifyEmail(req: VerifyEmailRequest): AuthResponse {
        val invalid = ApiException(HttpStatus.BAD_REQUEST, "That code is incorrect or has expired.")
        val user = users.findByEmailIgnoreCase(req.email.trim()) ?: throw invalid
        if (user.emailVerified) throw invalid
        otp.verify(user, OtpPurpose.VERIFY_EMAIL, req.code.trim())
        user.emailVerified = true
        users.save(user)
        return AuthResponse(jwt.generate(user), user.toDto())
    }

    /** Replies the same whether or not the email exists. */
    @Transactional
    fun resendCode(rawEmail: String) {
        val user = users.findByEmailIgnoreCase(rawEmail.trim()) ?: return
        if (!user.emailVerified) otp.issue(user, OtpPurpose.VERIFY_EMAIL)
    }

    fun login(req: LoginRequest): AuthResponse {
        val email = req.email.trim().lowercase()
        throttle.check(email)
        val user = users.findByEmailIgnoreCase(email)
        val passwordOk = encoder.matches(req.password, user?.passwordHash ?: dummyHash)
        // Same message for "no such user" and "wrong password" so attackers cannot probe for emails.
        if (user == null || !passwordOk) {
            throttle.failed(email)
            throw ApiException(HttpStatus.UNAUTHORIZED, "Email or password is incorrect.")
        }
        if (!user.emailVerified) {
            throw ApiException(HttpStatus.FORBIDDEN, "Please verify your email to continue.", "EMAIL_NOT_VERIFIED")
        }
        throttle.succeeded(email)
        return AuthResponse(jwt.generate(user), user.toDto())
    }
}
