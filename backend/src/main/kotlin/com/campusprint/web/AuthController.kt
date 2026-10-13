package com.campusprint.web

import com.campusprint.domain.AppUser
import com.campusprint.dto.AuthResponse
import com.campusprint.dto.ForgotPasswordRequest
import com.campusprint.dto.LoginRequest
import com.campusprint.dto.RegisterRequest
import com.campusprint.dto.ResetPasswordRequest
import com.campusprint.dto.UserDto
import com.campusprint.dto.toDto
import com.campusprint.service.AuthService
import com.campusprint.service.PasswordResetService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/auth")
class AuthController(
    private val auth: AuthService,
    private val passwordReset: PasswordResetService,
) {

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    fun register(@Valid @RequestBody body: RegisterRequest): AuthResponse = auth.register(body)

    @PostMapping("/login")
    fun login(@Valid @RequestBody body: LoginRequest): AuthResponse = auth.login(body)

    /** Step 1 of "forgot password": emails a 6-digit code. The reply is identical whether or not the email exists. */
    @PostMapping("/forgot-password")
    fun forgotPassword(@Valid @RequestBody body: ForgotPasswordRequest): Map<String, String> {
        passwordReset.request(body.email)
        return mapOf("message" to "If that email has an account, we have sent a 6-digit code. It expires in 10 minutes.")
    }

    /** Step 2: checks the code and sets the new password. */
    @PostMapping("/reset-password")
    fun resetPassword(@Valid @RequestBody body: ResetPasswordRequest): Map<String, String> {
        passwordReset.confirm(body.email, body.code, body.newPassword)
        return mapOf("message" to "Password changed. You can sign in now.")
    }

    /** Who am I? The frontend calls this on page load to restore the session from the saved token. */
    @GetMapping("/me")
    fun me(@AuthenticationPrincipal user: AppUser): UserDto = user.toDto()
}
