package com.peakconnect.app.feature.guide.home

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Timer
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.peakconnect.app.R
import com.peakconnect.app.core.theme.spacing
import com.peakconnect.app.core.ui.OnlineOnlyButton
import com.peakconnect.app.core.ui.OnlineOnlyButtonStyle
import com.peakconnect.app.data.local.entity.GuideBookingEntity
import com.peakconnect.app.feature.trekker.booking.BookingCountdownCalculator
import com.peakconnect.app.feature.trekker.booking.rememberBookingCountdown
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun GuideRequestCard(
    booking: GuideBookingEntity,
    onCardClick: (String) -> Unit,
    onAccept: (String) -> Unit,
    onDecline: (String) -> Unit,
    modifier: Modifier = Modifier,
    isActing: Boolean = false
) {
    val dateTimeFormatter = remember {
        DateTimeFormatter.ofPattern("EEE, d MMM yyyy · h:mm a", Locale.ENGLISH)
    }

    val countdownState by rememberBookingCountdown(
        statusSinceMillis = booking.statusSinceMillis,
        windowDurationMillis = BookingCountdownCalculator.GUIDE_RESPONSE_WINDOW_MILLIS
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onCardClick(booking.id) },
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
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.sm)
        ) {
            // Header: Trekker name & countdown badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Person,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = stringResource(id = R.string.guide_trekker_label, booking.trekkerName),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                val isOnline = com.peakconnect.app.core.util.LocalIsOnline.current

                if (!isOnline) {
                    Text(
                        text = stringResource(id = R.string.booking_status_offline_may_have_changed),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Timer,
                            contentDescription = null,
                            tint = if (countdownState.isExpired) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = if (countdownState.isExpired) {
                                stringResource(id = R.string.guide_countdown_expired)
                            } else {
                                stringResource(
                                    id = R.string.booking_card_approx_left,
                                    "${countdownState.minutesRemaining} min"
                                )
                            },
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Medium,
                            color = if (countdownState.isExpired) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            // Activity title
            Text(
                text = booking.activityTitle,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Date
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

            Spacer(modifier = Modifier.height(MaterialTheme.spacing.xs))

            // Action buttons: Accept & Decline
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.sm)
            ) {
                OnlineOnlyButton(
                    text = stringResource(id = R.string.guide_action_accept),
                    actionLabel = stringResource(id = R.string.guide_action_accept_label),
                    onClick = { onAccept(booking.id) },
                    enabled = !isActing,
                    style = OnlineOnlyButtonStyle.PRIMARY,
                    modifier = Modifier.weight(1f)
                )

                OnlineOnlyButton(
                    text = stringResource(id = R.string.guide_action_decline),
                    actionLabel = stringResource(id = R.string.guide_action_decline_label),
                    onClick = { onDecline(booking.id) },
                    enabled = !isActing,
                    style = OnlineOnlyButtonStyle.DESTRUCTIVE,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}
