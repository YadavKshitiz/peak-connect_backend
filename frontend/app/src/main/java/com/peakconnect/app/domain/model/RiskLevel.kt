package com.peakconnect.app.domain.model

import com.peakconnect.app.core.network.SafeRiskLevelSerializer
import kotlinx.serialization.Serializable

@Serializable(with = SafeRiskLevelSerializer::class)
enum class RiskLevel {
    LOW,
    MODERATE,
    HIGH,
    UNKNOWN
}
