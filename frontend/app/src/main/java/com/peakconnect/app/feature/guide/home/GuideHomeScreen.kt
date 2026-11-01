package com.peakconnect.app.feature.guide.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.peakconnect.app.core.theme.semanticColors
import com.peakconnect.app.core.theme.spacing
import com.peakconnect.app.core.ui.AppPullToRefreshBox
import com.peakconnect.app.core.ui.AppTopBar
import com.peakconnect.app.core.ui.BannerType
import com.peakconnect.app.core.ui.InfoBanner
import com.peakconnect.app.core.ui.PrimaryButton
import com.peakconnect.app.core.ui.ShimmerBox
import com.peakconnect.app.core.ui.StatusChip
import com.peakconnect.app.data.local.entity.GuideBookingEntity
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GuideHomeScreen(
    onNavigateToBookingDetail: (String) -> Unit,
    onNavigateToProfileEdit: () -> Unit,
    onNotificationClick: () -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: GuideHomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LifecycleResumeEffect(Unit) {
        viewModel.refresh()
        onPauseOrDispose {}
    }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearMessages()
        }
    }

    LaunchedEffect(uiState.successMessage) {
        uiState.successMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearMessages()
        }
    }

    if (uiState.declineDialogBookingId != null) {
        val bookingId = uiState.declineDialogBookingId!!
        AlertDialog(
            onDismissRequest = { viewModel.showDeclineDialog(null) },
            title = {
                Text(
                    text = stringResource(id = R.string.guide_detail_decline_dialog_title),
                    style = MaterialTheme.typography.titleLarge
                )
            },
            text = {
                Text(
                    text = stringResource(id = R.string.guide_detail_decline_dialog_message),
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                TextButton(
                    onClick = { viewModel.declineBooking(bookingId) },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text(stringResource(id = R.string.guide_detail_decline_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.showDeclineDialog(null) }) {
                    Text(stringResource(id = R.string.guide_detail_decline_dismiss))
                }
            }
        )
    }

    Scaffold(
        topBar = {
            AppTopBar(
                title = stringResource(id = R.string.guide_home_title),
                actions = {
                    IconButton(onClick = onNotificationClick) {
                        Icon(
                            imageVector = Icons.Outlined.Notifications,
                            contentDescription = stringResource(id = R.string.profile_menu_notifications)
                        )
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        modifier = modifier
    ) { innerPadding ->
        AppPullToRefreshBox(
            isRefreshing = uiState.isRefreshing,
            onRefresh = { viewModel.refresh() },
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (uiState.isLoading) {
                GuideHomeSkeleton(modifier = Modifier.fillMaxSize())
            } else {
                val scrollState = rememberScrollState()

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(scrollState)
                        .padding(MaterialTheme.spacing.md),
                    verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.md)
                ) {
                    // 1. Verification Section
                    if (uiState.isVerified) {
                        val verifiedColor = MaterialTheme.semanticColors.bookingStatusColor(com.peakconnect.app.domain.model.BookingStatus.CONFIRMED)
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = verifiedColor.container,
                            contentColor = verifiedColor.content
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = verifiedColor.content
                                )
                                Text(
                                    text = stringResource(id = R.string.guide_home_verified_badge),
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = verifiedColor.content
                                )
                            }
                        }
                    } else {
                        InfoBanner(
                            text = stringResource(id = R.string.guide_home_awaiting_approval),
                            type = BannerType.WARNING
                        )

                        if (uiState.isProfileIncomplete) {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                                ),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(MaterialTheme.spacing.md),
                                    verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.xs)
                                ) {
                                    Text(
                                        text = stringResource(id = R.string.guide_home_complete_profile_title),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = stringResource(id = R.string.guide_home_complete_profile_desc),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.height(MaterialTheme.spacing.xs))
                                    PrimaryButton(
                                        text = stringResource(id = R.string.guide_home_action_complete_profile),
                                        onClick = onNavigateToProfileEdit,
                                        modifier = Modifier.align(Alignment.End)
                                    )
                                }
                            }
                        }
                    }

                    // 2. Stats Row
                    GuideStatsRow(
                        pendingCount = uiState.pendingCount,
                        upcomingCount = uiState.upcomingCount,
                        completedCount = uiState.completedCount
                    )

                    // 3. Pending Requests Section
                    Column(verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.sm)) {
                        Text(
                            text = stringResource(id = R.string.guide_home_section_pending),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )

                        if (uiState.pendingBookings.isEmpty()) {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                                ),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(MaterialTheme.spacing.lg),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = stringResource(id = R.string.guide_home_empty_pending),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        } else {
                            uiState.pendingBookings.forEach { booking ->
                                GuideRequestCard(
                                    booking = booking,
                                    onCardClick = onNavigateToBookingDetail,
                                    onAccept = viewModel::acceptBooking,
                                    onDecline = viewModel::showDeclineDialog,
                                    isActing = uiState.isActingBookingId == booking.id
                                )
                            }
                        }
                    }

                    // 4. Upcoming Trips Section
                    Column(verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.sm)) {
                        Text(
                            text = stringResource(id = R.string.guide_home_section_upcoming),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )

                        if (uiState.upcomingBookings.isEmpty()) {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                                ),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(MaterialTheme.spacing.lg),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = stringResource(id = R.string.guide_home_empty_upcoming),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        } else {
                            uiState.upcomingBookings.forEach { booking ->
                                GuideUpcomingTripCard(
                                    booking = booking,
                                    onCardClick = onNavigateToBookingDetail
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(MaterialTheme.spacing.xl))
                }
            }
        }
    }
}

@Composable
private fun GuideStatsRow(
    pendingCount: Int,
    upcomingCount: Int,
    completedCount: Int,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.sm)
    ) {
        StatItem(
            count = pendingCount,
            label = stringResource(id = R.string.guide_home_stats_pending),
            modifier = Modifier.weight(1f)
        )
        StatItem(
            count = upcomingCount,
            label = stringResource(id = R.string.guide_home_stats_upcoming),
            modifier = Modifier.weight(1f)
        )
        StatItem(
            count = completedCount,
            label = stringResource(id = R.string.guide_home_stats_completed),
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun StatItem(
    count: Int,
    label: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        shape = RoundedCornerShape(10.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = MaterialTheme.spacing.sm, horizontal = MaterialTheme.spacing.xs),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = count.toString(),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun GuideUpcomingTripCard(
    booking: GuideBookingEntity,
    onCardClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val dateTimeFormatter = remember {
        DateTimeFormatter.ofPattern("EEE, d MMM yyyy · h:mm a", Locale.ENGLISH)
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onCardClick(booking.id) },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        shape = RoundedCornerShape(10.dp)
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
                    fontWeight = FontWeight.SemiBold,
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
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = stringResource(id = R.string.guide_trekker_label, booking.trekkerName),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
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
private fun GuideHomeSkeleton(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .padding(MaterialTheme.spacing.md),
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.md)
    ) {
        ShimmerBox(
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp)
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.sm)
        ) {
            ShimmerBox(
                modifier = Modifier
                    .weight(1f)
                    .height(70.dp)
            )
            ShimmerBox(
                modifier = Modifier
                    .weight(1f)
                    .height(70.dp)
            )
            ShimmerBox(
                modifier = Modifier
                    .weight(1f)
                    .height(70.dp)
            )
        }
        ShimmerBox(
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp)
        )
        ShimmerBox(
            modifier = Modifier
                .fillMaxWidth()
                .height(100.dp)
        )
    }
}
