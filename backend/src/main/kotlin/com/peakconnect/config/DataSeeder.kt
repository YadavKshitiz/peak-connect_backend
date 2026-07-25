package com.peakconnect.config

import com.peakconnect.entity.Role
import com.peakconnect.entity.User
import com.peakconnect.repository.UserRepository
import org.springframework.boot.CommandLineRunner
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Component

@Component
class DataSeeder(
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoder
) : CommandLineRunner {

    override fun run(vararg args: String?) {
        if (userRepository.findByEmail("admin@example.com") == null) {
            val admin = User(
                name = "Admin User",
                email = "admin@example.com",
                password = passwordEncoder.encode("admin123"),
                role = Role.ADMIN
            )
            userRepository.save(admin)
        }
    }
}
