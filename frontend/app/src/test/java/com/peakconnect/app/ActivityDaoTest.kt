package com.peakconnect.app

import android.content.Context
import androidx.paging.PagingSource
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.peakconnect.app.data.local.PeakConnectDatabase
import com.peakconnect.app.data.local.dao.ActivityDao
import com.peakconnect.app.data.local.dao.ActivityFilter
import com.peakconnect.app.data.local.dao.ActivitySortOption
import com.peakconnect.app.data.local.dao.getActivitiesPaged
import com.peakconnect.app.data.local.entity.ActivityEntity
import com.peakconnect.app.data.local.entity.SlotEntity
import com.peakconnect.app.domain.model.CancellationPolicyType
import com.peakconnect.app.domain.model.DifficultyLevel
import com.peakconnect.app.domain.model.RiskLevel
import com.peakconnect.app.domain.model.Season
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.math.BigDecimal
import java.time.LocalDateTime

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ActivityDaoTest {

    private lateinit var db: PeakConnectDatabase
    private lateinit var dao: ActivityDao

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, PeakConnectDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = db.activityDao()
    }

    @After
    fun tearDown() {
        db.close()
    }

    private suspend fun seedActivities() {
        val a1 = ActivityEntity(
            id = "act-1",
            title = "Annapurna Circuit Trek",
            description = "Stunning Himalayan high mountain trek",
            location = "Pokhara",
            latitude = 28.2,
            longitude = 83.9,
            difficulty = DifficultyLevel.CHALLENGING,
            basePrice = BigDecimal("150.00"),
            cancellationPolicy = CancellationPolicyType.MODERATE,
            minSlotPrice = BigDecimal("120.00"),
            lastUpdated = 1000L
        )
        val a2 = ActivityEntity(
            id = "act-2",
            title = "Everest Base Camp",
            description = "Iconic Khumbu valley adventure to base camp",
            location = "Lukla",
            latitude = 27.6,
            longitude = 86.7,
            difficulty = DifficultyLevel.EXTREME,
            basePrice = BigDecimal("300.00"),
            cancellationPolicy = CancellationPolicyType.STRICT,
            minSlotPrice = BigDecimal("250.00"),
            lastUpdated = 1000L
        )
        val a3 = ActivityEntity(
            id = "act-3",
            title = "Poon Hill Sunrise Hike",
            description = "Gentle panoramic walk suitable for beginners",
            location = "Pokhara",
            latitude = 28.3,
            longitude = 83.7,
            difficulty = DifficultyLevel.EASY,
            basePrice = BigDecimal("50.00"),
            cancellationPolicy = CancellationPolicyType.FLEXIBLE,
            minSlotPrice = BigDecimal("45.00"),
            lastUpdated = 1000L
        )

        dao.insertActivities(listOf(a1, a2, a3))

        val s1 = SlotEntity(
            id = "slot-1",
            activityId = "act-1",
            date = LocalDateTime.of(2026, 6, 15, 8, 0),
            capacity = 10,
            currentOccupancy = 2,
            season = Season.LOW,
            currentPrice = BigDecimal("120.00"),
            riskLevel = RiskLevel.LOW
        )
        val s2 = SlotEntity(
            id = "slot-2",
            activityId = "act-2",
            date = LocalDateTime.of(2026, 7, 20, 8, 0),
            capacity = 8,
            currentOccupancy = 0,
            season = Season.SHOULDER,
            currentPrice = BigDecimal("250.00"),
            riskLevel = RiskLevel.MODERATE
        )
        dao.insertSlots(listOf(s1, s2))
    }

    private suspend fun loadPaged(filter: ActivityFilter): List<ActivityEntity> {
        val pagingSource = dao.getActivitiesPaged(filter)
        val result = pagingSource.load(
            PagingSource.LoadParams.Refresh(
                key = null,
                loadSize = 20,
                placeholdersEnabled = false
            )
        )
        assertTrue(result is PagingSource.LoadResult.Page)
        return (result as PagingSource.LoadResult.Page).data
    }

    @Test
    fun `text query filters by title description and location case-insensitively`() = runTest {
        seedActivities()

        // Match on title
        val titleMatch = loadPaged(ActivityFilter(query = "Everest"))
        assertEquals(1, titleMatch.size)
        assertEquals("act-2", titleMatch.first().id)

        // Match on description
        val descMatch = loadPaged(ActivityFilter(query = "panoramic"))
        assertEquals(1, descMatch.size)
        assertEquals("act-3", descMatch.first().id)

        // Match on location case-insensitively
        val locMatch = loadPaged(ActivityFilter(query = "pokhara"))
        assertEquals(2, locMatch.size)
    }

    @Test
    fun `difficulty set filter works`() = runTest {
        seedActivities()

        val challengingAndExtreme = loadPaged(
            ActivityFilter(difficulties = setOf(DifficultyLevel.CHALLENGING, DifficultyLevel.EXTREME))
        )
        assertEquals(2, challengingAndExtreme.size)
        assertTrue(challengingAndExtreme.none { it.difficulty == DifficultyLevel.EASY })
    }

    @Test
    fun `location filter matches exact location`() = runTest {
        seedActivities()

        val pokharaOnly = loadPaged(ActivityFilter(location = "Pokhara"))
        assertEquals(2, pokharaOnly.size)
        assertTrue(pokharaOnly.all { it.location == "Pokhara" })
    }

    @Test
    fun `hasSlotOnDate filters activities having slot on date`() = runTest {
        seedActivities()

        val onJune15 = loadPaged(ActivityFilter(hasSlotOnDate = "2026-06-15"))
        assertEquals(1, onJune15.size)
        assertEquals("act-1", onJune15.first().id)

        val noSlotsDate = loadPaged(ActivityFilter(hasSlotOnDate = "2026-12-31"))
        assertEquals(0, noSlotsDate.size)
    }

    @Test
    fun `sortOption orders correctly`() = runTest {
        seedActivities()

        val priceAsc = loadPaged(ActivityFilter(sortOption = ActivitySortOption.PRICE_LOW_TO_HIGH))
        assertEquals(listOf("act-3", "act-1", "act-2"), priceAsc.map { it.id })

        val priceDesc = loadPaged(ActivityFilter(sortOption = ActivitySortOption.PRICE_HIGH_TO_LOW))
        assertEquals(listOf("act-2", "act-1", "act-3"), priceDesc.map { it.id })

        val titleAsc = loadPaged(ActivityFilter(sortOption = ActivitySortOption.TITLE))
        assertEquals(listOf("act-1", "act-2", "act-3"), titleAsc.map { it.id })
    }
}
