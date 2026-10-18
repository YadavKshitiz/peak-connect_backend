package com.peakconnect.app.core.ui

import androidx.annotation.StringRes
import com.peakconnect.app.R
import com.peakconnect.app.domain.model.BookingStatus
import com.peakconnect.app.domain.model.Difficulty
import com.peakconnect.app.domain.model.ExperienceLevel
import com.peakconnect.app.domain.model.RiskLevel
import com.peakconnect.app.domain.model.Season

@StringRes
fun BookingStatus.labelRes(): Int = when (this) {
    BookingStatus.PENDING_GUIDE_RESPONSE -> R.string.status_pending_guide_response
    BookingStatus.AWAITING_PAYMENT -> R.string.status_awaiting_payment
    BookingStatus.CONFIRMED -> R.string.status_confirmed
    BookingStatus.CANCELLED -> R.string.status_cancelled
    BookingStatus.NO_SHOW -> R.string.status_no_show
    BookingStatus.REMATCH_FAILED -> R.string.status_rematch_failed
    BookingStatus.PENDING -> R.string.status_pending
}

@StringRes
fun RiskLevel.labelRes(): Int = when (this) {
    RiskLevel.LOW -> R.string.risk_low
    RiskLevel.MODERATE -> R.string.risk_moderate
    RiskLevel.HIGH -> R.string.risk_high
    RiskLevel.UNKNOWN -> R.string.risk_unknown
}

@StringRes
fun Difficulty.labelRes(): Int = when (this) {
    Difficulty.EASY -> R.string.difficulty_easy
    Difficulty.MODERATE -> R.string.difficulty_moderate
    Difficulty.CHALLENGING -> R.string.difficulty_challenging
    Difficulty.DIFFICULT -> R.string.difficulty_difficult
    Difficulty.EXPERT -> R.string.difficulty_expert
}

@StringRes
fun Season.labelRes(): Int = when (this) {
    Season.LOW -> R.string.season_low
    Season.SHOULDER -> R.string.season_shoulder
    Season.PEAK -> R.string.season_peak
}

@StringRes
fun ExperienceLevel.labelRes(): Int = when (this) {
    ExperienceLevel.BEGINNER -> R.string.experience_beginner
    ExperienceLevel.INTERMEDIATE -> R.string.experience_intermediate
    ExperienceLevel.ADVANCED -> R.string.experience_advanced
    ExperienceLevel.EXPERT -> R.string.experience_expert
}
