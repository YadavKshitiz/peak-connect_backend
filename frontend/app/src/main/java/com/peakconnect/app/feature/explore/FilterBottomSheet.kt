package com.peakconnect.app.feature.explore

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.peakconnect.app.R
import com.peakconnect.app.core.ui.PrimaryButton
import com.peakconnect.app.core.ui.AppModalBottomSheet
import com.peakconnect.app.core.ui.labelRes
import com.peakconnect.app.data.local.dao.ActivityFilter
import com.peakconnect.app.data.local.dao.ActivitySortOption
import com.peakconnect.app.domain.model.DifficultyLevel
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun FilterBottomSheet(
    currentFilter: ActivityFilter,
    distinctLocations: List<String>,
    matchingCount: Int,
    onDraftFilterChanged: (ActivityFilter) -> Unit,
    onApply: (ActivityFilter) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    var draftFilter by remember(currentFilter) { mutableStateOf(currentFilter) }
    var showDatePicker by remember { mutableStateOf(false) }

    LaunchedEffect(draftFilter) {
        onDraftFilterChanged(draftFilter)
    }

    AppModalBottomSheet(
        onDismissRequest = onDismiss,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(id = R.string.explore_filter_title),
                    style = MaterialTheme.typography.titleLarge
                )
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = stringResource(id = R.string.common_close)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Difficulty section
            Text(
                text = stringResource(id = R.string.explore_filter_difficulty),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val currentDifficulties = draftFilter.difficulties ?: emptySet()
                DifficultyLevel.entries.forEach { level ->
                    val selected = currentDifficulties.contains(level)
                    FilterChip(
                        selected = selected,
                        onClick = {
                            val newSet = if (selected) {
                                currentDifficulties - level
                            } else {
                                currentDifficulties + level
                            }
                            draftFilter = draftFilter.copy(
                                difficulties = if (newSet.isEmpty()) null else newSet
                            )
                        },
                        label = { Text(text = stringResource(id = level.labelRes())) },
                        leadingIcon = if (selected) {
                            {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        } else null
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Location section
            Text(
                text = stringResource(id = R.string.explore_filter_location),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // "Any" option
                val isAnyLocation = draftFilter.location.isNullOrBlank()
                FilterChip(
                    selected = isAnyLocation,
                    onClick = {
                        draftFilter = draftFilter.copy(location = null)
                    },
                    label = { Text(text = stringResource(id = R.string.explore_filter_location_any)) },
                    leadingIcon = if (isAnyLocation) {
                        {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    } else null
                )

                // Distinct locations
                distinctLocations.forEach { loc ->
                    val isSelected = draftFilter.location?.equals(loc, ignoreCase = true) == true
                    FilterChip(
                        selected = isSelected,
                        onClick = {
                            draftFilter = draftFilter.copy(
                                location = if (isSelected) null else loc
                            )
                        },
                        label = { Text(text = loc) },
                        leadingIcon = if (isSelected) {
                            {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        } else null
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Date section (Available because GET /api/activities returns slots)
            Text(
                text = stringResource(id = R.string.explore_filter_date),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = { showDatePicker = true },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.CalendarToday,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = draftFilter.hasSlotOnDate
                            ?: stringResource(id = R.string.explore_filter_date_any)
                    )
                }

                if (draftFilter.hasSlotOnDate != null) {
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(
                        onClick = { draftFilter = draftFilter.copy(hasSlotOnDate = null) }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = stringResource(id = R.string.common_clear)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Sort section
            Text(
                text = stringResource(id = R.string.explore_filter_sort),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ActivitySortOption.entries.forEach { option ->
                    val isSelected = draftFilter.sortOption == option
                    val labelRes = when (option) {
                        ActivitySortOption.PRICE_LOW_TO_HIGH -> R.string.explore_sort_price_low_high
                        ActivitySortOption.PRICE_HIGH_TO_LOW -> R.string.explore_sort_price_high_low
                        ActivitySortOption.DIFFICULTY -> R.string.explore_sort_difficulty
                        ActivitySortOption.TITLE -> R.string.explore_sort_name_az
                    }

                    FilterChip(
                        selected = isSelected,
                        onClick = {
                            draftFilter = draftFilter.copy(sortOption = option)
                        },
                        label = { Text(text = stringResource(id = labelRes)) },
                        leadingIcon = if (isSelected) {
                            {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        } else null
                    )
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Actions: Reset & Apply (N)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        draftFilter = ActivityFilter(
                            query = currentFilter.query,
                            difficulties = null,
                            location = null,
                            hasSlotOnDate = null,
                            sortOption = ActivitySortOption.TITLE
                        )
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(text = stringResource(id = R.string.explore_filter_reset))
                }

                val applyText = stringResource(id = R.string.explore_filter_apply, matchingCount)
                PrimaryButton(
                    text = applyText,
                    onClick = {
                        onApply(draftFilter)
                    },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState()
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        val selectedMillis = datePickerState.selectedDateMillis
                        if (selectedMillis != null) {
                            val localDate = Instant.ofEpochMilli(selectedMillis)
                                .atZone(ZoneId.of("UTC"))
                                .toLocalDate()
                            val formatted = localDate.format(DateTimeFormatter.ISO_LOCAL_DATE)
                            draftFilter = draftFilter.copy(hasSlotOnDate = formatted)
                        }
                        showDatePicker = false
                    }
                ) {
                    Text(text = stringResource(id = R.string.common_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text(text = stringResource(id = R.string.common_cancel))
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}
