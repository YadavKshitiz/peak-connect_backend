package com.peakconnect.app.core.images

import android.content.Context
import com.peakconnect.app.BuildConfig
import com.peakconnect.app.core.util.IoDispatcher
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

interface ImageManifestRepository {
    val manifestFlow: StateFlow<ImageManifest>
    val currentManifest: ImageManifest
    suspend fun refreshManifest(): Boolean
    fun resolve(title: String, location: String): String?
}

@Singleton
class ImageManifestRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val okHttpClient: OkHttpClient,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : ImageManifestRepository {

    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    private val scope = CoroutineScope(SupervisorJob() + ioDispatcher)
    private val cachedFile = File(context.filesDir, "cached_manifest.json")

    private val _manifestFlow = MutableStateFlow(loadInitialManifest())
    override val manifestFlow: StateFlow<ImageManifest> = _manifestFlow.asStateFlow()
    override val currentManifest: ImageManifest get() = _manifestFlow.value

    init {
        ImageUrlResolver.setActiveManifest(_manifestFlow.value)
        // Non-blocking background fetch on startup
        scope.launch {
            refreshManifest()
        }
    }

    override suspend fun refreshManifest(): Boolean = withContext(ioDispatcher) {
        val remoteUrl = BuildConfig.GITHUB_ASSETS_BASE_URL.trimEnd('/') + "/manifest.json"
        try {
            val request = Request.Builder().url(remoteUrl).build()
            val response = okHttpClient.newCall(request).execute()
            if (response.isSuccessful) {
                val bodyString = response.body?.string()
                if (!bodyString.isNullOrBlank()) {
                    val parsed = json.decodeFromString<ImageManifest>(bodyString)
                    // Validate basic integrity
                    if (parsed.baseUrl.isNotBlank()) {
                        try {
                            cachedFile.writeText(bodyString)
                        } catch (_: Exception) {
                        }
                        _manifestFlow.value = parsed
                        ImageUrlResolver.setActiveManifest(parsed)
                        return@withContext true
                    }
                }
            }
        } catch (_: Exception) {
            // Silently fallback; network errors or 404s must never cause crashes
        }
        false
    }

    override fun resolve(title: String, location: String): String? {
        return ImageUrlResolver.resolve(title, location, currentManifest)
    }

    private fun loadInitialManifest(): ImageManifest {
        // 1. Try cached file
        if (cachedFile.exists()) {
            try {
                val cachedContent = cachedFile.readText()
                if (cachedContent.isNotBlank()) {
                    val parsed = json.decodeFromString<ImageManifest>(cachedContent)
                    if (parsed.baseUrl.isNotBlank()) {
                        return parsed
                    }
                }
            } catch (_: Exception) {
            }
        }

        // 2. Fallback to bundled asset
        return try {
            val bundledContent = context.assets.open("manifest.json").bufferedReader().use { it.readText() }
            json.decodeFromString<ImageManifest>(bundledContent)
        } catch (_: Exception) {
            // Safe in-memory fallback
            ImageManifest(
                version = 1,
                baseUrl = BuildConfig.GITHUB_ASSETS_BASE_URL,
                items = listOf(
                    ImageManifestItem(
                        location = "mcleod-ganj",
                        activity = "triund-sunset-trek",
                        image = "mcleod-ganj/triund-sunset-trek.webp"
                    )
                ),
                defaults = mapOf(
                    "rafting" to "defaults/rafting.webp",
                    "trekking" to "defaults/trekking.webp",
                    "camping" to "defaults/camping.webp",
                    "hiking" to "defaults/hiking.webp"
                )
            )
        }
    }
}
