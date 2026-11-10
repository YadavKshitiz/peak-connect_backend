package com.peakconnect.app

import com.peakconnect.app.data.local.entity.ActivityEntity
import com.peakconnect.app.data.repository.ActivityRepository
import com.peakconnect.app.domain.model.CancellationPolicyType
import com.peakconnect.app.domain.model.DifficultyLevel
import com.peakconnect.app.feature.saved.SavedViewModel
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal

@OptIn(ExperimentalCoroutinesApi::class)
class SavedViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val activityRepository: ActivityRepository = mockk(relaxed = true)
    private val savedActivitiesFlow = MutableStateFlow<List<ActivityEntity>>(emptyList())

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        every { activityRepository.getSavedActivities() } returns savedActivitiesFlow
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `savedActivities flow is populated from repository`() = runTest {
        val sampleEntity = ActivityEntity(
            id = "act_1",
            title = "Hampta Pass",
            description = "Great trek",
            location = "Manali",
            latitude = 32.2,
            longitude = 77.1,
            difficulty = DifficultyLevel.MODERATE,
            basePrice = BigDecimal("5000"),
            cancellationPolicy = CancellationPolicyType.MODERATE,
            minSlotPrice = BigDecimal("5000"),
            lastUpdated = 1000L
        )
        savedActivitiesFlow.value = listOf(sampleEntity)

        val viewModel = SavedViewModel(activityRepository)
        advanceUntilIdle()

        assertEquals(1, viewModel.savedActivities.value.size)
        assertEquals("Hampta Pass", viewModel.savedActivities.value[0].title)
    }

    @Test
    fun `toggleFavourite calls repository`() = runTest {
        val viewModel = SavedViewModel(activityRepository)
        advanceUntilIdle()

        viewModel.toggleFavourite("act_1")
        advanceUntilIdle()

        coVerify { activityRepository.toggleFavourite("act_1") }
    }

    @Test
    fun `onActivityClicked records recently viewed`() = runTest {
        val viewModel = SavedViewModel(activityRepository)
        advanceUntilIdle()

        viewModel.onActivityClicked("act_1")
        advanceUntilIdle()

        coVerify { activityRepository.recordRecentlyViewed("act_1") }
    }
}
