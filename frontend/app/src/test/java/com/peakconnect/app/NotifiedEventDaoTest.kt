package com.peakconnect.app

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.peakconnect.app.data.local.PeakConnectDatabase
import com.peakconnect.app.data.local.dao.NotifiedEventDao
import com.peakconnect.app.data.local.entity.NotifiedEventEntity
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class NotifiedEventDaoTest {

    private lateinit var db: PeakConnectDatabase
    private lateinit var dao: NotifiedEventDao

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, PeakConnectDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = db.notifiedEventDao()
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun `insert and exists work correctly`() = runTest {
        val key = "booking_123_CONFIRMED"
        assertFalse(dao.exists(key))

        dao.insert(NotifiedEventEntity(key = key, notifiedAt = 1000L))
        assertTrue(dao.exists(key))
    }

    @Test
    fun `deleteOlderThan removes only expired records`() = runTest {
        dao.insert(NotifiedEventEntity(key = "event_old", notifiedAt = 500L))
        dao.insert(NotifiedEventEntity(key = "event_new", notifiedAt = 1500L))

        val deleted = dao.deleteOlderThan(1000L)
        assertEquals(1, deleted)

        assertFalse(dao.exists("event_old"))
        assertTrue(dao.exists("event_new"))
    }
}
