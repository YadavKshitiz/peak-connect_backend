package com.peakconnect.app.domain.model

data class CancellationTier(
    val timeline: String,
    val refundPercentage: Int // e.g. 100, 50, 0
)

/**
 * Cancellation policy thresholds, refund tiers, and non-refundable booking deposit percentage.
 * Numbers match backend application.yml and cancellation policy classes exactly.
 * Reused in Task 7 (Activity Detail) and Task 9 (Booking Cancellation).
 */
object CancellationPolicyInfo {
    const val DEPOSIT_PERCENTAGE: Double = 0.20
    const val DEPOSIT_PERCENTAGE_INT: Int = 20

    // Flexible thresholds (hours)
    const val FLEXIBLE_FULL_REFUND_HOURS: Long = 48
    const val FLEXIBLE_PARTIAL_REFUND_HOURS: Long = 12

    // Moderate thresholds (hours)
    const val MODERATE_FULL_REFUND_HOURS: Long = 168 // 7 days
    const val MODERATE_PARTIAL_REFUND_HOURS: Long = 48 // 2 days

    // Strict thresholds (hours)
    const val STRICT_PARTIAL_REFUND_HOURS: Long = 336 // 14 days

    fun getTiers(policy: CancellationPolicyType): List<CancellationTier> {
        return when (policy) {
            CancellationPolicyType.FLEXIBLE -> listOf(
                CancellationTier("More than 48 hours before", 100),
                CancellationTier("12 to 48 hours before", 50),
                CancellationTier("Under 12 hours before", 0)
            )
            CancellationPolicyType.MODERATE -> listOf(
                CancellationTier("More than 7 days before", 100),
                CancellationTier("2 to 7 days before", 50),
                CancellationTier("Under 48 hours before", 0)
            )
            CancellationPolicyType.STRICT -> listOf(
                CancellationTier("More than 14 days before", 50),
                CancellationTier("Under 14 days before", 0)
            )
        }
    }

    fun calculateRefundPercentage(policy: CancellationPolicyType, hoursBeforeActivity: Long): Double {
        return when (policy) {
            CancellationPolicyType.FLEXIBLE -> when {
                hoursBeforeActivity >= FLEXIBLE_FULL_REFUND_HOURS -> 1.0
                hoursBeforeActivity >= FLEXIBLE_PARTIAL_REFUND_HOURS -> 0.5
                else -> 0.0
            }
            CancellationPolicyType.MODERATE -> when {
                hoursBeforeActivity >= MODERATE_FULL_REFUND_HOURS -> 1.0
                hoursBeforeActivity >= MODERATE_PARTIAL_REFUND_HOURS -> 0.5
                else -> 0.0
            }
            CancellationPolicyType.STRICT -> when {
                hoursBeforeActivity >= STRICT_PARTIAL_REFUND_HOURS -> 0.5
                else -> 0.0
            }
        }
    }
}
