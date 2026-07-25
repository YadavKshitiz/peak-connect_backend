package com.peakconnect.repository

import com.peakconnect.entity.Activity
import com.peakconnect.entity.DifficultyLevel
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface ActivityRepository : JpaRepository<Activity, UUID> {
    fun findByDifficultyLevel(difficultyLevel: DifficultyLevel): List<Activity>
}
