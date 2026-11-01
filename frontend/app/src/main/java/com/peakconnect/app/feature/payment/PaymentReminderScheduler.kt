package com.peakconnect.app.feature.payment

import javax.inject.Inject
import javax.inject.Singleton

interface PaymentReminderScheduler {
    fun schedulePaymentReminder(bookingId: String, reminderTimeMillis: Long)
    fun cancelPaymentReminder(bookingId: String)
}

@Singleton
class NoOpPaymentReminderScheduler @Inject constructor() : PaymentReminderScheduler {
    override fun schedulePaymentReminder(bookingId: String, reminderTimeMillis: Long) {
        // No-op hook. Real implementation will be provided in Task 13.
    }

    override fun cancelPaymentReminder(bookingId: String) {
        // No-op hook. Real implementation will be provided in Task 13.
    }
}
