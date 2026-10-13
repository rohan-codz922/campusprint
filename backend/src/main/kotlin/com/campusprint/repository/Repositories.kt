package com.campusprint.repository

import com.campusprint.domain.AppUser
import com.campusprint.domain.OtpCode
import com.campusprint.domain.OtpPurpose
import com.campusprint.domain.PaymentSettings
import com.campusprint.domain.PrintRequest
import com.campusprint.domain.PrintService
import com.campusprint.domain.Role
import com.campusprint.domain.StoredFile
import org.springframework.data.jpa.repository.JpaRepository

// Spring Data JPA writes the SQL for these from the method names. No SQL needed.

interface UserRepository : JpaRepository<AppUser, Long> {
    fun findByEmailIgnoreCase(email: String): AppUser?
    fun existsByRole(role: Role): Boolean
    fun findFirstByRole(role: Role): AppUser?
}

interface PrintServiceRepository : JpaRepository<PrintService, Long>

interface PrintRequestRepository : JpaRepository<PrintRequest, Long> {
    fun findByStudentIdOrderByIdDesc(studentId: Long): List<PrintRequest>
    fun findAllByOrderByIdDesc(): List<PrintRequest>
}

interface StoredFileRepository : JpaRepository<StoredFile, Long>

interface OtpCodeRepository : JpaRepository<OtpCode, Long> {
    fun findByEmailAndPurpose(email: String, purpose: OtpPurpose): OtpCode?
}

interface PaymentSettingsRepository : JpaRepository<PaymentSettings, Long>
