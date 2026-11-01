package com.peakconnect.app

import com.peakconnect.app.core.network.ApiResult
import com.peakconnect.app.core.network.AppError
import com.peakconnect.app.data.local.entity.GuideProfileEntity
import com.peakconnect.app.data.repository.GuideRepository
import com.peakconnect.app.domain.model.ExperienceLevel
import com.peakconnect.app.domain.model.GuideProfile
import com.peakconnect.app.feature.guide.profile.GuideProfileEditViewModel
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
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class GuideProfileEditViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val guideRepository: GuideRepository = mockk(relaxed = true)
    private val profileFlow = MutableStateFlow<GuideProfileEntity?>(null)

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        every { guideRepository.getProfileFlow() } returns profileFlow
        coEvery { guideRepository.refreshProfile() } returns ApiResult.Success(Unit)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `profile loads initial values from repository`() = runTest {
        profileFlow.value = GuideProfileEntity(
            id = "g1",
            name = "Ramesh",
            email = "ramesh@test.com",
            skills = listOf("Navigation", "Wilderness First Aid"),
            languages = listOf("English", "Hindi"),
            location = "Manali",
            latitude = 32.2396,
            longitude = 77.1887,
            experienceLevel = ExperienceLevel.INTERMEDIATE,
            isVerified = true,
            lastUpdated = 1000L
        )

        val viewModel = GuideProfileEditViewModel(guideRepository)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals(listOf("Navigation", "Wilderness First Aid"), state.skills)
        assertEquals(listOf("English", "Hindi"), state.languages)
        assertEquals("Manali", state.location)
        assertEquals(32.2396, state.latitude!!, 0.0001)
        assertEquals(77.1887, state.longitude!!, 0.0001)
        assertEquals(ExperienceLevel.INTERMEDIATE, state.experienceLevel)
    }

    @Test
    fun `validation requires at least one skill`() = runTest {
        val viewModel = GuideProfileEditViewModel(guideRepository)
        advanceUntilIdle()

        viewModel.setLocation("Dharamshala")
        viewModel.addLanguage("English")
        // skills empty
        viewModel.saveProfile()

        assertEquals("Please add at least one skill.", viewModel.uiState.value.validationError)
        coVerify(exactly = 0) { guideRepository.updateProfile(any()) }
    }

    @Test
    fun `validation requires at least one language`() = runTest {
        val viewModel = GuideProfileEditViewModel(guideRepository)
        advanceUntilIdle()

        viewModel.addSkill("Rock Climbing")
        viewModel.setLocation("Dharamshala")
        // languages empty
        viewModel.saveProfile()

        assertEquals("Please add at least one language.", viewModel.uiState.value.validationError)
        coVerify(exactly = 0) { guideRepository.updateProfile(any()) }
    }

    @Test
    fun `validation requires base location`() = runTest {
        val viewModel = GuideProfileEditViewModel(guideRepository)
        advanceUntilIdle()

        viewModel.addSkill("Rock Climbing")
        viewModel.addLanguage("English")
        viewModel.setLocation("   ") // blank
        viewModel.saveProfile()

        assertEquals("Please enter your base location.", viewModel.uiState.value.validationError)
        coVerify(exactly = 0) { guideRepository.updateProfile(any()) }
    }

    @Test
    fun `successful save calls updateProfile and updates state`() = runTest {
        val mockProfile: GuideProfile = mockk(relaxed = true)
        coEvery { guideRepository.updateProfile(any()) } returns ApiResult.Success(mockProfile)

        val viewModel = GuideProfileEditViewModel(guideRepository)
        advanceUntilIdle()

        viewModel.addSkill("Camp Setup")
        viewModel.addLanguage("Hindi")
        viewModel.setExperienceLevel(ExperienceLevel.EXPERT)
        viewModel.setLocation("Rishikesh")
        viewModel.setCoordinates(30.0869, 78.2676)

        viewModel.saveProfile()
        advanceUntilIdle()

        coVerify {
            guideRepository.updateProfile(
                match {
                    it.skills == listOf("Camp Setup") &&
                        it.languages == listOf("Hindi") &&
                        it.experienceLevel == ExperienceLevel.EXPERT &&
                        it.location == "Rishikesh" &&
                        it.latitude == 30.0869 &&
                        it.longitude == 78.2676
                }
            )
        }

        assertTrue(viewModel.uiState.value.isSavedSuccess)
        assertNull(viewModel.uiState.value.errorMessage)
    }

    @Test
    fun `save error sets error message`() = runTest {
        coEvery { guideRepository.updateProfile(any()) } returns ApiResult.Error(
            type = AppError.Validation("Server rejected profile"),
            message = "Server rejected profile"
        )

        val viewModel = GuideProfileEditViewModel(guideRepository)
        advanceUntilIdle()

        viewModel.addSkill("Camp Setup")
        viewModel.addLanguage("Hindi")
        viewModel.setLocation("Rishikesh")

        viewModel.saveProfile()
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isSavedSuccess)
        assertEquals("Server rejected profile", viewModel.uiState.value.errorMessage)
    }
}
