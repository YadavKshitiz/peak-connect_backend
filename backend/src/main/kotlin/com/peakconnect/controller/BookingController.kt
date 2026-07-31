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
    private val bookingService: BookingService
) {

    @PostMapping("/request")
    @PreAuthorize("hasRole('TREKKER')")
    fun requestBooking(@RequestBody request: BookingRequestDto): ResponseEntity<List<MatchedGuideResponse>> {
        return ResponseEntity.ok(bookingService.requestBooking(request))
    }

    @PostMapping("/confirm")
    @PreAuthorize("hasRole('TREKKER')")
    fun confirmBooking(
        @RequestBody request: BookingConfirmDto,
        principal: Principal
    ): ResponseEntity<BookingResponse> {
        return ResponseEntity.ok(bookingService.confirmBooking(request, principal.name))
    }

    @GetMapping("/my")
    @PreAuthorize("hasRole('TREKKER')")
    fun getMyBookings(principal: Principal): ResponseEntity<List<BookingResponse>> {
        return ResponseEntity.ok(bookingService.getMyBookings(principal.name))
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasRole('TREKKER')")
    fun cancelBooking(
        @PathVariable id: UUID,
        principal: Principal
    ): ResponseEntity<BookingResponse> {
        return ResponseEntity.ok(bookingService.cancelBooking(id, principal.name))
    }
}
