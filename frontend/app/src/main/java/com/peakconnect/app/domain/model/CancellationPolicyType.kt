package com.peakconnect.app.domain.model

import com.peakconnect.app.core.network.CancellationPolicyTypeSerializer
import kotlinx.serialization.Serializable

@Serializable(with = CancellationPolicyTypeSerializer::class)
enum class CancellationPolicyType {
    FLEXIBLE,
    MODERATE,
    STRICT
}
