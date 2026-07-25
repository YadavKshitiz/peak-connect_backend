package com.peakconnect.security

import io.jsonwebtoken.Jwts
import io.jsonwebtoken.io.Decoders
import io.jsonwebtoken.security.Keys
import org.springframework.beans.factory.annotation.Value
import org.springframework.security.core.Authentication
import org.springframework.stereotype.Component
import java.util.Date
import javax.crypto.SecretKey

@Component
class JwtTokenProvider(
    @Value("\${jwt.secret}")
    private val jwtSecret: String,
    
    @Value("\${jwt.expiration-ms}")
    private val jwtExpirationMs: Long
) {
    private fun getSigningKey(): SecretKey {
        val keyBytes = Decoders.BASE64.decode(jwtSecret)
        return Keys.hmacShaKeyFor(keyBytes)
    }

    fun generateToken(authentication: Authentication): String {
        val userPrincipal = authentication.principal as CustomUserDetails
        
        val now = Date()
        val expiryDate = Date(now.time + jwtExpirationMs)

        return Jwts.builder()
            .subject(userPrincipal.username)
            .claim("userId", userPrincipal.id)
            .claim("role", userPrincipal.authorities.first().authority)
            .issuedAt(now)
            .expiration(expiryDate)
            .signWith(getSigningKey())
            .compact()
    }

    fun getEmailFromToken(token: String): String {
        return Jwts.parser()
            .verifyWith(getSigningKey())
            .build()
            .parseSignedClaims(token)
            .payload
            .subject
    }

    fun validateToken(authToken: String): Boolean {
        try {
            Jwts.parser().verifyWith(getSigningKey()).build().parseSignedClaims(authToken)
            return true
        } catch (ex: Exception) {
            // Log exception here in production
        }
        return false
    }
}
