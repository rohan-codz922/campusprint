package com.campusprint.service

import com.campusprint.domain.OtpPurpose
import com.campusprint.domain.Role
import com.campusprint.repository.UserRepository
import org.springframework.http.HttpStatus
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class PasswordResetService(
    private val users: UserRepository,
    private val otp: OtpService,
    private val encoder: PasswordEncoder,
) {
    /**
     * Step 1. Always returns normally, whether or not the email has an account,
     * so the form cannot be used to discover who is registered.
     */
    @Transactional
    fun request(rawEmail: String) {
        val user = users.findByEmailIgnoreCase(rawEmail.trim()) ?: return
        // The admin password is controlled from the server settings, not by email.
        if (user.role == Role.ADMIN) return
        otp.issue(user, OtpPurpose.RESET_PASSWORD)
    }

    /** Step 2. Checks the code and sets the new password. */
    @Transactional(noRollbackFor = [ApiException::class])
    fun confirm(rawEmail: String, code: String, newPassword: String) {
        val invalid = ApiException(HttpStatus.BAD_REQUEST, "That code is incorrect or has expired.")
        val user = users.findByEmailIgnoreCase(rawEmail.trim()) ?: throw invalid
        if (user.role == Role.ADMIN) throw invalid
        otp.verify(user, OtpPurpose.RESET_PASSWORD, code.trim())
        user.passwordHash = requireNotNull(encoder.encode(newPassword))
        user.emailVerified = true // they just proved they control this mailbox
        users.save(user)
    }
}
