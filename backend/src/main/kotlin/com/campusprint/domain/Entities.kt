package com.campusprint.domain

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import java.math.BigDecimal
import java.time.Instant

enum class Role { STUDENT, ADMIN }

enum class RequestStatus { PENDING, PRINTING, READY, COLLECTED, REJECTED, CANCELLED }

/** A person who can sign in. The role decides what they may do. */
@Entity
@Table(name = "app_users") // "user" is a reserved word in PostgreSQL
class AppUser(
    @Column(nullable = false, length = 100)
    var name: String,

    @Column(nullable = false, unique = true)
    var email: String,

    /** BCrypt hash. The real password is never stored. */
    @Column(nullable = false)
    var passwordHash: String,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var role: Role,

    @Column(length = 30)
    var rollNo: String? = null,

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,
)

/** A print option on the price list, e.g. "Colour, A4" at Rs 6 per page. */
@Entity
@Table(name = "print_services")
class PrintService(
    @Column(nullable = false, length = 100)
    var name: String,

    @Column(length = 200)
    var description: String = "",

    @Column(nullable = false, precision = 8, scale = 2)
    var pricePerPage: BigDecimal,

    /** Inactive options are hidden from students but kept for old jobs. */
    @Column(nullable = false)
    var active: Boolean = true,

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,
)

/** One print job submitted by a student. */
@Entity
@Table(name = "print_requests")
class PrintRequest(
    @ManyToOne(optional = false)
    var student: AppUser,

    @ManyToOne(optional = false)
    var service: PrintService,

    /** Name the student's file had (shown in the UI). */
    @Column(nullable = false)
    var fileName: String,

    /** Id of the row in stored_files that holds the uploaded document's bytes. */
    @Column(nullable = false)
    var fileId: Long,

    @Column(nullable = false)
    var pages: Int,

    @Column(nullable = false)
    var copies: Int,

    @Column(length = 300)
    var notes: String = "",

    /** Calculated by the server: pricePerPage x pages x copies. */
    @Column(nullable = false, precision = 10, scale = 2)
    var totalPrice: BigDecimal,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var status: RequestStatus = RequestStatus.PENDING,

    @Column(nullable = false)
    var createdAt: Instant = Instant.now(),

    @Column(nullable = false)
    var updatedAt: Instant = Instant.now(),

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,
)

/**
 * The uploaded document itself, kept in the database (bytea in PostgreSQL).
 * It lives in its own table so listing print requests never loads file bytes.
 * Unlike files on a server disk, it survives redeploys and restarts.
 */
@Entity
@Table(name = "stored_files")
class StoredFile(
    @Column(nullable = false)
    var contentType: String,

    @JdbcTypeCode(SqlTypes.VARBINARY)
    @Column(nullable = false, length = 10_485_760)
    var data: ByteArray,

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,
)

/** One pending password-reset code per account. Only a hash of the 6-digit code is stored. */
@Entity
@Table(name = "password_resets")
class PasswordReset(
    @Column(nullable = false)
    var email: String,

    @Column(nullable = false)
    var codeHash: String,

    @Column(nullable = false)
    var expiresAt: Instant,

    /** Wrong guesses so far. After 5 the code is destroyed. */
    @Column(nullable = false)
    var attempts: Int = 0,

    @Column(nullable = false)
    var createdAt: Instant = Instant.now(),

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,
)
