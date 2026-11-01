package com.peakconnect.app.feature.notifications.reminders

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.peakconnect.app.R
import com.peakconnect.app.core.notifications.LocalNotificationPoster
import com.peakconnect.app.core.notifications.NotificationChannels
import com.peakconnect.app.core.preferences.NotificationPreferences
import com.peakconnect.app.data.local.dao.BookingDao
import com.peakconnect.app.domain.model.BookingStatus
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import java.time.format.DateTimeFormatter

@HiltWorker
class TrekTomorrowReminderWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted workerParams: WorkerParameters,
    private val bookingDao: BookingDao,
    private val notificationPoster: LocalNotificationPoster,
    private val notificationPreferences: NotificationPreferences
) : CoroutineWorker(context, workerParams) {

    companion object {
        const val KEY_BOOKING_ID = "booking_id"
        private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")
    }

    override suspend fun doWork(): Result {
        val bookingId = inputData.getString(KEY_BOOKING_ID) ?: return Result.failure()

        // Check user preferences
        if (!notificationPreferences.masterEnabled.first() || !notificationPreferences.remindersEnabled.first()) {
            return Result.success()
        }

        // Verify booking still in CONFIRMED status
        val booking = bookingDao.getBookingById(bookingId) ?: return Result.success()
        if (booking.status != BookingStatus.CONFIRMED) {
            return Result.success()
        }

        val timeStr = try {
            booking.date.format(timeFormatter)
        } catch (_: Exception) {
            "morning"
        }

        val title = context.getString(R.string.notif_reminder_trek_tomorrow_title)
        val body = context.getString(R.string.notif_reminder_trek_tomorrow_body, booking.activityTitle, timeStr)
        val dedupeKey = "reminder_trek_tomorrow_$bookingId"

        notificationPoster.postNotification(
            channelId = NotificationChannels.CHANNEL_REMINDERS,
            title = title,
            body = body,
            type = "TREK_REMINDER",
            bookingId = bookingId,
            isGuide = false,
            dedupeKey = dedupeKey
        )

        return Result.success()
    }
}
