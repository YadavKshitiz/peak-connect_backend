package com.peakconnect.app.di

import android.content.Context
import androidx.room.Room
import com.peakconnect.app.data.local.PeakConnectDatabase
import com.peakconnect.app.data.local.dao.ActivityDao
import com.peakconnect.app.data.local.dao.BookingDao
import com.peakconnect.app.data.local.dao.GuideDao
import com.peakconnect.app.data.local.dao.NotifiedEventDao
import com.peakconnect.app.data.local.dao.UserContentDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): PeakConnectDatabase {
        return Room.databaseBuilder(
            context,
            PeakConnectDatabase::class.java,
            PeakConnectDatabase.DATABASE_NAME
        ).fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    fun provideActivityDao(db: PeakConnectDatabase): ActivityDao = db.activityDao()

    @Provides
    fun provideBookingDao(db: PeakConnectDatabase): BookingDao = db.bookingDao()

    @Provides
    fun provideGuideDao(db: PeakConnectDatabase): GuideDao = db.guideDao()

    @Provides
    fun provideUserContentDao(db: PeakConnectDatabase): UserContentDao = db.userContentDao()

    @Provides
    fun provideNotifiedEventDao(db: PeakConnectDatabase): NotifiedEventDao = db.notifiedEventDao()
}
