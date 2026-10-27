package com.peakconnect.app

import com.peakconnect.app.feature.trekker.detail.ActivityDetailUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.math.BigDecimal

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ActivityDetailUtilsTest {

    @Test
    fun `buildShareText formats title location price and repo url correctly`() {
        val title = "Triund Sunset Trek"
        val location = "McLeod Ganj"
        val lowestPrice = BigDecimal("1500.00")

        val result = ActivityDetailUtils.buildShareText(title, location, lowestPrice)

        assertTrue(result.contains("Triund Sunset Trek in McLeod Ganj — from ₹1,500 on PeakConnect"))
        assertTrue(result.contains(ActivityDetailUtils.GITHUB_REPO_URL))
    }

    @Test
    fun `buildGeoIntentUri constructs valid geo intent with query and label`() {
        val lat = 32.24
        val lng = 76.32
        val title = "Triund Trek"

        val uri = ActivityDetailUtils.buildGeoIntentUri(lat, lng, title)

        assertEquals("geo", uri.scheme)
        assertEquals("geo:32.24,76.32?q=32.24,76.32(Triund%20Trek)", uri.toString())
    }
}
