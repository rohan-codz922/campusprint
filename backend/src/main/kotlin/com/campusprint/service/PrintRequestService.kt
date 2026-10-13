package com.campusprint.service

import com.campusprint.domain.AppUser
import com.campusprint.domain.PrintRequest
import com.campusprint.domain.RequestStatus
import com.campusprint.domain.Role
import com.campusprint.dto.RequestDto
import com.campusprint.dto.StatsDto
import com.campusprint.dto.toDto
import com.campusprint.repository.PrintRequestRepository
import com.campusprint.repository.PrintServiceRepository
import org.springframework.core.io.Resource
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
) {

    @Transactional
    fun create(
        student: AppUser,
        file: MultipartFile,
        serviceId: Long,
        pages: Int,
        copies: Int,
        notes: String?,
    ): RequestDto {
        if (pages !in 1..500) throw ApiException(HttpStatus.BAD_REQUEST, "Pages must be between 1 and 500.")
        if (copies !in 1..100) throw ApiException(HttpStatus.BAD_REQUEST, "Copies must be between 1 and 100.")

        val service = services.findById(serviceId).orElse(null)
        if (service == null || !service.active) {
            throw ApiException(HttpStatus.BAD_REQUEST, "Choose a print option.")
        }

        val storedName = storage.store(file)

        // The price is ALWAYS calculated here. The browser's estimate is only a preview,
        // so a student cannot send a fake total.
        val total = service.pricePerPage
            .multiply(BigDecimal(pages).multiply(BigDecimal(copies)))
            .setScale(2, RoundingMode.HALF_UP)

        val saved = requests.save(
            PrintRequest(
                student = student,
                service = service,
                fileName = (file.originalFilename ?: "document").take(255),
                storedFileName = storedName,
                pages = pages,
                copies = copies,
                notes = (notes ?: "").trim().take(300),
                totalPrice = total,
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
        request.status = status
        request.updatedAt = Instant.now()
        return request.toDto()
    }

    /** A student may cancel only their OWN job, and only before printing starts. */
    @Transactional
    fun cancel(student: AppUser, id: Long): RequestDto {
        val request = find(id)
        if (request.student.id != student.id) {
            throw ApiException(HttpStatus.NOT_FOUND, "Print job not found.") // 404, not 403: do not reveal it exists
        }
        if (request.status != RequestStatus.PENDING) {
            throw ApiException(HttpStatus.CONFLICT, "Only jobs that have not started printing can be cancelled.")
        }
        request.status = RequestStatus.CANCELLED
        request.updatedAt = Instant.now()
        return request.toDto()
    }

    /** The owner or an admin may download the file. */
    @Transactional(readOnly = true)
    fun fileFor(user: AppUser, id: Long): Pair<PrintRequest, Resource> {
        val request = find(id)
        if (user.role != Role.ADMIN && request.student.id != user.id) {
            throw ApiException(HttpStatus.NOT_FOUND, "Print job not found.")
        }
        return request to storage.load(request.storedFileName)
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
        )
    }

    private fun find(id: Long): PrintRequest =
        requests.findById(id).orElseThrow { ApiException(HttpStatus.NOT_FOUND, "Print job not found.") }
}
