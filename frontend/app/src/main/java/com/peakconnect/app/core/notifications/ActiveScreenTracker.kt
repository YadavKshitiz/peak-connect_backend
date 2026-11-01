package com.peakconnect.app.core.notifications

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ActiveScreenTracker @Inject constructor() {
    @Volatile
    var activeBookingId: String? = null
        private set

    fun onBookingScreenEntered(bookingId: String) {
        activeBookingId = bookingId
    }

    fun onBookingScreenExited(bookingId: String) {
        if (activeBookingId == bookingId) {
            activeBookingId = null
        }
    }
}
