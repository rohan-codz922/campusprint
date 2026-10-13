package com.campusprint.service

import com.campusprint.domain.OtpPurpose
import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.web.util.HtmlUtils
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse

/**
 * Sends email through Brevo's HTTPS API (port 443). We use the web API, not SMTP,
 * because free hosting often blocks SMTP ports.
 */
@Service
class MailService(
    private val mapper: ObjectMapper,
    @Value("\${app.mail.api-key:}") private val apiKey: String,
    @Value("\${app.mail.from:}") private val from: String,
    @Value("\${app.mail.from-name:CampusPrint}") private val fromName: String,
    @Value("\${app.mail.log-codes:false}") private val logCodes: Boolean,
) {
    private val log = LoggerFactory.getLogger(MailService::class.java)
    private val client: HttpClient = HttpClient.newHttpClient()

    fun sendCode(toEmail: String, toName: String, code: String, purpose: OtpPurpose) {
        if (apiKey.isBlank() || from.isBlank()) {
            if (logCodes) log.warn("LOCAL TESTING ONLY - code for {} ({}): {}", toEmail, purpose, code)
            else log.warn("Email is not configured (BREVO_API_KEY / MAIL_FROM). Code was not sent.")
            return
        }
        val (subject, intro) = when (purpose) {
            OtpPurpose.VERIFY_EMAIL -> "Verify your CampusPrint email" to "Use this code to verify your email address:"
            OtpPurpose.RESET_PASSWORD -> "Your CampusPrint password reset code" to "Use this code to reset your CampusPrint password:"
        }
        val html = """
            <div style="font-family:Arial,sans-serif;max-width:420px">
              <p>Hi ${HtmlUtils.htmlEscape(toName)},</p>
              <p>$intro</p>
              <p style="font-size:32px;font-weight:bold;letter-spacing:6px">$code</p>
              <p>It expires in 10 minutes. If you did not ask for this, ignore this email.</p>
            </div>
        """.trimIndent()

        val payload = mapOf(
            "sender" to mapOf("name" to fromName, "email" to from),
            "to" to listOf(mapOf("email" to toEmail, "name" to toName)),
            "subject" to subject,
            "htmlContent" to html,
        )
        val request = HttpRequest.newBuilder(URI.create("https://api.brevo.com/v3/smtp/email"))
            .header("api-key", apiKey)
            .header("accept", "application/json")
            .header("content-type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(payload)))
            .build()

        try {
            val response = client.send(request, HttpResponse.BodyHandlers.ofString())
            if (response.statusCode() !in 200..299) {
                log.error("Brevo rejected the email: HTTP {} {}", response.statusCode(), response.body())
            }
        } catch (e: Exception) {
            log.error("Could not reach Brevo to send the email", e)
        }
    }
}
