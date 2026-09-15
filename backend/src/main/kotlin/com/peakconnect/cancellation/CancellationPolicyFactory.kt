package com.peakconnect.cancellation

import com.peakconnect.entity.CancellationPolicyType
import org.springframework.stereotype.Component

@Component
class CancellationPolicyFactory(
    private val flexiblePolicy: FlexiblePolicy,
    private val moderatePolicy: ModeratePolicy,
    private val strictPolicy: StrictPolicy
) {
    fun getPolicy(type: CancellationPolicyType): CancellationPolicy {
        return when (type) {
            CancellationPolicyType.FLEXIBLE -> flexiblePolicy
            CancellationPolicyType.MODERATE -> moderatePolicy
            CancellationPolicyType.STRICT -> strictPolicy
        }
    }
}
