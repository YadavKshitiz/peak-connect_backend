package com.peakconnect.matching

import com.peakconnect.entity.Guide
import com.peakconnect.entity.Slot
import org.springframework.stereotype.Component

/**
 * Scores a Guide based on their experience level vs the Activity's difficulty.
 * Simple V1 Logic: 
 * If guide experience level is >= activity difficulty level, score 1.0
 * Else score 0.0
 * 
 * Mapping:
 * BEGINNER -> 1, INTERMEDIATE -> 2, EXPERT -> 3
 * EASY -> 1, MODERATE -> 2, CHALLENGING -> 3, EXTREME -> 4
 */
@Component
class SkillBasedMatcher : Matcher {
    override fun match(slot: Slot, guide: Guide): Double {
        val guideLevelScore = when (guide.experienceLevel.name) {
            "BEGINNER" -> 1
            "INTERMEDIATE" -> 2
            "EXPERT" -> 3
            else -> 0
        }
        
        val activityLevelScore = when (slot.activity.difficultyLevel.name) {
            "EASY" -> 1
            "MODERATE" -> 2
            "CHALLENGING" -> 3
            "EXTREME" -> 4
            else -> 0
        }
        
        return if (guideLevelScore >= activityLevelScore) 1.0 else 0.0
    }
}
