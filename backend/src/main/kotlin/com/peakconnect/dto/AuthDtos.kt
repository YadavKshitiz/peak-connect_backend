package com.peakconnect.dto

import com.peakconnect.entity.Role
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull

data class RegisterRequest(
    @field:NotBlank
    val name: String,
    
    @field:NotBlank
    @field:Email
    val email: String,
    
    @field:NotBlank
    val password: String,
    
    @field:NotNull
    val role: Role
)

data class LoginRequest(
    @field:NotBlank
    @field:Email
    val email: String,
    
    @field:NotBlank
    val password: String
)

data class AuthResponse(
    val token: String
)
