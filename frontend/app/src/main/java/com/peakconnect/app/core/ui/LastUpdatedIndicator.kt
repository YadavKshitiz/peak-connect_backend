package com.peakconnect.app.core.ui

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.peakconnect.app.R
import com.peakconnect.app.core.theme.AppShapes
import com.peakconnect.app.core.util.LocalIsOnline

/**
 * Reusable indicator that displays relative time since data was last updated.
 * Renders prominently when the user is offline to make cached data state obvious.
 */
@Composable
fun LastUpdatedIndicator(
    lastUpdatedMillis: Long?,
    modifier: Modifier = Modifier,
    isOffline: Boolean = !LocalIsOnline.current,
    clock: () -> Long = { System.currentTimeMillis() }
) {
    if (lastUpdatedMillis == null || lastUpdatedMillis <= 0L) return

    val relativeTime = formatRelativeTime(lastUpdatedMillis, clock())

    if (isOffline) {
        Surface(
            shape = AppShapes.extraSmall,
            color = MaterialTheme.colorScheme.surfaceVariant,
            modifier = modifier.testTag("last_updated_prominent")
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Outlined.Schedule,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(12.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = stringResource(id = R.string.last_updated_prominent, relativeTime),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    } else {
        Row(
            modifier = modifier.testTag("last_updated_subtle"),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(id = R.string.last_updated_format, relativeTime),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun formatRelativeTime(timestamp: Long, now: Long = System.currentTimeMillis()): String {
    val diff = (now - timestamp).coerceAtLeast(0L)
    val minutes = diff / (60 * 1000L)
    val hours = diff / (60 * 60 * 1000L)
    val days = diff / (24 * 60 * 60 * 1000L)

    return when {
        minutes < 1 -> stringResource(id = R.string.last_updated_just_now)
        minutes < 60 -> stringResource(id = R.string.last_updated_minutes_ago, minutes.toInt())
        hours < 24 -> stringResource(id = R.string.last_updated_hours_ago, hours.toInt())
        days < 2 -> stringResource(id = R.string.last_updated_yesterday)
        else -> stringResource(id = R.string.last_updated_days_ago, days.toInt())
    }
}
