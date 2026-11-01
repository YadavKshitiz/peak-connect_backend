package com.peakconnect.app.feature.guide.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.peakconnect.app.core.network.ApiResult
import com.peakconnect.app.data.dto.GuideProfileUpdateRequest
import com.peakconnect.app.data.repository.GuideRepository
import com.peakconnect.app.domain.model.ExperienceLevel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class GuideProfileEditUiState(
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val skills: List<String> = emptyList(),
    val languages: List<String> = emptyList(),
    val experienceLevel: ExperienceLevel = ExperienceLevel.BEGINNER,
    val location: String = "",
    val latitude: Double? = null,
    val longitude: Double? = null,
    val validationError: String? = null,
    val errorMessage: String? = null,
    val isSavedSuccess: Boolean = false
)

@HiltViewModel
class GuideProfileEditViewModel @Inject constructor(
    private val guideRepository: GuideRepository,
    private val networkRefreshNotifier: com.peakconnect.app.core.network.NetworkRefreshNotifier? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(GuideProfileEditUiState())
    val uiState: StateFlow<GuideProfileEditUiState> = _uiState.asStateFlow()

    init {
        loadProfile()

        // Auto-refresh when back online (Requirement 1 & 2)
        networkRefreshNotifier?.refreshEvents?.onEach {
            guideRepository.refreshProfile()
        }?.launchIn(viewModelScope)
    }

    private fun loadProfile() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            guideRepository.refreshProfile()
            val profile = guideRepository.getProfileFlow().firstOrNull()
            if (profile != null) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        skills = profile.skills,
                        languages = profile.languages,
                        experienceLevel = profile.experienceLevel,
                        location = profile.location ?: "",
                        latitude = profile.latitude,
                        longitude = profile.longitude
                    )
                }
            } else {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    fun addSkill(skill: String) {
        val trimmed = skill.trim()
        if (trimmed.isNotBlank() && !_uiState.value.skills.contains(trimmed)) {
            _uiState.update { it.copy(skills = it.skills + trimmed, validationError = null) }
        }
    }

    fun removeSkill(skill: String) {
        _uiState.update { it.copy(skills = it.skills.filter { s -> s != skill }) }
    }

    fun addLanguage(language: String) {
        val trimmed = language.trim()
        if (trimmed.isNotBlank() && !_uiState.value.languages.contains(trimmed)) {
            _uiState.update { it.copy(languages = it.languages + trimmed, validationError = null) }
        }
    }

    fun removeLanguage(language: String) {
        _uiState.update { it.copy(languages = it.languages.filter { l -> l != language }) }
    }

    fun setExperienceLevel(level: ExperienceLevel) {
        _uiState.update { it.copy(experienceLevel = level, validationError = null) }
    }

    fun setLocation(location: String) {
        _uiState.update { it.copy(location = location, validationError = null) }
    }

    fun setCoordinates(lat: Double, lng: Double) {
        _uiState.update { it.copy(latitude = lat, longitude = lng) }
    }

    fun clearSuccess() {
        _uiState.update { it.copy(isSavedSuccess = false) }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null, validationError = null) }
    }

    fun saveProfile() {
        val state = _uiState.value
        if (state.skills.isEmpty()) {
            _uiState.update { it.copy(validationError = "Please add at least one skill.") }
            return
        }
        if (state.languages.isEmpty()) {
            _uiState.update { it.copy(validationError = "Please add at least one language.") }
            return
        }
        if (state.location.trim().isBlank()) {
            _uiState.update { it.copy(validationError = "Please enter your base location.") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, errorMessage = null, validationError = null) }
            val request = GuideProfileUpdateRequest(
                skills = state.skills,
                languages = state.languages,
                location = state.location.trim(),
                latitude = state.latitude,
                longitude = state.longitude,
                experienceLevel = state.experienceLevel
            )
            val result = guideRepository.updateProfile(request)
            when (result) {
                is ApiResult.Success -> {
                    _uiState.update {
                        it.copy(
                            isSaving = false,
                            isSavedSuccess = true,
                            errorMessage = null,
                            validationError = null
                        )
                    }
                }
                is ApiResult.Error -> {
                    _uiState.update {
                        it.copy(
                            isSaving = false,
                            errorMessage = result.message
                        )
                    }
                }
                is ApiResult.NetworkError -> {
                    _uiState.update {
                        it.copy(
                            isSaving = false,
                            errorMessage = result.exception.message ?: "Network error"
                        )
                    }
                }
            }
        }
    }
}
