package com.campusprint.dto

import com.campusprint.domain.AppUser
import com.campusprint.domain.PrintRequest
import com.campusprint.domain.PrintService
import com.campusprint.domain.Role
import jakarta.validation.constraints.DecimalMin
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import java.math.BigDecimal
import java.time.Instant

// DTOs (Data Transfer Objects) are the exact JSON shapes of the API.
// They keep internals such as passwordHash out of responses.

data class RegisterRequest(
    @field:NotBlank(message = "Name is required.")
    @field:Size(max = 100, message = "Name is too long.")
    val name: String,

    @field:NotBlank(message = "Email is required.")
    @field:Email(message = "Enter a valid email address.")
    val email: String,

    @field:NotBlank(message = "Password is required.")
    @field:Size(min = 6, max = 100, message = "Password must be at least 6 characters.")
    val password: String,

    @field:Size(max = 30, message = "Roll number is too long.")
    val rollNo: String? = null,
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

data class RequestDto(
    val id: Long,
    val fileName: String,
    val pages: Int,
    val copies: Int,
    val notes: String,
    val totalPrice: BigDecimal,
    val status: String,
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
    createdAt = createdAt,
    updatedAt = updatedAt,
    service = ServiceSummary(service.id!!, service.name, service.pricePerPage),
    student = StudentSummary(student.id!!, student.name, student.email, student.rollNo),
)
