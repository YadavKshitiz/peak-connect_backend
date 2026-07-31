package com.peakconnect.matching

import com.peakconnect.entity.Guide
import com.peakconnect.entity.Slot

/**
 * Interface for guide matching strategies.
 * Returns a numeric score representing how well a Guide matches a Slot's requirements.
 */
interface Matcher {
    fun match(slot: Slot, guide: Guide): Double
}
