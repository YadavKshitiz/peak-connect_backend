package com.peakconnect.app.core.notifications

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.peakconnect.app.MainActivity
import com.peakconnect.app.R
import com.peakconnect.app.data.local.dao.NotifiedEventDao
import com.peakconnect.app.data.local.dao.UserContentDao
import com.peakconnect.app.data.local.entity.NotificationEntity
import com.peakconnect.app.data.local.entity.NotifiedEventEntity
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LocalNotificationPoster @Inject constructor(
    @ApplicationContext private val context: Context,
    private val userContentDao: UserContentDao,
    private val notifiedEventDao: NotifiedEventDao
) {
    companion object {
        const val NOTIFICATION_COLOR = 0xFF1F6B45.toInt()
        const val EXTRA_BOOKING_ID = "extra_booking_id"
        const val EXTRA_IS_GUIDE = "extra_is_guide"
    }

    suspend fun postNotification(
        channelId: String,
        title: String,
        body: String,
        type: String,
        bookingId: String? = null,
        isGuide: Boolean = false,
        dedupeKey: String? = null,
        notificationId: Int = (System.currentTimeMillis() % Int.MAX_VALUE).toInt()
    ) {
        // 1. Check deduplication
        if (dedupeKey != null && notifiedEventDao.exists(dedupeKey)) {
            return
        }

        // 2. Persist in-app notification inbox row
        val inboxEntity = NotificationEntity(
            id = UUID.randomUUID().toString(),
            type = type,
            title = title,
            message = body,
            bookingId = bookingId,
            createdAt = System.currentTimeMillis(),
            isRead = false
        )
        userContentDao.insertNotification(inboxEntity)

        // 3. Persist deduplication record
        if (dedupeKey != null) {
            notifiedEventDao.insert(
                NotifiedEventEntity(
                    key = dedupeKey,
                    notifiedAt = System.currentTimeMillis()
                )
            )
        }

        // 4. Post local system notification if permissions are granted
        val manager = NotificationManagerCompat.from(context)
        if (!manager.areNotificationsEnabled()) {
            return
        }

        val launchIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            if (bookingId != null) {
                putExtra(EXTRA_BOOKING_ID, bookingId)
                putExtra(EXTRA_IS_GUIDE, isGuide)
            }
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_notification_mountain)
            .setColor(NOTIFICATION_COLOR)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .build()

        try {
            manager.notify(notificationId, notification)
        } catch (_: SecurityException) {
            // In case notification permission was revoked at runtime
        }
    }
}
