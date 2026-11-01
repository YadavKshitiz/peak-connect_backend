package com.peakconnect.app.feature.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.peakconnect.app.R
import com.peakconnect.app.core.theme.spacing
import com.peakconnect.app.core.ui.OnlineOnlyButton
import com.peakconnect.app.core.ui.OnlineOnlyButtonStyle
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun NotificationSettingsDialog(
    masterEnabled: Boolean,
    remindersEnabled: Boolean,
    lastSyncTimestamp: Long?,
    onMasterToggled: (Boolean) -> Unit,
    onRemindersToggled: (Boolean) -> Unit,
    onSyncNow: () -> Unit,
    onDismiss: () -> Unit
) {
    val dateFormatter = DateTimeFormatter.ofPattern("d MMM, HH:mm")
    val lastSyncText = if (lastSyncTimestamp != null) {
        val ldt = Instant.ofEpochMilli(lastSyncTimestamp).atZone(ZoneId.systemDefault()).toLocalDateTime()
        stringResource(id = R.string.notification_settings_last_synced, ldt.format(dateFormatter))
    } else {
        stringResource(id = R.string.notification_settings_never_synced)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(id = R.string.notification_settings_title),
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Master notifications switch
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(id = R.string.notification_settings_master_title),
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Text(
                            text = stringResource(id = R.string.notification_settings_master_desc),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.width(MaterialTheme.spacing.sm))
                    Switch(
                        checked = masterEnabled,
                        onCheckedChange = onMasterToggled
                    )
                }

                Spacer(modifier = Modifier.height(MaterialTheme.spacing.md))

                // Reminders switch
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(id = R.string.notification_settings_reminders_title),
                            style = MaterialTheme.typography.bodyLarge,
                            color = if (masterEnabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                        )
                        Text(
                            text = stringResource(id = R.string.notification_settings_reminders_desc),
                            style = MaterialTheme.typography.bodySmall,
                            color = if (masterEnabled) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
                        )
                    }
                    Spacer(modifier = Modifier.width(MaterialTheme.spacing.sm))
                    Switch(
                        checked = remindersEnabled && masterEnabled,
                        onCheckedChange = onRemindersToggled,
                        enabled = masterEnabled
                    )
                }

                Spacer(modifier = Modifier.height(MaterialTheme.spacing.md))

                // Sync Now button and last sync time
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OnlineOnlyButton(
                        text = stringResource(id = R.string.notification_settings_sync_button),
                        actionLabel = stringResource(id = R.string.notification_settings_sync_button_label),
                        onClick = onSyncNow,
                        style = OnlineOnlyButtonStyle.SECONDARY,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(MaterialTheme.spacing.xs))

                Text(
                    text = lastSyncText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(MaterialTheme.spacing.md))

                // Educational note
                Text(
                    text = stringResource(id = R.string.notification_settings_note),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(id = R.string.common_cancel))
            }
        }
    )
}
