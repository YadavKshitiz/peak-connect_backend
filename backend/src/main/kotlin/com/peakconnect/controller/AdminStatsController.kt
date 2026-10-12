package com.peakconnect.controller

import com.peakconnect.dto.BookingsTodayStats
import com.peakconnect.dto.CacheStats
import com.peakconnect.dto.GuideResponseStats
import com.peakconnect.dto.WaitlistStats
import com.peakconnect.service.AdminStatsService
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/admin/stats")
@PreAuthorize("hasRole('ADMIN')")
class AdminStatsController(
    private val adminStatsService: AdminStatsService
) {

    @GetMapping("/bookings-today")
    fun getBookingsToday(): ResponseEntity<BookingsTodayStats> {
        return ResponseEntity.ok(adminStatsService.getBookingsToday())
    }

    @GetMapping("/cache-hit-rate")
    fun getCacheHitRate(): ResponseEntity<CacheStats> {
        return ResponseEntity.ok(adminStatsService.getCacheHitRate())
    }

    @GetMapping("/waitlists")
    fun getActiveWaitlists(): ResponseEntity<WaitlistStats> {
        return ResponseEntity.ok(adminStatsService.getActiveWaitlists())
    }

    @GetMapping("/guide-responses")
    fun getGuideResponseStats(): ResponseEntity<GuideResponseStats> {
        return ResponseEntity.ok(adminStatsService.getGuideResponseStats())
    }
}
