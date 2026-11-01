package com.peakconnect.app.feature.trekker.waitlist

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.peakconnect.app.R
import com.peakconnect.app.core.images.ImageUrlResolver
import com.peakconnect.app.core.theme.AppShapes
import com.peakconnect.app.core.theme.spacing
import com.peakconnect.app.core.ui.AppAsyncImage
import com.peakconnect.app.core.ui.AppChip
import com.peakconnect.app.core.ui.BannerType
import com.peakconnect.app.core.ui.InfoBanner
import com.peakconnect.app.data.local.entity.WaitlistEntryEntity
import com.peakconnect.app.domain.model.WaitlistStatus
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun WaitlistStatusChip(
    status: WaitlistStatus,
    modifier: Modifier = Modifier
) {
    val (labelRes, containerColor, contentColor) = when (status) {
        WaitlistStatus.WAITING -> Triple(
            R.string.waitlist_chip_waiting,
            MaterialTheme.colorScheme.surfaceVariant,
            MaterialTheme.colorScheme.onSurfaceVariant
        )
        WaitlistStatus.PROMOTED -> Triple(
            R.string.waitlist_chip_promoted,
            Color(0xFFE8F5E9),
            Color(0xFF2E7D32)
        )
        WaitlistStatus.CLOSED,
        WaitlistStatus.EXPIRED,
        WaitlistStatus.CONFIRMED -> Triple(
            R.string.waitlist_chip_closed,
            Color(0xFFECEFF1),
            Color(0xFF546E7A)
        )
    }

    AppChip(
        text = stringResource(id = labelRes),
        modifier = modifier.testTag("waitlist_chip_${status.name.lowercase()}"),
        containerColor = containerColor,
        contentColor = contentColor
    )
}

@Composable
fun WaitlistTrackingNote(
    modifier: Modifier = Modifier
) {
    InfoBanner(
        type = BannerType.INFO,
        text = stringResource(id = R.string.waitlist_local_tracking_note),
        modifier = modifier.testTag("waitlist_tracking_note")
    )
}

@Composable
fun PromotedWaitlistBanner(
    guideName: String?,
    onViewBooking: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("promoted_waitlist_banner"),
        shape = AppShapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(MaterialTheme.spacing.md),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.sm)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                val guideText = guideName ?: stringResource(id = R.string.waitlist_banner_guide_fallback)
                Text(
                    text = stringResource(id = R.string.waitlist_banner_spot_opened, guideText),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.weight(1f)
                )
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .size(24.dp)
                        .testTag("dismiss_promoted_banner")
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = stringResource(id = R.string.waitlist_banner_dismiss),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
            Button(
                onClick = onViewBooking,
                modifier = Modifier
                    .align(Alignment.End)
                    .testTag("view_promoted_booking_button"),
                shape = AppShapes.small
            ) {
                Text(text = stringResource(id = R.string.waitlist_banner_view_booking))
            }
        }
    }
}

@Composable
fun WaitlistCard(
    entry: WaitlistEntryEntity,
    onRemoveClick: (WaitlistEntryEntity) -> Unit,
    onViewBookingClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val dateTimeFormatter = remember {
        DateTimeFormatter.ofPattern("EEE, d MMM yyyy · h:mm a", Locale.ENGLISH)
    }

    val joinedDateStr = remember(entry.joinedAt) {
        val instant = Instant.ofEpochMilli(entry.joinedAt)
        val ldt = LocalDateTime.ofInstant(instant, ZoneId.systemDefault())
        DateTimeFormatter.ofPattern("d MMM yyyy", Locale.ENGLISH).format(ldt)
    }

    val resolvedImageUrl = remember(entry.activityTitle) {
        ImageUrlResolver.resolve(entry.activityTitle, "")
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(AppShapes.medium)
            .testTag("waitlist_card_${entry.slotId}"),
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
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.sm)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.md),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AppAsyncImage(
                    url = resolvedImageUrl,
                    contentDescription = entry.activityTitle,
                    modifier = Modifier
                        .size(72.dp)
                        .clip(AppShapes.small)
                )

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.xs)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = entry.activityTitle,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(MaterialTheme.spacing.xs))
                        WaitlistStatusChip(status = entry.status)
                    }

                    // Slot date
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.xs)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.CalendarToday,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = entry.slotDate.format(dateTimeFormatter),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Chosen guide
                    val guideText = if (!entry.guideName.isNullOrBlank()) {
                        stringResource(id = R.string.waitlist_guide_label, entry.guideName)
                    } else {
                        stringResource(id = R.string.waitlist_guide_none)
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.xs)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Person,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = guideText,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Joined on
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.xs)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Schedule,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = stringResource(id = R.string.waitlist_joined_on, joinedDateStr),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Closed reason if applicable
                    if (entry.status == WaitlistStatus.CLOSED && !entry.closureReason.isNullOrBlank()) {
                        Text(
                            text = entry.closureReason,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.testTag("waitlist_closure_reason")
                        )
                    }
                }
            }

            // Card Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (entry.status == WaitlistStatus.PROMOTED && entry.bookingId != null) {
                    Button(
                        onClick = { onViewBookingClick(entry.bookingId) },
                        shape = AppShapes.small,
                        modifier = Modifier.testTag("waitlist_view_booking_btn")
                    ) {
                        Text(text = stringResource(id = R.string.waitlist_action_view_booking))
                    }
                    Spacer(modifier = Modifier.width(MaterialTheme.spacing.sm))
                }

                TextButton(
                    onClick = { onRemoveClick(entry) },
                    modifier = Modifier.testTag("waitlist_remove_btn")
                ) {
                    Text(
                        text = stringResource(id = R.string.waitlist_action_remove),
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

@Composable
fun RemoveWaitlistDialog(
    entry: WaitlistEntryEntity?,
    onConfirm: (WaitlistEntryEntity) -> Unit,
    onDismiss: () -> Unit
) {
    if (entry != null) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = {
                Text(
                    text = stringResource(id = R.string.waitlist_remove_dialog_title),
                    style = MaterialTheme.typography.titleMedium
                )
            },
            text = {
                Text(
                    text = stringResource(id = R.string.waitlist_remove_dialog_message),
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                TextButton(
                    onClick = { onConfirm(entry) },
                    modifier = Modifier.testTag("confirm_remove_waitlist_btn")
                ) {
                    Text(
                        text = stringResource(id = R.string.waitlist_remove_dialog_confirm),
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) {
                    Text(text = stringResource(id = R.string.waitlist_remove_dialog_cancel))
                }
            }
        )
    }
}
