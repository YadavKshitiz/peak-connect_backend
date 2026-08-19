package com.peakconnect.repository

import com.peakconnect.entity.Booking
import com.peakconnect.entity.BookingStatus
import org.springframework.data.jpa.repository.JpaRepository
import java.time.LocalDateTime
import java.util.UUID

interface BookingRepository : JpaRepository<Booking, UUID> {
    fun findByTrekkerId(trekkerId: UUID): List<Booking>
    fun findByGuideId(guideId: UUID): List<Booking>
    fun findByStatus(status: BookingStatus): List<Booking>
    fun findByStatusAndCreatedAtBefore(status: BookingStatus, createdAt: LocalDateTime): List<Booking>
    fun findByPaymentOrderId(paymentOrderId: String): Booking?
}
