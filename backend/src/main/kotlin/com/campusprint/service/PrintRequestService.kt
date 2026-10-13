package com.campusprint.service

import com.campusprint.domain.AppUser
import com.campusprint.domain.PaymentStatus
import com.campusprint.domain.PrintRequest
import com.campusprint.domain.RequestStatus
import com.campusprint.domain.Role
import com.campusprint.domain.StoredFile
import com.campusprint.dto.RequestDto
import com.campusprint.dto.StatsDto
import com.campusprint.dto.toDto
import com.campusprint.repository.PrintRequestRepository
import com.campusprint.repository.PrintServiceRepository
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.multipart.MultipartFile
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Instant

@Service
class PrintRequestService(
    private val requests: PrintRequestRepository,
    private val services: PrintServiceRepository,
    private val storage: FileStorageService,
    @Value("\${app.payment.required-pages:20}") private val requiredPages: Int,
) {

    @Transactional
    fun create(
        student: AppUser,
        file: MultipartFile,
        serviceId: Long,
        copies: Int,
        notes: String?,
        proof: MultipartFile?,
    ): RequestDto {
        if (copies !in 1..100) throw ApiException(HttpStatus.BAD_REQUEST, "Copies must be between 1 and 100.")

        val service = services.findById(serviceId).orElse(null)
        if (service == null || !service.active) {
            throw ApiException(HttpStatus.BAD_REQUEST, "Choose a print option.")
        }

        // The document is inspected here: type checked and pages counted by the server.
        val doc = storage.storeDocument(file)
        val sheets = doc.pages * copies

        // Big jobs must be paid BEFORE the print desk starts, so nobody can walk away from them.
        val paymentRequired = sheets >= requiredPages
        val proofFile = proof?.takeIf { !it.isEmpty }
        if (paymentRequired && proofFile == null) {
            throw ApiException(
                HttpStatus.BAD_REQUEST,
                "Jobs of $requiredPages or more pages must be paid first. Attach your payment screenshot.",
            )
        }
        val proofId = proofFile?.let { storage.storeScreenshot(it) }

        // The price is ALWAYS calculated here, from the server's own page count.
        val total = service.pricePerPage
            .multiply(BigDecimal(sheets))
            .setScale(2, RoundingMode.HALF_UP)

        val saved = requests.save(
            PrintRequest(
                student = student,
                service = service,
                fileName = cleanName(file.originalFilename),
                fileId = doc.fileId,
                pages = doc.pages,
                copies = copies,
                notes = (notes ?: "").trim().take(300),
                totalPrice = total,
                paymentRequired = paymentRequired,
                paymentStatus = if (proofId != null) PaymentStatus.SUBMITTED else PaymentStatus.NONE,
                paymentFileId = proofId,
            ),
        )
        return saved.toDto()
    }

    @Transactional(readOnly = true)
    fun mine(student: AppUser): List<RequestDto> =
        requests.findByStudentIdOrderByIdDesc(student.id!!).map { it.toDto() }

    @Transactional(readOnly = true)
    fun all(): List<RequestDto> = requests.findAllByOrderByIdDesc().map { it.toDto() }

    /** Admin only (enforced in SecurityConfig). */
    @Transactional
    fun updateStatus(id: Long, statusText: String): RequestDto {
        val status = runCatching { RequestStatus.valueOf(statusText.trim().uppercase()) }.getOrNull()
            ?: throw ApiException(HttpStatus.BAD_REQUEST, "Unknown status.")
        if (status == RequestStatus.CANCELLED) {
            throw ApiException(HttpStatus.BAD_REQUEST, "Only the student can cancel a job.")
        }
        val request = find(id)
        if (request.status == RequestStatus.CANCELLED) {
            throw ApiException(HttpStatus.CONFLICT, "This job was cancelled by the student.")
        }
        if (request.paymentStatus == PaymentStatus.REJECTED && status != RequestStatus.REJECTED) {
            throw ApiException(HttpStatus.CONFLICT, "This job's payment was rejected.")
        }
        val startsPrinting = status == RequestStatus.PRINTING || status == RequestStatus.READY || status == RequestStatus.COLLECTED
        if (startsPrinting && request.paymentRequired && request.paymentStatus != PaymentStatus.VERIFIED) {
            throw ApiException(HttpStatus.CONFLICT, "Check and verify the payment screenshot before printing this job.")
        }
        request.status = status
        request.updatedAt = Instant.now()
        return request.toDto()
    }

    /** Admin only: accept or reject the payment screenshot. Rejecting also rejects the job. */
    @Transactional
    fun decidePayment(id: Long, decisionText: String): RequestDto {
        val decision = when (decisionText.trim().uppercase()) {
            "VERIFIED" -> PaymentStatus.VERIFIED
            "REJECTED" -> PaymentStatus.REJECTED
            else -> throw ApiException(HttpStatus.BAD_REQUEST, "Decision must be VERIFIED or REJECTED.")
        }
        val request = find(id)
        if (request.paymentStatus != PaymentStatus.SUBMITTED) {
            throw ApiException(HttpStatus.CONFLICT, "There is no payment waiting to be checked for this job.")
        }
        request.paymentStatus = decision
        if (decision == PaymentStatus.REJECTED) request.status = RequestStatus.REJECTED
        request.updatedAt = Instant.now()
        return request.toDto()
    }

    /** A student may cancel only their OWN job, only before printing starts, and only if they have not paid online. */
    @Transactional
    fun cancel(student: AppUser, id: Long): RequestDto {
        val request = find(id)
        if (request.student.id != student.id) {
            throw ApiException(HttpStatus.NOT_FOUND, "Print job not found.") // 404, not 403: do not reveal it exists
        }
        if (request.status != RequestStatus.PENDING || request.paymentStatus != PaymentStatus.NONE) {
            throw ApiException(HttpStatus.CONFLICT, "This job can no longer be cancelled. Please ask the print desk.")
        }
        request.status = RequestStatus.CANCELLED
        request.updatedAt = Instant.now()
        return request.toDto()
    }

    /** The owner or an admin may download the document. */
    @Transactional(readOnly = true)
    fun fileFor(user: AppUser, id: Long): Pair<PrintRequest, StoredFile> {
        val request = owned(user, id)
        return request to storage.load(request.fileId)
    }

    /** The owner or an admin may view the payment screenshot. */
    @Transactional(readOnly = true)
    fun proofFor(user: AppUser, id: Long): StoredFile {
        val request = owned(user, id)
        val proofId = request.paymentFileId ?: throw ApiException(HttpStatus.NOT_FOUND, "No payment screenshot for this job.")
        return storage.load(proofId)
    }

    @Transactional(readOnly = true)
    fun stats(): StatsDto {
        val all = requests.findAll()
        val finished = all.filter { it.status == RequestStatus.READY || it.status == RequestStatus.COLLECTED }
        return StatsDto(
            totalRequests = all.size.toLong(),
            byStatus = all.groupingBy { it.status.name }.eachCount().mapValues { it.value.toLong() },
            pagesPrinted = finished.sumOf { (it.pages * it.copies).toLong() },
            revenue = all.filter { it.status == RequestStatus.COLLECTED }
                .fold(BigDecimal.ZERO) { sum, r -> sum.add(r.totalPrice) },
            paymentsToVerify = all.count { it.paymentStatus == PaymentStatus.SUBMITTED }.toLong(),
        )
    }

    private fun owned(user: AppUser, id: Long): PrintRequest {
        val request = find(id)
        if (user.role != Role.ADMIN && request.student.id != user.id) {
            throw ApiException(HttpStatus.NOT_FOUND, "Print job not found.")
        }
        return request
    }

    private fun find(id: Long): PrintRequest =
        requests.findById(id).orElseThrow { ApiException(HttpStatus.NOT_FOUND, "Print job not found.") }

    /** Keeps only the file's own name (no folders) and a safe length. */
    private fun cleanName(raw: String?): String =
        (raw ?: "document").substringAfterLast('/').substringAfterLast('\\').trim().ifBlank { "document" }.take(150)
}
