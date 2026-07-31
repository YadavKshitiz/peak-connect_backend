package com.peakconnect.dto

import com.peakconnect.entity.ExperienceLevel
import java.util.UUID

data class MatchedGuideResponse(
    val guideId: UUID,
    val name: String,
    val skills: List<String>,
    val languages: List<String>,
    val location: String?,
    val experienceLevel: ExperienceLevel,
    val score: Double
)
