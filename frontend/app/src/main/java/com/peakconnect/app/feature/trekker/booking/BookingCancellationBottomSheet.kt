package com.peakconnect.app.feature.trekker.booking

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.peakconnect.app.R
import com.peakconnect.app.core.theme.AppShapes
import com.peakconnect.app.core.theme.spacing
import com.peakconnect.app.core.ui.AppModalBottomSheet
import com.peakconnect.app.core.ui.DestructiveButton
import com.peakconnect.app.core.ui.OnlineOnlyButton
import com.peakconnect.app.core.ui.OnlineOnlyButtonStyle
import com.peakconnect.app.core.ui.PrimaryButton
import com.peakconnect.app.core.ui.SecondaryButton
import com.peakconnect.app.core.ui.formatRupees
import com.peakconnect.app.data.local.entity.BookingEntity
import com.peakconnect.app.data.local.entity.SlotEntity
import com.peakconnect.app.domain.model.BookingStatus
import com.peakconnect.app.domain.model.CancellationPolicyType
import java.math.BigDecimal
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookingCancellationBottomSheet(
    booking: BookingEntity,
    slot: SlotEntity?,
    policy: CancellationPolicyType,
    isCancelling: Boolean,
    cancellationResult: ServerCancellationResult?,
    cancellationError: String?,
    showConfirmDialog: Boolean,
    onDismissRequest: () -> Unit,
    onRequestCancel: () -> Unit,
    onConfirmCancel: () -> Unit,
    onDismissConfirmDialog: () -> Unit,
    onDone: () -> Unit,
    modifier: Modifier = Modifier
) {
    AppModalBottomSheet(
        onDismissRequest = onDismissRequest,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = MaterialTheme.spacing.lg)
                .padding(bottom = MaterialTheme.spacing.xl),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.md)
        ) {
            if (cancellationResult != null) {
                // Server Authoritative Result Card
                AuthoritativeResultView(
                    result = cancellationResult,
                    onDone = onDone
                )
            } else {
                // Cancellation preview & confirmation
                CancellationPreviewView(
                    booking = booking,
                    slot = slot,
                    policy = policy,
                    isCancelling = isCancelling,
                    cancellationError = cancellationError,
                    onKeepBooking = onDismissRequest,
                    onRequestCancel = onRequestCancel
                )
            }
        }
    }

    if (showConfirmDialog) {
        AlertDialog(
            onDismissRequest = onDismissConfirmDialog,
            title = {
                Text(
                    text = stringResource(id = R.string.cancel_booking_dialog_title),
                    style = MaterialTheme.typography.titleLarge
                )
            },
            text = {
                Text(
                    text = stringResource(id = R.string.cancel_booking_dialog_message),
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                DestructiveButton(
                    text = stringResource(id = R.string.cancel_booking_dialog_confirm),
                    onClick = onConfirmCancel,
                    isLoading = isCancelling
                )
            },
            dismissButton = {
                TextButton(onClick = onDismissConfirmDialog) {
                    Text(text = stringResource(id = R.string.cancel_booking_dialog_dismiss))
                }
            },
            shape = AppShapes.medium
        )
    }
}

