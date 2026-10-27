package com.peakconnect.app.feature.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.peakconnect.app.R
import com.peakconnect.app.core.theme.ThemeMode
import com.peakconnect.app.core.theme.spacing

@Composable
fun AppearanceDialog(
    currentThemeMode: ThemeMode,
    onThemeSelected: (ThemeMode) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(id = R.string.appearance_title),
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                ThemeOptionRow(
                    label = stringResource(id = R.string.theme_system),
                    isSelected = currentThemeMode == ThemeMode.SYSTEM,
                    onSelect = { onThemeSelected(ThemeMode.SYSTEM) }
                )
                Spacer(modifier = Modifier.height(MaterialTheme.spacing.xs))
                ThemeOptionRow(
                    label = stringResource(id = R.string.theme_light),
                    isSelected = currentThemeMode == ThemeMode.LIGHT,
                    onSelect = { onThemeSelected(ThemeMode.LIGHT) }
                )
                Spacer(modifier = Modifier.height(MaterialTheme.spacing.xs))
                ThemeOptionRow(
                    label = stringResource(id = R.string.theme_dark),
                    isSelected = currentThemeMode == ThemeMode.DARK,
                    onSelect = { onThemeSelected(ThemeMode.DARK) }
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

@Composable
private fun ThemeOptionRow(
    label: String,
    isSelected: Boolean,
    onSelect: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onSelect)
            .padding(vertical = MaterialTheme.spacing.xs),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = isSelected,
            onClick = onSelect
        )
        Spacer(modifier = Modifier.width(MaterialTheme.spacing.sm))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
