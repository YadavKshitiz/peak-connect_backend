package com.peakconnect.app.feature.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.peakconnect.app.core.preferences.OnboardingStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val onboardingStore: OnboardingStore
) : ViewModel() {

    fun completeOnboarding(onCompleted: () -> Unit) {
        viewModelScope.launch {
            onboardingStore.setOnboardingCompleted(true)
            onCompleted()
        }
    }
}
