package com.peakconnect.app.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import com.peakconnect.app.core.session.AndroidKeystoreCipher
import com.peakconnect.app.core.session.DataStoreTokenStore
import com.peakconnect.app.core.session.TokenCipher
import com.peakconnect.app.core.session.TokenStore
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

import com.peakconnect.app.core.preferences.DataStoreOnboardingStore
import com.peakconnect.app.core.preferences.OnboardingStore

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "peakconnect_prefs")

@Module
@InstallIn(SingletonComponent::class)
abstract class StorageBindingsModule {
    @Binds
    @Singleton
    abstract fun bindTokenStore(impl: DataStoreTokenStore): TokenStore

    @Binds
    @Singleton
    abstract fun bindOnboardingStore(impl: DataStoreOnboardingStore): OnboardingStore
}

@Module
@InstallIn(SingletonComponent::class)
object StorageModule {

    @Provides
    @Singleton
    fun provideDataStore(@ApplicationContext context: Context): DataStore<Preferences> {
        return context.dataStore
    }

    @Provides
    @Singleton
    fun provideTokenCipher(): TokenCipher {
        return AndroidKeystoreCipher()
    }
}
