package com.peakconnect.app.domain.model

import com.peakconnect.app.core.network.WaitlistStatusSerializer
import kotlinx.serialization.Serializable

@Serializable(with = WaitlistStatusSerializer::class)
enum class WaitlistStatus {
    WAITING,
    PROMOTED,
    EXPIRED,
    CONFIRMED
}
