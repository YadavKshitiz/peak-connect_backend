package com.peakconnect.controller

import com.peakconnect.dto.ActivityResponse
import com.peakconnect.service.ActivityService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.time.LocalDate
import java.util.UUID

@RestController
@RequestMapping("/api/activities")
class PublicActivityController(private val activityService: ActivityService) {

    @GetMapping
    fun getActivities(
        @RequestParam(required = false) location: String?,
        @RequestParam(required = false) date: LocalDate?
    ): ResponseEntity<List<ActivityResponse>> {
        return ResponseEntity.ok(activityService.getFilteredActivities(location, date))
    }

    @GetMapping("/{id}")
    fun getActivity(@PathVariable id: UUID): ResponseEntity<ActivityResponse> {
        return ResponseEntity.ok(activityService.getActivity(id))
    }
}
