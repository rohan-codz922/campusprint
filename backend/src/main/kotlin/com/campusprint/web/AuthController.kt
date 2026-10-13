package com.campusprint.web

import com.campusprint.domain.AppUser
import com.campusprint.dto.AuthResponse
import com.campusprint.dto.LoginRequest
import com.campusprint.dto.RegisterRequest
import com.campusprint.dto.UserDto
import com.campusprint.dto.toDto
import com.campusprint.service.AuthService
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
class AuthController(private val auth: AuthService) {

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    fun register(@Valid @RequestBody body: RegisterRequest): AuthResponse = auth.register(body)

    @PostMapping("/login")
    fun login(@Valid @RequestBody body: LoginRequest): AuthResponse = auth.login(body)

    /** Who am I? The frontend calls this on page load to restore the session from the saved token. */
    @GetMapping("/me")
    fun me(@AuthenticationPrincipal user: AppUser): UserDto = user.toDto()
}
