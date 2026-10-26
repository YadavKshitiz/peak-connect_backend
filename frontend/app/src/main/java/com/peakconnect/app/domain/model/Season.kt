package com.peakconnect.app.domain.model

import com.peakconnect.app.core.network.SeasonSerializer
import kotlinx.serialization.Serializable

@Serializable(with = SeasonSerializer::class)
enum class Season {
    LOW,
    SHOULDER,
    PEAK
}
