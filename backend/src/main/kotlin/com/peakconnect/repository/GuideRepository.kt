package com.peakconnect.repository

import com.peakconnect.entity.Guide
import com.peakconnect.entity.ExperienceLevel
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface GuideRepository : JpaRepository<Guide, UUID> {
    fun findByExperienceLevel(experienceLevel: ExperienceLevel): List<Guide>
    fun findByIsVerified(isVerified: Boolean): List<Guide>
}
