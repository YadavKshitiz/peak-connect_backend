package com.peakconnect.service

import com.peakconnect.dto.GuideProfileUpdateRequest
import com.peakconnect.dto.GuideResponse
import com.peakconnect.entity.Guide
import com.peakconnect.repository.GuideRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class GuideService(
    private val guideRepository: GuideRepository
) {
    @Transactional
    fun updateMyProfile(guideId: UUID, request: GuideProfileUpdateRequest): GuideResponse {
        val guide = guideRepository.findById(guideId).orElseThrow { IllegalArgumentException("Guide profile not found") }
        request.skills?.let { guide.skills = it.toMutableList() }
        request.languages?.let { guide.languages = it.toMutableList() }
        request.location?.let { guide.location = it }
        guide.experienceLevel = request.experienceLevel
        return toGuideResponse(guideRepository.save(guide))
    }

    @Transactional(readOnly = true)
    fun getMyProfile(guideId: UUID): GuideResponse {
        val guide = guideRepository.findById(guideId).orElseThrow { IllegalArgumentException("Guide profile not found") }
        return toGuideResponse(guide)
    }

    @Transactional
    fun approveGuide(guideId: UUID): GuideResponse {
        val guide = guideRepository.findById(guideId).orElseThrow { IllegalArgumentException("Guide profile not found") }
        guide.isVerified = true
        return toGuideResponse(guideRepository.save(guide))
    }

    @Transactional
    fun rejectGuide(guideId: UUID): GuideResponse {
        val guide = guideRepository.findById(guideId).orElseThrow { IllegalArgumentException("Guide profile not found") }
        guide.isVerified = false
        return toGuideResponse(guideRepository.save(guide))
    }

    @Transactional(readOnly = true)
    fun listGuides(isVerified: Boolean?): List<GuideResponse> {
        val guides = if (isVerified != null) {
            guideRepository.findByIsVerified(isVerified)
        } else {
            guideRepository.findAll()
        }
        return guides.map { toGuideResponse(it) }
    }

    private fun toGuideResponse(g: Guide): GuideResponse {
        return GuideResponse(
            id = g.id!!,
            name = g.user.name,
            email = g.user.email,
            skills = g.skills.toList(),
            languages = g.languages.toList(),
            location = g.location,
            experienceLevel = g.experienceLevel,
            isVerified = g.isVerified
        )
    }
}
