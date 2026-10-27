package com.peakconnect.app.feature.trekker.matches

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.peakconnect.app.core.network.ApiResult
import com.peakconnect.app.core.network.AppError
import com.peakconnect.app.data.local.entity.ActivityEntity
import com.peakconnect.app.data.local.entity.SlotEntity
import com.peakconnect.app.data.repository.ActivityRepository
import com.peakconnect.app.data.repository.BookingRepository
import com.peakconnect.app.domain.model.MatchedGuide
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import javax.inject.Inject

data class MatchesUiState(
    val slotId: String = "",
    val isWaitlist: Boolean = false,
    val slot: SlotEntity? = null,
    val activity: ActivityEntity? = null,
    val skillSlider: Int = 50,
    val locationSlider: Int = 50,
    val isWeightsPanelExpanded: Boolean = false,
    val isLoading: Boolean = false,
    val isServerWakingUp: Boolean = false,
    val guides: List<MatchedGuide> = emptyList(),
    val errorMessage: String? = null,
    val selectedGuideForProfile: MatchedGuide? = null,
    val selectedGuideForWaitlistDialog: MatchedGuide? = null,
    val isWaitlistSubmitting: Boolean = false
)

sealed interface MatchesUiEvent {
    data class WaitlistSuccess(val message: String) : MatchesUiEvent
    data object WaitlistSlotNotFull409 : MatchesUiEvent
    data object WaitlistTimeout : MatchesUiEvent
    data class ShowError(val message: String) : MatchesUiEvent
}

@HiltViewModel
class GuideMatchesViewModel @Inject constructor(
    private val bookingRepository: BookingRepository,
    private val activityRepository: ActivityRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val slotId: String = checkNotNull(savedStateHandle["slotId"]) { "slotId is required" }
    private val initialIsWaitlist: Boolean = savedStateHandle["isWaitlist"] ?: false

    private val _uiState = MutableStateFlow(
        MatchesUiState(
            slotId = slotId,
            isWaitlist = initialIsWaitlist
        )
    )
    val uiState: StateFlow<MatchesUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<MatchesUiEvent>()
    val events: SharedFlow<MatchesUiEvent> = _events.asSharedFlow()

    private var debounceJob: Job? = null
    private var slowServerJob: Job? = null

    init {
        loadSlotAndActivity()
        fetchMatches()
    }

    private fun loadSlotAndActivity() {
        viewModelScope.launch {
            val slot = activityRepository.getSlotById(slotId)
            val activity = activityRepository.getActivityForSlot(slotId)
            _uiState.update { it.copy(slot = slot, activity = activity) }
        }
    }

    fun toggleWeightsPanel() {
        _uiState.update { it.copy(isWeightsPanelExpanded = !it.isWeightsPanelExpanded) }
    }

    fun onSkillSliderChanged(value: Int) {
        val coerced = (value / 10) * 10
        if (coerced != _uiState.value.skillSlider) {
            _uiState.update { it.copy(skillSlider = coerced) }
            scheduleDebouncedFetch()
        }
    }

    fun onLocationSliderChanged(value: Int) {
        val coerced = (value / 10) * 10
        if (coerced != _uiState.value.locationSlider) {
            _uiState.update { it.copy(locationSlider = coerced) }
            scheduleDebouncedFetch()
        }
    }

    fun resetWeights() {
        if (_uiState.value.skillSlider != 50 || _uiState.value.locationSlider != 50) {
            _uiState.update { it.copy(skillSlider = 50, locationSlider = 50) }
            scheduleDebouncedFetch()
        }
    }

    private fun scheduleDebouncedFetch() {
        debounceJob?.cancel()
        debounceJob = viewModelScope.launch {
            delay(600)
            fetchMatches()
        }
    }

    fun fetchMatches() {
        slowServerJob?.cancel()
        _uiState.update { it.copy(isLoading = true, isServerWakingUp = false, errorMessage = null) }

        // Trigger slow server message after 6 seconds of loading
        slowServerJob = viewModelScope.launch {
            delay(6000)
            if (_uiState.value.isLoading) {
                _uiState.update { it.copy(isServerWakingUp = true) }
            }
        }

        viewModelScope.launch {
            val (skillWeight, locationWeight, languageWeight) = calculateWeights(
                _uiState.value.skillSlider,
                _uiState.value.locationSlider
            )

            val result = bookingRepository.requestMatches(
                slotId = slotId,
                skillWeight = skillWeight,
                locationWeight = locationWeight,
                languageWeight = languageWeight
            )

            slowServerJob?.cancel()

            when (result) {
                is ApiResult.Success -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isServerWakingUp = false,
                            guides = result.data,
                            errorMessage = null
                        )
                    }
                }
                is ApiResult.Error -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isServerWakingUp = false,
                            errorMessage = result.message
                        )
                    }
                }
                is ApiResult.NetworkError -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isServerWakingUp = false,
                            errorMessage = result.exception.message ?: "Network error"
                        )
                    }
                }
            }
        }
    }

    fun selectGuideForProfile(guide: MatchedGuide?) {
        _uiState.update { it.copy(selectedGuideForProfile = guide) }
    }

    fun openWaitlistConfirmation(guide: MatchedGuide) {
        _uiState.update {
            it.copy(
                selectedGuideForProfile = null,
                selectedGuideForWaitlistDialog = guide
            )
        }
    }

    fun dismissWaitlistConfirmation() {
        _uiState.update { it.copy(selectedGuideForWaitlistDialog = null) }
    }

    fun confirmJoinWaitlist() {
        val guide = _uiState.value.selectedGuideForWaitlistDialog ?: return
        if (_uiState.value.isWaitlistSubmitting) return

        _uiState.update { it.copy(isWaitlistSubmitting = true) }

        viewModelScope.launch {
            val slot = _uiState.value.slot
            val activity = _uiState.value.activity

            val result = bookingRepository.joinWaitlist(
                slotId = slotId,
                guideId = guide.guideId,
                activityTitle = activity?.title ?: "Trek Activity",
                slotDate = slot?.date ?: LocalDateTime.now()
            )

            _uiState.update {
                it.copy(
                    isWaitlistSubmitting = false,
                    selectedGuideForWaitlistDialog = null
                )
            }

            when (result) {
                is ApiResult.Success -> {
                    _events.emit(MatchesUiEvent.WaitlistSuccess(result.data))
                }
                is ApiResult.Error -> {
                    val appError = result.type
                    if (appError is AppError.Conflict) {
                        _events.emit(MatchesUiEvent.WaitlistSlotNotFull409)
                    } else if (appError is AppError.Timeout) {
                        _events.emit(MatchesUiEvent.WaitlistTimeout)
                    } else {
                        _events.emit(MatchesUiEvent.ShowError(result.message))
                    }
                }
                is ApiResult.NetworkError -> {
                    _events.emit(MatchesUiEvent.WaitlistTimeout)
                }
            }
        }
    }

    fun switchToBookMode() {
        _uiState.update { it.copy(isWaitlist = false) }
    }

    companion object {
        fun calculateWeights(skillSlider: Int, locationSlider: Int): Triple<Double, Double, Double> {
            val s = skillSlider.toDouble()
            val l = locationSlider.toDouble()
            val (skillWeight, locationWeight) = if (s == 0.0 && l == 0.0) {
                0.5 to 0.5
            } else {
                val sum = s + l
                (s / sum) to (l / sum)
            }
            return Triple(skillWeight, locationWeight, 0.0)
        }
    }
}
