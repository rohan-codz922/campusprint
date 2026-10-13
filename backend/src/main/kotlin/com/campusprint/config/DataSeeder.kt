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
 *  - Creates the admin account if none exists.
 *  - If one exists, keeps its email and password in sync with ADMIN_EMAIL / ADMIN_PASSWORD,
 *    so the admin password is changed by editing the environment variable and restarting.
 *  - Creates a starter price list if the table is empty.
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
                ),
            )
            log.info("Admin account created for {}", email)
            return
        }

        var changed = false
        val takenBySomeoneElse = users.findByEmailIgnoreCase(email)?.let { it.id != admin.id } ?: false
        if (admin.email != email && !takenBySomeoneElse) {
            admin.email = email
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
