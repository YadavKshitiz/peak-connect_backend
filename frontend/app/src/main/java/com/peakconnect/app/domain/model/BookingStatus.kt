package com.peakconnect.app.domain.model

import com.peakconnect.app.core.network.BookingStatusSerializer
import kotlinx.serialization.Serializable

@Serializable(with = BookingStatusSerializer::class)
enum class BookingStatus {
    PENDING,
    PENDING_GUIDE_RESPONSE,
    AWAITING_PAYMENT,
    CONFIRMED,
    CANCELLED,
    NO_SHOW,
    REMATCH_FAILED
}
