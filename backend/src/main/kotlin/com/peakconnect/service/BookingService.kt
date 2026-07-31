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
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class BookingService(
    private val slotRepository: SlotRepository,
    private val guideRepository: GuideRepository,
    private val userRepository: UserRepository,
    private val bookingRepository: BookingRepository,
    private val guideMatchingService: GuideMatchingService
) {

    @Transactional(readOnly = true)
    fun requestBooking(dto: BookingRequestDto): List<MatchedGuideResponse> {
        val slot = slotRepository.findById(dto.slotId)
            .orElseThrow { com.peakconnect.exception.ResourceNotFoundException("Slot not found") }
            
        return guideMatchingService.matchGuidesForSlot(slot)
    }

    @Transactional
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

        val booking = Booking(
            slot = slot,
            trekker = trekker,
            guide = guide,
            status = BookingStatus.CONFIRMED
        )
        val savedBooking = bookingRepository.save(booking)

        return toBookingResponse(savedBooking)
    }

    @Transactional(readOnly = true)
    fun getMyBookings(email: String): List<BookingResponse> {
        val user = userRepository.findByEmail(email)
            ?: throw com.peakconnect.exception.ResourceNotFoundException("User not found")
            
        return bookingRepository.findByTrekkerId(user.id!!).map { toBookingResponse(it) }
    }

    @Transactional
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
            date = b.slot.date
        )
    }
}
