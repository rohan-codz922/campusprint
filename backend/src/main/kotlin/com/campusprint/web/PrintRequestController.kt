package com.campusprint.web

import com.campusprint.domain.AppUser
import com.campusprint.domain.StoredFile
import com.campusprint.dto.PaymentDecision
import com.campusprint.dto.RequestDto
import com.campusprint.dto.StatsDto
import com.campusprint.dto.StatusUpdate
import com.campusprint.service.PrintRequestService
import jakarta.validation.Valid
import org.springframework.http.ContentDisposition
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile

@RestController
class PrintRequestController(private val requests: PrintRequestService) {

    /** STUDENT: submit a print job. multipart/form-data = the file, a few text fields, and an optional payment screenshot. */
    @PostMapping("/api/requests", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    @ResponseStatus(HttpStatus.CREATED)
    fun create(
        @AuthenticationPrincipal user: AppUser,
        @RequestParam("file") file: MultipartFile,
        @RequestParam("serviceId") serviceId: Long,
        @RequestParam("copies") copies: Int,
        @RequestParam(name = "notes", required = false) notes: String?,
        @RequestParam(name = "paymentProof", required = false) paymentProof: MultipartFile?,
    ): RequestDto = requests.create(user, file, serviceId, copies, notes, paymentProof)

    /** STUDENT: my own jobs only. */
    @GetMapping("/api/requests/mine")
    fun mine(@AuthenticationPrincipal user: AppUser): List<RequestDto> = requests.mine(user)

    /** ADMIN: every job in the queue. */
    @GetMapping("/api/requests")
    fun all(): List<RequestDto> = requests.all()

    /** ADMIN: move a job along (printing, ready, collected, rejected). */
    @PatchMapping("/api/requests/{id}/status")
    fun updateStatus(
        @PathVariable("id") id: Long,
        @Valid @RequestBody body: StatusUpdate,
    ): RequestDto = requests.updateStatus(id, body.status)

    /** ADMIN: accept or reject the payment screenshot. */
    @PatchMapping("/api/requests/{id}/payment")
    fun decidePayment(
        @PathVariable("id") id: Long,
        @Valid @RequestBody body: PaymentDecision,
    ): RequestDto = requests.decidePayment(id, body.decision)

    /** STUDENT: cancel my own job while it is still pending and unpaid. */
    @PatchMapping("/api/requests/{id}/cancel")
    fun cancel(@AuthenticationPrincipal user: AppUser, @PathVariable("id") id: Long): RequestDto =
        requests.cancel(user, id)

    /** Owner or ADMIN: download the uploaded document. */
    @GetMapping("/api/requests/{id}/file")
    fun file(@AuthenticationPrincipal user: AppUser, @PathVariable("id") id: Long): ResponseEntity<ByteArray> {
        val (request, stored) = requests.fileFor(user, id)
        return download(stored, request.fileName)
    }

    /** Owner or ADMIN: view the payment screenshot. */
    @GetMapping("/api/requests/{id}/payment-proof")
    fun paymentProof(@AuthenticationPrincipal user: AppUser, @PathVariable("id") id: Long): ResponseEntity<ByteArray> =
        download(requests.proofFor(user, id), "payment-proof")

    /** ADMIN: numbers for the dashboard strip. */
    @GetMapping("/api/admin/stats")
    fun stats(): StatsDto = requests.stats()

    private fun download(stored: StoredFile, name: String): ResponseEntity<ByteArray> =
        ResponseEntity.ok()
            .contentType(MediaType.parseMediaType(stored.contentType))
            .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(name).build().toString())
            .header("X-Content-Type-Options", "nosniff")
            .body(stored.data)
}
