package com.peakconnect.app.feature.trekker.booking

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.peakconnect.app.R
import com.peakconnect.app.core.theme.semanticColors
import com.peakconnect.app.core.theme.spacing
import com.peakconnect.app.domain.model.BookingStatus

enum class StepState {
    COMPLETED,
    CURRENT,
    FUTURE,
    TERMINAL
}

data class StepperItem(
    val title: String,
    val state: StepState,
    val isTerminal: Boolean = false,
    val statusColor: Color? = null
)

@Composable
fun BookingStepper(
    status: BookingStatus,
    hasGuide: Boolean,
    modifier: Modifier = Modifier
) {
    val items = buildStepperItems(status = status, hasGuide = hasGuide)

    Column(
        modifier = modifier.padding(vertical = MaterialTheme.spacing.sm)
    ) {
        items.forEachIndexed { index, item ->
            val isLast = index == items.lastIndex

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Column for indicator and vertical connector line
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    StepIndicator(item = item)

                    if (!isLast) {
                        val lineColor = if (item.state == StepState.COMPLETED) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.outlineVariant
                        }
                        Box(
                            modifier = Modifier
                                .width(2.dp)
                                .height(28.dp)
                                .background(lineColor)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(MaterialTheme.spacing.md))

                // Step label
                Text(
                    text = item.title,
                    style = if (item.state == StepState.CURRENT) {
                        MaterialTheme.typography.titleMedium
                    } else {
                        MaterialTheme.typography.bodyMedium
                    },
                    color = when (item.state) {
                        StepState.COMPLETED -> MaterialTheme.colorScheme.onSurface
                        StepState.CURRENT -> MaterialTheme.colorScheme.primary
                        StepState.FUTURE -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        StepState.TERMINAL -> item.statusColor ?: MaterialTheme.colorScheme.error
                    }
                )
            }
        }
    }
}

@Composable
private fun StepIndicator(
    item: StepperItem,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "step_pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 800),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    when (item.state) {
        StepState.COMPLETED -> {
            Box(
                modifier = modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(14.dp)
                )
            }
        }

        StepState.CURRENT -> {
            Box(
                modifier = modifier.size(24.dp),
                contentAlignment = Alignment.Center
            ) {
                // Pulsing dot
                Box(
                    modifier = Modifier
                        .size(14.dp)
                        .scale(pulseScale)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.35f))
                )
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary)
                )
            }
        }

        StepState.FUTURE -> {
            Box(
                modifier = modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .border(2.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
            )
        }

        StepState.TERMINAL -> {
            val termColor = item.statusColor ?: MaterialTheme.colorScheme.error
            Box(
                modifier = modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(termColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (item.statusColor == MaterialTheme.colorScheme.error) {
                        Icons.Default.Close
                    } else {
                        Icons.Default.Warning
                    },
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}

@Composable
private fun buildStepperItems(
    status: BookingStatus,
    hasGuide: Boolean
): List<StepperItem> {
    val semanticColors = MaterialTheme.semanticColors

    val reqSentTitle = stringResource(id = R.string.booking_status_step_request_sent)
    val guideAcceptedTitle = stringResource(id = R.string.booking_status_step_guide_accepted)
    val depositPaidTitle = stringResource(id = R.string.booking_status_step_deposit_paid)
    val confirmedTitle = stringResource(id = R.string.booking_status_step_confirmed)

    val cancelledTitle = stringResource(id = R.string.booking_status_step_cancelled)
    val noShowTitle = stringResource(id = R.string.booking_status_step_no_show)
    val rematchFailedTitle = stringResource(id = R.string.booking_status_step_rematch_failed)

    val items = mutableListOf<StepperItem>()

    when (status) {
        BookingStatus.PENDING_GUIDE_RESPONSE -> {
            items.add(StepperItem(reqSentTitle, StepState.COMPLETED))
            if (hasGuide) {
                items.add(StepperItem(guideAcceptedTitle, StepState.CURRENT))
                items.add(StepperItem(depositPaidTitle, StepState.FUTURE))
                items.add(StepperItem(confirmedTitle, StepState.FUTURE))
            } else {
                items.add(StepperItem(depositPaidTitle, StepState.CURRENT))
                items.add(StepperItem(confirmedTitle, StepState.FUTURE))
            }
        }

        BookingStatus.AWAITING_PAYMENT -> {
            items.add(StepperItem(reqSentTitle, StepState.COMPLETED))
            if (hasGuide) {
                items.add(StepperItem(guideAcceptedTitle, StepState.COMPLETED))
            }
            items.add(StepperItem(depositPaidTitle, StepState.CURRENT))
            items.add(StepperItem(confirmedTitle, StepState.FUTURE))
        }

        BookingStatus.CONFIRMED -> {
            items.add(StepperItem(reqSentTitle, StepState.COMPLETED))
            if (hasGuide) {
                items.add(StepperItem(guideAcceptedTitle, StepState.COMPLETED))
            }
            items.add(StepperItem(depositPaidTitle, StepState.COMPLETED))
            items.add(StepperItem(confirmedTitle, StepState.COMPLETED))
        }

        BookingStatus.CANCELLED -> {
            items.add(StepperItem(reqSentTitle, StepState.COMPLETED))
            val errorColor = semanticColors.bookingStatusColor(BookingStatus.CANCELLED).content
            items.add(
                StepperItem(
                    title = cancelledTitle,
                    state = StepState.TERMINAL,
                    isTerminal = true,
                    statusColor = errorColor
                )
            )
        }

        BookingStatus.NO_SHOW -> {
            items.add(StepperItem(reqSentTitle, StepState.COMPLETED))
            if (hasGuide) {
                items.add(StepperItem(guideAcceptedTitle, StepState.COMPLETED))
            }
            items.add(StepperItem(depositPaidTitle, StepState.COMPLETED))
            val errorColor = semanticColors.bookingStatusColor(BookingStatus.NO_SHOW).content
            items.add(
                StepperItem(
                    title = noShowTitle,
                    state = StepState.TERMINAL,
                    isTerminal = true,
                    statusColor = errorColor
                )
            )
        }

        BookingStatus.REMATCH_FAILED -> {
            items.add(StepperItem(reqSentTitle, StepState.COMPLETED))
            val warningColor = semanticColors.bookingStatusColor(BookingStatus.REMATCH_FAILED).content
            items.add(
                StepperItem(
                    title = rematchFailedTitle,
                    state = StepState.TERMINAL,
                    isTerminal = true,
                    statusColor = warningColor
                )
            )
        }

        BookingStatus.PENDING -> {
            items.add(StepperItem(reqSentTitle, StepState.CURRENT))
            if (hasGuide) {
                items.add(StepperItem(guideAcceptedTitle, StepState.FUTURE))
            }
            items.add(StepperItem(depositPaidTitle, StepState.FUTURE))
            items.add(StepperItem(confirmedTitle, StepState.FUTURE))
        }
    }

    return items
}
