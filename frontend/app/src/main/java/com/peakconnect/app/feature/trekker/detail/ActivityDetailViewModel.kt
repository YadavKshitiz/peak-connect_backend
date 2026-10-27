package com.peakconnect.app.feature.trekker.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.peakconnect.app.core.network.ApiResult
import com.peakconnect.app.data.local.entity.ActivityWithSlots
import com.peakconnect.app.data.local.entity.ChecklistItemEntity
import com.peakconnect.app.data.local.entity.SlotEntity
import com.peakconnect.app.data.local.entity.WaitlistEntryEntity
import com.peakconnect.app.data.repository.ActivityRepository
import com.peakconnect.app.domain.model.DifficultyLevel
import com.peakconnect.app.domain.model.WaitlistStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.time.LocalDateTime
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ActivityDetailViewModel @Inject constructor(
    private val activityRepository: ActivityRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    val activityId: String = checkNotNull(savedStateHandle["activityId"]) {
        "activityId must be supplied to ActivityDetailViewModel"
    }

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _snackBarMessage = MutableSharedFlow<String>()
    val snackBarMessage: SharedFlow<String> = _snackBarMessage.asSharedFlow()

    val activityWithSlots: StateFlow<ActivityWithSlots?> =
        activityRepository.getActivityWithSlots(activityId)
            .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val isFavourite: StateFlow<Boolean> = activityRepository.getFavouritesSetFlow()
        .map { it.contains(activityId) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    val waitlistEntries: StateFlow<List<WaitlistEntryEntity>> =
        activityRepository.getWaitlistEntriesFlow()
            .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    // Checklist items for current activity
    private val difficultyFlow = activityWithSlots.map { it?.activity?.difficulty }.distinctUntilChanged()

    val checklistItems: StateFlow<List<CategorizedChecklistItem>> = difficultyFlow
        .flatMapLatest { difficulty ->
            if (difficulty != null) {
                activityRepository.getChecklistItems(activityId, difficulty)
                    .map { entities ->
                        entities.map { PackingChecklistCatalog.parseItem(it) }
                    }
            } else {
                flowOf(emptyList())
            }
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    init {
        // Record recently viewed
        viewModelScope.launch {
            activityRepository.recordRecentlyViewed(activityId)
        }

        // Initial background refresh
        refresh(isManual = false)

        // Initialize default checklist if none exist once activity loads
        viewModelScope.launch {
            difficultyFlow.collect { difficulty ->
                if (difficulty != null) {
                    val currentItems = checklistItems.value
                    if (currentItems.isEmpty()) {
                        val defaultEntities = PackingChecklistCatalog.getDefaultItems(difficulty, activityId)
                        activityRepository.resetChecklist(activityId, difficulty, defaultEntities)
                    }
                }
            }
        }
    }

    fun refresh(isManual: Boolean = false) {
        viewModelScope.launch {
            _isRefreshing.value = true
            val result = activityRepository.getActivity(activityId)
            _isRefreshing.value = false

            when (result) {
                is ApiResult.Success -> Unit
                is ApiResult.Error, is ApiResult.NetworkError -> {
                    if (isManual) {
                        _snackBarMessage.emit("Couldn't refresh activity details")
                    }
                }
            }
        }
    }

    fun onResume() {
        refresh(isManual = false)
    }

    fun toggleFavourite() {
        viewModelScope.launch {
            activityRepository.toggleFavourite(activityId)
        }
    }

    fun isSlotOnWaitlist(slotId: String): Boolean {
        return waitlistEntries.value.any { it.slotId == slotId && it.status == WaitlistStatus.WAITING }
    }

    fun getUpcomingSlots(now: LocalDateTime = LocalDateTime.now()): List<SlotEntity> {
        val allSlots = activityWithSlots.value?.slots ?: emptyList()
        return allSlots
            .filter { it.date.isAfter(now) }
            .sortedBy { it.date }
    }

    fun getLowestPrice(now: LocalDateTime = LocalDateTime.now()): BigDecimal {
        val upcoming = getUpcomingSlots(now)
        val minSlotPrice = upcoming.minOfOrNull { it.currentPrice }
        return minSlotPrice
            ?: activityWithSlots.value?.activity?.minSlotPrice
            ?: activityWithSlots.value?.activity?.basePrice
            ?: BigDecimal.ZERO
    }

    fun toggleChecklistItem(item: CategorizedChecklistItem) {
        viewModelScope.launch {
            val updated = item.rawEntity.copy(isChecked = !item.isChecked)
            activityRepository.updateChecklistItem(updated)
        }
    }

    fun addCustomChecklistItem(category: PackingCategory, text: String) {
        val difficulty = activityWithSlots.value?.activity?.difficulty ?: DifficultyLevel.EASY
        viewModelScope.launch {
            val newEntity = PackingChecklistCatalog.createCustomEntity(
                activityId = activityId,
                difficulty = difficulty,
                category = category,
                text = text
            )
            activityRepository.addChecklistItem(newEntity)
        }
    }

    fun deleteChecklistItem(itemId: Long) {
        viewModelScope.launch {
            activityRepository.deleteChecklistItem(itemId)
        }
    }

    fun resetChecklist() {
        val difficulty = activityWithSlots.value?.activity?.difficulty ?: return
        viewModelScope.launch {
            val defaultEntities = PackingChecklistCatalog.getDefaultItems(difficulty, activityId)
            activityRepository.resetChecklist(activityId, difficulty, defaultEntities)
        }
    }
}
