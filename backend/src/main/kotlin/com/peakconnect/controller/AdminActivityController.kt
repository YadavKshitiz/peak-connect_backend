package com.peakconnect.controller

import com.peakconnect.dto.ActivityRequest
import com.peakconnect.dto.ActivityResponse
import com.peakconnect.dto.SlotRequest
import com.peakconnect.dto.SlotResponse
import com.peakconnect.service.ActivityService
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*
import java.util.UUID

@RestController
@RequestMapping("/api/admin/activities")
@PreAuthorize("hasRole('ADMIN')")
class AdminActivityController(private val activityService: ActivityService) {

    @PostMapping
    fun createActivity(@Valid @RequestBody request: ActivityRequest): ResponseEntity<ActivityResponse> =
        ResponseEntity.ok(activityService.createActivity(request))

    @GetMapping
    fun getAllActivities(): ResponseEntity<List<ActivityResponse>> =
        ResponseEntity.ok(activityService.getAllActivities())

    @GetMapping("/{id}")
    fun getActivity(@PathVariable id: UUID): ResponseEntity<ActivityResponse> =
        ResponseEntity.ok(activityService.getActivity(id))

    @PutMapping("/{id}")
    fun updateActivity(@PathVariable id: UUID, @Valid @RequestBody request: ActivityRequest): ResponseEntity<ActivityResponse> =
        ResponseEntity.ok(activityService.updateActivity(id, request))

    @DeleteMapping("/{id}")
    fun deleteActivity(@PathVariable id: UUID): ResponseEntity<Void> {
        activityService.deleteActivity(id)
        return ResponseEntity.noContent().build()
    }

    @PostMapping("/{activityId}/slots")
    fun createSlot(@PathVariable activityId: UUID, @Valid @RequestBody request: SlotRequest): ResponseEntity<SlotResponse> =
        ResponseEntity.ok(activityService.createSlot(activityId, request))

    @GetMapping("/{activityId}/slots")
    fun getSlots(@PathVariable activityId: UUID): ResponseEntity<List<SlotResponse>> =
        ResponseEntity.ok(activityService.getSlots(activityId))

    @PutMapping("/{activityId}/slots/{slotId}")
    fun updateSlot(@PathVariable activityId: UUID, @PathVariable slotId: UUID, @Valid @RequestBody request: SlotRequest): ResponseEntity<SlotResponse> =
        ResponseEntity.ok(activityService.updateSlot(activityId, slotId, request))

    @DeleteMapping("/{activityId}/slots/{slotId}")
    fun deleteSlot(@PathVariable activityId: UUID, @PathVariable slotId: UUID): ResponseEntity<Void> {
        activityService.deleteSlot(activityId, slotId)
        return ResponseEntity.noContent().build()
    }
}
