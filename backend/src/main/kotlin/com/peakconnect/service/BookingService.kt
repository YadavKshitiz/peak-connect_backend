package com.peakconnect.service

import com.peakconnect.dto.BookingConfirmDto
import com.peakconnect.dto.BookingRequestDto
import com.peakconnect.dto.BookingResponse
import com.peakconnect.dto.MatchedGuideResponse
import com.peakconnect.entity.Booking
import com.peakconnect.entity.BookingStatus
import com.peakconnect.matching.GuideMatchingService
import com.peakconnect.repository.BookingRepository
import com.peakconnect.repository.GuideRepository
import com.peakconnect.repository.SlotRepository
import com.peakconnect.repository.UserRepository
import com.peakconnect.payment.DepositCalculator
import com.peakconnect.payment.PaymentApiClient
import com.peakconnect.pricing.PriceCalculator
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.cache.annotation.CacheEvict
import java.util.UUID

@Service
class BookingService(
    private val slotRepository: SlotRepository,
    private val guideRepository: GuideRepository,
    private val userRepository: UserRepository,
    private val bookingRepository: BookingRepository,
    private val guideMatchingService: GuideMatchingService,
    private val priceCalculator: PriceCalculator,
    private val depositCalculator: DepositCalculator,
    private val paymentApiClient: PaymentApiClient
) {

    @Transactional(readOnly = true)
    fun requestBooking(dto: BookingRequestDto): List<MatchedGuideResponse> {
        val slot = slotRepository.findById(dto.slotId)
            .orElseThrow { com.peakconnect.exception.ResourceNotFoundException("Slot not found") }
            
        return guideMatchingService.matchGuidesForSlot(slot)
    }

    @Transactional
    @CacheEvict(value = ["guideAvailabilityCache"], key = "#dto.slotId.toString()")
    fun confirmBooking(dto: BookingConfirmDto, trekkerEmail: String): BookingResponse {
        val trekker = userRepository.findByEmail(trekkerEmail)
            ?: throw com.peakconnect.exception.ResourceNotFoundException("Trekker not found")
            
        val guide = guideRepository.findById(dto.guideId)
            .orElseThrow { com.peakconnect.exception.ResourceNotFoundException("Guide not found") }

        // Acquire Pessimistic Write Lock on the Slot row to serialize concurrent booking attempts
        val slot = slotRepository.findSlotByIdForUpdate(dto.slotId)
            .orElseThrow { com.peakconnect.exception.ResourceNotFoundException("Slot not found") }

        if (slot.currentOccupancy >= slot.capacity) {
            throw com.peakconnect.exception.ConflictException("Slot is already full")
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
            trekker = trekker,
            guide = guide,
            status = BookingStatus.AWAITING_PAYMENT,
            paymentOrderId = orderId
        )
        val savedBooking = bookingRepository.save(booking)

        val response = toBookingResponse(savedBooking)
        return response.copy(depositAmount = depositAmount)
    }

    @Transactional
    @CacheEvict(value = ["guideAvailabilityCache"], key = "#result.slotId.toString()")
    fun markBookingConfirmed(paymentOrderId: String): BookingResponse {
        val booking = bookingRepository.findByPaymentOrderId(paymentOrderId)
            ?: throw com.peakconnect.exception.ResourceNotFoundException("Booking with payment order ID not found")
            
        if (booking.status != BookingStatus.AWAITING_PAYMENT) {
            return toBookingResponse(booking) // already processed
        }
        
        booking.status = BookingStatus.CONFIRMED
        return toBookingResponse(bookingRepository.save(booking))
    }

    @Transactional
    @CacheEvict(value = ["guideAvailabilityCache"], key = "#result.slotId.toString()")
    fun markBookingCancelled(paymentOrderId: String): BookingResponse {
        val booking = bookingRepository.findByPaymentOrderId(paymentOrderId)
            ?: throw com.peakconnect.exception.ResourceNotFoundException("Booking with payment order ID not found")
            
        if (booking.status == BookingStatus.CANCELLED) {
            return toBookingResponse(booking) // already cancelled
        }
        
        booking.status = BookingStatus.CANCELLED
        
        val slot = booking.slot
        if (slot.currentOccupancy > 0) {
            slot.currentOccupancy--
            slotRepository.save(slot)
        }
        
        return toBookingResponse(bookingRepository.save(booking))
    }

    @Transactional(readOnly = true)
    fun getMyBookings(email: String): List<BookingResponse> {
        val user = userRepository.findByEmail(email)
            ?: throw com.peakconnect.exception.ResourceNotFoundException("User not found")
            
        return bookingRepository.findByTrekkerId(user.id!!).map { toBookingResponse(it) }
    }

    @Transactional
    @org.springframework.cache.annotation.CacheEvict(value = ["guideAvailabilityCache"], key = "#result.slotId.toString()")
    fun cancelBooking(bookingId: UUID, email: String): BookingResponse {
        val user = userRepository.findByEmail(email)
            ?: throw com.peakconnect.exception.ResourceNotFoundException("User not found")
            
        val booking = bookingRepository.findById(bookingId)
            .orElseThrow { com.peakconnect.exception.ResourceNotFoundException("Booking not found") }
            
        if (booking.trekker.id != user.id) {
            throw org.springframework.security.access.AccessDeniedException("You can only cancel your own bookings")
        }

        if (booking.status == BookingStatus.CANCELLED) {
            throw com.peakconnect.exception.ConflictException("Booking is already cancelled")
        }

        booking.status = BookingStatus.CANCELLED
        
        val slot = booking.slot
        if (slot.currentOccupancy > 0) {
            slot.currentOccupancy--
            slotRepository.save(slot)
        }

        return toBookingResponse(bookingRepository.save(booking))
    }

    @Transactional(readOnly = true)
    fun getGuideBookings(email: String): List<BookingResponse> {
        val guideUser = userRepository.findByEmail(email)
            ?: throw com.peakconnect.exception.ResourceNotFoundException("Guide user not found")
            
        return bookingRepository.findByGuideId(guideUser.id!!).map { toBookingResponse(it) }
    }

    private fun toBookingResponse(b: Booking): BookingResponse {
        return BookingResponse(
            id = b.id!!,
            slotId = b.slot.id!!,
            activityTitle = b.slot.activity.title,
            trekkerId = b.trekker.id!!,
            trekkerName = b.trekker.name,
            guideId = b.guide?.id,
            guideName = b.guide?.user?.name,
            status = b.status,
            date = b.slot.date,
            paymentOrderId = b.paymentOrderId
        )
    }
}
