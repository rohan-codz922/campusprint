package com.campusprint.web

import com.campusprint.service.ApiException
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.MissingServletRequestParameterException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException
import org.springframework.web.multipart.MaxUploadSizeExceededException
import org.springframework.web.multipart.support.MissingServletRequestPartException

/** One place that turns exceptions into the {"message": "..."} JSON the frontend shows to the user. */
@RestControllerAdvice
class ApiExceptionHandler {

    private fun body(status: HttpStatus, message: String) =
        ResponseEntity.status(status).body(mapOf("message" to message))

    @ExceptionHandler(ApiException::class)
    fun handleApi(e: ApiException) = body(e.status, e.message)

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidation(e: MethodArgumentNotValidException) =
        body(HttpStatus.BAD_REQUEST, e.bindingResult.fieldErrors.firstOrNull()?.defaultMessage ?: "Invalid request.")

    @ExceptionHandler(MaxUploadSizeExceededException::class)
    fun handleTooLarge(e: MaxUploadSizeExceededException) =
        body(HttpStatus.PAYLOAD_TOO_LARGE, "That file is too large. The limit is 10 MB.")

    @ExceptionHandler(
        MissingServletRequestParameterException::class,
        MissingServletRequestPartException::class,
        HttpMessageNotReadableException::class,
        MethodArgumentTypeMismatchException::class,
    )
    fun handleBadRequest(e: Exception) = body(HttpStatus.BAD_REQUEST, "The request is missing something or has an invalid value.")
}
