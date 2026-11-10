package com.peakconnect.app.core.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import com.peakconnect.app.R

object NotificationChannels {
    const val CHANNEL_BOOKING_UPDATES = "booking_updates"
    const val CHANNEL_REMINDERS = "reminders"

    fun createChannels(context: Context) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            ?: return

        val bookingChannel = NotificationChannel(
            CHANNEL_BOOKING_UPDATES,
            context.getString(R.string.channel_booking_updates_name),
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = context.getString(R.string.channel_booking_updates_desc)
            enableLights(true)
            enableVibration(true)
        }

        val remindersChannel = NotificationChannel(
            CHANNEL_REMINDERS,
            context.getString(R.string.channel_reminders_name),
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = context.getString(R.string.channel_reminders_desc)
            enableLights(true)
            enableVibration(true)
        }

        notificationManager.createNotificationChannels(listOf(bookingChannel, remindersChannel))
    }
}
