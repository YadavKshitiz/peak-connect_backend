package com.peakconnect.service

import com.peakconnect.dto.ActivityRequest
import com.peakconnect.dto.ActivityResponse
import com.peakconnect.dto.SlotRequest
import com.peakconnect.dto.SlotResponse
import com.peakconnect.entity.Activity
import com.peakconnect.entity.Slot
import com.peakconnect.repository.ActivityRepository
import com.peakconnect.repository.SlotRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.time.LocalDate
import java.util.UUID

@Service
class ActivityService(
    private val activityRepository: ActivityRepository,
    private val slotRepository: SlotRepository,
    private val priceCalculator: com.peakconnect.pricing.PriceCalculator,
    private val weatherClient: com.peakconnect.risk.WeatherClient,
    private val riskCalculator: com.peakconnect.risk.RiskCalculator
) {
    @Transactional
    fun createActivity(request: ActivityRequest): ActivityResponse {
        val activity = Activity(
            title = request.title,
            description = request.description,
            location = request.location,
            difficultyLevel = request.difficultyLevel,
            basePrice = request.basePrice
        )
        return toActivityResponse(activityRepository.save(activity))
    }

    @Transactional
    fun updateActivity(id: UUID, request: ActivityRequest): ActivityResponse {
        val activity = activityRepository.findById(id).orElseThrow { com.peakconnect.exception.ResourceNotFoundException("Activity not found") }
        activity.title = request.title
        activity.description = request.description
        activity.location = request.location
        activity.difficultyLevel = request.difficultyLevel
        activity.basePrice = request.basePrice
        return toActivityResponse(activityRepository.save(activity))
    }

    @Transactional
    fun deleteActivity(id: UUID) {
        if (!activityRepository.existsById(id)) throw com.peakconnect.exception.ResourceNotFoundException("Activity not found")
        activityRepository.deleteById(id)
    }

    @Transactional(readOnly = true)
    fun getActivity(id: UUID): ActivityResponse {
        val activity = activityRepository.findById(id).orElseThrow { com.peakconnect.exception.ResourceNotFoundException("Activity not found") }
        return toActivityResponse(activity)
    }

    @Transactional(readOnly = true)
    fun getAllActivities(): List<ActivityResponse> {
        return activityRepository.findAll().map { toActivityResponse(it) }
    }

    @Transactional(readOnly = true)
    fun getFilteredActivities(location: String?, date: LocalDate?): List<ActivityResponse> {
        val activities = activityRepository.findAll()
        val filtered = activities.filter { a ->
            val matchLocation = location == null || a.location.equals(location, ignoreCase = true)
            val matchDate = date == null || a.slots.any { s -> s.date.toLocalDate() == date }
            matchLocation && matchDate
        }
        return filtered.map { toActivityResponse(it) }
    }

    @Transactional
    fun createSlot(activityId: UUID, request: SlotRequest): SlotResponse {
        val activity = activityRepository.findById(activityId).orElseThrow { com.peakconnect.exception.ResourceNotFoundException("Activity not found") }
        val slot = Slot(
            activity = activity,
            date = request.date,
            capacity = request.capacity,
            season = request.season
        )
        return toSlotResponse(slotRepository.save(slot))
    }

    fun getSlots(activityId: UUID): List<SlotResponse> {
        return slotRepository.findByActivityId(activityId).map { toSlotResponse(it) }
    }

    @Transactional
    @org.springframework.cache.annotation.CacheEvict(value = ["priceCache", "guideAvailabilityCache"], key = "#slotId.toString()")
    fun updateSlot(activityId: UUID, slotId: UUID, request: SlotRequest): SlotResponse {
        val slot = slotRepository.findById(slotId).orElseThrow { com.peakconnect.exception.ResourceNotFoundException("Slot not found") }
        if (slot.activity.id != activityId) throw IllegalArgumentException("Slot does not belong to activity")
        slot.date = request.date
        slot.capacity = request.capacity
        slot.season = request.season
        return toSlotResponse(slotRepository.save(slot))
    }

    @Transactional
    fun deleteSlot(activityId: UUID, slotId: UUID) {
        val slot = slotRepository.findById(slotId).orElseThrow { com.peakconnect.exception.ResourceNotFoundException("Slot not found") }
        if (slot.activity.id != activityId) throw IllegalArgumentException("Slot does not belong to activity")
        slotRepository.delete(slot)
    }

    private fun toActivityResponse(a: Activity): ActivityResponse {
        return ActivityResponse(
            a.id!!, a.title, a.description, a.location, a.difficultyLevel, a.basePrice,
            a.slots.map { toSlotResponse(it) }
        )
    }

    private fun toSlotResponse(s: Slot): SlotResponse {
        val computedPrice = priceCalculator.calculateFinalPrice(s.activity.basePrice, s)
        val risk = riskCalculator.calculateRiskForSlot(s.id!!, s.activity.location)
        return SlotResponse(s.id!!, s.activity.id!!, s.date, s.capacity, s.currentOccupancy, s.season, computedPrice, risk)
    }
}
