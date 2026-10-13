package com.campusprint.web

import com.campusprint.domain.PaymentSettings
import com.campusprint.dto.PaymentSettingsDto
import com.campusprint.dto.PaymentSettingsRequest
import com.campusprint.repository.PaymentSettingsRepository
import jakarta.validation.Valid
import org.springframework.beans.factory.annotation.Value
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.time.Instant

/** The print shop's UPI details. Any signed-in user can read them (to pay); only ADMIN can change them. */
@RestController
@RequestMapping("/api/settings")
class SettingsController(
    private val repo: PaymentSettingsRepository,
    @Value("\${app.payment.required-pages:20}") private val requiredPages: Int,
) {
    @GetMapping("/payment")
    fun get(): PaymentSettingsDto {
        val s = repo.findById(1L).orElse(null)
        return PaymentSettingsDto(s != null, s?.payeeName ?: "", s?.upiId ?: "", requiredPages)
    }

    @PutMapping("/payment")
    fun save(@Valid @RequestBody body: PaymentSettingsRequest): PaymentSettingsDto {
        val s = repo.findById(1L).orElse(null) ?: PaymentSettings(payeeName = body.payeeName.trim(), upiId = body.upiId.trim())
        s.payeeName = body.payeeName.trim()
        s.upiId = body.upiId.trim()
        s.updatedAt = Instant.now()
        repo.save(s)
        return PaymentSettingsDto(true, s.payeeName, s.upiId, requiredPages)
    }
}
