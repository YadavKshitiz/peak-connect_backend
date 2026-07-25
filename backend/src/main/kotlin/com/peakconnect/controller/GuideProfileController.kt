package com.peakconnect.controller

import com.peakconnect.dto.GuideProfileUpdateRequest
import com.peakconnect.dto.GuideResponse
import com.peakconnect.security.CustomUserDetails
import com.peakconnect.service.GuideService
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.bind.annotation.*
import java.util.UUID

@RestController
@RequestMapping("/api/guides/me")
@PreAuthorize("hasRole('GUIDE')")
class GuideProfileController(private val guideService: GuideService) {

    private fun getCurrentGuideId(): UUID {
        val userDetails = SecurityContextHolder.getContext().authentication.principal as CustomUserDetails
        return userDetails.id!!
    }

    @GetMapping
    fun getMyProfile(): ResponseEntity<GuideResponse> =
        ResponseEntity.ok(guideService.getMyProfile(getCurrentGuideId()))

    @PutMapping
    fun updateMyProfile(@Valid @RequestBody request: GuideProfileUpdateRequest): ResponseEntity<GuideResponse> =
        ResponseEntity.ok(guideService.updateMyProfile(getCurrentGuideId(), request))
}
