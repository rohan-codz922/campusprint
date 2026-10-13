package com.campusprint.web

import com.campusprint.domain.PrintService
import com.campusprint.dto.ServiceDto
import com.campusprint.dto.ServiceRequest
import com.campusprint.dto.toDto
import com.campusprint.repository.PrintServiceRepository
import com.campusprint.service.ApiException
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

/** CRUD for the price list. GET is public; create/update need the ADMIN role (see SecurityConfig). */
@RestController
@RequestMapping("/api/services")
class ServiceController(private val repo: PrintServiceRepository) {

    @GetMapping
    fun list(): List<ServiceDto> = repo.findAll().sortedBy { it.id }.map { it.toDto() }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun create(@Valid @RequestBody body: ServiceRequest): ServiceDto =
        repo.save(
            PrintService(
                name = body.name.trim(),
                description = body.description.trim(),
                pricePerPage = body.pricePerPage,
                active = body.active,
            ),
        ).toDto()

    @PutMapping("/{id}")
    fun update(@PathVariable("id") id: Long, @Valid @RequestBody body: ServiceRequest): ServiceDto {
        val service = repo.findById(id).orElseThrow { ApiException(HttpStatus.NOT_FOUND, "Service not found.") }
        service.name = body.name.trim()
        service.description = body.description.trim()
        service.pricePerPage = body.pricePerPage
        service.active = body.active
        return repo.save(service).toDto()
    }
}
