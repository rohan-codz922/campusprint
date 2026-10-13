package com.campusprint.config

import com.campusprint.domain.AppUser
import com.campusprint.domain.PrintService
import com.campusprint.domain.Role
import com.campusprint.repository.PrintServiceRepository
import com.campusprint.repository.UserRepository
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.CommandLineRunner
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Component
import java.math.BigDecimal

/**
 * Runs at every startup.
 *  - Creates the admin account if none exists, or keeps its email and password in sync with
 *    ADMIN_EMAIL / ADMIN_PASSWORD. To change the admin password: edit the variable and restart.
 *  - Creates a starter price list if the table is empty.
 * There are no default credentials: the app refuses to start without strong ones.
 */
@Component
class DataSeeder(
    private val users: UserRepository,
    private val services: PrintServiceRepository,
    private val encoder: PasswordEncoder,
    @Value("\${app.admin.email}") adminEmail: String,
    @Value("\${app.admin.password}") adminPassword: String,
) : CommandLineRunner {

    private val log = LoggerFactory.getLogger(DataSeeder::class.java)

    // trim() protects against a stray space or newline pasted into the environment variable
    private val email = adminEmail.trim().lowercase()
    private val password = adminPassword.trim()

    init {
        require(email.contains("@")) { "ADMIN_EMAIL must be a valid email address." }
        require(password.length in 8..72) { "ADMIN_PASSWORD must be 8 to 72 characters long." }
    }

    override fun run(vararg args: String) {
        syncAdmin()
        if (services.count() == 0L) {
            services.saveAll(
                listOf(
                    PrintService("Black & white, A4", "Single-sided, 80 gsm paper", BigDecimal("1.00")),
                    PrintService("Black & white, double-sided", "Both sides, 80 gsm paper", BigDecimal("0.75")),
                    PrintService("Colour, A4", "Single-sided, 100 gsm paper", BigDecimal("6.00")),
                    PrintService("Colour, A3 poster", "Posters and charts, 120 gsm paper", BigDecimal("25.00")),
                ),
            )
        }
    }

    private fun syncAdmin() {
        val admin = users.findFirstByRole(Role.ADMIN)
        if (admin == null) {
            users.save(
                AppUser(
                    name = "Print Desk Admin",
                    email = email,
                    passwordHash = requireNotNull(encoder.encode(password)),
                    role = Role.ADMIN,
                    emailVerified = true,
                ),
            )
            log.info("Admin account created")
            return
        }

        var changed = false
        val takenBySomeoneElse = users.findByEmailIgnoreCase(email)?.let { it.id != admin.id } ?: false
        if (admin.email != email && !takenBySomeoneElse) {
            admin.email = email
            changed = true
        }
        if (!admin.emailVerified) {
            admin.emailVerified = true
            changed = true
        }
        if (!encoder.matches(password, admin.passwordHash)) {
            admin.passwordHash = requireNotNull(encoder.encode(password))
            changed = true
        }
        if (changed) {
            users.save(admin)
            log.info("Admin account updated from environment settings")
        }
    }
}
