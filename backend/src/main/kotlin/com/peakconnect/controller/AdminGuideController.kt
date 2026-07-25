package com.peakconnect.controller

import com.peakconnect.dto.GuideResponse
import com.peakconnect.service.GuideService
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*
import java.util.UUID

@RestController
@RequestMapping("/api/admin/guides")
@PreAuthorize("hasRole('ADMIN')")
class AdminGuideController(private val guideService: GuideService) {

    @GetMapping
    fun listGuides(@RequestParam(required = false) isVerified: Boolean?): ResponseEntity<List<GuideResponse>> =
        ResponseEntity.ok(guideService.listGuides(isVerified))

    @PostMapping("/{guideId}/approve")
    fun approveGuide(@PathVariable guideId: UUID): ResponseEntity<GuideResponse> =
        ResponseEntity.ok(guideService.approveGuide(guideId))

    @PostMapping("/{guideId}/reject")
    fun rejectGuide(@PathVariable guideId: UUID): ResponseEntity<GuideResponse> =
        ResponseEntity.ok(guideService.rejectGuide(guideId))
}
