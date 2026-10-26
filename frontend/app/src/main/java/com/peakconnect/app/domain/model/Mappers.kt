package com.peakconnect.app.domain.model

import com.peakconnect.app.data.dto.ActivityResponse
import com.peakconnect.app.data.dto.BookingResponse
import com.peakconnect.app.data.dto.GuideResponse
import com.peakconnect.app.data.dto.MatchedGuideResponse
import com.peakconnect.app.data.dto.SlotResponse
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

fun String.toLocalDateTimeOrNow(): LocalDateTime {
    return try {
        LocalDateTime.parse(this, DateTimeFormatter.ISO_LOCAL_DATE_TIME)
    } catch (_: Exception) {
        try {
            LocalDateTime.parse(this, DateTimeFormatter.ISO_DATE_TIME)
        } catch (_: Exception) {
            LocalDateTime.now()
        }
    }
}

fun parseRiskLevel(raw: String?): RiskLevel = when {
    raw.isNullOrBlank() -> RiskLevel.UNKNOWN
    raw.startsWith("LOW", ignoreCase = true) -> RiskLevel.LOW
    raw.startsWith("MODERATE", ignoreCase = true) -> RiskLevel.MODERATE
    raw.startsWith("HIGH", ignoreCase = true) -> RiskLevel.HIGH
    else -> RiskLevel.UNKNOWN
}

fun SlotResponse.toDomain(): Slot {
    return Slot(
        id = id,
        activityId = activityId,
        date = date.toLocalDateTimeOrNow(),
        capacity = capacity,
        currentOccupancy = currentOccupancy,
        season = season,
        currentPrice = currentPrice,
        riskLevel = parseRiskLevel(riskLevel)
    )
}

fun ActivityResponse.toDomain(): Activity {
    val domainSlots = slots.map { it.toDomain() }
    val minPrice = domainSlots.minOfOrNull { it.currentPrice } ?: basePrice
    return Activity(
        id = id,
        title = title,
        description = description,
        location = location,
        latitude = latitude,
        longitude = longitude,
        difficultyLevel = difficultyLevel,
        basePrice = basePrice,
        cancellationPolicy = cancellationPolicy,
        slots = domainSlots,
        minSlotPrice = minPrice
    )
}

fun BookingResponse.toDomain(
    statusSinceMillis: Long = System.currentTimeMillis(),
    lastSyncMillis: Long = System.currentTimeMillis()
): Booking {
    return Booking(
        id = id,
        slotId = slotId,
        activityTitle = activityTitle,
        trekkerId = trekkerId,
        trekkerName = trekkerName,
        guideId = guideId,
        guideName = guideName,
        status = status,
        date = date.toLocalDateTimeOrNow(),
        paymentOrderId = paymentOrderId,
        depositAmount = depositAmount,
        refundAmount = refundAmount,
        refundPercentage = refundPercentage,
        statusSinceMillis = statusSinceMillis,
        lastSyncMillis = lastSyncMillis
    )
}

fun MatchedGuideResponse.toDomain(): MatchedGuide {
    return MatchedGuide(
        guideId = guideId,
        name = name,
        skills = skills,
        languages = languages,
        location = location,
        experienceLevel = experienceLevel,
        score = score
    )
}

fun GuideResponse.toDomain(lastUpdated: Long = System.currentTimeMillis()): GuideProfile {
    return GuideProfile(
        id = id,
        name = name,
        email = email,
        skills = skills,
        languages = languages,
        location = location,
        latitude = latitude,
        longitude = longitude,
        experienceLevel = experienceLevel,
        isVerified = isVerified,
        lastUpdated = lastUpdated
    )
}
