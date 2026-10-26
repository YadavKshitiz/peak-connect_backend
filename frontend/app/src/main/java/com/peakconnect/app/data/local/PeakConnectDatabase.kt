package com.peakconnect.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.peakconnect.app.data.local.dao.ActivityDao
import com.peakconnect.app.data.local.dao.BookingDao
import com.peakconnect.app.data.local.dao.GuideDao
import com.peakconnect.app.data.local.dao.NotifiedEventDao
import com.peakconnect.app.data.local.dao.UserContentDao
import com.peakconnect.app.data.local.entity.ActivityEntity
import com.peakconnect.app.data.local.entity.BookingEntity
import com.peakconnect.app.data.local.entity.ChecklistItemEntity
import com.peakconnect.app.data.local.entity.FavouriteEntity
import com.peakconnect.app.data.local.entity.GuideBookingEntity
import com.peakconnect.app.data.local.entity.GuideProfileEntity
import com.peakconnect.app.data.local.entity.NotificationEntity
import com.peakconnect.app.data.local.entity.NotifiedEventEntity
import com.peakconnect.app.data.local.entity.RecentlyViewedEntity
import com.peakconnect.app.data.local.entity.SearchHistoryEntity
import com.peakconnect.app.data.local.entity.SlotEntity
import com.peakconnect.app.data.local.entity.WaitlistEntryEntity

@Database(
    entities = [
        ActivityEntity::class,
        SlotEntity::class,
        BookingEntity::class,
        GuideBookingEntity::class,
        GuideProfileEntity::class,
        FavouriteEntity::class,
        RecentlyViewedEntity::class,
        SearchHistoryEntity::class,
        NotificationEntity::class,
        WaitlistEntryEntity::class,
        ChecklistItemEntity::class,
        NotifiedEventEntity::class
    ],
    version = 1,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class PeakConnectDatabase : RoomDatabase() {

    abstract fun activityDao(): ActivityDao
    abstract fun bookingDao(): BookingDao
    abstract fun guideDao(): GuideDao
    abstract fun userContentDao(): UserContentDao
    abstract fun notifiedEventDao(): NotifiedEventDao

    companion object {
        const val DATABASE_NAME = "peakconnect_database"
    }
}
