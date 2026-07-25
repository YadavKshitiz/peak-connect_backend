package com.peakconnect.dto

import com.peakconnect.entity.DifficultyLevel
import jakarta.validation.constraints.DecimalMin
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import java.math.BigDecimal
import java.util.UUID

data class ActivityRequest(
    @field:NotBlank val title: String,
    val description: String?,
    @field:NotBlank val location: String,
    @field:NotNull val difficultyLevel: DifficultyLevel,
    @field:NotNull @field:DecimalMin("0.0") val basePrice: BigDecimal
)

data class ActivityResponse(
    val id: UUID,
    val title: String,
    val description: String?,
    val location: String,
    val difficultyLevel: DifficultyLevel,
    val basePrice: BigDecimal,
    val slots: List<SlotResponse> = emptyList()
)
