package com.peakconnect.matching

import com.peakconnect.entity.Guide
import com.peakconnect.entity.Slot
import com.peakconnect.repository.UserRepository
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component

/**
 * Scores a guide higher if their profile's listed languages overlap
 * with languages the trekker indicates they need.
 */
@Component
class LanguageMatcher(
    private val userRepository: UserRepository
) : Matcher {
    
    override fun match(slot: Slot, guide: Guide): Double {
        val authentication = SecurityContextHolder.getContext().authentication
        if (authentication == null || !authentication.isAuthenticated || authentication.name == "anonymousUser") {
            return 0.0 // No user context
        }

        val trekkerEmail = authentication.name
        val trekker = userRepository.findByEmail(trekkerEmail) ?: return 0.0
        
        val preferred = trekker.preferredLanguages
        if (preferred.isEmpty()) {
            return 1.0 // If trekker didn't specify languages, any guide is a perfect language match
        }

        val guideLangs = guide.languages.map { it.lowercase() }
        var matchCount = 0
        for (lang in preferred) {
            if (guideLangs.contains(lang.lowercase())) {
                matchCount++
            }
        }

        return matchCount.toDouble() / preferred.size.toDouble()
    }
}
