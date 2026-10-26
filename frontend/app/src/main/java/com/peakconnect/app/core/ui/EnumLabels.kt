package com.peakconnect.app.core.ui

import androidx.annotation.StringRes
import com.peakconnect.app.R
import com.peakconnect.app.domain.model.BookingStatus
import com.peakconnect.app.domain.model.CancellationPolicyType
import com.peakconnect.app.domain.model.DifficultyLevel
import com.peakconnect.app.domain.model.ExperienceLevel
import com.peakconnect.app.domain.model.RiskLevel
import com.peakconnect.app.domain.model.Role
import com.peakconnect.app.domain.model.Season

@StringRes
fun BookingStatus.labelRes(): Int = when (this) {
    BookingStatus.PENDING -> R.string.status_pending
    BookingStatus.PENDING_GUIDE_RESPONSE -> R.string.status_pending_guide_response
    BookingStatus.AWAITING_PAYMENT -> R.string.status_awaiting_payment
    BookingStatus.CONFIRMED -> R.string.status_confirmed
    BookingStatus.CANCELLED -> R.string.status_cancelled
    BookingStatus.NO_SHOW -> R.string.status_no_show
    BookingStatus.REMATCH_FAILED -> R.string.status_rematch_failed
}

@StringRes
fun RiskLevel.labelRes(): Int = when (this) {
    RiskLevel.LOW -> R.string.risk_low
    RiskLevel.MODERATE -> R.string.risk_moderate
    RiskLevel.HIGH -> R.string.risk_high
    RiskLevel.UNKNOWN -> R.string.risk_unknown
}

@StringRes
fun DifficultyLevel.labelRes(): Int = when (this) {
    DifficultyLevel.EASY -> R.string.difficulty_easy
    DifficultyLevel.MODERATE -> R.string.difficulty_moderate
    DifficultyLevel.CHALLENGING -> R.string.difficulty_challenging
    DifficultyLevel.EXTREME -> R.string.difficulty_extreme
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
    ExperienceLevel.EXPERT -> R.string.experience_expert
}

@StringRes
fun Role.labelRes(): Int = when (this) {
    Role.TREKKER -> R.string.role_trekker
    Role.GUIDE -> R.string.role_guide
    Role.ADMIN -> R.string.role_admin
}

@StringRes
fun CancellationPolicyType.labelRes(): Int = when (this) {
    CancellationPolicyType.FLEXIBLE -> R.string.cancellation_flexible
    CancellationPolicyType.MODERATE -> R.string.cancellation_moderate
    CancellationPolicyType.STRICT -> R.string.cancellation_strict
}
