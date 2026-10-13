package com.campusprint.service

import com.campusprint.domain.PasswordReset
import com.campusprint.domain.Role
import com.campusprint.repository.PasswordResetRepository
import com.campusprint.repository.UserRepository
import org.springframework.http.HttpStatus
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.security.SecureRandom
import java.time.Instant

@Service
class PasswordResetService(
    private val users: UserRepository,
    private val resets: PasswordResetRepository,
    private val encoder: PasswordEncoder,
    private val mail: MailService,
) {
    private val random = SecureRandom()

    /**
     * Step 1. Always returns normally, whether or not the email has an account,
     * so the form cannot be used to discover who is registered.
     */
    @Transactional
    fun request(rawEmail: String) {
        val user = users.findByEmailIgnoreCase(rawEmail.trim()) ?: return
        // The admin password is controlled from the server settings, not by email.
        if (user.role == Role.ADMIN) return

        val existing = resets.findByEmail(user.email)
        // At most one new code per minute for the same account.
        if (existing != null && existing.createdAt.isAfter(Instant.now().minusSeconds(60))) return

        val code = "%06d".format(random.nextInt(1_000_000))
        val now = Instant.now()
        val hash = requireNotNull(encoder.encode(code))
        if (existing != null) {
            existing.codeHash = hash
            existing.expiresAt = now.plusSeconds(600)
            existing.attempts = 0
            existing.createdAt = now
        } else {
            resets.save(PasswordReset(email = user.email, codeHash = hash, expiresAt = now.plusSeconds(600), createdAt = now))
        }
        mail.sendResetCode(user.email, user.name, code)
    }

    /**
     * Step 2. Checks the code and sets the new password.
     * noRollbackFor: the wrong-guess counter must still be saved when we throw.
     */
    @Transactional(noRollbackFor = [ApiException::class])
    fun confirm(rawEmail: String, code: String, newPassword: String) {
        val invalid = ApiException(HttpStatus.BAD_REQUEST, "That code is incorrect or has expired.")

        val user = users.findByEmailIgnoreCase(rawEmail.trim()) ?: throw invalid
        if (user.role == Role.ADMIN) throw invalid
        val reset = resets.findByEmail(user.email) ?: throw invalid

        if (Instant.now().isAfter(reset.expiresAt) || reset.attempts >= 5) {
            resets.delete(reset)
            throw invalid
        }
        if (!encoder.matches(code.trim(), reset.codeHash)) {
            reset.attempts += 1
            throw invalid
        }

        user.passwordHash = requireNotNull(encoder.encode(newPassword))
        users.save(user)
        resets.delete(reset)
    }
}
