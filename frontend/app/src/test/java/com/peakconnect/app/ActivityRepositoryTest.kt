package com.peakconnect.app

import com.peakconnect.app.core.network.WakeUpRetryHelper
import com.peakconnect.app.data.api.PeakConnectApi
import com.peakconnect.app.data.local.dao.ActivityDao
import com.peakconnect.app.data.local.dao.UserContentDao
import com.peakconnect.app.data.local.entity.FavouriteEntity
import com.peakconnect.app.data.repository.ActivityRepositoryImpl
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ActivityRepositoryTest {

    private val testDispatcher = StandardTestDispatcher()
    private val api: PeakConnectApi = mockk(relaxed = true)
    private val activityDao: ActivityDao = mockk(relaxed = true)
    private val userContentDao: UserContentDao = mockk(relaxed = true)
    private val wakeUpRetryHelper = WakeUpRetryHelper(healthRepository = mockk(relaxed = true))

    private val repository = ActivityRepositoryImpl(
        api = api,
        activityDao = activityDao,
        userContentDao = userContentDao,
        wakeUpRetryHelper = wakeUpRetryHelper,
        ioDispatcher = testDispatcher
    )

    @Test
    fun `hasCachedActivities returns true when count is greater than zero`() = runTest(testDispatcher) {
        coEvery { activityDao.getActivityCount() } returns 5

        assertTrue(repository.hasCachedActivities())

        coEvery { activityDao.getActivityCount() } returns 0
        assertFalse(repository.hasCachedActivities())
    }

    @Test
    fun `toggleFavourite adds when not favourite and removes when favourite`() = runTest(testDispatcher) {
        // When not favourite, should insert
        coEvery { userContentDao.isFavouriteFlow("act_1") } returns flowOf(false)
        val nowFav = repository.toggleFavourite("act_1")
        assertTrue(nowFav)
        coVerify { userContentDao.insertFavourite(any()) }

        // When favourite, should delete
        coEvery { userContentDao.isFavouriteFlow("act_1") } returns flowOf(true)
        val nowUnfav = repository.toggleFavourite("act_1")
        assertFalse(nowUnfav)
        coVerify { userContentDao.deleteFavourite("act_1") }
    }

    @Test
    fun `getFavouritesSetFlow transforms FavouriteEntity list into set of ids`() = runTest(testDispatcher) {
        every { userContentDao.getFavouritesFlow() } returns flowOf(
            listOf(FavouriteEntity("act_1", 100L), FavouriteEntity("act_2", 200L))
        )

        val set = repository.getFavouritesSetFlow().first()
        assertEquals(setOf("act_1", "act_2"), set)
    }

    @Test
    fun `recordRecentlyViewed calls userContentDao`() = runTest(testDispatcher) {
        repository.recordRecentlyViewed("act_9")
        coVerify { userContentDao.insertRecentlyViewed(any()) }
    }
}
