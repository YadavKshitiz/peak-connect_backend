package com.peakconnect.matching

import com.peakconnect.entity.Guide
import com.peakconnect.entity.Slot

/**
 * A runtime-configurable combination of SkillBasedMatcher, LocationMatcher, and LanguageMatcher.
 * The WEIGHT given to each can be specified per-request.
 */
class HybridMatcher(
    private val skillBasedMatcher: Matcher,
    private val locationMatcher: Matcher,
    private val languageMatcher: Matcher,
    private val skillWeight: Double,
    private val locationWeight: Double,
    private val languageWeight: Double
) : Matcher {

    override fun match(slot: Slot, guide: Guide): Double {
        val skillScore = skillBasedMatcher.match(slot, guide)
        val locationScore = locationMatcher.match(slot, guide)
        val languageScore = languageMatcher.match(slot, guide)

        // The matchers return a normalized 0.0 to 1.0 score. 
        // We multiply them by their respective weights and sum them up.
        return (skillScore * skillWeight) +
               (locationScore * locationWeight) +
               (languageScore * languageWeight)
    }
}
