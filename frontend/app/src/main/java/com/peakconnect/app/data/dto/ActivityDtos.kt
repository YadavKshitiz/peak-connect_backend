package com.peakconnect.app.data.dto

import com.peakconnect.app.core.network.BigDecimalSerializer
import com.peakconnect.app.domain.model.CancellationPolicyType
import com.peakconnect.app.domain.model.DifficultyLevel
import kotlinx.serialization.Serializable
import java.math.BigDecimal

@Serializable
data class ActivityResponse(
    val id: String,
    val title: String,
    val description: String? = null,
    val location: String,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val difficultyLevel: DifficultyLevel,
    @Serializable(with = BigDecimalSerializer::class)
    val basePrice: BigDecimal,
    val cancellationPolicy: CancellationPolicyType,
    val slots: List<SlotResponse> = emptyList()
)
