package com.peakconnect.service

import com.peakconnect.dto.AuthResponse
import com.peakconnect.dto.LoginRequest
import com.peakconnect.dto.RegisterRequest
import com.peakconnect.entity.Role
import com.peakconnect.entity.User
import com.peakconnect.entity.Guide
import com.peakconnect.repository.UserRepository
import com.peakconnect.repository.GuideRepository
import com.peakconnect.security.JwtTokenProvider
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import com.peakconnect.entity.ExperienceLevel

@Service
class AuthService(
    private val authenticationManager: AuthenticationManager,
    private val userRepository: UserRepository,
    private val guideRepository: GuideRepository,
    private val passwordEncoder: PasswordEncoder,
    private val jwtTokenProvider: JwtTokenProvider
) {

    @Transactional
    fun register(request: RegisterRequest): AuthResponse {
        if (request.role == Role.ADMIN) {
            throw IllegalArgumentException("Cannot register as ADMIN")
        }
        
        if (userRepository.findByEmail(request.email) != null) {
            throw com.peakconnect.exception.ConflictException("Email is already taken")
        }

        val user = User(
            name = request.name,
            email = request.email,
            password = passwordEncoder.encode(request.password),
            role = request.role
        )
        val savedUser = userRepository.saveAndFlush(user)
        
        if (savedUser.role == Role.GUIDE) {
            val guide = Guide(
                user = savedUser,
                experienceLevel = ExperienceLevel.BEGINNER
            )
            guideRepository.save(guide)
        }

        val authentication = authenticationManager.authenticate(
            UsernamePasswordAuthenticationToken(request.email, request.password)
        )
        val token = jwtTokenProvider.generateToken(authentication)
        return AuthResponse(token)
    }

    fun login(request: LoginRequest): AuthResponse {
        val authentication = authenticationManager.authenticate(
            UsernamePasswordAuthenticationToken(request.email, request.password)
        )
        SecurityContextHolder.getContext().authentication = authentication
        val token = jwtTokenProvider.generateToken(authentication)
        return AuthResponse(token)
    }
}
