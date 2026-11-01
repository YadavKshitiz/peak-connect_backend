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
import com.peakconnect.app.R
import com.peakconnect.app.core.locale.AppLanguage
import com.peakconnect.app.core.theme.spacing

@Composable
fun LanguageSelectionDialog(
    currentLanguage: AppLanguage,
    onLanguageSelected: (AppLanguage) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(id = R.string.language_dialog_title),
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                LanguageOptionRow(
                    label = stringResource(id = R.string.language_system_default),
                    isSelected = currentLanguage == AppLanguage.SYSTEM_DEFAULT,
                    onSelect = { onLanguageSelected(AppLanguage.SYSTEM_DEFAULT) }
                )
                Spacer(modifier = Modifier.height(MaterialTheme.spacing.xs))
                LanguageOptionRow(
                    label = stringResource(id = R.string.language_english),
                    isSelected = currentLanguage == AppLanguage.ENGLISH,
                    onSelect = { onLanguageSelected(AppLanguage.ENGLISH) }
                )
                Spacer(modifier = Modifier.height(MaterialTheme.spacing.xs))
                LanguageOptionRow(
                    label = stringResource(id = R.string.language_hindi),
                    isSelected = currentLanguage == AppLanguage.HINDI,
                    onSelect = { onLanguageSelected(AppLanguage.HINDI) }
                )
                Spacer(modifier = Modifier.height(MaterialTheme.spacing.md))
                Text(
                    text = stringResource(id = R.string.language_server_data_note),
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

@Composable
private fun LanguageOptionRow(
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
