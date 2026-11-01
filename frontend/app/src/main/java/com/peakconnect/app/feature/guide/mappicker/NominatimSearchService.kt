package com.peakconnect.app.feature.guide.mappicker

import com.peakconnect.app.core.util.IoDispatcher
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import java.net.URLEncoder
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

data class NominatimPlace(
    val placeId: Long,
    val displayName: String,
    val latitude: Double,
    val longitude: Double
)

@Singleton
class NominatimSearchService @Inject constructor(
    @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher
) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    private val rateLimitMutex = Mutex()
    private var lastRequestTime = 0L

    // Injected time / sleeper functions for unit testing
    var clock: () -> Long = { System.currentTimeMillis() }
    var sleeper: suspend (Long) -> Unit = { delay(it) }

    suspend fun search(query: String): List<NominatimPlace> = withContext(ioDispatcher) {
        val trimmed = query.trim()
        if (trimmed.length < 3) return@withContext emptyList()

        // Rate limit: at most one request per second
        rateLimitMutex.withLock {
            val now = clock()
            val elapsed = now - lastRequestTime
            if (elapsed in 0 until 1000L) {
                sleeper(1000L - elapsed)
            }
            lastRequestTime = clock()
        }

        try {
            val encoded = URLEncoder.encode(trimmed, "UTF-8")
            val url = "https://nominatim.openstreetmap.org/search?format=json&limit=5&q=$encoded"
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "PeakConnect-Android-App/1.0 (contact@peakconnect.com)")
                .build()

            val response = client.newCall(request).execute()
            if (!response.isSuccessful) return@withContext emptyList()

            val body = response.body?.string() ?: return@withContext emptyList()
            parseNominatimJson(body)
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun parseNominatimJson(json: String): List<NominatimPlace> {
        val results = mutableListOf<NominatimPlace>()
        try {
            val array = JSONArray(json)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val placeId = obj.optLong("place_id", i.toLong())
                val displayName = obj.optString("display_name", "")
                val lat = obj.optString("lat").toDoubleOrNull()
                val lon = obj.optString("lon").toDoubleOrNull()
                if (lat != null && lon != null && displayName.isNotBlank()) {
                    results.add(
                        NominatimPlace(
                            placeId = placeId,
                            displayName = displayName,
                            latitude = lat,
                            longitude = lon
                        )
                    )
                }
            }
        } catch (e: Exception) {
            // Ignore parse errors, return parsed results so far
        }
        return results
    }
}
