package com.campusprint.service

import com.campusprint.domain.StoredFile
import com.campusprint.repository.StoredFileRepository
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile

/** Saves uploaded documents in the database and reads them back. */
@Service
class FileStorageService(private val files: StoredFileRepository) {

    private val allowedTypes = setOf("pdf", "doc", "docx", "ppt", "pptx", "jpg", "jpeg", "png", "txt")

    /** Returns the id of the saved file. */
    fun store(file: MultipartFile): Long {
        if (file.isEmpty) throw ApiException(HttpStatus.BAD_REQUEST, "Please attach a file to print.")
        val extension = (file.originalFilename ?: "").substringAfterLast('.', "").lowercase()
        if (extension !in allowedTypes) {
            throw ApiException(HttpStatus.BAD_REQUEST, "Unsupported file type. Use PDF, Word, PowerPoint, an image or text.")
        }
        val saved = files.save(
            StoredFile(
                contentType = file.contentType ?: "application/octet-stream",
                data = file.bytes,
            ),
        )
        return requireNotNull(saved.id)
    }

    fun load(id: Long): StoredFile =
        files.findById(id).orElseThrow { ApiException(HttpStatus.NOT_FOUND, "The file is no longer available.") }
}
