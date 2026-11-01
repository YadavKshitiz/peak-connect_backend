package com.peakconnect.app.feature.notifications

import android.content.Context
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.DeleteSweep
import androidx.compose.material.icons.outlined.Hiking
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Payment
import androidx.compose.material.icons.outlined.PersonPin
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.peakconnect.app.R
import com.peakconnect.app.core.theme.AppShapes
import com.peakconnect.app.core.theme.spacing
import com.peakconnect.app.core.ui.AppTopBar
import com.peakconnect.app.core.ui.EmptyState
import com.peakconnect.app.data.local.entity.NotificationEntity
import com.peakconnect.app.domain.model.Role

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsScreen(
    onBack: () -> Unit,
    onNavigateToBooking: (bookingId: String, isGuide: Boolean) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: NotificationsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            AppTopBar(
                title = stringResource(id = R.string.notifications_inbox_title),
                onBackClick = onBack,
                actions = {
                    if (uiState.notifications.isNotEmpty()) {
                        TextButton(onClick = { viewModel.markAllAsRead() }) {
                            Text(
                                text = stringResource(id = R.string.notifications_action_mark_all_read),
                                style = MaterialTheme.typography.labelLarge
                            )
                        }
                        IconButton(onClick = { viewModel.clearAll() }) {
                            Icon(
                                imageVector = Icons.Outlined.DeleteSweep,
                                contentDescription = stringResource(id = R.string.notifications_action_clear_all)
                            )
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        if (uiState.notifications.isEmpty()) {
            EmptyState(
                icon = Icons.Outlined.Notifications,
                title = stringResource(id = R.string.notifications_empty_title),
                message = stringResource(id = R.string.notifications_empty_desc),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = PaddingValues(MaterialTheme.spacing.md),
                verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.sm)
            ) {
                items(
                    items = uiState.notifications,
                    key = { it.id }
                ) { notification ->
                    val dismissState = rememberSwipeToDismissBoxState(
                        confirmValueChange = { value ->
                            if (value == SwipeToDismissBoxValue.EndToStart || value == SwipeToDismissBoxValue.StartToEnd) {
                                viewModel.deleteNotification(notification.id)
                                true
                            } else {
                                false
                            }
                        }
                    )

                    SwipeToDismissBox(
                        state = dismissState,
                        backgroundContent = {
                            val color by animateColorAsState(
                                targetValue = when (dismissState.targetValue) {
                                    SwipeToDismissBoxValue.EndToStart,
                                    SwipeToDismissBoxValue.StartToEnd -> MaterialTheme.colorScheme.errorContainer
                                    else -> Color.Transparent
                                },
                                label = "dismiss_color"
                            )
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(AppShapes.medium)
                                    .background(color)
                                    .padding(horizontal = MaterialTheme.spacing.lg),
                                contentAlignment = Alignment.CenterEnd
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Delete,
                                    contentDescription = stringResource(id = R.string.common_cancel),
                                    tint = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                        }
                    ) {
                        NotificationItemCard(
                            notification = notification,
                            relativeTime = formatRelativeTime(notification.createdAt, context),
                            onClick = {
                                viewModel.markAsRead(notification.id)
                                val bookingId = notification.bookingId
                                if (bookingId != null) {
                                    val isGuide = notification.type == "GUIDE_REMINDER" || uiState.userRole == Role.GUIDE
                                    onNavigateToBooking(bookingId, isGuide)
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun NotificationItemCard(
    notification: NotificationEntity,
    relativeTime: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = AppShapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = if (notification.isRead) {
                MaterialTheme.colorScheme.surface
            } else {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            }
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (notification.isRead) 1.dp else 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(MaterialTheme.spacing.md),
            verticalAlignment = Alignment.Top
        ) {
            // Icon by type
            val icon = getNotificationIcon(notification.type)
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(MaterialTheme.spacing.md))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = notification.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = if (notification.isRead) FontWeight.Medium else FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f)
                    )

                    Spacer(modifier = Modifier.width(MaterialTheme.spacing.xs))

                    Text(
                        text = relativeTime,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(MaterialTheme.spacing.xs))

                Text(
                    text = notification.message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (!notification.isRead) {
                Spacer(modifier = Modifier.width(MaterialTheme.spacing.sm))
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary)
                        .align(Alignment.CenterVertically)
                )
            }
        }
    }
}

private fun getNotificationIcon(type: String): ImageVector {
    return when (type) {
        "PAYMENT_REMINDER" -> Icons.Outlined.Payment
        "TREK_REMINDER" -> Icons.Outlined.Hiking
        "GUIDE_REMINDER" -> Icons.Outlined.PersonPin
        "TOKEN_EXPIRED" -> Icons.Outlined.Lock
        else -> Icons.Outlined.CheckCircle
    }
}

private fun formatRelativeTime(createdAt: Long, context: Context): String {
    val diff = (System.currentTimeMillis() - createdAt).coerceAtLeast(0L)
    val minutes = diff / 60_000L
    val hours = diff / 3600_000L
    val days = diff / 86400_000L

    return when {
        minutes < 1 -> context.getString(R.string.notifications_relative_just_now)
        minutes < 60 -> context.getString(R.string.notifications_relative_mins_ago, minutes.toInt())
        hours < 24 -> context.getString(R.string.notifications_relative_hours_ago, hours.toInt())
        else -> context.getString(R.string.notifications_relative_days_ago, days.toInt())
    }
}
