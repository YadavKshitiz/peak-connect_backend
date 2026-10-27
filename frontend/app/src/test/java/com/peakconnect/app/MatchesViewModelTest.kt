package com.peakconnect.app

import androidx.lifecycle.SavedStateHandle
import com.peakconnect.app.core.network.ApiResult
import com.peakconnect.app.core.network.AppError
import com.peakconnect.app.data.local.entity.ActivityEntity
import com.peakconnect.app.data.local.entity.SlotEntity
import com.peakconnect.app.data.repository.ActivityRepository
import com.peakconnect.app.data.repository.BookingRepository
import com.peakconnect.app.domain.model.CancellationPolicyType
import com.peakconnect.app.domain.model.DifficultyLevel
import com.peakconnect.app.domain.model.ExperienceLevel
import com.peakconnect.app.domain.model.MatchedGuide
import com.peakconnect.app.domain.model.RiskLevel
import com.peakconnect.app.domain.model.Season
import com.peakconnect.app.feature.trekker.matches.GuideMatchesViewModel
import com.peakconnect.app.feature.trekker.matches.MatchesUiEvent
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.math.BigDecimal
import java.time.LocalDateTime

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class MatchesViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val bookingRepository: BookingRepository = mockk(relaxed = true)
    private val activityRepository: ActivityRepository = mockk(relaxed = true)

    private val slotId = "slot-test-123"

    private val sampleSlot = SlotEntity(
        id = slotId,
        activityId = "act-1",
        date = LocalDateTime.now().plusDays(2),
        capacity = 10,
        currentOccupancy = 4,
        season = Season.PEAK,
        currentPrice = BigDecimal("1500.00"),
        riskLevel = RiskLevel.LOW
    )

    private val sampleActivity = ActivityEntity(
        id = "act-1",
        title = "Triund Trek",
        description = "Scenic trek",
        location = "Dharamshala",
        latitude = 32.24,
        longitude = 76.32,
        difficulty = DifficultyLevel.MODERATE,
        basePrice = BigDecimal("1200.00"),
        cancellationPolicy = CancellationPolicyType.MODERATE,
        minSlotPrice = BigDecimal("1500.00"),
        lastUpdated = System.currentTimeMillis()
    )

    private val sampleGuide = MatchedGuide(
        guideId = "guide-1",
        name = "Vikram Sharma",
        skills = listOf("First Aid", "Navigation", "Cooking"),
        languages = listOf("English", "Hindi"),
        location = "Dharamshala",
        experienceLevel = ExperienceLevel.EXPERT,
        score = 0.92
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        coEvery { activityRepository.getSlotById(slotId) } returns sampleSlot
        coEvery { activityRepository.getActivityForSlot(slotId) } returns sampleActivity
        coEvery {
            bookingRepository.requestMatches(
                slotId = slotId,
                skillWeight = any(),
                locationWeight = any(),
                languageWeight = any()
            )
        } returns ApiResult.Success(listOf(sampleGuide))
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `calculateWeights converts sliders to fractions summing to 1 and language 0`() {
        // Equal weights
        val (s1, l1, lang1) = GuideMatchesViewModel.calculateWeights(50, 50)
        assertEquals(0.5, s1, 0.001)
        assertEquals(0.5, l1, 0.001)
        assertEquals(0.0, lang1, 0.001)
        assertEquals(1.0, s1 + l1, 0.001)

        // Both zero defaults to equal fractions
        val (s2, l2, lang2) = GuideMatchesViewModel.calculateWeights(0, 0)
        assertEquals(0.5, s2, 0.001)
        assertEquals(0.5, l2, 0.001)
        assertEquals(0.0, lang2, 0.001)

        // Asymmetric
        val (s3, l3, lang3) = GuideMatchesViewModel.calculateWeights(80, 20)
        assertEquals(0.8, s3, 0.001)
        assertEquals(0.2, l3, 0.001)
        assertEquals(0.0, lang3, 0.001)
        assertEquals(1.0, s3 + l3, 0.001)
    }

    @Test
    fun `initial fetch loads slot activity and guides with default weights`() = runTest(testDispatcher) {
        val savedStateHandle = SavedStateHandle(mapOf("slotId" to slotId, "isWaitlist" to false))
        val viewModel = GuideMatchesViewModel(bookingRepository, activityRepository, savedStateHandle)

        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(sampleSlot, state.slot)
        assertEquals(sampleActivity, state.activity)
        assertEquals(1, state.guides.size)
        assertEquals("Vikram Sharma", state.guides.first().name)
        assertFalse(state.isLoading)

        coVerify {
            bookingRepository.requestMatches(
                slotId = slotId,
                skillWeight = 0.5,
                locationWeight = 0.5,
                languageWeight = 0.0
            )
        }
    }

    @Test
    fun `changing sliders triggers debounced request after 600 ms`() = runTest(testDispatcher) {
        val savedStateHandle = SavedStateHandle(mapOf("slotId" to slotId, "isWaitlist" to false))
        val viewModel = GuideMatchesViewModel(bookingRepository, activityRepository, savedStateHandle)
        advanceUntilIdle()

        viewModel.onSkillSliderChanged(80)
        viewModel.onLocationSliderChanged(20)

        // Before 600ms debounce
        advanceTimeBy(500)
        coVerify(exactly = 1) {
            bookingRepository.requestMatches(any(), any(), any(), any())
        }

        // After 600ms debounce expires
        advanceTimeBy(200)
        advanceUntilIdle()

        coVerify(exactly = 2) {
            bookingRepository.requestMatches(any(), any(), any(), any())
        }
        coVerify(exactly = 1) {
            bookingRepository.requestMatches(
                slotId = slotId,
                skillWeight = 0.8,
                locationWeight = 0.2,
                languageWeight = 0.0
            )
        }
    }

    @Test
    fun `empty result updates uiState correctly`() = runTest(testDispatcher) {
        coEvery {
            bookingRepository.requestMatches(slotId, any(), any(), any())
        } returns ApiResult.Success(emptyList())

        val savedStateHandle = SavedStateHandle(mapOf("slotId" to slotId, "isWaitlist" to false))
        val viewModel = GuideMatchesViewModel(bookingRepository, activityRepository, savedStateHandle)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.guides.isEmpty())
        assertFalse(state.isLoading)
        assertEquals(null, state.errorMessage)
    }

    @Test
    fun `error response updates errorMessage in uiState`() = runTest(testDispatcher) {
        coEvery {
            bookingRepository.requestMatches(slotId, any(), any(), any())
        } returns ApiResult.Error(AppError.NotFound, "No slot found")

        val savedStateHandle = SavedStateHandle(mapOf("slotId" to slotId, "isWaitlist" to false))
        val viewModel = GuideMatchesViewModel(bookingRepository, activityRepository, savedStateHandle)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("No slot found", state.errorMessage)
        assertFalse(state.isLoading)
    }

    @Test
    fun `waitlist mode join with success emits WaitlistSuccess`() = runTest(testDispatcher) {
        coEvery {
            bookingRepository.joinWaitlist(any(), any(), any(), any())
        } returns ApiResult.Success("Successfully joined waitlist")

        val savedStateHandle = SavedStateHandle(mapOf("slotId" to slotId, "isWaitlist" to true))
        val viewModel = GuideMatchesViewModel(bookingRepository, activityRepository, savedStateHandle)
        advanceUntilIdle()

        val events = mutableListOf<MatchesUiEvent>()
        val job = launch { viewModel.events.toList(events) }

        viewModel.openWaitlistConfirmation(sampleGuide)
        viewModel.confirmJoinWaitlist()
        advanceUntilIdle()

        assertTrue(events.any { it is MatchesUiEvent.WaitlistSuccess && it.message == "Successfully joined waitlist" })
        job.cancel()
    }

    @Test
    fun `waitlist mode join with 409 not full emits WaitlistSlotNotFull409`() = runTest(testDispatcher) {
        coEvery {
            bookingRepository.joinWaitlist(any(), any(), any(), any())
        } returns ApiResult.Error(
            AppError.Conflict("Slot is not full yet. You can book directly."),
            "Slot is not full yet"
        )

        val savedStateHandle = SavedStateHandle(mapOf("slotId" to slotId, "isWaitlist" to true))
        val viewModel = GuideMatchesViewModel(bookingRepository, activityRepository, savedStateHandle)
        advanceUntilIdle()

        val events = mutableListOf<MatchesUiEvent>()
        val job = launch { viewModel.events.toList(events) }

        viewModel.openWaitlistConfirmation(sampleGuide)
        viewModel.confirmJoinWaitlist()
        advanceUntilIdle()

        assertTrue(events.any { it is MatchesUiEvent.WaitlistSlotNotFull409 })
        job.cancel()
    }

    @Test
    fun `waitlist mode join with timeout emits WaitlistTimeout`() = runTest(testDispatcher) {
        coEvery {
            bookingRepository.joinWaitlist(any(), any(), any(), any())
        } returns ApiResult.Error(AppError.Timeout, "Request timed out")

        val savedStateHandle = SavedStateHandle(mapOf("slotId" to slotId, "isWaitlist" to true))
        val viewModel = GuideMatchesViewModel(bookingRepository, activityRepository, savedStateHandle)
        advanceUntilIdle()

        val events = mutableListOf<MatchesUiEvent>()
        val job = launch { viewModel.events.toList(events) }

        viewModel.openWaitlistConfirmation(sampleGuide)
        viewModel.confirmJoinWaitlist()
        advanceUntilIdle()

        assertTrue(events.any { it is MatchesUiEvent.WaitlistTimeout })
        job.cancel()
    }
}
