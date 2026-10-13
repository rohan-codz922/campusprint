package com.campusprint.service

import com.campusprint.domain.AppUser
import com.campusprint.domain.OtpCode
import com.campusprint.domain.OtpPurpose
import com.campusprint.repository.OtpCodeRepository
import org.springframework.http.HttpStatus
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.security.SecureRandom
import java.time.Instant

/** Issues and checks the 6-digit email codes used for email verification and password reset. */
@Service
class OtpService(
    private val codes: OtpCodeRepository,
    private val encoder: PasswordEncoder,
    private val mail: MailService,
) {
    private val random = SecureRandom()

    /** Sends a new code. Does nothing if one was already sent in the last minute. */
    @Transactional
    fun issue(user: AppUser, purpose: OtpPurpose) {
        val now = Instant.now()
        val existing = codes.findByEmailAndPurpose(user.email, purpose)
        if (existing != null && existing.createdAt.isAfter(now.minusSeconds(60))) return

        val code = "%06d".format(random.nextInt(1_000_000))
        val hash = requireNotNull(encoder.encode(code))
        if (existing != null) {
            existing.codeHash = hash
            existing.expiresAt = now.plusSeconds(600)
            existing.attempts = 0
            existing.createdAt = now
        } else {
            codes.save(OtpCode(email = user.email, purpose = purpose, codeHash = hash, expiresAt = now.plusSeconds(600), createdAt = now))
        }
        mail.sendCode(user.email, user.name, code, purpose)
    }

    /**
     * Checks a code. Throws if it is wrong, expired or used up; deletes it when correct.
     * noRollbackFor: the wrong-guess counter must still be saved when we throw.
     */
    @Transactional(noRollbackFor = [ApiException::class])
    fun verify(user: AppUser, purpose: OtpPurpose, code: String) {
        val invalid = ApiException(HttpStatus.BAD_REQUEST, "That code is incorrect or has expired.")
        val otp = codes.findByEmailAndPurpose(user.email, purpose) ?: throw invalid
        if (Instant.now().isAfter(otp.expiresAt) || otp.attempts >= 5) {
            codes.delete(otp)
            throw invalid
        }
        if (!encoder.matches(code, otp.codeHash)) {
            otp.attempts += 1
            throw invalid
        }
        codes.delete(otp)
    }
}
