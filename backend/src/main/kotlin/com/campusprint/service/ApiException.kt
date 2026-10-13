package com.campusprint.service

import org.springframework.http.HttpStatus

/** Thrown anywhere in the app to send a clean JSON error like {"message": "..."} with a status code. */
class ApiException(val status: HttpStatus, override val message: String) : RuntimeException(message)
