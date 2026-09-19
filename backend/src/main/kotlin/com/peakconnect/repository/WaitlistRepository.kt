package com.peakconnect.repository

import com.peakconnect.entity.Waitlist
import com.peakconnect.entity.WaitlistStatus
import org.springframework.data.jpa.repository.JpaRepository
import java.time.LocalDateTime
import java.util.UUID

interface WaitlistRepository : JpaRepository<Waitlist, UUID> {
    fun findBySlotIdAndStatusOrderByCreatedAtAsc(slotId: UUID, status: WaitlistStatus): List<Waitlist>
    fun findByStatusAndPromotedAtBefore(status: WaitlistStatus, promotedAt: LocalDateTime): List<Waitlist>
    fun findByBookingId(bookingId: UUID): Waitlist?
}
