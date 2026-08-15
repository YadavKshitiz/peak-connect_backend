package com.peakconnect.matching

import com.peakconnect.dto.MatchedGuideResponse
import com.peakconnect.entity.Guide
import com.peakconnect.entity.Slot
import com.peakconnect.repository.GuideRepository
import org.springframework.stereotype.Service
import org.springframework.cache.annotation.Cacheable

@Service
class GuideMatchingService(
    private val guideRepository: GuideRepository,
    private val matchers: List<Matcher>
) {
    /**
     * Finds and ranks eligible, verified guides for a given slot.
     * Weights:
     * - Skill: 60%
     * - Location: 40%
     */
    @Cacheable(value = ["guideAvailabilityCache"], key = "#slot.id.toString()")
    fun matchGuidesForSlot(slot: Slot): List<MatchedGuideResponse> {
        println("COMPUTING GUIDE AVAILABILITY FOR SLOT ${slot.id} (This should not print on cache hit)")
        val verifiedGuides = guideRepository.findByIsVerified(true)
        
        val matchedGuides = verifiedGuides.map { guide ->
            var totalScore = 0.0
            
            for (matcher in matchers) {
                val score = matcher.match(slot, guide)
                val weight = when (matcher) {
                    is SkillBasedMatcher -> 0.60
                    is LocationMatcher -> 0.40
                    else -> 0.0
                }
                totalScore += (score * weight)
            }
            
            MatchedGuideResponse(
                guideId = guide.id!!,
                name = guide.user.name,
                skills = guide.skills.toList(),
                languages = guide.languages.toList(),
                location = guide.location,
                experienceLevel = guide.experienceLevel,
                score = totalScore
            )
        }
        
        // Return descending sorted list by score, filtering out completely unmatched guides (score == 0.0)
        return matchedGuides.filter { it.score > 0.0 }.sortedByDescending { it.score }
    }
}
