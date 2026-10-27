package com.peakconnect.app.feature.explore

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.SearchOff
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import com.peakconnect.app.R
import com.peakconnect.app.core.theme.AppShapes
import com.peakconnect.app.core.ui.ActivityCard
import com.peakconnect.app.core.ui.AppChip
import com.peakconnect.app.core.ui.AppPullToRefreshBox
import com.peakconnect.app.core.ui.BannerType
import com.peakconnect.app.core.ui.EmptyState
import com.peakconnect.app.core.ui.ErrorState
import com.peakconnect.app.core.ui.InfoBanner
import com.peakconnect.app.core.ui.RecentlyViewedCard
import com.peakconnect.app.core.ui.ShimmerBox
import com.peakconnect.app.core.ui.labelRes
import com.peakconnect.app.core.util.LocalIsOnline
import com.peakconnect.app.data.local.dao.ActivityFilter
import com.peakconnect.app.data.local.dao.ActivitySortOption
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ExploreScreen(
    onNavigateToActivityDetail: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ExploreViewModel = hiltViewModel()
) {
    val searchQuery by viewModel.searchQuery.collectAsState()
    val isSearchFocused by viewModel.isSearchFocused.collectAsState()
    val filter by viewModel.filter.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val isFirstLoading by viewModel.isFirstLoading.collectAsState()
    val hasCachedActivities by viewModel.hasCachedActivities.collectAsState()
    val userFirstName by viewModel.userFirstName.collectAsState()
    val distinctLocations by viewModel.distinctLocations.collectAsState()
    val recentlyViewed by viewModel.recentlyViewed.collectAsState()
    val searchHistory by viewModel.searchHistory.collectAsState()
    val favourites by viewModel.favouritesSet.collectAsState()

    val pagedActivities = viewModel.pagedActivities.collectAsLazyPagingItems()
    val isOnline = LocalIsOnline.current

    val snackbarHostState = remember { SnackbarHostState() }
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val scope = rememberCoroutineScope()

    var showFilterSheet by remember { mutableStateOf(false) }
    var filterSheetCount by remember { mutableIntStateOf(0) }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        viewModel.onResume()
    }

    LaunchedEffect(Unit) {
        viewModel.snackBarMessage.collectLatest { msg ->
            snackbarHostState.showSnackbar(msg)
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        AppPullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = { viewModel.refreshActivities(isManual = true) },
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {

                // Offline InfoBanner
                if (!isOnline) {
                    InfoBanner(
                        type = BannerType.INFO,
                        text = stringResource(id = R.string.explore_offline_banner),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp)
                    )
                }

                // Greeting & Bell Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(id = R.string.explore_greeting, userFirstName),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )

                    IconButton(
                        onClick = { /* Notification bell wired in Task 13 */ }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Notifications,
                            contentDescription = stringResource(id = R.string.nav_tab_explore),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Search Bar & Filter Button
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { viewModel.onSearchQueryChanged(it) },
                        modifier = Modifier
                            .weight(1f)
                            .onFocusChanged { viewModel.onSearchFocusChanged(it.isFocused) },
                        placeholder = {
                            Text(
                                text = stringResource(id = R.string.explore_search_hint),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(
                                    onClick = {
                                        viewModel.onSearchQueryChanged("")
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = stringResource(id = R.string.common_clear_text)
                                    )
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface
                        ),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(
                            onSearch = {
                                keyboardController?.hide()
                                focusManager.clearFocus()
                                viewModel.onSearchSubmitted(searchQuery)
                            }
                        )
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    val hasActiveFilters = filter.difficulties != null ||
                            filter.location != null ||
                            filter.hasSlotOnDate != null ||
                            filter.sortOption != ActivitySortOption.TITLE

                    IconButton(
                        onClick = {
                            scope.launch {
                                filterSheetCount = viewModel.getMatchingCount(filter)
                            }
                            showFilterSheet = true
                        },
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.FilterList,
                            contentDescription = stringResource(id = R.string.explore_filter_title),
                            tint = if (hasActiveFilters) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Recent Searches Chips (Shown while search is focused and query is empty)
                if (isSearchFocused && searchQuery.isBlank() && searchHistory.isNotEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = stringResource(id = R.string.explore_search_history_title),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            TextButton(onClick = { viewModel.clearSearchHistory() }) {
                                Text(
                                    text = stringResource(id = R.string.explore_search_history_clear),
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                        }

                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            searchHistory.forEach { item ->
                                FilterChip(
                                    selected = false,
                                    onClick = {
                                        keyboardController?.hide()
                                        focusManager.clearFocus()
                                        viewModel.onSearchTermClicked(item.query)
                                    },
                                    label = { Text(text = item.query) },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Default.History,
                                            contentDescription = null,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                )
                            }
                        }
                    }
                }

                // Active Filter Chips Under Search Field
                val hasActiveFilters = filter.difficulties != null ||
                        filter.location != null ||
                        filter.hasSlotOnDate != null ||
                        filter.sortOption != ActivitySortOption.TITLE

                if (hasActiveFilters) {
                    FlowRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // Difficulty chips
                        filter.difficulties?.forEach { level ->
                            FilterChip(
                                selected = true,
                                onClick = { viewModel.removeDifficultyFilter(level) },
                                label = { Text(text = stringResource(id = level.labelRes())) },
                                trailingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = stringResource(id = R.string.common_clear),
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            )
                        }

                        // Location chip
                        filter.location?.let { loc ->
                            FilterChip(
                                selected = true,
                                onClick = { viewModel.removeLocationFilter() },
                                label = { Text(text = loc) },
                                trailingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = stringResource(id = R.string.common_clear),
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            )
                        }

                        // Date chip
                        filter.hasSlotOnDate?.let { date ->
                            FilterChip(
                                selected = true,
                                onClick = { viewModel.removeDateFilter() },
                                label = { Text(text = date) },
                                trailingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = stringResource(id = R.string.common_clear),
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            )
                        }

                        // Sort chip
                        if (filter.sortOption != ActivitySortOption.TITLE) {
                            val sortLabelRes = when (filter.sortOption) {
                                ActivitySortOption.PRICE_LOW_TO_HIGH -> R.string.explore_sort_price_low_high
                                ActivitySortOption.PRICE_HIGH_TO_LOW -> R.string.explore_sort_price_high_low
                                ActivitySortOption.DIFFICULTY -> R.string.explore_sort_difficulty
                                ActivitySortOption.TITLE -> R.string.explore_sort_name_az
                            }
                            FilterChip(
                                selected = true,
                                onClick = { viewModel.resetSortOption() },
                                label = { Text(text = stringResource(id = sortLabelRes)) },
                                trailingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = stringResource(id = R.string.common_clear),
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            )
                        }
                    }
                }

                // Grid Content with states
                val isPagingRefreshing = pagedActivities.loadState.refresh is LoadState.Loading
                val isPagingError = pagedActivities.loadState.refresh is LoadState.Error

                if (isFirstLoading || (isPagingRefreshing && pagedActivities.itemCount == 0)) {
                    // 6 Shimmer Skeleton Cards Grid
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        contentPadding = PaddingValues(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(6) {
                            ShimmerBox(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(1f),
                                shape = RoundedCornerShape(16.dp)
                            )
                        }
                    }
                } else if (isPagingError && pagedActivities.itemCount == 0 && !hasCachedActivities) {
                    // Error state when no cached data exists
                    ErrorState(
                        message = stringResource(id = R.string.error_default_message),
                        onRetry = {
                            pagedActivities.retry()
                            viewModel.refreshActivities(isManual = true)
                        }
                    )
                } else if (!isPagingRefreshing && pagedActivities.itemCount == 0) {
                    // Empty state when non-matching search / filters
                    EmptyState(
                        icon = Icons.Outlined.SearchOff,
                        title = stringResource(id = R.string.explore_empty_title),
                        message = stringResource(id = R.string.explore_empty_subtitle),
                        actionLabel = stringResource(id = R.string.explore_action_clear_filters),
                        onAction = { viewModel.clearAllFilters() }
                    )
                } else {
                    // Results Grid with 2 columns, 12dp spacing, 16dp padding
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        contentPadding = PaddingValues(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        // Horizontal "Recently viewed" row above grid when not searching and entries exist
                        if (searchQuery.isBlank() && recentlyViewed.isNotEmpty()) {
                            item(span = { GridItemSpan(2) }) {
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    Text(
                                        text = stringResource(id = R.string.explore_recently_viewed),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        modifier = Modifier.padding(bottom = 8.dp)
                                    )
                                    LazyRow(
                                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        items(
                                            items = recentlyViewed,
                                            key = { "recent_${it.id}" }
                                        ) { activity ->
                                            RecentlyViewedCard(
                                                activity = activity,
                                                onCardClick = { activityId ->
                                                    viewModel.onActivityClicked(activityId)
                                                    onNavigateToActivityDetail(activityId)
                                                }
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(12.dp))
                                }
                            }
                        }

                        // Grid items
                        items(
                            count = pagedActivities.itemCount,
                            key = pagedActivities.itemKey { it.id }
                        ) { index ->
                            val activity = pagedActivities[index]
                            if (activity != null) {
                                ActivityCard(
                                    activity = activity,
                                    isFavourite = favourites.contains(activity.id),
                                    onCardClick = { activityId ->
                                        viewModel.onActivityClicked(activityId)
                                        onNavigateToActivityDetail(activityId)
                                    },
                                    onFavouriteToggle = { activityId ->
                                        viewModel.toggleFavourite(activityId)
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showFilterSheet) {
        FilterBottomSheet(
            currentFilter = filter,
            distinctLocations = distinctLocations,
            matchingCount = filterSheetCount,
            onDraftFilterChanged = { draft ->
                scope.launch {
                    filterSheetCount = viewModel.getMatchingCount(draft)
                }
            },
            onApply = { newFilter ->
                viewModel.applyFilter(newFilter)
                showFilterSheet = false
            },
            onDismiss = { showFilterSheet = false }
        )
    }
}
