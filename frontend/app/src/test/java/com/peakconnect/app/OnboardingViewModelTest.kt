package com.peakconnect.app

import com.peakconnect.app.core.preferences.OnboardingStore
import com.peakconnect.app.feature.onboarding.OnboardingViewModel
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class OnboardingViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val onboardingStore: OnboardingStore = mockk(relaxed = true)

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `completeOnboarding saves completion state and invokes callback`() = runTest {
        val viewModel = OnboardingViewModel(onboardingStore)
        var callbackInvoked = false

        viewModel.completeOnboarding {
            callbackInvoked = true
        }
        advanceUntilIdle()

        coVerify { onboardingStore.setOnboardingCompleted(true) }
        assertTrue(callbackInvoked)
    }
}

