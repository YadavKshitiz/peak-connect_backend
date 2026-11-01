package com.peakconnect.app.feature.trekker.booking

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Directions
import androidx.compose.material.icons.outlined.Event
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.peakconnect.app.R
import com.peakconnect.app.core.theme.AppShapes
import com.peakconnect.app.core.theme.semanticColors
import com.peakconnect.app.core.theme.spacing
import com.peakconnect.app.core.ui.AppTopBar
import com.peakconnect.app.core.ui.CancellationPolicySummaryCard
import com.peakconnect.app.core.ui.OnlineOnlyButton
import com.peakconnect.app.core.ui.OnlineOnlyButtonStyle
import com.peakconnect.app.core.ui.PrimaryButton
import com.peakconnect.app.core.ui.SecondaryButton
import com.peakconnect.app.core.ui.StatusChip
import com.peakconnect.app.core.ui.formatRupees
import com.peakconnect.app.data.local.entity.ActivityEntity
import com.peakconnect.app.data.local.entity.BookingEntity
import com.peakconnect.app.data.local.entity.SlotEntity
import com.peakconnect.app.domain.model.BookingStatus
import com.peakconnect.app.domain.model.CancellationPolicyType
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookingStatusScreen(
    onBack: () -> Unit,
    onNavigateToPayment: (String) -> Unit,
    onNavigateToMatches: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: BookingStatusViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val haptic = LocalHapticFeedback.current
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // Listen for status changes for light haptic feedback
    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is BookingStatusEvent.StatusChanged -> {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                }
            }
        }
    }

    // Lifecycle-aware polling: only active when STARTED/RESUMED and status is pending or awaiting payment
    LifecycleResumeEffect(uiState.booking?.status) {
        val status = uiState.booking?.status
        if (status == BookingStatus.PENDING_GUIDE_RESPONSE || status == BookingStatus.AWAITING_PAYMENT) {
            viewModel.startPolling()
        }
        onPauseOrDispose {
            viewModel.stopPolling()
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            AppTopBar(
                title = stringResource(id = R.string.booking_status_title),
                onBackClick = onBack
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { innerPadding ->
        if (uiState.isLoading && uiState.booking == null) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            val booking = uiState.booking
            if (booking != null) {
                val hasGuide = !booking.guideName.isNullOrBlank()
                val displayGuide = if (hasGuide) {
                    booking.guideName!!
                } else {
                    stringResource(id = R.string.guide_assigned_placeholder)
                }

                val dateTimeFormatter = remember {
                    DateTimeFormatter.ofPattern("EEE, d MMM yyyy · h:mm a", Locale.ENGLISH)
                }

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .verticalScroll(rememberScrollState())
                        .padding(MaterialTheme.spacing.md),
                    verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.lg)
                ) {
                    // Top Section: Title, StatusChip, Date/Time, Guide
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = AppShapes.medium,
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
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
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.weight(1f)
                                )
                                Spacer(modifier = Modifier.width(MaterialTheme.spacing.sm))
                                StatusChip(bookingStatus = booking.status)
                            }

                            Spacer(modifier = Modifier.height(MaterialTheme.spacing.xs))

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Outlined.CalendarToday,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(MaterialTheme.spacing.xs))
                                Text(
                                    text = booking.date.format(dateTimeFormatter),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Outlined.Person,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(MaterialTheme.spacing.xs))
                                Text(
                                    text = displayGuide,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    // Stepper Timeline
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = AppShapes.medium,
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(MaterialTheme.spacing.md)
                        ) {
                            BookingStepper(
                                status = booking.status,
                                hasGuide = hasGuide,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }

                    // Animated Status Panel
                    AnimatedContent(
                        targetState = booking.status,
                        transitionSpec = { fadeIn() togetherWith fadeOut() },
                        label = "status_panel"
                    ) { targetStatus ->
                        StatusActionPanel(
                            status = targetStatus,
                            booking = booking,
                            slot = uiState.slot,
                            activity = uiState.activity,
                            hasGuide = hasGuide,
                            guideName = displayGuide,
                            onRefresh = { viewModel.refresh() },
                            onPayNow = { onNavigateToPayment(booking.id) },
                            onCancelBooking = { viewModel.showCancelSheet(true) },
                            onChooseGuide = { onNavigateToMatches(booking.slotId) },
                            onGetDirections = {
                                val lat = uiState.activity?.latitude
                                val lng = uiState.activity?.longitude
                                if (lat != null && lng != null) {
                                    val geoUri = Uri.parse("geo:$lat,$lng?q=${Uri.encode(booking.activityTitle)}")
                                    val mapIntent = Intent(Intent.ACTION_VIEW, geoUri)
                                    try {
                                        context.startActivity(mapIntent)
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "No map application available", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            },
                            onAddToCalendar = {
                                val toastMsg = context.getString(R.string.booking_status_action_calendar_toast)
                                scope.launch {
                                    snackbarHostState.showSnackbar(toastMsg)
                                }
                            }
                        )
                    }
                }

                // Cancellation Bottom Sheet
                if (uiState.showCancelSheet) {
                    BookingCancellationBottomSheet(
                        booking = booking,
                        slot = uiState.slot,
                        policy = uiState.activity?.cancellationPolicy ?: CancellationPolicyType.FLEXIBLE,
                        isCancelling = uiState.isCancelling,
                        cancellationResult = uiState.cancellationResult,
                        cancellationError = uiState.cancellationError,
                        showConfirmDialog = uiState.showCancelDialog,
                        onDismissRequest = { viewModel.showCancelSheet(false) },
                        onRequestCancel = { viewModel.showCancelConfirmDialog(true) },
                        onConfirmCancel = { viewModel.cancelBooking() },
                        onDismissConfirmDialog = { viewModel.showCancelConfirmDialog(false) },
                        onDone = {
                            viewModel.clearCancellationResult()
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun StatusActionPanel(
    status: BookingStatus,
    booking: BookingEntity,
    slot: SlotEntity?,
    activity: ActivityEntity?,
    hasGuide: Boolean,
    guideName: String,
    onRefresh: () -> Unit,
    onPayNow: () -> Unit,
    onCancelBooking: () -> Unit,
    onChooseGuide: () -> Unit,
    onGetDirections: () -> Unit,
    onAddToCalendar: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = AppShapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(MaterialTheme.spacing.md),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.md)
        ) {
            when (status) {
                BookingStatus.PENDING_GUIDE_RESPONSE -> {
                    val countdownState by rememberBookingCountdown(
                        statusSinceMillis = booking.statusSinceMillis,
                        windowDurationMillis = BookingCountdownCalculator.GUIDE_RESPONSE_WINDOW_MILLIS
                    )

                    if (countdownState.isExpired) {
                        Text(
                            text = stringResource(id = R.string.booking_status_window_ended),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                        SecondaryButton(
                            text = stringResource(id = R.string.booking_status_action_refresh),
                            onClick = onRefresh,
                            leadingIcon = Icons.Outlined.Refresh
                        )
                    } else {
                        val titleText = if (hasGuide) {
                            stringResource(
                                id = R.string.booking_status_pending_with_guide,
                                guideName,
                                countdownState.formattedTime
                            )
                        } else {
                            stringResource(
                                id = R.string.booking_status_pending_no_guide,
                                countdownState.formattedTime
                            )
                        }
                        Text(
                            text = titleText,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = stringResource(id = R.string.booking_status_pending_note),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.height(MaterialTheme.spacing.xs))

                    OnlineOnlyButton(
                        text = stringResource(id = R.string.booking_status_action_cancel_request),
                        actionLabel = stringResource(id = R.string.booking_card_cancel_action_label),
                        onClick = onCancelBooking,
                        style = OnlineOnlyButtonStyle.SECONDARY
                    )
                }

                BookingStatus.AWAITING_PAYMENT -> {
                    val depositAmount = booking.depositAmount?.let { BigDecimal.valueOf(it) }
                        ?: BigDecimal.ZERO
                    val formattedDeposit = formatRupees(depositAmount)

                    val countdownState by rememberBookingCountdown(
                        statusSinceMillis = booking.statusSinceMillis,
                        windowDurationMillis = BookingCountdownCalculator.PAYMENT_WINDOW_MILLIS
                    )

                    if (countdownState.isExpired) {
                        Text(
                            text = stringResource(id = R.string.booking_status_window_ended),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                        SecondaryButton(
                            text = stringResource(id = R.string.booking_status_action_refresh),
                            onClick = onRefresh,
                            leadingIcon = Icons.Outlined.Refresh
                        )
                    } else {
                        Text(
                            text = stringResource(
                                id = R.string.booking_status_awaiting_payment_title,
                                formattedDeposit
                            ) + " (" + stringResource(
                                id = R.string.booking_card_approx_left,
                                countdownState.formattedTime
                            ) + ")",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Spacer(modifier = Modifier.height(MaterialTheme.spacing.xs))

                    OnlineOnlyButton(
                        text = stringResource(
                            id = R.string.booking_status_action_pay_now,
                            formattedDeposit
                        ),
                        actionLabel = stringResource(id = R.string.booking_card_pay_action_label),
                        onClick = onPayNow,
                        style = OnlineOnlyButtonStyle.PRIMARY
                    )

                    SecondaryButton(
                        text = stringResource(id = R.string.booking_status_action_cancel_booking),
                        onClick = onCancelBooking
                    )
                }

                BookingStatus.CONFIRMED -> {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Outlined.CheckCircle,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(MaterialTheme.spacing.sm))
                        Text(
                            text = stringResource(id = R.string.booking_status_confirmed_title),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    booking.depositAmount?.let {
                        val formattedDeposit = formatRupees(BigDecimal.valueOf(it))
                        Text(
                            text = stringResource(
                                id = R.string.booking_status_confirmed_deposit_paid,
                                formattedDeposit
                            ),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.height(MaterialTheme.spacing.xs))

                    // Actions
                    SecondaryButton(
                        text = stringResource(id = R.string.booking_status_action_calendar),
                        onClick = onAddToCalendar,
                        leadingIcon = Icons.Outlined.Event
                    )

                    if (activity?.latitude != null && activity.longitude != null) {
                        SecondaryButton(
                            text = stringResource(id = R.string.booking_status_action_directions),
                            onClick = onGetDirections,
                            leadingIcon = Icons.Outlined.Directions
                        )
                    }

                    // Cancellation policy card reminder
                    val policy = activity?.cancellationPolicy ?: CancellationPolicyType.FLEXIBLE
                    CancellationPolicySummaryCard(cancellationPolicy = policy)

                    // Cancel booking (outlined destructive)
                    OnlineOnlyButton(
                        text = stringResource(id = R.string.booking_status_action_cancel_booking),
                        actionLabel = stringResource(id = R.string.booking_card_cancel_action_label),
                        onClick = onCancelBooking,
                        style = OnlineOnlyButtonStyle.DESTRUCTIVE
                    )
                }

                BookingStatus.CANCELLED, BookingStatus.NO_SHOW -> {
                    val summaryText = if (status == BookingStatus.CANCELLED) {
                        stringResource(id = R.string.booking_status_terminal_cancelled_summary)
                    } else {
                        stringResource(id = R.string.booking_status_terminal_no_show_summary)
                    }

                    Text(
                        text = summaryText,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    val refundAmount = booking.refundAmount
                    val refundPct = booking.refundPercentage?.let { (it * 100).toInt() }

                    if (refundAmount != null && refundAmount > 0.0) {
                        val formatted = formatRupees(BigDecimal.valueOf(refundAmount))
                        Text(
                            text = stringResource(
                                id = R.string.booking_status_terminal_refund_details,
                                formatted,
                                (refundPct ?: 0).toString()
                            ),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    } else {
                        Text(
                            text = stringResource(id = R.string.booking_status_terminal_deposit_non_refundable),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                BookingStatus.REMATCH_FAILED -> {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Outlined.Warning,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(MaterialTheme.spacing.sm))
                        Text(
                            text = stringResource(id = R.string.booking_status_rematch_failed_title),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Spacer(modifier = Modifier.height(MaterialTheme.spacing.xs))

                    PrimaryButton(
                        text = stringResource(id = R.string.booking_status_action_choose_guide),
                        onClick = onChooseGuide
                    )

                    OnlineOnlyButton(
                        text = stringResource(id = R.string.booking_status_action_cancel_booking),
                        actionLabel = stringResource(id = R.string.booking_card_cancel_action_label),
                        onClick = onCancelBooking,
                        style = OnlineOnlyButtonStyle.SECONDARY
                    )
                }

                BookingStatus.PENDING -> {
                    Text(
                        text = stringResource(id = R.string.status_pending),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}
