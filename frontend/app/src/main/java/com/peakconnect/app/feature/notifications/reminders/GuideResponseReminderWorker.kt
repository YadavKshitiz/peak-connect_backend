package com.peakconnect.app.feature.notifications.reminders

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.peakconnect.app.R
import com.peakconnect.app.core.notifications.LocalNotificationPoster
import com.peakconnect.app.core.notifications.NotificationChannels
import com.peakconnect.app.core.preferences.NotificationPreferences
import com.peakconnect.app.data.local.dao.GuideDao
import com.peakconnect.app.domain.model.BookingStatus
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first

@HiltWorker
class GuideResponseReminderWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted workerParams: WorkerParameters,
    private val guideDao: GuideDao,
    private val notificationPoster: LocalNotificationPoster,
    private val notificationPreferences: NotificationPreferences
) : CoroutineWorker(context, workerParams) {

    companion object {
        const val KEY_BOOKING_ID = "booking_id"
    }

    override suspend fun doWork(): Result {
        val bookingId = inputData.getString(KEY_BOOKING_ID) ?: return Result.failure()

        // Check user preferences
        if (!notificationPreferences.masterEnabled.first() || !notificationPreferences.remindersEnabled.first()) {
            return Result.success()
        }

        // Verify guide booking still in PENDING_GUIDE_RESPONSE status
        val booking = guideDao.getGuideBookingById(bookingId) ?: return Result.success()
        if (booking.status != BookingStatus.PENDING_GUIDE_RESPONSE) {
            return Result.success()
        }

        val title = context.getString(R.string.notif_reminder_guide_title)
        val body = context.getString(R.string.notif_reminder_guide_body)
        val dedupeKey = "reminder_guide_$bookingId"

        notificationPoster.postNotification(
            channelId = NotificationChannels.CHANNEL_REMINDERS,
            title = title,
            body = body,
            type = "GUIDE_REMINDER",
            bookingId = bookingId,
            isGuide = true,
            dedupeKey = dedupeKey
        )

        return Result.success()
    }
}
