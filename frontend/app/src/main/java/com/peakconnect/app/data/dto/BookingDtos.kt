package com.peakconnect.app.data.dto

import com.peakconnect.app.domain.model.BookingStatus
import com.peakconnect.app.domain.model.ExperienceLevel
import kotlinx.serialization.Serializable

@Serializable
data class BookingRequestDto(
    val slotId: String,
    val skillWeight: Double? = null,
    val locationWeight: Double? = null,
    val languageWeight: Double? = null
)

@Serializable
data class BookingConfirmDto(
    val slotId: String,
    val guideId: String
)

@Serializable
data class BookingResponse(
    val id: String,
    val slotId: String,
    val activityTitle: String,
    val trekkerId: String,
    val trekkerName: String,
    val guideId: String? = null,
    val guideName: String? = null,
    val status: BookingStatus,
    val date: String, // ISO-8601 LocalDateTime
    val paymentOrderId: String? = null,
    val depositAmount: Double? = null,
    val refundAmount: Double? = null,
    val refundPercentage: Double? = null
)

@Serializable
data class MatchedGuideResponse(
    val guideId: String,
    val name: String,
    val skills: List<String> = emptyList(),
    val languages: List<String> = emptyList(),
    val location: String? = null,
    val experienceLevel: ExperienceLevel,
    val score: Double
)

@Serializable
data class WaitlistMessageResponse(
    val message: String? = null
)
