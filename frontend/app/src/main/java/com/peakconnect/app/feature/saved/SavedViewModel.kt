package com.peakconnect.app.feature.saved

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.peakconnect.app.data.local.entity.ActivityEntity
import com.peakconnect.app.data.repository.ActivityRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SavedViewModel @Inject constructor(
    private val activityRepository: ActivityRepository
) : ViewModel() {

    val savedActivities: StateFlow<List<ActivityEntity>> = activityRepository.getSavedActivities()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    fun toggleFavourite(activityId: String) {
        viewModelScope.launch {
            activityRepository.toggleFavourite(activityId)
        }
    }

    fun onActivityClicked(activityId: String) {
        viewModelScope.launch {
            activityRepository.recordRecentlyViewed(activityId)
        }
    }
}
