package com.peakconnect.matching

import com.peakconnect.entity.Guide
import com.peakconnect.entity.Slot
import org.springframework.stereotype.Component

/**
 * Scores a Guide based on location proximity to the Activity.
 * Simple V1 Logic:
 * Exact string match between guide location and activity location -> score 1.0
 * Otherwise -> score 0.0
 */
@Component
class LocationMatcher : Matcher {
    override fun match(slot: Slot, guide: Guide): Double {
        val activityLocation = slot.activity.location
        val guideLocation = guide.location
        
        if (guideLocation == null) return 0.0
        
        return if (activityLocation.equals(guideLocation, ignoreCase = true)) 1.0 else 0.0
    }
}
