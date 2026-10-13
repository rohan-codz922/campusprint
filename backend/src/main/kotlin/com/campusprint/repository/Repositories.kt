package com.campusprint.repository

import com.campusprint.domain.AppUser
import com.campusprint.domain.PrintRequest
import com.campusprint.domain.PrintService
import com.campusprint.domain.Role
import com.campusprint.domain.StoredFile
import org.springframework.data.jpa.repository.JpaRepository

// Spring Data JPA writes the SQL for these from the method names. No SQL needed.

interface UserRepository : JpaRepository<AppUser, Long> {
    fun findByEmailIgnoreCase(email: String): AppUser?
    fun existsByEmailIgnoreCase(email: String): Boolean
    fun existsByRole(role: Role): Boolean
}

interface PrintServiceRepository : JpaRepository<PrintService, Long>

interface PrintRequestRepository : JpaRepository<PrintRequest, Long> {
    fun findByStudentIdOrderByIdDesc(studentId: Long): List<PrintRequest>
    fun findAllByOrderByIdDesc(): List<PrintRequest>
}

interface StoredFileRepository : JpaRepository<StoredFile, Long>
