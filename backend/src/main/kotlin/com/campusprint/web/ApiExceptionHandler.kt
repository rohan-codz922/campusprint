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

/** One place that turns exceptions into the {"message": "..."} JSON the website shows to the user. */
@RestControllerAdvice
class ApiExceptionHandler {

    private fun body(status: HttpStatus, message: String, code: String? = null): ResponseEntity<Map<String, String>> {
        val map = mutableMapOf("message" to message)
        if (code != null) map["code"] = code
        return ResponseEntity.status(status).body(map)
    }

    @ExceptionHandler(ApiException::class)
    fun handleApi(e: ApiException) = body(e.status, e.message, e.code)

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidation(e: MethodArgumentNotValidException) =
        body(HttpStatus.BAD_REQUEST, e.bindingResult.fieldErrors.firstOrNull()?.defaultMessage ?: "Invalid request.")

    @ExceptionHandler(MaxUploadSizeExceededException::class)
    fun handleTooLarge(e: MaxUploadSizeExceededException) =
        body(HttpStatus.PAYLOAD_TOO_LARGE, "That upload is too large. Documents can be up to 10 MB and screenshots up to 3 MB.")

    @ExceptionHandler(
        MissingServletRequestParameterException::class,
        MissingServletRequestPartException::class,
        HttpMessageNotReadableException::class,
        MethodArgumentTypeMismatchException::class,
    )
    fun handleBadRequest(e: Exception) = body(HttpStatus.BAD_REQUEST, "The request is missing something or has an invalid value.")
}
