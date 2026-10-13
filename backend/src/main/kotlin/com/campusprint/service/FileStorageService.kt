package com.campusprint.service

import com.campusprint.domain.StoredFile
import com.campusprint.repository.StoredFileRepository
import org.apache.pdfbox.Loader
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile

/**
 * Validates and stores uploads in the database. We never trust the file name or the
 * browser's content type: the real type is read from the file's first bytes ("magic bytes").
 */
@Service
class FileStorageService(
    private val files: StoredFileRepository,
    @Value("\${app.limits.max-pages:500}") private val maxPages: Int,
) {
    data class StoredDocument(val fileId: Long, val pages: Int)

    private enum class Kind(val mime: String, val magic: ByteArray) {
        PDF("application/pdf", "%PDF-".toByteArray(Charsets.US_ASCII)),
        PNG("image/png", byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A)),
        JPEG("image/jpeg", byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte())),
    }

    /** A document to print: PDF, PNG or JPG up to 10 MB. The SERVER counts the pages. */
    fun storeDocument(file: MultipartFile): StoredDocument {
        val bytes = read(file, 10 * 1024 * 1024, "The document is larger than 10 MB.")
        val kind = detect(bytes)
        if (kind == null) {
            throw ApiException(HttpStatus.BAD_REQUEST, "Only PDF, JPG or PNG files can be printed. Save Word or PowerPoint files as PDF first.")
        }
        val pages = if (kind == Kind.PDF) countPdfPages(bytes) else 1
        if (pages > maxPages) {
            throw ApiException(HttpStatus.BAD_REQUEST, "This file has $pages pages. The limit is $maxPages pages per job.")
        }
        val saved = files.save(StoredFile(contentType = kind.mime, data = bytes))
        return StoredDocument(requireNotNull(saved.id), pages)
    }

    /** A payment screenshot: PNG or JPG up to 3 MB. */
    fun storeScreenshot(file: MultipartFile): Long {
        val bytes = read(file, 3 * 1024 * 1024, "The payment screenshot is larger than 3 MB.")
        val kind = detect(bytes)
        if (kind == null || (kind != Kind.PNG && kind != Kind.JPEG)) {
            throw ApiException(HttpStatus.BAD_REQUEST, "The payment proof must be a PNG or JPG screenshot.")
        }
        val saved = files.save(StoredFile(contentType = kind.mime, data = bytes))
        return requireNotNull(saved.id)
    }

    fun load(id: Long): StoredFile =
        files.findById(id).orElseThrow { ApiException(HttpStatus.NOT_FOUND, "The file is no longer available.") }

    private fun read(file: MultipartFile, maxBytes: Int, tooLarge: String): ByteArray {
        if (file.isEmpty) throw ApiException(HttpStatus.BAD_REQUEST, "Please attach a file.")
        if (file.size > maxBytes) throw ApiException(HttpStatus.PAYLOAD_TOO_LARGE, tooLarge)
        return file.bytes
    }

    private fun detect(bytes: ByteArray): Kind? =
        Kind.values().firstOrNull { k -> bytes.size >= k.magic.size && k.magic.indices.all { bytes[it] == k.magic[it] } }

    private fun countPdfPages(bytes: ByteArray): Int {
        val pages = try {
            Loader.loadPDF(bytes).use { it.numberOfPages }
        } catch (e: Exception) {
            throw ApiException(HttpStatus.BAD_REQUEST, "We could not read this PDF. It may be password-protected or damaged.")
        }
        if (pages < 1) throw ApiException(HttpStatus.BAD_REQUEST, "This PDF has no pages.")
        return pages
    }
}
