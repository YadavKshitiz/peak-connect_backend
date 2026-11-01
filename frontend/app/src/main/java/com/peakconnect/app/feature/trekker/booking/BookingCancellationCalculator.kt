package com.peakconnect.app.feature.trekker.booking

import com.peakconnect.app.data.local.entity.BookingEntity
import com.peakconnect.app.domain.model.BookingStatus
import com.peakconnect.app.domain.model.CancellationPolicyInfo
import com.peakconnect.app.domain.model.CancellationPolicyType
import com.peakconnect.app.domain.model.CancellationTier
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

data class CancellationPreview(
    val isPaymentTaken: Boolean,
    val refundPercentage: Int, // e.g. 100, 50, 0
    val refundPercentageRatio: Double, // 1.0, 0.5, 0.0
    val depositAmount: BigDecimal,
    val refundableBase: BigDecimal,
    val estimatedRefundAmount: BigDecimal,
    val policy: CancellationPolicyType,
    val policyTiers: List<CancellationTier>,
    val activeTierIndex: Int,
    val hoursBeforeActivity: Long
)

object BookingCancellationCalculator {

    fun calculatePreview(
        booking: BookingEntity,
        policy: CancellationPolicyType = CancellationPolicyType.FLEXIBLE,
        totalPrice: BigDecimal? = null,
        now: LocalDateTime = LocalDateTime.now()
    ): CancellationPreview {
        val isPaymentTaken = booking.status == BookingStatus.CONFIRMED

        val resolvedDeposit = booking.depositAmount?.let {
            BigDecimal.valueOf(it).setScale(2, RoundingMode.HALF_UP)
        } ?: if (totalPrice != null) {
            CancellationPolicyInfo.calculateEstimatedDeposit(totalPrice)
        } else {
            BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP)
        }

        val resolvedTotalPrice = totalPrice ?: if (resolvedDeposit > BigDecimal.ZERO) {
            resolvedDeposit.divide(BigDecimal.valueOf(CancellationPolicyInfo.DEPOSIT_PERCENTAGE), 2, RoundingMode.HALF_UP)
        } else {
            BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP)
        }

        val refundableBase = maxOf(BigDecimal.ZERO, resolvedTotalPrice.subtract(resolvedDeposit))

        val hoursBefore = ChronoUnit.HOURS.between(now, booking.date)
        val refundRatio = CancellationPolicyInfo.calculateRefundPercentage(policy, hoursBefore)
        val refundPercentInt = (refundRatio * 100).toInt()

        val estimatedRefund = refundableBase.multiply(BigDecimal.valueOf(refundRatio))
            .setScale(2, RoundingMode.HALF_UP)

        val tiers = CancellationPolicyInfo.getTiers(policy)
        val activeTierIndex = determineActiveTierIndex(policy, hoursBefore)

        return CancellationPreview(
            isPaymentTaken = isPaymentTaken,
            refundPercentage = refundPercentInt,
            refundPercentageRatio = refundRatio,
            depositAmount = resolvedDeposit,
            refundableBase = refundableBase,
            estimatedRefundAmount = estimatedRefund,
            policy = policy,
            policyTiers = tiers,
            activeTierIndex = activeTierIndex,
            hoursBeforeActivity = hoursBefore
        )
    }

    fun determineActiveTierIndex(policy: CancellationPolicyType, hoursBeforeActivity: Long): Int {
        return when (policy) {
            CancellationPolicyType.FLEXIBLE -> when {
                hoursBeforeActivity >= CancellationPolicyInfo.FLEXIBLE_FULL_REFUND_HOURS -> 0
                hoursBeforeActivity >= CancellationPolicyInfo.FLEXIBLE_PARTIAL_REFUND_HOURS -> 1
                else -> 2
            }
            CancellationPolicyType.MODERATE -> when {
                hoursBeforeActivity >= CancellationPolicyInfo.MODERATE_FULL_REFUND_HOURS -> 0
                hoursBeforeActivity >= CancellationPolicyInfo.MODERATE_PARTIAL_REFUND_HOURS -> 1
                else -> 2
            }
            CancellationPolicyType.STRICT -> when {
                hoursBeforeActivity >= CancellationPolicyInfo.STRICT_PARTIAL_REFUND_HOURS -> 0
                else -> 1
            }
        }
    }
}
