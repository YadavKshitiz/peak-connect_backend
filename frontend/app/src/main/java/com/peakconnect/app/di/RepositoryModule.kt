package com.peakconnect.app.di

import com.peakconnect.app.data.repository.ActivityRepository
import com.peakconnect.app.data.repository.ActivityRepositoryImpl
import com.peakconnect.app.data.repository.AuthRepository
import com.peakconnect.app.data.repository.AuthRepositoryImpl
import com.peakconnect.app.data.repository.BookingRepository
import com.peakconnect.app.data.repository.BookingRepositoryImpl
import com.peakconnect.app.data.repository.GuideRepository
import com.peakconnect.app.data.repository.GuideRepositoryImpl
import com.peakconnect.app.data.repository.HealthRepository
import com.peakconnect.app.data.repository.HealthRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindAuthRepository(impl: AuthRepositoryImpl): AuthRepository

    @Binds
    @Singleton
    abstract fun bindActivityRepository(impl: ActivityRepositoryImpl): ActivityRepository

    @Binds
    @Singleton
    abstract fun bindBookingRepository(impl: BookingRepositoryImpl): BookingRepository

    @Binds
    @Singleton
    abstract fun bindGuideRepository(impl: GuideRepositoryImpl): GuideRepository

    @Binds
    @Singleton
    abstract fun bindHealthRepository(impl: HealthRepositoryImpl): HealthRepository
}
