package com.peakconnect.app

import com.peakconnect.app.core.images.ImageManifest
import com.peakconnect.app.core.images.ImageManifestItem
import com.peakconnect.app.core.images.ImageUrlResolver
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

class ImageUrlResolverTest {

    private val sampleManifest = ImageManifest(
        version = 1,
        baseUrl = "https://raw.githubusercontent.com/test/repo/main/assets/",
        items = listOf(
            ImageManifestItem(
                location = "mcleod-ganj",
                activity = "triund-sunset-trek",
                image = "mcleod-ganj/triund-sunset-trek.webp"
            ),
            ImageManifestItem(
                location = "manali",
                activity = "hampta-pass",
                image = "manali/hampta-pass.webp"
            )
        ),
        defaults = mapOf(
            "rafting" to "defaults/rafting.webp",
            "trekking" to "defaults/trekking.webp",
            "camping" to "defaults/camping.webp",
            "hiking" to "defaults/hiking.webp"
        )
    )

    @Before
    fun setUp() {
        ImageUrlResolver.setActiveManifest(sampleManifest)
    }

    @Test
    fun `slugify handles casing whitespace and special characters`() {
        assertEquals("mcleod-ganj", ImageUrlResolver.slugify("  McLeod Ganj  "))
        assertEquals("triund-sunset-trek", ImageUrlResolver.slugify("Triund - Sunset Trek!"))
        assertEquals("a-b-c", ImageUrlResolver.slugify("---a---b---c---"))
        assertEquals("rishikesh-rafting-123", ImageUrlResolver.slugify("Rishikesh  Rafting #123"))
    }

    @Test
    fun `tier 1 exact match returns correct full url`() {
        val result = ImageUrlResolver.resolve("Triund Sunset Trek", "McLeod Ganj", sampleManifest)
        assertEquals(
            "https://raw.githubusercontent.com/test/repo/main/assets/mcleod-ganj/triund-sunset-trek.webp",
            result
        )
    }

    @Test
    fun `tier 2 substring match within same location returns url`() {
        // Title contains activity slug
        val result1 = ImageUrlResolver.resolve("Exciting Triund Sunset Trek Expedition", "McLeod Ganj", sampleManifest)
        assertEquals(
            "https://raw.githubusercontent.com/test/repo/main/assets/mcleod-ganj/triund-sunset-trek.webp",
            result1
        )

        // Activity slug contains title slug
        val result2 = ImageUrlResolver.resolve("Hampta", "Manali", sampleManifest)
        assertEquals(
            "https://raw.githubusercontent.com/test/repo/main/assets/manali/hampta-pass.webp",
            result2
        )
    }

    @Test
    fun `tier 3 keyword defaults match keywords in title`() {
        val rafting = ImageUrlResolver.resolve("Ganges River Raft Trip", "Rishikesh", sampleManifest)
        assertEquals(
            "https://raw.githubusercontent.com/test/repo/main/assets/defaults/rafting.webp",
            rafting
        )

        val trek = ImageUrlResolver.resolve("Alpine Mountain Trek", "Kashmir", sampleManifest)
        assertEquals(
            "https://raw.githubusercontent.com/test/repo/main/assets/defaults/trekking.webp",
            trek
        )

        val camp = ImageUrlResolver.resolve("Under the Stars Camp", "Jaisalmer", sampleManifest)
        assertEquals(
            "https://raw.githubusercontent.com/test/repo/main/assets/defaults/camping.webp",
            camp
        )

        val hike = ImageUrlResolver.resolve("Morning Nature Hike", "Shimla", sampleManifest)
        assertEquals(
            "https://raw.githubusercontent.com/test/repo/main/assets/defaults/hiking.webp",
            hike
        )
    }

    @Test
    fun `tier 4 returns null when no item or keyword matches`() {
        val result = ImageUrlResolver.resolve("Sightseeing tour", "Delhi", sampleManifest)
        assertNull(result)
    }

    @Test
    fun `resolve with active manifest works`() {
        val result = ImageUrlResolver.resolve("Triund Sunset Trek", "McLeod Ganj")
        assertEquals(
            "https://raw.githubusercontent.com/test/repo/main/assets/mcleod-ganj/triund-sunset-trek.webp",
            result
        )
    }

    @Test
    fun `blank title returns null`() {
        assertNull(ImageUrlResolver.resolve("", "Manali", sampleManifest))
        assertNull(ImageUrlResolver.resolve("   ", "Manali", sampleManifest))
    }
}
