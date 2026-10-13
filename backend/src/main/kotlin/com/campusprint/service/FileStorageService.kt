package com.campusprint.service

import org.springframework.beans.factory.annotation.Value
import org.springframework.core.io.Resource
import org.springframework.core.io.UrlResource
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardCopyOption
import java.util.UUID

/** Saves uploaded documents to disk under a random name and reads them back. */
@Service
class FileStorageService(@Value("\${app.upload-dir}") uploadDir: String) {

    private val root: Path = Paths.get(uploadDir).toAbsolutePath().normalize()
    private val allowedTypes = setOf("pdf", "doc", "docx", "ppt", "pptx", "jpg", "jpeg", "png", "txt")

    init {
        Files.createDirectories(root)
    }

    /** Returns the random file name used on disk. */
    fun store(file: MultipartFile): String {
        if (file.isEmpty) throw ApiException(HttpStatus.BAD_REQUEST, "Please attach a file to print.")
        val original = file.originalFilename ?: ""
        val extension = original.substringAfterLast('.', "").lowercase()
        if (extension !in allowedTypes) {
            throw ApiException(HttpStatus.BAD_REQUEST, "Unsupported file type. Use PDF, Word, PowerPoint, an image or text.")
        }
        // We never trust the uploaded name for the path: random UUID + a checked extension.
        val storedName = "${UUID.randomUUID()}.$extension"
        file.inputStream.use { Files.copy(it, root.resolve(storedName), StandardCopyOption.REPLACE_EXISTING) }
        return storedName
    }

    fun load(storedName: String): Resource {
        val path = root.resolve(storedName).normalize()
        // Block path tricks like ../../etc/passwd
        if (!path.startsWith(root) || !Files.exists(path)) {
            throw ApiException(HttpStatus.NOT_FOUND, "The file is no longer available on the server.")
        }
        return UrlResource(path.toUri())
    }
}
