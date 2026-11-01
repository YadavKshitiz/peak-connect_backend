package com.peakconnect.app.feature.guide.bookings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.Hiking
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.peakconnect.app.R
import com.peakconnect.app.core.theme.spacing
import com.peakconnect.app.core.ui.AppPullToRefreshBox
import com.peakconnect.app.core.ui.AppTopBar
import com.peakconnect.app.core.ui.EmptyState
import com.peakconnect.app.core.ui.ShimmerBox
import com.peakconnect.app.core.ui.StatusChip
import com.peakconnect.app.data.local.entity.GuideBookingEntity
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GuideBookingsScreen(
    onNavigateToBookingDetail: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: GuideBookingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LifecycleResumeEffect(Unit) {
        viewModel.refresh()
        onPauseOrDispose {}
    }

    Scaffold(
        topBar = {
            AppTopBar(
                title = stringResource(id = R.string.guide_bookings_title)
            )
        },
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Filter Chips Row
            val chipScrollState = rememberScrollState()
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(chipScrollState)
                    .padding(horizontal = MaterialTheme.spacing.md, vertical = MaterialTheme.spacing.xs),
                horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.xs)
            ) {
                GuideFilterChip(
                    label = stringResource(id = R.string.guide_filter_all),
                    selected = uiState.selectedFilter == GuideBookingFilter.ALL,
                    onClick = { viewModel.setFilter(GuideBookingFilter.ALL) }
                )
                GuideFilterChip(
                    label = stringResource(id = R.string.guide_filter_pending),
                    selected = uiState.selectedFilter == GuideBookingFilter.PENDING,
                    onClick = { viewModel.setFilter(GuideBookingFilter.PENDING) }
                )
                GuideFilterChip(
                    label = stringResource(id = R.string.guide_filter_awaiting_payment),
                    selected = uiState.selectedFilter == GuideBookingFilter.AWAITING_PAYMENT,
                    onClick = { viewModel.setFilter(GuideBookingFilter.AWAITING_PAYMENT) }
                )
                GuideFilterChip(
                    label = stringResource(id = R.string.guide_filter_confirmed),
                    selected = uiState.selectedFilter == GuideBookingFilter.CONFIRMED,
                    onClick = { viewModel.setFilter(GuideBookingFilter.CONFIRMED) }
                )
                GuideFilterChip(
                    label = stringResource(id = R.string.guide_filter_past),
                    selected = uiState.selectedFilter == GuideBookingFilter.PAST,
                    onClick = { viewModel.setFilter(GuideBookingFilter.PAST) }
                )
                GuideFilterChip(
                    label = stringResource(id = R.string.guide_filter_cancelled),
                    selected = uiState.selectedFilter == GuideBookingFilter.CANCELLED,
                    onClick = { viewModel.setFilter(GuideBookingFilter.CANCELLED) }
                )
            }

            AppPullToRefreshBox(
                isRefreshing = uiState.isRefreshing,
                onRefresh = { viewModel.refresh() },
                modifier = Modifier.fillMaxSize()
            ) {
                if (uiState.isLoading && uiState.bookings.isEmpty()) {
                    GuideBookingsSkeleton(modifier = Modifier.fillMaxSize())
                } else if (uiState.filteredBookings.isEmpty()) {
                    EmptyState(
                        icon = Icons.Outlined.Hiking,
                        title = stringResource(id = R.string.guide_bookings_empty_title),
                        message = stringResource(id = R.string.guide_bookings_empty_message)
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(MaterialTheme.spacing.md),
                        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.sm)
                    ) {
                        items(
                            items = uiState.filteredBookings,
                            key = { it.id }
                        ) { booking ->
                            GuideBookingCard(
                                booking = booking,
                                onClick = { onNavigateToBookingDetail(booking.id) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun GuideFilterChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(text = label) }
    )
}

@Composable
private fun GuideBookingCard(
    booking: GuideBookingEntity,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dateTimeFormatter = remember {
        DateTimeFormatter.ofPattern("EEE, d MMM yyyy · h:mm a", Locale.ENGLISH)
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(MaterialTheme.spacing.md),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.xs)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = booking.activityTitle,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(MaterialTheme.spacing.sm))
                StatusChip(bookingStatus = booking.status)
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Person,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = stringResource(id = R.string.guide_trekker_label, booking.trekkerName),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.CalendarToday,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = booking.date.format(dateTimeFormatter),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun GuideBookingsSkeleton(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(MaterialTheme.spacing.md),
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.sm)
    ) {
        repeat(4) {
            ShimmerBox(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(90.dp)
            )
        }
    }
}
