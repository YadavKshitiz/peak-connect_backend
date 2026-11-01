package com.peakconnect.app.feature.guide.detail

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
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.peakconnect.app.R
import com.peakconnect.app.core.images.ImageUrlResolver
import com.peakconnect.app.core.theme.spacing
import com.peakconnect.app.core.ui.AppAsyncImage
import com.peakconnect.app.core.ui.AppTopBar
import com.peakconnect.app.core.ui.BannerType
import com.peakconnect.app.core.ui.InfoBanner
import com.peakconnect.app.core.ui.OnlineOnlyButton
import com.peakconnect.app.core.ui.OnlineOnlyButtonStyle
import com.peakconnect.app.core.ui.StatusChip
import com.peakconnect.app.domain.model.BookingStatus
import com.peakconnect.app.feature.trekker.booking.BookingStepper
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GuideBookingDetailScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: GuideBookingDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

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

    if (uiState.showDeclineDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.showDeclineDialog(false) },
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
                    onClick = { viewModel.declineBooking() },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text(stringResource(id = R.string.guide_detail_decline_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.showDeclineDialog(false) }) {
                    Text(stringResource(id = R.string.guide_detail_decline_dismiss))
                }
            }
        )
    }

    if (uiState.showNoShowDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.showNoShowDialog(false) },
            title = {
                Text(
                    text = stringResource(id = R.string.guide_detail_no_show_dialog_title),
                    style = MaterialTheme.typography.titleLarge
                )
            },
            text = {
                Text(
                    text = stringResource(id = R.string.guide_detail_no_show_dialog_message),
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                TextButton(
                    onClick = { viewModel.markNoShow() },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text(stringResource(id = R.string.guide_detail_no_show_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.showNoShowDialog(false) }) {
                    Text(stringResource(id = R.string.guide_detail_no_show_dismiss))
                }
            }
        )
    }

    Scaffold(
        topBar = {
            AppTopBar(
                title = stringResource(id = R.string.guide_detail_title),
                onBackClick = onBack
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        modifier = modifier
    ) { innerPadding ->
        if (uiState.isLoading && uiState.booking == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            val booking = uiState.booking
            if (booking == null) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(id = R.string.guide_bookings_empty_title),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                val scrollState = rememberScrollState()
                val dateTimeFormatter = remember {
                    DateTimeFormatter.ofPattern("EEE, d MMM yyyy · h:mm a", Locale.ENGLISH)
                }
                val resolvedImageUrl = remember(booking.activityTitle, uiState.activityLocation) {
                    ImageUrlResolver.resolve(booking.activityTitle, uiState.activityLocation ?: "")
                }
                val isDatePassed = remember(booking.date) {
                    booking.date.isBefore(viewModel.nowSupplier())
                }

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .verticalScroll(scrollState)
                        .padding(MaterialTheme.spacing.md),
                    verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.md)
                ) {
                    // Header card with activity image, title, location, status
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column {
                            AppAsyncImage(
                                url = resolvedImageUrl,
                                contentDescription = booking.activityTitle,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(180.dp)
                                    .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp))
                            )

                            Column(
                                modifier = Modifier.padding(MaterialTheme.spacing.md),
                                verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.xs)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = booking.activityTitle,
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Spacer(modifier = Modifier.width(MaterialTheme.spacing.sm))
                                    StatusChip(bookingStatus = booking.status)
                                }

                                if (!uiState.activityLocation.isNullOrBlank()) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.LocationOn,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Text(
                                            text = uiState.activityLocation.orEmpty(),
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
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
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }

                    // Trekker Details Card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(MaterialTheme.spacing.md),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.sm)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Person,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                            Column {
                                Text(
                                    text = stringResource(id = R.string.guide_detail_section_trekker),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = booking.trekkerName,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }

                    // Status Stepper Card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(MaterialTheme.spacing.md)) {
                            Text(
                                text = stringResource(id = R.string.booking_status_title),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(bottom = MaterialTheme.spacing.sm)
                            )
                            BookingStepper(
                                status = booking.status,
                                hasGuide = true
                            )
                        }
                    }

                    // Actions by Status
                    when (booking.status) {
                        BookingStatus.PENDING_GUIDE_RESPONSE -> {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.sm)
                            ) {
                                OnlineOnlyButton(
                                    text = stringResource(id = R.string.guide_action_accept),
                                    actionLabel = stringResource(id = R.string.guide_action_accept_label),
                                    onClick = { viewModel.acceptBooking() },
                                    enabled = !uiState.isActing,
                                    style = OnlineOnlyButtonStyle.PRIMARY,
                                    modifier = Modifier.fillMaxWidth()
                                )

                                OnlineOnlyButton(
                                    text = stringResource(id = R.string.guide_action_decline),
                                    actionLabel = stringResource(id = R.string.guide_action_decline_label),
                                    onClick = { viewModel.showDeclineDialog(true) },
                                    enabled = !uiState.isActing,
                                    style = OnlineOnlyButtonStyle.DESTRUCTIVE,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }

                        BookingStatus.AWAITING_PAYMENT -> {
                            InfoBanner(
                                text = stringResource(id = R.string.guide_detail_waiting_payment_desc),
                                type = BannerType.INFO
                            )
                        }

                        BookingStatus.CONFIRMED -> {
                            if (isDatePassed) {
                                OnlineOnlyButton(
                                    text = stringResource(id = R.string.guide_detail_action_no_show),
                                    actionLabel = stringResource(id = R.string.guide_detail_action_no_show_label),
                                    onClick = { viewModel.showNoShowDialog(true) },
                                    enabled = !uiState.isActing,
                                    style = OnlineOnlyButtonStyle.DESTRUCTIVE,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }

                        else -> {
                            // Read-only information for cancelled, past, etc.
                        }
                    }

                    Spacer(modifier = Modifier.height(MaterialTheme.spacing.lg))
                }
            }
        }
    }
}
