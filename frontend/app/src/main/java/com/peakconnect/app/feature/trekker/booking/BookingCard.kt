package com.peakconnect.app.feature.trekker.booking

import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.peakconnect.app.R
import com.peakconnect.app.core.images.ImageUrlResolver
import com.peakconnect.app.core.theme.AppShapes
import com.peakconnect.app.core.theme.spacing
import com.peakconnect.app.core.ui.AppAsyncImage
import com.peakconnect.app.core.ui.OnlineOnlyButton
import com.peakconnect.app.core.ui.StatusChip
import com.peakconnect.app.domain.model.BookingStatus
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun BookingCard(
    item: BookingListItem,
    onCardClick: (String) -> Unit,
    onPayClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val booking = item.booking
    val dateTimeFormatter = remember {
        DateTimeFormatter.ofPattern("EEE, d MMM yyyy · h:mm a", Locale.ENGLISH)
    }

    val displayGuideName = if (booking.guideName.isNullOrBlank()) {
        stringResource(id = R.string.guide_assigned_placeholder)
    } else {
        booking.guideName
    }

    val resolvedImageUrl = remember(booking.activityTitle, item.activityLocation) {
        ImageUrlResolver.resolve(booking.activityTitle, item.activityLocation ?: "")
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(AppShapes.medium)
            .clickable { onCardClick(booking.id) },
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.md)
            ) {
                // Activity thumbnail
                AppAsyncImage(
                    url = resolvedImageUrl,
                    contentDescription = booking.activityTitle,
                    modifier = Modifier
                        .size(80.dp)
                        .clip(AppShapes.small)
                )

                // Details column
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .align(Alignment.CenterVertically)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Text(
                            text = booking.activityTitle,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                    }

                    Spacer(modifier = Modifier.height(MaterialTheme.spacing.xs))

                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.CalendarToday,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(MaterialTheme.spacing.xs))
                        Text(
                            text = booking.date.format(dateTimeFormatter),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Spacer(modifier = Modifier.height(MaterialTheme.spacing.xs))

                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Person,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(MaterialTheme.spacing.xs))
                        Text(
                            text = displayGuideName,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(MaterialTheme.spacing.sm))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                StatusChip(bookingStatus = booking.status)

                val isOnline = com.peakconnect.app.core.util.LocalIsOnline.current

                if (booking.status == BookingStatus.PENDING_GUIDE_RESPONSE) {
                    if (!isOnline) {
                        Text(
                            text = stringResource(id = R.string.booking_status_offline_may_have_changed),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        val countdownState by rememberBookingCountdown(
                            statusSinceMillis = booking.statusSinceMillis,
                            windowDurationMillis = BookingCountdownCalculator.GUIDE_RESPONSE_WINDOW_MILLIS
                        )
                        if (!countdownState.isExpired) {
                            Text(
                                text = stringResource(
                                    id = R.string.booking_card_approx_left,
                                    countdownState.formattedTime
                                ),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Pay deposit button with approximate countdown for AWAITING_PAYMENT
            if (booking.status == BookingStatus.AWAITING_PAYMENT) {
                Spacer(modifier = Modifier.height(MaterialTheme.spacing.md))
                val isOnline = com.peakconnect.app.core.util.LocalIsOnline.current

                if (!isOnline) {
                    Text(
                        text = stringResource(id = R.string.booking_status_offline_may_have_changed),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    val countdownState by rememberBookingCountdown(
                        statusSinceMillis = booking.statusSinceMillis,
                        windowDurationMillis = BookingCountdownCalculator.PAYMENT_WINDOW_MILLIS
                    )

                    val payButtonText = if (countdownState.isExpired) {
                        stringResource(id = R.string.booking_card_pay_deposit)
                    } else {
                        stringResource(
                            id = R.string.booking_card_pay_deposit
                        ) + " • " + stringResource(
                            id = R.string.booking_card_approx_left,
                            countdownState.formattedTime
                        )
                    }

                    OnlineOnlyButton(
                        text = payButtonText,
                        actionLabel = stringResource(id = R.string.booking_card_pay_action_label),
                        onClick = { onPayClick(booking.id) },
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (countdownState.isExpired) {
                        Spacer(modifier = Modifier.height(MaterialTheme.spacing.xs))
                        Text(
                            text = stringResource(id = R.string.booking_status_window_ended),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }
    }
}
