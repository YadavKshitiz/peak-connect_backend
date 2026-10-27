package com.peakconnect.app

import androidx.lifecycle.SavedStateHandle
import com.peakconnect.app.core.network.ApiResult
import com.peakconnect.app.data.local.entity.ActivityEntity
import com.peakconnect.app.data.local.entity.ActivityWithSlots
import com.peakconnect.app.data.local.entity.ChecklistItemEntity
import com.peakconnect.app.data.local.entity.SlotEntity
import com.peakconnect.app.data.local.entity.WaitlistEntryEntity
import com.peakconnect.app.data.repository.ActivityRepository
import com.peakconnect.app.domain.model.CancellationPolicyType
import com.peakconnect.app.domain.model.DifficultyLevel
import com.peakconnect.app.domain.model.RiskLevel
import com.peakconnect.app.domain.model.Season
import com.peakconnect.app.domain.model.WaitlistStatus
import com.peakconnect.app.feature.trekker.detail.ActivityDetailViewModel
import com.peakconnect.app.feature.trekker.detail.PackingCategory
import com.peakconnect.app.feature.trekker.detail.PackingChecklistCatalog
import io.mockk.coEvery
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
class ActivityDetailViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val activityRepository: ActivityRepository = mockk(relaxed = true)

    private val activityId = "act-detail-1"
    private val now: LocalDateTime = LocalDateTime.now()

    private val sampleActivity = ActivityEntity(
        id = activityId,
        title = "Hampta Pass Trek",
        description = "Scenic cross-over trek in Himachal Pradesh",
        location = "Manali",
        latitude = 32.24,
        longitude = 77.19,
        difficulty = DifficultyLevel.MODERATE,
        basePrice = BigDecimal("8500.00"),
        cancellationPolicy = CancellationPolicyType.MODERATE,
        minSlotPrice = BigDecimal("8000.00"),
        lastUpdated = 1000L
    )

    // Slot 1: in the past
    private val pastSlot = SlotEntity(
        id = "slot-past",
        activityId = activityId,
        date = now.minusDays(5),
        capacity = 10,
        currentOccupancy = 4,
        season = Season.SHOULDER,
        currentPrice = BigDecimal("8000.00"),
        riskLevel = RiskLevel.LOW
    )

    // Slot 2: upcoming in 10 days, available (5 of 10 left)
    private val futureSlot1 = SlotEntity(
        id = "slot-future-1",
        activityId = activityId,
        date = now.plusDays(10),
        capacity = 10,
        currentOccupancy = 5,
        season = Season.PEAK,
        currentPrice = BigDecimal("9500.00"),
        riskLevel = RiskLevel.LOW
    )

    // Slot 3: upcoming in 2 days, full (10 of 10)
    private val futureSlot2 = SlotEntity(
        id = "slot-future-2",
        activityId = activityId,
        date = now.plusDays(2),
        capacity = 10,
        currentOccupancy = 10,
        season = Season.PEAK,
        currentPrice = BigDecimal("9000.00"),
        riskLevel = RiskLevel.MODERATE
    )

    private val activityWithSlotsFlow = MutableStateFlow(
        ActivityWithSlots(
            activity = sampleActivity,
            slots = listOf(pastSlot, futureSlot1, futureSlot2)
        )
    )

    private val favouritesFlow = MutableStateFlow(setOf("act-other"))
    private val waitlistFlow = MutableStateFlow(
        listOf(
            WaitlistEntryEntity(
                slotId = "slot-future-2",
                activityTitle = "Hampta Pass Trek",
                slotDate = now.plusDays(2),
                status = WaitlistStatus.WAITING
            )
        )
    )

    private val checklistFlow = MutableStateFlow<List<ChecklistItemEntity>>(emptyList())

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)

        every { activityRepository.getActivityWithSlots(activityId) } returns activityWithSlotsFlow
        every { activityRepository.getFavouritesSetFlow() } returns favouritesFlow
        every { activityRepository.getWaitlistEntriesFlow() } returns waitlistFlow
        every { activityRepository.getChecklistItems(activityId, any()) } returns checklistFlow
        coEvery { activityRepository.getActivity(activityId) } returns ApiResult.Success(mockk(relaxed = true))
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `upcoming slots are filtered to future and sorted by date ascending`() = runTest(testDispatcher) {
        val savedStateHandle = SavedStateHandle(mapOf("activityId" to activityId))
        val viewModel = ActivityDetailViewModel(activityRepository, savedStateHandle)
        advanceUntilIdle()

        val upcoming = viewModel.getUpcomingSlots(now)

        // Only future slots are present (pastSlot excluded)
        assertEquals(2, upcoming.size)
        // Sorted ascending by date: slot-future-2 (in 2 days) comes before slot-future-1 (in 10 days)
        assertEquals("slot-future-2", upcoming[0].id)
        assertEquals("slot-future-1", upcoming[1].id)
    }

    @Test
    fun `lowest price reflects upcoming slots lowest price`() = runTest(testDispatcher) {
        val savedStateHandle = SavedStateHandle(mapOf("activityId" to activityId))
        val viewModel = ActivityDetailViewModel(activityRepository, savedStateHandle)
        advanceUntilIdle()

        // futureSlot2 has currentPrice 9000, futureSlot1 has 9500
        val lowest = viewModel.getLowestPrice(now)
        assertEquals(BigDecimal("9000.00"), lowest)
    }

    @Test
    fun `isSlotOnWaitlist returns true only when slot is in local waiting state`() = runTest(testDispatcher) {
        val savedStateHandle = SavedStateHandle(mapOf("activityId" to activityId))
        val viewModel = ActivityDetailViewModel(activityRepository, savedStateHandle)
        advanceUntilIdle()

        // slot-future-2 is in waitlistFlow with WAITING status
        assertTrue(viewModel.isSlotOnWaitlist("slot-future-2"))
        // slot-future-1 is not on waitlist
        assertFalse(viewModel.isSlotOnWaitlist("slot-future-1"))
    }

    @Test
    fun `toggleFavourite delegates to repository`() = runTest(testDispatcher) {
        val savedStateHandle = SavedStateHandle(mapOf("activityId" to activityId))
        val viewModel = ActivityDetailViewModel(activityRepository, savedStateHandle)
        advanceUntilIdle()

        viewModel.toggleFavourite()
        advanceUntilIdle()

        coVerify { activityRepository.toggleFavourite(activityId) }
    }

    @Test
    fun `checklist operations delegate to repository`() = runTest(testDispatcher) {
        val savedStateHandle = SavedStateHandle(mapOf("activityId" to activityId))
        val viewModel = ActivityDetailViewModel(activityRepository, savedStateHandle)
        advanceUntilIdle()

        // Add custom item
        viewModel.addCustomChecklistItem(PackingCategory.GEAR, "Camera tripod")
        advanceUntilIdle()
        coVerify {
            activityRepository.addChecklistItem(
                match { it.text == "GEAR|Camera tripod" && it.isCustom }
            )
        }

        // Toggle item
        val dummyEntity = ChecklistItemEntity(
            id = 42L,
            activityId = activityId,
            difficulty = DifficultyLevel.MODERATE,
            text = "GEAR|Trekking poles",
            isChecked = false
        )
        val categorized = PackingChecklistCatalog.parseItem(dummyEntity)
        viewModel.toggleChecklistItem(categorized)
        advanceUntilIdle()
        coVerify {
            activityRepository.updateChecklistItem(
                match { it.id == 42L && it.isChecked }
            )
        }

        // Delete item
        viewModel.deleteChecklistItem(42L)
        advanceUntilIdle()
        coVerify { activityRepository.deleteChecklistItem(42L) }

        // Reset checklist
        viewModel.resetChecklist()
        advanceUntilIdle()
        coVerify { activityRepository.resetChecklist(activityId, DifficultyLevel.MODERATE, any()) }
    }
}
