package com.peakconnect.app.domain.model

enum class BookingStatus {
    PENDING_GUIDE_RESPONSE,
    AWAITING_PAYMENT,
    CONFIRMED,
    CANCELLED,
    NO_SHOW,
    REMATCH_FAILED,
    PENDING
}
