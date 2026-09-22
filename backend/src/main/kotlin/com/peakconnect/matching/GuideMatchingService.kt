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
     * Optionally uses HybridMatcher if weights are provided.
     */
    @Cacheable(
        value = ["guideAvailabilityCache"],
        key = "#slot.id.toString() + '_' + #trekkerEmail + '_' + (#skillWeight ?: 'def') + '_' + (#locationWeight ?: 'def') + '_' + (#languageWeight ?: 'def')"
    )
    fun matchGuidesForSlot(
        slot: Slot,
        trekkerEmail: String,
        skillWeight: Double? = null,
        locationWeight: Double? = null,
        languageWeight: Double? = null
    ): List<MatchedGuideResponse> {
        println("COMPUTING GUIDE AVAILABILITY FOR SLOT ${slot.id} (This should not print on cache hit)")
        val verifiedGuides = guideRepository.findByIsVerified(true)
        
        val useHybrid = skillWeight != null || locationWeight != null || languageWeight != null
        val hybridMatcher = if (useHybrid) {
            val skill = matchers.find { it is SkillBasedMatcher }!!
            val loc = matchers.find { it is LocationMatcher }!!
            val lang = matchers.find { it is LanguageMatcher }!!
            HybridMatcher(
                skillBasedMatcher = skill,
                locationMatcher = loc,
                languageMatcher = lang,
                skillWeight = skillWeight ?: 0.33,
                locationWeight = locationWeight ?: 0.33,
                languageWeight = languageWeight ?: 0.33
            )
        } else null

        val matchedGuides = verifiedGuides.map { guide ->
            var totalScore = 0.0
            
            if (hybridMatcher != null) {
                totalScore = hybridMatcher.match(slot, guide)
            } else {
                for (matcher in matchers) {
                    val score = matcher.match(slot, guide)
                    val weight = when (matcher) {
                        is SkillBasedMatcher -> 0.60
                        is LocationMatcher -> 0.40
                        else -> 0.0 // LanguageMatcher is ignored in V1 default logic
                    }
                    totalScore += (score * weight)
                }
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