@Composable
private fun AuthoritativeResultView(
    result: ServerCancellationResult,
    onDone: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.md)
    ) {
        Icon(
            imageVector = Icons.Default.CheckCircle,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(56.dp)
        )

        Text(
            text = stringResource(id = R.string.cancel_booking_success_title),
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface
        )

        val refundAmount = result.refundAmount
        val refundPercentage = result.refundPercentage?.let { (it * 100).toInt() } ?: 0

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = AppShapes.medium,
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(MaterialTheme.spacing.md),
                verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.xs)
            ) {
                if (refundAmount != null && refundAmount > 0.0) {
                    val formatted = formatRupees(BigDecimal.valueOf(refundAmount))
                    Text(
                        text = stringResource(
                            id = R.string.cancel_booking_authoritative_refund,
                            formatted,
                            refundPercentage.toString()
                        ),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                } else {
                    Text(
                        text = stringResource(id = R.string.booking_status_terminal_deposit_non_refundable),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(MaterialTheme.spacing.sm))

        PrimaryButton(
            text = stringResource(id = R.string.cancel_booking_done),
            onClick = onDone,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun CancellationPreviewView(
    booking: BookingEntity,
    slot: SlotEntity?,
    policy: CancellationPolicyType,
    isCancelling: Boolean,
    cancellationError: String?,
    onKeepBooking: () -> Unit,
    onRequestCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.md)
    ) {
        Text(
            text = stringResource(id = R.string.cancel_booking_title),
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface
        )

        if (booking.status == BookingStatus.PENDING_GUIDE_RESPONSE ||
            booking.status == BookingStatus.AWAITING_PAYMENT
        ) {
            // Nothing paid yet
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = AppShapes.medium,
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                )
            ) {
                Row(
                    modifier = Modifier.padding(MaterialTheme.spacing.md),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(MaterialTheme.spacing.sm))
                    Text(
                        text = stringResource(id = R.string.cancel_booking_no_payment),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        } else {
            // CONFIRMED: calculate refund preview
            val preview = remember(booking, slot, policy) {
                BookingCancellationCalculator.calculatePreview(
                    booking = booking,
                    policy = policy,
                    totalPrice = slot?.currentPrice
                )
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = AppShapes.medium,
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(MaterialTheme.spacing.md),
                    verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.sm)
                ) {
                    val refundFormattedBase = formatRupees(preview.refundableBase)
                    Text(
                        text = stringResource(
                            id = R.string.cancel_booking_estimate_refund,
                            preview.refundPercentage,
                            refundFormattedBase
                        ),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    val depositFormatted = formatRupees(preview.depositAmount)
                    Text(
                        text = stringResource(
                            id = R.string.cancel_booking_deposit_non_refundable,
                            depositFormatted
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(MaterialTheme.spacing.xs))

                    // Policy Tiers with the matching tier highlighted
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.xs)
                    ) {
                        preview.policyTiers.forEachIndexed { index, tier ->
                            val isActive = index == preview.activeTierIndex
                            val rowColor = if (isActive) {
                                MaterialTheme.colorScheme.primaryContainer
                            } else {
                                Color.Transparent
                            }
                            val textColor = if (isActive) {
                                MaterialTheme.colorScheme.onPrimaryContainer
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(AppShapes.small)
                                    .background(rowColor)
                                    .padding(
                                        horizontal = MaterialTheme.spacing.sm,
                                        vertical = MaterialTheme.spacing.xs
                                    ),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (isActive) {
                                        Icon(
                                            imageVector = Icons.Outlined.Check,
                                            contentDescription = null,
                                            tint = textColor,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(MaterialTheme.spacing.xs))
                                    }
                                    Text(
                                        text = tier.timeline,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = textColor,
                                        fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal
                                    )
                                }
                                Text(
                                    text = String.format(Locale.US, "%d%%", tier.refundPercentage),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = textColor,
                                    fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(MaterialTheme.spacing.xs))

                    Text(
                        text = stringResource(id = R.string.cancel_booking_disclaimer),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        if (cancellationError != null) {
            Text(
                text = cancellationError,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error
            )
        }

        Spacer(modifier = Modifier.height(MaterialTheme.spacing.sm))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.md)
        ) {
            SecondaryButton(
                text = stringResource(id = R.string.cancel_booking_action_keep),
                onClick = onKeepBooking,
                modifier = Modifier.weight(1f)
            )

            OnlineOnlyButton(
                text = stringResource(id = R.string.cancel_booking_action_cancel),
                actionLabel = stringResource(id = R.string.cancel_booking_action_label),
                onClick = onRequestCancel,
                style = OnlineOnlyButtonStyle.DESTRUCTIVE,
                isLoading = isCancelling,
                modifier = Modifier.weight(1f)
            )
        }
    }
}
