package com.peakconnect.matching

import com.peakconnect.entity.Guide
import com.peakconnect.entity.Slot
import com.peakconnect.routing.OsrmRoutingClient
import org.springframework.stereotype.Component
import kotlin.math.max

/**
 * Scores a Guide based on location proximity to the Activity.
 * Upgraded logic:
 * - If lat/lng available, uses OSRM for real driving distance (normalized to 0-1).
 * - Fallback: Exact string match between guide location and activity location -> score 1.0, otherwise 0.0.
 */
@Component
class LocationMatcher(
    private val osrmRoutingClient: OsrmRoutingClient
) : Matcher {
    
    override fun match(slot: Slot, guide: Guide): Double {
        val actLat = slot.activity.latitude
        val actLng = slot.activity.longitude
        val guideLat = guide.latitude
        val guideLng = guide.longitude

        if (actLat != null && actLng != null && guideLat != null && guideLng != null) {
            val distance = osrmRoutingClient.getDistance(guideLat, guideLng, actLat, actLng)
            if (distance >= 0.0) {
                // Normalize distance to 0-1 scale. Cap at 100km (100,000 meters).
                // 0 meters -> score 1.0, 100+ km -> score 0.0
                return max(0.0, 1.0 - (distance / 100_000.0))
            }
        }

        // Fallback to old string matching if lat/lng missing or OSRM failed (returned -1.0)
        val activityLocation = slot.activity.location
        val guideLocation = guide.location
        
        if (guideLocation == null) return 0.0
        
        return if (activityLocation.equals(guideLocation, ignoreCase = true)) 1.0 else 0.0
    }
}
