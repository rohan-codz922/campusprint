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

/** NONE = pay at the counter. SUBMITTED = screenshot uploaded, waiting for the print desk to check it. */
enum class PaymentStatus { NONE, SUBMITTED, VERIFIED, REJECTED }

enum class OtpPurpose { VERIFY_EMAIL, RESET_PASSWORD }

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

    /** False until the person types the code we emailed them. Unverified accounts cannot sign in. */
    @Column(nullable = false)
    var emailVerified: Boolean = false,

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

    /** Id of the row in stored_files that holds the document's bytes. */
    @Column(nullable = false)
    var fileId: Long,

    /** Counted by the SERVER from the file itself. The student cannot choose it. */
    @Column(nullable = false)
    var pages: Int,

    @Column(nullable = false)
    var copies: Int,

    @Column(length = 300)
    var notes: String = "",

    /** Calculated by the server: pricePerPage x pages x copies. */
    @Column(nullable = false, precision = 10, scale = 2)
    var totalPrice: BigDecimal,

    /** True when pages x copies reached the limit, so payment must be verified before printing. */
    @Column(nullable = false)
    var paymentRequired: Boolean,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var paymentStatus: PaymentStatus = PaymentStatus.NONE,

    /** stored_files id of the payment screenshot, if one was uploaded. */
    @Column
    var paymentFileId: Long? = null,

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

/** An uploaded file (a document to print, or a payment screenshot), kept in the database. */
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

/** One pending 6-digit code per account and purpose. Only a hash of the code is stored. */
@Entity
@Table(name = "otp_codes")
class OtpCode(
    @Column(nullable = false)
    var email: String,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var purpose: OtpPurpose,

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

/** The print shop's UPI details, read from the QR code the admin uploads. A single row (id = 1). */
@Entity
@Table(name = "payment_settings")
class PaymentSettings(
    @Column(nullable = false, length = 100)
    var payeeName: String,

    @Column(nullable = false, length = 256)
    var upiId: String,

    @Column(nullable = false)
    var updatedAt: Instant = Instant.now(),

    @Id
    var id: Long = 1,
)
