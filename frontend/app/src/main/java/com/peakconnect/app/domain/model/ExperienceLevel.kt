package com.peakconnect.app.domain.model

import com.peakconnect.app.core.network.ExperienceLevelSerializer
import kotlinx.serialization.Serializable

@Serializable(with = ExperienceLevelSerializer::class)
enum class ExperienceLevel {
    BEGINNER,
    INTERMEDIATE,
    EXPERT
}
