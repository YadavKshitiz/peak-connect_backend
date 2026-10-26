package com.peakconnect.app.core.session

import com.peakconnect.app.domain.model.Role
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.util.Base64

data class JwtPayload(
    val email: String,
    val userId: String,
    val role: Role,
    val expiresAt: Long // Epoch seconds
)

object JwtDecoder {

    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    @Serializable
    private data class JwtClaims(
        val sub: String? = null,
        val userId: String? = null,
        val role: String? = null,
        val exp: Long? = null
    )

    fun decode(token: String): JwtPayload? {
        val parts = token.trim().split(".")
        if (parts.size != 3) return null

        return try {
            val payloadBase64 = parts[1]
            val decodedBytes = Base64.getUrlDecoder().decode(payloadBase64)
            val jsonString = String(decodedBytes, Charsets.UTF_8)
            val claims = json.decodeFromString<JwtClaims>(jsonString)

            val email = claims.sub?.takeIf { it.isNotBlank() } ?: return null
            val userId = claims.userId?.takeIf { it.isNotBlank() } ?: return null
            val exp = claims.exp ?: 0L

            val rawRole = claims.role ?: ""
            val role = when {
                rawRole.contains("GUIDE", ignoreCase = true) -> Role.GUIDE
                rawRole.contains("ADMIN", ignoreCase = true) -> Role.ADMIN
                else -> Role.TREKKER
            }

            JwtPayload(
                email = email,
                userId = userId,
                role = role,
                expiresAt = exp
            )
        } catch (_: Exception) {
            null
        }
    }

    fun isExpired(token: String, nowEpochSeconds: Long = System.currentTimeMillis() / 1000): Boolean {
        val payload = decode(token) ?: return true
        return payload.expiresAt <= nowEpochSeconds
    }
}
