package com.peakconnect.dto

import com.peakconnect.entity.BookingStatus
import java.time.LocalDateTime
import java.util.UUID

data class BookingRequestDto(
    val slotId: UUID
)

data class BookingConfirmDto(
    val slotId: UUID,
    val guideId: UUID
)

data class BookingResponse(
    val id: UUID,
    val slotId: UUID,
    val activityTitle: String,
    val trekkerId: UUID,
    val trekkerName: String,
    val guideId: UUID?,
    val guideName: String?,
    val status: BookingStatus,
    val date: LocalDateTime,
    val paymentOrderId: String? = null,
    val depositAmount: Double? = null
)
