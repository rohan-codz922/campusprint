package com.campusprint.dto

import com.campusprint.domain.AppUser
import com.campusprint.domain.PrintRequest
import com.campusprint.domain.PrintService
import com.campusprint.domain.Role
import jakarta.validation.constraints.DecimalMin
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size
import java.math.BigDecimal
import java.time.Instant

// DTOs (Data Transfer Objects) are the exact JSON shapes of the API.
// They keep internals such as passwordHash out of responses.

private const val PASSWORD_RULE = "^(?=.*[A-Za-z])(?=.*\\d).+$"
private const val PASSWORD_MSG = "Password needs 8 to 72 characters with at least one letter and one number."

data class RegisterRequest(
    @field:NotBlank(message = "Name is required.")
    @field:Size(max = 100, message = "Name is too long.")
    val name: String,

    @field:NotBlank(message = "Email is required.")
    @field:Email(message = "Enter a valid email address.")
    val email: String,

    @field:Size(min = 8, max = 72, message = PASSWORD_MSG)
    @field:Pattern(regexp = PASSWORD_RULE, message = PASSWORD_MSG)
    val password: String,

    @field:Size(max = 30, message = "Roll number is too long.")
    val rollNo: String? = null,
)

data class VerifyEmailRequest(
    @field:NotBlank(message = "Email is required.") val email: String,
    @field:Pattern(regexp = "\\d{6}", message = "Enter the 6-digit code from your email.") val code: String,
)

data class EmailOnlyRequest(
    @field:NotBlank(message = "Email is required.")
    @field:Email(message = "Enter a valid email address.")
    val email: String,
)

data class ResetPasswordRequest(
    @field:NotBlank(message = "Email is required.") val email: String,

    @field:Pattern(regexp = "\\d{6}", message = "Enter the 6-digit code from your email.")
    val code: String,

    @field:Size(min = 8, max = 72, message = PASSWORD_MSG)
    @field:Pattern(regexp = PASSWORD_RULE, message = PASSWORD_MSG)
    val newPassword: String,
)

data class LoginRequest(
    @field:NotBlank(message = "Email is required.") val email: String,
    @field:NotBlank(message = "Password is required.") val password: String,
)

data class UserDto(val id: Long, val name: String, val email: String, val role: Role, val rollNo: String?)

data class AuthResponse(val token: String, val user: UserDto)

data class ServiceRequest(
    @field:NotBlank(message = "Name is required.")
    @field:Size(max = 100, message = "Name is too long.")
    val name: String,

    @field:Size(max = 200, message = "Description is too long.")
    val description: String = "",

    @field:DecimalMin(value = "0.01", message = "Price per page must be above zero.")
    val pricePerPage: BigDecimal,

    val active: Boolean = true,
)

data class ServiceDto(
    val id: Long,
    val name: String,
    val description: String,
    val pricePerPage: BigDecimal,
    val active: Boolean,
)

data class ServiceSummary(val id: Long, val name: String, val pricePerPage: BigDecimal)

data class StudentSummary(val id: Long, val name: String, val email: String, val rollNo: String?)

data class StatusUpdate(@field:NotBlank(message = "Status is required.") val status: String)

data class PaymentDecision(@field:NotBlank(message = "Decision is required.") val decision: String)

data class PaymentSettingsRequest(
    @field:Pattern(regexp = "^[\\p{L}0-9 .&'\\-]{1,100}$", message = "Shop name can use letters, numbers and spaces only.")
    val payeeName: String,

    @field:Pattern(regexp = "^[A-Za-z0-9.\\-_]{2,256}@[A-Za-z0-9]{2,64}$", message = "Enter a valid UPI ID, like shop@bank.")
    val upiId: String,
)

data class PaymentSettingsDto(
    val configured: Boolean,
    val payeeName: String,
    val upiId: String,
    val requiredPages: Int,
)

data class RequestDto(
    val id: Long,
    val fileName: String,
    val pages: Int,
    val copies: Int,
    val notes: String,
    val totalPrice: BigDecimal,
    val status: String,
    val paymentRequired: Boolean,
    val paymentStatus: String,
    val hasPaymentProof: Boolean,
    val createdAt: Instant,
    val updatedAt: Instant,
    val service: ServiceSummary,
    val student: StudentSummary,
)

data class StatsDto(
    val totalRequests: Long,
    val byStatus: Map<String, Long>,
    val pagesPrinted: Long,
    val revenue: BigDecimal,
    val paymentsToVerify: Long,
)

// ---- Entity -> DTO mappers ----

fun AppUser.toDto() = UserDto(id!!, name, email, role, rollNo)

fun PrintService.toDto() = ServiceDto(id!!, name, description, pricePerPage, active)

fun PrintRequest.toDto() = RequestDto(
    id = id!!,
    fileName = fileName,
    pages = pages,
    copies = copies,
    notes = notes,
    totalPrice = totalPrice,
    status = status.name,
    paymentRequired = paymentRequired,
    paymentStatus = paymentStatus.name,
    hasPaymentProof = paymentFileId != null,
    createdAt = createdAt,
    updatedAt = updatedAt,
    service = ServiceSummary(service.id!!, service.name, service.pricePerPage),
    student = StudentSummary(student.id!!, student.name, student.email, student.rollNo),
)
