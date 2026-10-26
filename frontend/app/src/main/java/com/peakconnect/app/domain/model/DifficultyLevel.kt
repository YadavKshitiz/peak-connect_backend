package com.peakconnect.app.domain.model

import com.peakconnect.app.core.network.DifficultyLevelSerializer
import kotlinx.serialization.Serializable

@Serializable(with = DifficultyLevelSerializer::class)
enum class DifficultyLevel {
    EASY,
    MODERATE,
    CHALLENGING,
    EXTREME
}

typealias Difficulty = DifficultyLevel
