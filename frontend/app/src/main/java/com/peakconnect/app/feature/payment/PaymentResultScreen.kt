package com.peakconnect.app.feature.payment

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.HourglassTop
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.peakconnect.app.R
import com.peakconnect.app.core.theme.spacing
import com.peakconnect.app.core.ui.AppCard
import com.peakconnect.app.core.ui.PriceText
import com.peakconnect.app.core.ui.PrimaryButton
import com.peakconnect.app.core.ui.SecondaryButton
import com.peakconnect.app.domain.model.Booking
import java.math.BigDecimal
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun PaymentResultScreen(
    state: PaymentUiState,
    onTryAgain: () -> Unit,
    onCheckAgain: () -> Unit,
    onViewBooking: (String) -> Unit,
    onBackToBooking: () -> Unit,
    onExplore: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.Center
        ) {
            when (state) {
                is PaymentUiState.Processing -> {
                    ProcessingStateContent(booking = state.booking)
                }
                is PaymentUiState.Success -> {
                    SuccessStateContent(
                        booking = state.booking,
                        onViewBooking = { onViewBooking(state.booking.id) },
                        onExplore = onExplore
                    )
                }
                is PaymentUiState.Failed -> {
                    FailedStateContent(
                        reason = state.reason,
                        canRetry = state.canRetry,
                        onTryAgain = onTryAgain,
                        onBackToBooking = onBackToBooking
                    )
                }
                is PaymentUiState.Delayed -> {
                    DelayedStateContent(
                        onCheckAgain = onCheckAgain,
                        onMyBookings = onBackToBooking
                    )
                }
                else -> {
                    // Handled outside
                }
            }
        }
    }
}

@Composable
fun ProcessingStateContent(
    booking: Booking,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(MaterialTheme.spacing.xl)
            .testTag("payment_result_processing"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(64.dp),
            color = MaterialTheme.colorScheme.primary,
            strokeWidth = 4.dp
        )

        Spacer(modifier = Modifier.height(MaterialTheme.spacing.lg))

        Text(
            text = stringResource(id = R.string.payment_result_processing_title),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(MaterialTheme.spacing.sm))

        Text(
            text = stringResource(id = R.string.payment_result_processing_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun SuccessStateContent(
    booking: Booking,
    onViewBooking: () -> Unit,
    onExplore: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    var animatedStart by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        animatedStart = true
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
    }

    val checkScale by animateFloatAsState(
        targetValue = if (animatedStart) 1f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "checkScale"
    )

    val dateFormatter = remember {
        DateTimeFormatter.ofPattern("EEE, d MMM yyyy · h:mm a", Locale.ENGLISH)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(MaterialTheme.spacing.lg)
            .testTag("payment_result_success"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            Spacer(modifier = Modifier.height(MaterialTheme.spacing.xl))

            // Big animated check
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .scale(checkScale)
                    .background(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.Check,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(54.dp)
                )
            }

            Spacer(modifier = Modifier.height(MaterialTheme.spacing.lg))

            Text(
                text = stringResource(id = R.string.payment_result_success_title),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(MaterialTheme.spacing.xl))

            // Summary Card
            AppCard(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(MaterialTheme.spacing.md)
                ) {
                    Text(
                        text = booking.activityTitle,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(MaterialTheme.spacing.xs))

                    Text(
                        text = booking.date.format(dateFormatter),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(MaterialTheme.spacing.sm))
                    HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                    Spacer(modifier = Modifier.height(MaterialTheme.spacing.sm))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(id = R.string.payment_guide_label),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = booking.guideName ?: stringResource(id = R.string.payment_guide_unassigned),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    val deposit = booking.depositAmount
                    if (deposit != null && deposit > 0.0) {
                        Spacer(modifier = Modifier.height(MaterialTheme.spacing.xs))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = stringResource(id = R.string.booking_status_confirmed_deposit_paid, ""),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            PriceText(
                                amount = BigDecimal.valueOf(deposit),
                                style = MaterialTheme.typography.titleSmall
                            )
                        }
                    }
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = MaterialTheme.spacing.xl)
        ) {
            PrimaryButton(
                text = stringResource(id = R.string.payment_result_success_action_view_booking),
                onClick = onViewBooking,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("payment_button_view_booking")
            )

            Spacer(modifier = Modifier.height(MaterialTheme.spacing.sm))

            SecondaryButton(
                text = stringResource(id = R.string.payment_result_success_action_explore),
                onClick = onExplore,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("payment_button_back_explore")
            )
        }
    }
}

@Composable
fun FailedStateContent(
    reason: String?,
    canRetry: Boolean,
    onTryAgain: () -> Unit,
    onBackToBooking: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(MaterialTheme.spacing.lg)
            .testTag("payment_result_failed"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            Spacer(modifier = Modifier.height(MaterialTheme.spacing.xxl))

            Box(
                modifier = Modifier
                    .size(88.dp)
                    .background(
                        color = MaterialTheme.colorScheme.error.copy(alpha = 0.12f),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.Close,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(48.dp)
                )
            }

            Spacer(modifier = Modifier.height(MaterialTheme.spacing.lg))

            Text(
                text = stringResource(id = R.string.payment_result_failed_title),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center
            )

            if (!reason.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(MaterialTheme.spacing.sm))
                Text(
                    text = reason,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = MaterialTheme.spacing.xl)
        ) {
            if (canRetry) {
                PrimaryButton(
                    text = stringResource(id = R.string.payment_result_failed_action_retry),
                    onClick = onTryAgain,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("payment_button_try_again")
                )

                Spacer(modifier = Modifier.height(MaterialTheme.spacing.sm))
            }

            SecondaryButton(
                text = stringResource(id = R.string.payment_result_failed_action_back),
                onClick = onBackToBooking,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("payment_button_back_booking")
            )
        }
    }
}

@Composable
fun DelayedStateContent(
    onCheckAgain: () -> Unit,
    onMyBookings: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(MaterialTheme.spacing.lg)
            .testTag("payment_result_delayed"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            Spacer(modifier = Modifier.height(MaterialTheme.spacing.xxl))

            Box(
                modifier = Modifier
                    .size(88.dp)
                    .background(
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.HourglassTop,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.size(48.dp)
                )
            }

            Spacer(modifier = Modifier.height(MaterialTheme.spacing.lg))

            Text(
                text = stringResource(id = R.string.payment_result_delayed_title),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(MaterialTheme.spacing.sm))

            Text(
                text = stringResource(id = R.string.payment_result_delayed_message),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = MaterialTheme.spacing.xl)
        ) {
            PrimaryButton(
                text = stringResource(id = R.string.payment_result_delayed_action_check_again),
                onClick = onCheckAgain,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("payment_button_check_again")
            )

            Spacer(modifier = Modifier.height(MaterialTheme.spacing.sm))

            SecondaryButton(
                text = stringResource(id = R.string.payment_result_delayed_action_my_bookings),
                onClick = onMyBookings,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("payment_button_my_bookings")
            )
        }
    }
}
