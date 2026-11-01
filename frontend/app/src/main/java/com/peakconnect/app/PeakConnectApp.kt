package com.peakconnect.app

import android.app.Application
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import coil3.disk.DiskCache
import coil3.disk.directory
import coil3.memory.MemoryCache
import coil3.request.crossfade
import com.razorpay.Checkout
import dagger.hilt.android.HiltAndroidApp
import okio.Path.Companion.toPath

import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.peakconnect.app.core.notifications.NotificationChannels
import javax.inject.Inject

@HiltAndroidApp
class PeakConnectApp : Application(), SingletonImageLoader.Factory, Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()
        Checkout.preload(applicationContext)
        NotificationChannels.createChannels(this)
    }

    override fun newImageLoader(context: PlatformContext): ImageLoader {
        return ImageLoader.Builder(context)
            .memoryCache {
                MemoryCache.Builder()
                    .maxSizePercent(context, 0.25)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(cacheDir.resolve("image_cache").absolutePath.toPath())
                    .maxSizeBytes(100L * 1024 * 1024) // ~100 MB
                    .build()
            }
            .crossfade(true)
            .build()
    }
}
