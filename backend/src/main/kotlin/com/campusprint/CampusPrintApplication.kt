package com.campusprint

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration
import org.springframework.boot.runApplication

// We authenticate with our own JWT filter, so Spring's default in-memory user is switched off.
@SpringBootApplication(exclude = [UserDetailsServiceAutoConfiguration::class])
class CampusPrintApplication

fun main(args: Array<String>) {
    runApplication<CampusPrintApplication>(*args)
}
