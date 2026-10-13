package com.campusprint.web

import com.campusprint.domain.AppUser
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
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile

@RestController
class PrintRequestController(private val requests: PrintRequestService) {

    /** STUDENT: submit a print job. multipart/form-data = the file plus a few text fields. */
    @PostMapping("/api/requests", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    @ResponseStatus(HttpStatus.CREATED)
    fun create(
        @AuthenticationPrincipal user: AppUser,
        @RequestParam("file") file: MultipartFile,
        @RequestParam("serviceId") serviceId: Long,
        @RequestParam("pages") pages: Int,
        @RequestParam("copies") copies: Int,
        @RequestParam(name = "notes", required = false) notes: String?,
    ): RequestDto = requests.create(user, file, serviceId, pages, copies, notes)

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

    /** STUDENT: cancel my own job while it is still pending. */
    @PatchMapping("/api/requests/{id}/cancel")
    fun cancel(@AuthenticationPrincipal user: AppUser, @PathVariable("id") id: Long): RequestDto =
        requests.cancel(user, id)

    /** Owner or ADMIN: download the uploaded file. */
    @GetMapping("/api/requests/{id}/file")
    fun file(@AuthenticationPrincipal user: AppUser, @PathVariable("id") id: Long): ResponseEntity<ByteArray> {
        val (request, stored) = requests.fileFor(user, id)
        return ResponseEntity.ok()
            .contentType(MediaType.APPLICATION_OCTET_STREAM)
            .header(
                HttpHeaders.CONTENT_DISPOSITION,
                ContentDisposition.attachment().filename(request.fileName).build().toString(),
            )
            .body(stored.data)
    }

    /** ADMIN: numbers for the dashboard strip. */
    @GetMapping("/api/admin/stats")
    fun stats(): StatsDto = requests.stats()
}
