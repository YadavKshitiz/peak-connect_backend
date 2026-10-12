package com.peakconnect.controller

import com.peakconnect.dto.BookingConfirmDto
import com.peakconnect.dto.BookingRequestDto
import com.peakconnect.dto.BookingResponse
import com.peakconnect.dto.MatchedGuideResponse
import com.peakconnect.service.BookingService
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*
import java.security.Principal
import java.util.UUID

@RestController
@RequestMapping("/api/bookings")
class BookingController(
    private val bookingService: BookingService,
    private val waitlistService: com.peakconnect.service.WaitlistService,
    private val demandTrackerService: com.peakconnect.pricing.DemandTrackerService,
    private val appMetricsService: com.peakconnect.metrics.AppMetricsService
) {

    @PostMapping("/request")
    @PreAuthorize("hasRole('TREKKER')")
    fun requestBooking(
        @RequestBody request: BookingRequestDto,
        principal: Principal
    ): ResponseEntity<List<MatchedGuideResponse>> {
        return appMetricsService.timeBookingEndpoint("request") {
            demandTrackerService.trackSlotView(request.slotId)
            ResponseEntity.ok(bookingService.requestBooking(request, principal.name))
        }
    }

    @PostMapping("/confirm")
    @PreAuthorize("hasRole('TREKKER')")
    fun confirmBooking(
        @RequestBody request: BookingConfirmDto,
        principal: Principal
    ): ResponseEntity<BookingResponse> {
        return appMetricsService.timeBookingEndpoint("confirm") {
            ResponseEntity.ok(bookingService.confirmBooking(request, principal.name))
        }
    }

    @GetMapping("/my")
    @PreAuthorize("hasRole('TREKKER')")
    fun getMyBookings(principal: Principal): ResponseEntity<List<BookingResponse>> {
        return appMetricsService.timeBookingEndpoint("my") {
            ResponseEntity.ok(bookingService.getMyBookings(principal.name))
        }
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasRole('TREKKER')")
    fun cancelBooking(
        @PathVariable id: UUID,
        principal: Principal
    ): ResponseEntity<BookingResponse> {
        return appMetricsService.timeBookingEndpoint("cancel") {
            ResponseEntity.ok(bookingService.cancelBooking(id, principal.name))
        }
    }

    @PostMapping("/{id}/no-show")
    @PreAuthorize("hasRole('GUIDE')")
    fun markNoShow(
        @PathVariable id: UUID,
        principal: Principal
    ): ResponseEntity<BookingResponse> {
        return appMetricsService.timeBookingEndpoint("no-show") {
            ResponseEntity.ok(bookingService.markNoShow(id, principal.name))
        }
    }

    @PostMapping("/{id}/accept")
    @PreAuthorize("hasRole('GUIDE')")
    fun acceptBooking(
        @PathVariable id: UUID,
        principal: Principal
    ): ResponseEntity<BookingResponse> {
        return appMetricsService.timeBookingEndpoint("accept") {
            ResponseEntity.ok(bookingService.acceptBooking(id, principal.name))
        }
    }

    @PostMapping("/{id}/decline")
    @PreAuthorize("hasRole('GUIDE')")
    fun declineBooking(
        @PathVariable id: UUID,
        principal: Principal
    ): ResponseEntity<BookingResponse> {
        return appMetricsService.timeBookingEndpoint("decline") {
            ResponseEntity.ok(bookingService.declineBooking(id, principal.name))
        }
    }

    @PostMapping("/waitlist")
    @PreAuthorize("hasRole('TREKKER')")
    fun joinWaitlist(
        @RequestBody request: BookingConfirmDto,
        principal: Principal
    ): ResponseEntity<Map<String, String>> {
        return appMetricsService.timeBookingEndpoint("waitlist") {
            waitlistService.joinWaitlist(request, principal.name)
            ResponseEntity.ok(mapOf("message" to "Successfully joined waitlist"))
        }
    }
}
