package com.peakconnect.app.domain.model

import java.math.BigDecimal
import java.time.LocalDateTime

data class Activity(
    val id: String,
    val title: String,
    val description: String?,
    val location: String,
    val latitude: Double?,
    val longitude: Double?,
    val difficultyLevel: DifficultyLevel,
    val basePrice: BigDecimal,
    val cancellationPolicy: CancellationPolicyType,
    val slots: List<Slot> = emptyList(),
    val minSlotPrice: BigDecimal? = null,
    val lastUpdated: Long = System.currentTimeMillis()
)

data class Slot(
    val id: String,
    val activityId: String,
    val date: LocalDateTime,
    val capacity: Int,
    val currentOccupancy: Int,
    val season: Season,
    val currentPrice: BigDecimal,
    val riskLevel: RiskLevel
)

data class Booking(
    val id: String,
    val slotId: String,
    val activityTitle: String,
    val trekkerId: String,
    val trekkerName: String,
    val guideId: String?, // Nullable as per contract
    val guideName: String?, // Nullable as per contract
    val status: BookingStatus,
    val date: LocalDateTime,
    val paymentOrderId: String? = null,
    val depositAmount: Double? = null,
    val refundAmount: Double? = null,
    val refundPercentage: Double? = null,
    val statusSinceMillis: Long = System.currentTimeMillis(),
    val lastSyncMillis: Long = System.currentTimeMillis()
)

data class MatchedGuide(
    val guideId: String,
    val name: String,
    val skills: List<String>,
    val languages: List<String>,
    val location: String?,
    val experienceLevel: ExperienceLevel,
    val score: Double
)

data class GuideProfile(
    val id: String,
    val name: String,
    val email: String,
    val skills: List<String>,
    val languages: List<String>,
    val location: String?,
    val latitude: Double?,
    val longitude: Double?,
    val experienceLevel: ExperienceLevel,
    val isVerified: Boolean,
    val lastUpdated: Long = System.currentTimeMillis()
)

data class WaitlistEntry(
    val slotId: String,
    val activityTitle: String,
    val slotDate: LocalDateTime,
    val joinedAt: Long,
    val status: WaitlistStatus
)
