package com.peakconnect.app.core.ui

import android.content.res.Configuration
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.peakconnect.app.core.theme.AppShapes
import com.peakconnect.app.core.theme.PeakConnectTheme
import com.peakconnect.app.core.theme.spacing

@Composable
fun AppCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    outlined: Boolean = true,
    content: @Composable ColumnScope.() -> Unit
) {
    val border = if (outlined) {
        BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
    } else {
        null
    }

    val elevation = if (outlined) {
        CardDefaults.cardElevation(defaultElevation = 0.dp)
    } else {
        CardDefaults.cardElevation(defaultElevation = 2.dp)
    }

    if (onClick != null) {
        Card(
            onClick = onClick,
            modifier = modifier.fillMaxWidth(),
            shape = AppShapes.medium,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = border,
            elevation = elevation,
            content = content
        )
    } else {
        Card(
            modifier = modifier.fillMaxWidth(),
            shape = AppShapes.medium,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = border,
            elevation = elevation,
            content = content
        )
    }
}

@Preview(name = "Light AppCard", showBackground = true)
@Composable
private fun AppCardPreviewLight() {
    PeakConnectTheme(themeMode = com.peakconnect.app.core.theme.ThemeMode.LIGHT) {
        Column(modifier = Modifier.padding(16.dp)) {
            AppCard {
                Column(modifier = Modifier.padding(MaterialTheme.spacing.lg)) {
                    Text(text = "Kedarkantha Trek", style = MaterialTheme.typography.titleMedium)
                    Text(text = "Uttarakhand, India", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

@Preview(name = "Dark AppCard", uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true)
@Composable
private fun AppCardPreviewDark() {
    PeakConnectTheme(themeMode = com.peakconnect.app.core.theme.ThemeMode.DARK) {
        Column(modifier = Modifier.padding(16.dp)) {
            AppCard(outlined = false) {
                Column(modifier = Modifier.padding(MaterialTheme.spacing.lg)) {
                    Text(text = "Hampta Pass Trek", style = MaterialTheme.typography.titleMedium)
                    Text(text = "Himachal Pradesh, India", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}
