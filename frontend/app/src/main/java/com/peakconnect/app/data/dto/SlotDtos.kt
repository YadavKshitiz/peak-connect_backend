package com.peakconnect.app.data.dto

import com.peakconnect.app.core.network.BigDecimalSerializer
import com.peakconnect.app.domain.model.Season
import kotlinx.serialization.Serializable
import java.math.BigDecimal

@Serializable
data class SlotResponse(
    val id: String,
    val activityId: String,
    val date: String, // ISO-8601 LocalDateTime, e.g. "2026-11-01T06:00:00"
    val capacity: Int,
    val currentOccupancy: Int,
    val season: Season,
    @Serializable(with = BigDecimalSerializer::class)
    val currentPrice: BigDecimal,
    val riskLevel: String? = null
)
