package com.peakconnect.dto

import com.peakconnect.entity.Season
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotNull
import java.time.LocalDateTime
import java.util.UUID

data class SlotRequest(
    @field:NotNull val date: LocalDateTime,
    @field:NotNull @field:Min(1) val capacity: Int,
    @field:NotNull val season: Season
)

data class SlotResponse(
    val id: UUID,
    val activityId: UUID,
    val date: LocalDateTime,
    val capacity: Int,
    val currentOccupancy: Int,
    val season: Season,
    val currentPrice: java.math.BigDecimal
)
