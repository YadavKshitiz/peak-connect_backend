package com.peakconnect.service

import com.peakconnect.dto.BookingConfirmDto
import com.peakconnect.entity.*
import com.peakconnect.exception.ConflictException
import com.peakconnect.exception.ResourceNotFoundException
import com.peakconnect.repository.*
import com.peakconnect.pricing.PriceCalculator
import com.peakconnect.payment.DepositCalculator
import com.peakconnect.payment.PaymentApiClient
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.util.UUID

@Service
class WaitlistService(
    private val waitlistRepository: WaitlistRepository,
    private val slotRepository: SlotRepository,
    private val userRepository: UserRepository,
    private val guideRepository: GuideRepository,
    private val bookingRepository: BookingRepository,
    private val priceCalculator: PriceCalculator,
    private val depositCalculator: DepositCalculator,
    private val paymentApiClient: PaymentApiClient
) {

    @Transactional
    fun joinWaitlist(dto: BookingConfirmDto, trekkerEmail: String) {
        val trekker = userRepository.findByEmail(trekkerEmail)
            ?: throw ResourceNotFoundException("Trekker not found")
            
        val slot = slotRepository.findById(dto.slotId)
            .orElseThrow { ResourceNotFoundException("Slot not found") }

        if (slot.currentOccupancy < slot.capacity) {
            throw ConflictException("Slot is not full yet. You can book directly.")
        }

        val guide = guideRepository.findById(dto.guideId)
            .orElseThrow { ResourceNotFoundException("Guide not found") }

        val waitlist = Waitlist(
            slot = slot,
            trekker = trekker,
            guide = guide,
            status = WaitlistStatus.WAITING
        )
        waitlistRepository.save(waitlist)
    }

    @Transactional
    fun promoteNext(slotId: UUID) {
        // Find earliest WAITING entry
        val waitingEntries = waitlistRepository.findBySlotIdAndStatusOrderByCreatedAtAsc(slotId, WaitlistStatus.WAITING)
        if (waitingEntries.isEmpty()) {
            return
        }

        val nextEntry = waitingEntries.first()
        val slot = nextEntry.slot

        // Ensure slot actually has capacity now
        if (slot.currentOccupancy >= slot.capacity) {
            return
        }

        slot.currentOccupancy++
        slotRepository.save(slot)

        // Calculate price and deposit
        val slotPrice = priceCalculator.calculateFinalPrice(slot.activity.basePrice, slot)
        val depositAmount = depositCalculator.calculateDeposit(slotPrice.toDouble())

        // Create Razorpay order
        val orderId = paymentApiClient.createOrder(depositAmount)

        val booking = Booking(
            slot = slot,
            trekker = nextEntry.trekker,
            guide = nextEntry.guide,
            status = BookingStatus.AWAITING_PAYMENT,
            paymentOrderId = orderId,
            depositAmount = depositAmount,
            totalPrice = slotPrice.toDouble(),
            isFromWaitlist = true
        )
        val savedBooking = bookingRepository.save(booking)

        // Mark waitlist entry as promoted
        nextEntry.status = WaitlistStatus.PROMOTED
        nextEntry.promotedAt = LocalDateTime.now()
        nextEntry.booking = savedBooking
        waitlistRepository.save(nextEntry)

        // TODO: trigger push notification here once FCM is added in V2 Part 2
    }

    @Transactional
    fun markConfirmed(bookingId: UUID) {
        val waitlist = waitlistRepository.findByBookingId(bookingId)
        if (waitlist != null && waitlist.status == WaitlistStatus.PROMOTED) {
            waitlist.status = WaitlistStatus.CONFIRMED
            waitlistRepository.save(waitlist)
        }
    }
}
