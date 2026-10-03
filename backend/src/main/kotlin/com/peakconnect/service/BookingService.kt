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
    private val paymentApiClient: PaymentApiClient,
    private val cancellationPolicyFactory: com.peakconnect.cancellation.CancellationPolicyFactory,
    private val waitlistService: WaitlistService
) {

    @Transactional(readOnly = true)
    fun requestBooking(dto: BookingRequestDto, trekkerEmail: String): List<MatchedGuideResponse> {
        val slot = slotRepository.findById(dto.slotId)
            .orElseThrow { com.peakconnect.exception.ResourceNotFoundException("Slot not found") }
            
        return guideMatchingService.matchGuidesForSlot(
            slot = slot,
            trekkerEmail = trekkerEmail,
            skillWeight = dto.skillWeight,
            locationWeight = dto.locationWeight,
            languageWeight = dto.languageWeight
        )
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
            throw com.peakconnect.exception.ConflictException("Slot is already full. Join waitlist?")
        }

        slot.currentOccupancy++
        slotRepository.save(slot)

        // Calculate price (deposit calculation deferred until guide accepts)
        val slotPrice = priceCalculator.calculateFinalPrice(slot.activity.basePrice, slot)

        val booking = Booking(
            slot = slot,
            trekker = trekker,
            guide = guide,
            status = BookingStatus.PENDING_GUIDE_RESPONSE,
            lastGuideAssignedAt = java.time.LocalDateTime.now(),
            totalPrice = slotPrice.toDouble()
        )
        val savedBooking = bookingRepository.save(booking)

        return toBookingResponse(savedBooking)
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
        val savedBooking = bookingRepository.save(booking)
        
        waitlistService.markConfirmed(savedBooking.id!!)
        
        return toBookingResponse(savedBooking)
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

        if (booking.status == BookingStatus.CONFIRMED) {
            // Refund logic
            val policy = cancellationPolicyFactory.getPolicy(booking.slot.activity.cancellationPolicy)
            val refundPercentage = policy.calculateRefundPercentage(booking, java.time.LocalDateTime.now())
            
            // Safeguard: ensure we have persisted price data
            val totalPrice = booking.totalPrice
                ?: throw IllegalStateException("Cannot process refund: Booking is missing original totalPrice.")
            val depositAmount = booking.depositAmount
                ?: throw IllegalStateException("Cannot process refund: Booking is missing original depositAmount.")
            
            // Calculate non-deposit amount which is refundable
            val refundableBase = totalPrice - depositAmount
            val refundAmount = refundableBase * refundPercentage
            
            booking.refundPercentage = refundPercentage
            booking.refundAmount = refundAmount
            
            // TODO: Real gateway refund call goes here if available (e.g. Razorpay refunds API).
            // Currently recorded as calculated, gateway refund pending.
            println("Refund calculated and recorded for booking ${booking.id}: Amount=$refundAmount ($refundPercentage%), gateway refund call pending.")
        }

        booking.status = BookingStatus.CANCELLED
        
        val slot = booking.slot
        if (slot.currentOccupancy > 0) {
            slot.currentOccupancy--
            slotRepository.save(slot)
            waitlistService.promoteNext(slot.id!!)
        }

        return toBookingResponse(bookingRepository.save(booking))
    }

    @Transactional
    @org.springframework.cache.annotation.CacheEvict(value = ["guideAvailabilityCache"], key = "#result.slotId.toString()")
    fun markNoShow(bookingId: UUID, email: String): BookingResponse {
        val guideUser = userRepository.findByEmail(email)
            ?: throw com.peakconnect.exception.ResourceNotFoundException("User not found")
            
        val booking = bookingRepository.findById(bookingId)
            .orElseThrow { com.peakconnect.exception.ResourceNotFoundException("Booking not found") }
            
        if (booking.guide?.user?.id != guideUser.id) {
            throw org.springframework.security.access.AccessDeniedException("You can only mark no-show for your assigned bookings")
        }

        if (booking.status != BookingStatus.CONFIRMED) {
            throw IllegalArgumentException("Only confirmed bookings can be marked as no-show")
        }

        if (java.time.LocalDateTime.now().isBefore(booking.slot.date)) {
            throw IllegalArgumentException("Cannot mark no-show before the activity date")
        }

        booking.status = BookingStatus.NO_SHOW
        booking.refundPercentage = 0.0
        booking.refundAmount = 0.0

        // In a NO_SHOW, the slot capacity remains consumed as the trekker didn't cancel in time.
        return toBookingResponse(bookingRepository.save(booking))
    }

    @Transactional(readOnly = true)
    fun getGuideBookings(email: String): List<BookingResponse> {
        val guideUser = userRepository.findByEmail(email)
            ?: throw com.peakconnect.exception.ResourceNotFoundException("Guide user not found")
            
        return bookingRepository.findByGuideId(guideUser.id!!).map { toBookingResponse(it) }
    }

    @Transactional
    @org.springframework.cache.annotation.CacheEvict(value = ["guideAvailabilityCache"], key = "#result.slotId.toString()")
    fun acceptBooking(bookingId: UUID, guideEmail: String): BookingResponse {
        val guideUser = userRepository.findByEmail(guideEmail)
            ?: throw com.peakconnect.exception.ResourceNotFoundException("Guide user not found")
            
        val booking = bookingRepository.findById(bookingId)
            .orElseThrow { com.peakconnect.exception.ResourceNotFoundException("Booking not found") }
            
        if (booking.guide?.user?.id != guideUser.id) {
            throw org.springframework.security.access.AccessDeniedException("You can only accept bookings assigned to you")
        }

        if (booking.status != BookingStatus.PENDING_GUIDE_RESPONSE) {
            throw com.peakconnect.exception.ConflictException("Booking is not pending your response")
        }

        // Calculate deposit based on the already computed totalPrice
        val depositAmount = depositCalculator.calculateDeposit(booking.totalPrice ?: 0.0)

        // Create Razorpay order
        val orderId = paymentApiClient.createOrder(depositAmount)

        booking.status = BookingStatus.AWAITING_PAYMENT
        booking.paymentOrderId = orderId
        booking.depositAmount = depositAmount

        val savedBooking = bookingRepository.save(booking)
        return toBookingResponse(savedBooking).copy(depositAmount = depositAmount)
    }

    @Transactional
    @org.springframework.cache.annotation.CacheEvict(value = ["guideAvailabilityCache"], key = "#result.slotId.toString()")
    fun declineBooking(bookingId: UUID, guideEmail: String): BookingResponse {
        val guideUser = userRepository.findByEmail(guideEmail)
            ?: throw com.peakconnect.exception.ResourceNotFoundException("Guide user not found")
            
        val booking = bookingRepository.findById(bookingId)
            .orElseThrow { com.peakconnect.exception.ResourceNotFoundException("Booking not found") }
            
        if (booking.guide?.user?.id != guideUser.id) {
            throw org.springframework.security.access.AccessDeniedException("You can only decline bookings assigned to you")
        }

        if (booking.status != BookingStatus.PENDING_GUIDE_RESPONSE) {
            throw com.peakconnect.exception.ConflictException("Booking is not pending your response")
        }

        // Create new set to avoid Hibernate collection sharing/caching anomalies
        val newSet = booking.declinedGuideIds.toMutableSet()
        newSet.add(booking.guide!!.id!!)
        booking.declinedGuideIds = newSet
        
        rematchGuide(booking)
        
        return toBookingResponse(bookingRepository.save(booking))
    }

    fun rematchGuide(booking: Booking) {
        // Reuse matching logic to find next best guide, excluding declined ones
        // Since we don't store original weights, we use default V1 weights for auto-rematch
        val matches = guideMatchingService.matchGuidesForSlot(
            slot = booking.slot,
            trekkerEmail = booking.trekker.email,
            excludeGuideIds = booking.declinedGuideIds
        )

        val nextBestGuideResponse = matches.firstOrNull()

        if (nextBestGuideResponse != null) {
            val nextGuide = guideRepository.findById(nextBestGuideResponse.guideId).orElse(null)
            if (nextGuide != null) {
                booking.guide = nextGuide
                booking.lastGuideAssignedAt = java.time.LocalDateTime.now()
                booking.status = BookingStatus.PENDING_GUIDE_RESPONSE
                return
            }
        }

        // Rematch failed
        booking.guide = null
        booking.status = BookingStatus.REMATCH_FAILED

        // Free slot capacity
        val slot = booking.slot
        if (slot.currentOccupancy > 0) {
            slot.currentOccupancy--
            slotRepository.save(slot)
        }
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
            paymentOrderId = b.paymentOrderId,
            depositAmount = b.depositAmount,
            refundAmount = b.refundAmount,
            refundPercentage = b.refundPercentage
        )
    }
}
