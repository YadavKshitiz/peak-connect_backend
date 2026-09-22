package com.peakconnect.dto

import com.peakconnect.entity.ExperienceLevel
import jakarta.validation.constraints.NotNull
import java.util.UUID

data class GuideProfileUpdateRequest(
    val skills: List<String>?,
    val languages: List<String>?,
    val location: String?,
    val latitude: Double? = null,
    val longitude: Double? = null,
    @field:NotNull val experienceLevel: ExperienceLevel
)

data class GuideResponse(
    val id: UUID,
    val name: String,
    val email: String,
    val skills: List<String>,
    val languages: List<String>,
    val location: String?,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val experienceLevel: ExperienceLevel,
    val isVerified: Boolean
)
