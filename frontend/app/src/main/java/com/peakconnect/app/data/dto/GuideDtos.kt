package com.peakconnect.app.data.dto

import com.peakconnect.app.domain.model.ExperienceLevel
import kotlinx.serialization.Serializable

@Serializable
data class GuideResponse(
    val id: String,
    val name: String,
    val email: String,
    val skills: List<String> = emptyList(),
    val languages: List<String> = emptyList(),
    val location: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val experienceLevel: ExperienceLevel,
    val isVerified: Boolean = false
)

@Serializable
data class GuideProfileUpdateRequest(
    val skills: List<String>? = null,
    val languages: List<String>? = null,
    val location: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val experienceLevel: ExperienceLevel
)
