package com.peakconnect.app.feature.notifications.reminders

import android.content.Context
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.peakconnect.app.feature.payment.PaymentReminderScheduler
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WorkManagerReminderScheduler @Inject constructor(
    @ApplicationContext private val context: Context
) : PaymentReminderScheduler {

    private val workManager by lazy { WorkManager.getInstance(context) }

    override fun schedulePaymentReminder(bookingId: String, reminderTimeMillis: Long) {
        val delay = (reminderTimeMillis - System.currentTimeMillis()).coerceAtLeast(0L)
        val request = OneTimeWorkRequestBuilder<PaymentReminderWorker>()
            .setInitialDelay(delay, TimeUnit.MILLISECONDS)
            .setInputData(workDataOf(PaymentReminderWorker.KEY_BOOKING_ID to bookingId))
            .build()

        workManager.enqueueUniqueWork(
            "reminder_payment_$bookingId",
            ExistingWorkPolicy.REPLACE,
            request
        )
    }

    override fun cancelPaymentReminder(bookingId: String) {
        workManager.cancelUniqueWork("reminder_payment_$bookingId")
    }

    fun scheduleGuideResponseReminder(bookingId: String, reminderTimeMillis: Long) {
        val delay = (reminderTimeMillis - System.currentTimeMillis()).coerceAtLeast(0L)
        val request = OneTimeWorkRequestBuilder<GuideResponseReminderWorker>()
            .setInitialDelay(delay, TimeUnit.MILLISECONDS)
            .setInputData(workDataOf(GuideResponseReminderWorker.KEY_BOOKING_ID to bookingId))
            .build()

        workManager.enqueueUniqueWork(
            "reminder_guide_$bookingId",
            ExistingWorkPolicy.REPLACE,
            request
        )
    }

    fun cancelGuideResponseReminder(bookingId: String) {
        workManager.cancelUniqueWork("reminder_guide_$bookingId")
    }

    fun scheduleTrekTomorrowReminder(bookingId: String, reminderTimeMillis: Long) {
        val delay = reminderTimeMillis - System.currentTimeMillis()
        if (delay <= 0L) {
            // Skip if less than 24h remain per prompt requirements
            return
        }

        val request = OneTimeWorkRequestBuilder<TrekTomorrowReminderWorker>()
            .setInitialDelay(delay, TimeUnit.MILLISECONDS)
            .setInputData(workDataOf(TrekTomorrowReminderWorker.KEY_BOOKING_ID to bookingId))
            .build()

        workManager.enqueueUniqueWork(
            "reminder_trek_tomorrow_$bookingId",
            ExistingWorkPolicy.REPLACE,
            request
        )
    }

    fun cancelTrekTomorrowReminder(bookingId: String) {
        workManager.cancelUniqueWork("reminder_trek_tomorrow_$bookingId")
    }

    fun cancelAllRemindersForBooking(bookingId: String) {
        cancelPaymentReminder(bookingId)
        cancelGuideResponseReminder(bookingId)
        cancelTrekTomorrowReminder(bookingId)
    }
}
