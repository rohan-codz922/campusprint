package com.campusprint.config

import com.campusprint.domain.AppUser
import com.campusprint.domain.PrintService
import com.campusprint.domain.Role
import com.campusprint.repository.PrintServiceRepository
import com.campusprint.repository.UserRepository
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.CommandLineRunner
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Component
import java.math.BigDecimal

/** Runs once at startup: makes sure an admin account and a starter price list exist. */
@Component
class DataSeeder(
    private val users: UserRepository,
    private val services: PrintServiceRepository,
    private val encoder: PasswordEncoder,
    @Value("\${app.admin.email}") private val adminEmail: String,
    @Value("\${app.admin.password}") private val adminPassword: String,
) : CommandLineRunner {

    override fun run(vararg args: String) {
        if (!users.existsByRole(Role.ADMIN)) {
            users.save(
                AppUser(
                    name = "Print Desk Admin",
                    email = adminEmail.lowercase(),
                    passwordHash = requireNotNull(encoder.encode(adminPassword)),
                    role = Role.ADMIN,
                ),
            )
        }
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
}
