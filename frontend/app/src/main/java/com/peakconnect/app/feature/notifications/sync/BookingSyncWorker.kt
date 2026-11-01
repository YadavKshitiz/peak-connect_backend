package com.peakconnect.app.feature.notifications.sync

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.peakconnect.app.R
import com.peakconnect.app.core.notifications.ActiveScreenTracker
import com.peakconnect.app.core.notifications.LocalNotificationPoster
import com.peakconnect.app.core.notifications.NotificationChannels
import com.peakconnect.app.core.preferences.NotificationPreferences
import com.peakconnect.app.core.session.JwtDecoder
import com.peakconnect.app.core.session.TokenStore
import com.peakconnect.app.data.api.PeakConnectApi
import com.peakconnect.app.data.local.dao.BookingDao
import com.peakconnect.app.data.local.dao.GuideDao
import com.peakconnect.app.data.local.dao.NotifiedEventDao
import com.peakconnect.app.data.local.dao.UserContentDao
import com.peakconnect.app.domain.model.Role
import com.peakconnect.app.feature.notifications.reminders.WorkManagerReminderScheduler
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import java.time.ZoneId

@HiltWorker
class BookingSyncWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted workerParams: WorkerParameters,
    private val tokenStore: TokenStore,
    private val api: PeakConnectApi,
    private val bookingDao: BookingDao,
    private val guideDao: GuideDao,
    private val userContentDao: UserContentDao,
    private val notifiedEventDao: NotifiedEventDao,
    private val notificationPoster: LocalNotificationPoster,
    private val notificationPreferences: NotificationPreferences,
    private val reminderScheduler: WorkManagerReminderScheduler,
    private val activeScreenTracker: ActiveScreenTracker
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val token = tokenStore.getToken()
        // Skip entirely if the session is logged out
        if (token.isNullOrBlank()) {
            return Result.success()
        }

        // Token expiry handling
        if (JwtDecoder.isExpired(token)) {
            val tokenKey = "token_expired_session"
            if (!notifiedEventDao.exists(tokenKey)) {
                notificationPoster.postNotification(
                    channelId = NotificationChannels.CHANNEL_BOOKING_UPDATES,
                    title = context.getString(R.string.notif_token_expired_title),
                    body = context.getString(R.string.notif_token_expired_body),
                    type = "TOKEN_EXPIRED",
                    dedupeKey = tokenKey
                )
            }
            return Result.success()
        }

        // Check if master notifications are enabled
        if (!notificationPreferences.masterEnabled.first()) {
            return Result.success()
        }

        val payload = JwtDecoder.decode(token) ?: return Result.success()
        val role = payload.role

        try {
            when (role) {
                Role.TREKKER -> syncTrekkerBookings()
                Role.GUIDE -> syncGuideBookings()
                else -> { /* Admin unsupported */ }
            }
            notificationPreferences.setLastSyncTimestamp(System.currentTimeMillis())
        } catch (e: Exception) {
            // Periodic sync will retry on next interval or network reconnect
            return Result.retry()
        }

        return Result.success()
    }

    private suspend fun syncTrekkerBookings() {
        val response = api.getMyBookings()
        if (!response.isSuccessful) return

        val incoming = response.body() ?: emptyList()
        val previous = bookingDao.getAllBookings()
        val isFirstSync = previous.isEmpty()
        val waitlistEntries = userContentDao.getAllWaitlistEntries()
        val waitlistSlotIds = waitlistEntries.map { it.slotId }.toSet()

        val diffResult = SyncDiffCalculator.calculateTrekkerDiff(
            previousBookings = previous,
            incomingBookings = incoming,
            isFirstSync = isFirstSync,
            activeBookingId = activeScreenTracker.activeBookingId,
            alreadyNotifiedKeys = emptySet(),
            waitlistSlotIds = waitlistSlotIds
        )

        // Persist updated bookings to Room
        bookingDao.insertBookings(diffResult.updatedBookings)

        // Post notifications if not first sync
        for (notif in diffResult.notifications) {
            if (!notifiedEventDao.exists(notif.dedupeKey)) {
                val title = if (notif.titleArgs.isEmpty()) {
                    context.getString(notif.titleRes)
                } else {
                    context.getString(notif.titleRes, *notif.titleArgs.toTypedArray())
                }
                val body = if (notif.bodyArgs.isEmpty()) {
                    context.getString(notif.bodyRes)
                } else {
                    context.getString(notif.bodyRes, *notif.bodyArgs.toTypedArray())
                }
                notificationPoster.postNotification(
                    channelId = notif.channelId,
                    title = title,
                    body = body,
                    type = notif.type,
                    bookingId = notif.bookingId,
                    isGuide = false,
                    dedupeKey = notif.dedupeKey
                )
            }
        }

        // Cancel reminders
        for (cancel in diffResult.remindersToCancel) {
            when (cancel.reminderType) {
                ReminderType.PAYMENT -> reminderScheduler.cancelPaymentReminder(cancel.bookingId)
                ReminderType.GUIDE_RESPONSE -> reminderScheduler.cancelGuideResponseReminder(cancel.bookingId)
                ReminderType.TREK_TOMORROW -> reminderScheduler.cancelTrekTomorrowReminder(cancel.bookingId)
                ReminderType.ALL -> reminderScheduler.cancelAllRemindersForBooking(cancel.bookingId)
            }
        }

        // Schedule reminders
        for (schedule in diffResult.remindersToSchedule) {
            when (schedule) {
                is PendingReminder.PaymentReminder -> {
                    // 10 minutes after statusSince
                    reminderScheduler.schedulePaymentReminder(
                        schedule.bookingId,
                        schedule.statusSinceMillis + 10 * 60 * 1000L
                    )
                }
                is PendingReminder.GuideResponseReminder -> {
                    reminderScheduler.scheduleGuideResponseReminder(
                        schedule.bookingId,
                        schedule.firstSeenMillis + 12 * 60 * 1000L
                    )
                }
                is PendingReminder.TrekTomorrowReminder -> {
                    val targetMillis = schedule.slotDateTime
                        .atZone(ZoneId.systemDefault())
                        .toInstant()
                        .toEpochMilli() - 24 * 3600 * 1000L
                    reminderScheduler.scheduleTrekTomorrowReminder(
                        schedule.bookingId,
                        targetMillis
                    )
                }
            }
        }
    }

    private suspend fun syncGuideBookings() {
        val response = api.getGuideBookings()
        if (!response.isSuccessful) return

        val incoming = response.body() ?: emptyList()
        val previous = guideDao.getAllGuideBookings()
        val isFirstSync = previous.isEmpty()

        val diffResult = SyncDiffCalculator.calculateGuideDiff(
            previousBookings = previous,
            incomingBookings = incoming,
            isFirstSync = isFirstSync,
            activeBookingId = activeScreenTracker.activeBookingId,
            alreadyNotifiedKeys = emptySet()
        )

        // Persist updated guide bookings to Room
        guideDao.insertGuideBookings(diffResult.updatedBookings)

        // Post notifications
        for (notif in diffResult.notifications) {
            if (!notifiedEventDao.exists(notif.dedupeKey)) {
                val title = if (notif.titleArgs.isEmpty()) {
                    context.getString(notif.titleRes)
                } else {
                    context.getString(notif.titleRes, *notif.titleArgs.toTypedArray())
                }
                val body = if (notif.bodyArgs.isEmpty()) {
                    context.getString(notif.bodyRes)
                } else {
                    context.getString(notif.bodyRes, *notif.bodyArgs.toTypedArray())
                }
                notificationPoster.postNotification(
                    channelId = notif.channelId,
                    title = title,
                    body = body,
                    type = notif.type,
                    bookingId = notif.bookingId,
                    isGuide = true,
                    dedupeKey = notif.dedupeKey
                )
            }
        }

        // Cancel reminders
        for (cancel in diffResult.remindersToCancel) {
            when (cancel.reminderType) {
                ReminderType.PAYMENT -> reminderScheduler.cancelPaymentReminder(cancel.bookingId)
                ReminderType.GUIDE_RESPONSE -> reminderScheduler.cancelGuideResponseReminder(cancel.bookingId)
                ReminderType.TREK_TOMORROW -> reminderScheduler.cancelTrekTomorrowReminder(cancel.bookingId)
                ReminderType.ALL -> reminderScheduler.cancelAllRemindersForBooking(cancel.bookingId)
            }
        }

        // Schedule reminders
        for (schedule in diffResult.remindersToSchedule) {
            if (schedule is PendingReminder.GuideResponseReminder) {
                reminderScheduler.scheduleGuideResponseReminder(
                    schedule.bookingId,
                    schedule.firstSeenMillis + 12 * 60 * 1000L
                )
            }
        }
    }
}
