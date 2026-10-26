package com.peakconnect.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.peakconnect.app.domain.model.ExperienceLevel
import com.peakconnect.app.domain.model.GuideProfile

@Entity(tableName = "guide_profile")
data class GuideProfileEntity(
    @PrimaryKey val id: String,
    val name: String,
    val email: String,
    val skills: List<String>,
    val languages: List<String>,
    val location: String?,
    val latitude: Double?,
    val longitude: Double?,
    val experienceLevel: ExperienceLevel,
    val isVerified: Boolean,
    val lastUpdated: Long
)

fun GuideProfileEntity.toDomain(): GuideProfile = GuideProfile(
    id = id,
    name = name,
    email = email,
    skills = skills,
    languages = languages,
    location = location,
    latitude = latitude,
    longitude = longitude,
    experienceLevel = experienceLevel,
    isVerified = isVerified,
    lastUpdated = lastUpdated
)
