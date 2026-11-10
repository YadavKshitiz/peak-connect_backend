package com.peakconnect.app.feature.trekker.booking

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Cancel
import androidx.compose.material.icons.outlined.Event
import androidx.compose.material.icons.outlined.Hiking
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.HourglassEmpty
import androidx.compose.material3.Badge
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.peakconnect.app.R
import com.peakconnect.app.core.theme.AppShapes
import com.peakconnect.app.core.theme.spacing
import com.peakconnect.app.core.ui.AppPullToRefreshBox
import com.peakconnect.app.core.ui.AppTopBar
import com.peakconnect.app.core.ui.EmptyState
import com.peakconnect.app.core.ui.ShimmerBox
import com.peakconnect.app.data.local.entity.WaitlistEntryEntity
import com.peakconnect.app.feature.trekker.waitlist.PromotedWaitlistBanner
import com.peakconnect.app.feature.trekker.waitlist.RemoveWaitlistDialog
import com.peakconnect.app.feature.trekker.waitlist.WaitlistCard
import com.peakconnect.app.feature.trekker.waitlist.WaitlistTrackingNote

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyBookingsScreen(
    onNavigateToBookingStatus: (String) -> Unit,
    onNavigateToPayment: (String) -> Unit,
    modifier: Modifier = Modifier,
    onNavigateToExplore: () -> Unit = {},
    viewModel: BookingListViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val lastUpdated by viewModel.lastUpdated.collectAsStateWithLifecycle()
    var entryToRemove by remember { mutableStateOf<WaitlistEntryEntity?>(null) }

    LifecycleResumeEffect(Unit) {
        viewModel.refresh()
        onPauseOrDispose {}
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            AppTopBar(
                title = stringResource(id = R.string.nav_tab_bookings)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Dismissible Promotion Banner at top of My Bookings
            uiState.promotedBanner?.let { banner ->
                PromotedWaitlistBanner(
                    guideName = banner.guideName,
                    onViewBooking = { onNavigateToBookingStatus(banner.bookingId) },
                    onDismiss = { viewModel.dismissPromotedBanner(banner.bookingId) },
                    modifier = Modifier.padding(horizontal = MaterialTheme.spacing.md, vertical = MaterialTheme.spacing.xs)
                )
            }

            // Scrollable Tab Row
            PrimaryScrollableTabRow(
                selectedTabIndex = uiState.selectedTab.ordinal,
                edgePadding = MaterialTheme.spacing.md,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.primary,
                divider = {}
            ) {
                BookingTab.entries.forEach { tab ->
                    val isSelected = uiState.selectedTab == tab
                    Tab(
                        selected = isSelected,
                        onClick = { viewModel.selectTab(tab) },
                        text = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = stringResource(id = tab.labelRes),
                                    style = MaterialTheme.typography.titleSmall
                                )
                                if (tab == BookingTab.ACTIVE && uiState.activeCount > 0) {
                                    Spacer(modifier = Modifier.width(MaterialTheme.spacing.xs))
                                    Badge(
                                        containerColor = MaterialTheme.colorScheme.primary,
                                        contentColor = MaterialTheme.colorScheme.onPrimary
                                    ) {
                                        Text(text = uiState.activeCount.toString())
                                    }
                                }
                            }
                        }
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = MaterialTheme.spacing.md, vertical = MaterialTheme.spacing.xs),
                horizontalArrangement = Arrangement.End
            ) {
                com.peakconnect.app.core.ui.LastUpdatedIndicator(lastUpdatedMillis = lastUpdated)
            }

            // Pull-to-refresh container
            AppPullToRefreshBox(
                isRefreshing = uiState.isRefreshing,
                onRefresh = { viewModel.refresh() },
                modifier = Modifier.fillMaxSize()
            ) {
                when {
                    uiState.isLoading && uiState.currentList.isEmpty() && uiState.waitlistEntries.isEmpty() -> {
                        // Skeleton loading
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(MaterialTheme.spacing.md),
                            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.md)
                        ) {
                            repeat(3) {
                                ShimmerBox(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(130.dp),
                                    shape = AppShapes.medium
                                )
                            }
                        }
                    }

                    uiState.selectedTab == BookingTab.WAITLIST -> {
                        if (uiState.waitlistEntries.isEmpty()) {
                            EmptyState(
                                icon = Icons.Outlined.HourglassEmpty,
                                title = stringResource(id = R.string.bookings_empty_waitlist_title),
                                message = stringResource(id = R.string.bookings_empty_waitlist_message)
                            )
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(MaterialTheme.spacing.md),
                                verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.md)
                            ) {
                                item(key = "waitlist_tracking_note") {
                                    WaitlistTrackingNote(modifier = Modifier.fillMaxWidth())
                                }
                                items(
                                    items = uiState.waitlistEntries,
                                    key = { it.slotId }
                                ) { entry ->
                                    WaitlistCard(
                                        entry = entry,
                                        onRemoveClick = { entryToRemove = it },
                                        onViewBookingClick = onNavigateToBookingStatus
                                    )
                                }
                            }
                        }
                    }

                    uiState.currentList.isEmpty() -> {
                        when (uiState.selectedTab) {
                            BookingTab.ACTIVE -> {
                                EmptyState(
                                    icon = Icons.Outlined.Hiking,
                                    title = stringResource(id = R.string.bookings_empty_active_title),
                                    message = stringResource(id = R.string.bookings_empty_active_message)
                                )
                            }
                            BookingTab.UPCOMING -> {
                                EmptyState(
                                    icon = Icons.Outlined.Event,
                                    title = stringResource(id = R.string.bookings_empty_upcoming_title),
                                    message = stringResource(id = R.string.bookings_empty_upcoming_message),
                                    actionLabel = stringResource(id = R.string.bookings_empty_explore_action),
                                    onAction = onNavigateToExplore
                                )
                            }
                            BookingTab.PAST -> {
                                EmptyState(
                                    icon = Icons.Outlined.History,
                                    title = stringResource(id = R.string.bookings_empty_past_title),
                                    message = stringResource(id = R.string.bookings_empty_past_message)
                                )
                            }
                            BookingTab.CANCELLED -> {
                                EmptyState(
                                    icon = Icons.Outlined.Cancel,
                                    title = stringResource(id = R.string.bookings_empty_cancelled_title),
                                    message = stringResource(id = R.string.bookings_empty_cancelled_message)
                                )
                            }
                            BookingTab.WAITLIST -> Unit
                        }
                    }

                    else -> {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(MaterialTheme.spacing.md),
                            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.md)
                        ) {
                            items(
                                items = uiState.currentList,
                                key = { it.booking.id }
                            ) { item ->
                                BookingCard(
                                    item = item,
                                    onCardClick = onNavigateToBookingStatus,
                                    onPayClick = onNavigateToPayment
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    RemoveWaitlistDialog(
        entry = entryToRemove,
        onConfirm = { entry ->
            viewModel.removeFromWaitlist(entry.slotId)
            entryToRemove = null
        },
        onDismiss = { entryToRemove = null }
    )
}

