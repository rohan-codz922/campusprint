package com.campusprint.service

import org.springframework.http.HttpStatus

/**
 * Thrown anywhere in the app to send a clean JSON error like {"message": "..."} with a status code.
 * `code` is an optional machine-readable tag the website can react to (e.g. EMAIL_NOT_VERIFIED).
 */
class ApiException(
    val status: HttpStatus,
    override val message: String,
    val code: String? = null,
) : RuntimeException(message)
