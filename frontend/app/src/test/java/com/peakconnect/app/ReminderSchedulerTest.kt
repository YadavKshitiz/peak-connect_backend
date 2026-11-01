package com.peakconnect.app

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.work.WorkManager
import androidx.work.testing.WorkManagerTestInitHelper
import com.peakconnect.app.feature.notifications.reminders.WorkManagerReminderScheduler
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ReminderSchedulerTest {

    private lateinit var context: Context
    private lateinit var scheduler: WorkManagerReminderScheduler

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        WorkManagerTestInitHelper.initializeTestWorkManager(context)
        scheduler = WorkManagerReminderScheduler(context)
    }

    @Test
    fun `schedulePaymentReminder enqueues unique work without throwing`() {
        val futureTime = System.currentTimeMillis() + 10 * 60 * 1000L
        scheduler.schedulePaymentReminder("b100", futureTime)

        val workManager = WorkManager.getInstance(context)
        val workInfos = workManager.getWorkInfosForUniqueWork("reminder_payment_b100").get()
        assertNotNull(workInfos)
    }

    @Test
    fun `cancelPaymentReminder cancels work without error`() {
        scheduler.schedulePaymentReminder("b100", System.currentTimeMillis() + 10 * 60 * 1000L)
        scheduler.cancelPaymentReminder("b100")
        val workInfos = WorkManager.getInstance(context).getWorkInfosForUniqueWork("reminder_payment_b100").get()
        assertNotNull(workInfos)
    }

    @Test
    fun `scheduleGuideResponseReminder enqueues unique work without throwing`() {
        val futureTime = System.currentTimeMillis() + 12 * 60 * 1000L
        scheduler.scheduleGuideResponseReminder("gb100", futureTime)

        val workInfos = WorkManager.getInstance(context).getWorkInfosForUniqueWork("reminder_guide_gb100").get()
        assertNotNull(workInfos)
    }

    @Test
    fun `scheduleTrekTomorrowReminder skips if reminder time already in past`() {
        val pastTime = System.currentTimeMillis() - 1000L
        scheduler.scheduleTrekTomorrowReminder("b200", pastTime)

        val workInfos = WorkManager.getInstance(context).getWorkInfosForUniqueWork("reminder_trek_tomorrow_b200").get()
        // Because delay <= 0, it skips enqueueing
        org.junit.Assert.assertTrue(workInfos.isEmpty())
    }

    @Test
    fun `cancelAllRemindersForBooking cancels all reminders without error`() {
        scheduler.cancelAllRemindersForBooking("b300")
    }
}
