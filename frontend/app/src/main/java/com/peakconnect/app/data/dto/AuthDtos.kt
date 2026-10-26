package com.peakconnect.app.data.dto

import com.peakconnect.app.domain.model.Role
import kotlinx.serialization.Serializable

@Serializable
data class RegisterRequest(
    val name: String,
    val email: String,
    val password: String,
    val role: Role,
    val preferredLanguages: List<String>? = null
)

@Serializable
data class LoginRequest(
    val email: String,
    val password: String
)

@Serializable
data class AuthResponse(
    val token: String
)
