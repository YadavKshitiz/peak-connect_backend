package com.peakconnect.app.core.ui

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.peakconnect.app.core.theme.AppShapes
import com.peakconnect.app.core.theme.PeakConnectTheme
import com.peakconnect.app.core.theme.spacing

enum class BannerType {
    INFO,
    WARNING,
    ERROR,
    SUCCESS
}

@Composable
fun InfoBanner(
    type: BannerType,
    text: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    leadingIcon: ImageVector? = null
) {
    val (containerColor, contentColor, icon) = when (type) {
        BannerType.INFO -> Triple(
            MaterialTheme.colorScheme.secondaryContainer,
            MaterialTheme.colorScheme.onSecondaryContainer,
            Icons.Default.Info
        )
        BannerType.WARNING -> Triple(
            Color(0xFFFFF3D6),
            Color(0xFF6E4E00),
            Icons.Default.Warning
        )
        BannerType.ERROR -> Triple(
            MaterialTheme.colorScheme.errorContainer,
            MaterialTheme.colorScheme.onErrorContainer,
            Icons.Default.Error
        )
        BannerType.SUCCESS -> Triple(
            MaterialTheme.colorScheme.primaryContainer,
            MaterialTheme.colorScheme.onPrimaryContainer,
            Icons.Default.CheckCircle
        )
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(AppShapes.small)
            .background(containerColor)
            .border(1.dp, contentColor.copy(alpha = 0.2f), AppShapes.small)
            .padding(
                horizontal = MaterialTheme.spacing.md,
                vertical = MaterialTheme.spacing.sm
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier.weight(1f, fill = false),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = leadingIcon ?: icon,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(MaterialTheme.spacing.sm))
            Text(
                text = text,
                style = MaterialTheme.typography.bodySmall,
                color = contentColor
            )
        }

        if (actionLabel != null && onAction != null) {
            Spacer(modifier = Modifier.width(MaterialTheme.spacing.sm))
            TextButton(
                onClick = onAction,
                shape = AppShapes.extraSmall
            ) {
                Text(
                    text = actionLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = contentColor
                )
            }
        }
    }
}

@Preview(name = "Light InfoBanners", showBackground = true)
@Composable
private fun InfoBannersPreviewLight() {
    PeakConnectTheme(themeMode = com.peakconnect.app.core.theme.ThemeMode.LIGHT) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            InfoBanner(
                type = BannerType.WARNING,
                text = "You are currently offline. Showing cached information.",
                actionLabel = "Retry",
                onAction = {}
            )
            InfoBanner(
                type = BannerType.INFO,
                text = "Server is waking up. Requests may take a moment."
            )
            InfoBanner(
                type = BannerType.SUCCESS,
                text = "Trek booking confirmed!"
            )
            InfoBanner(
                type = BannerType.ERROR,
                text = "Payment transaction failed. Please try again."
            )
        }
    }
}

@Preview(name = "Dark InfoBanners", uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true)
@Composable
private fun InfoBannersPreviewDark() {
    PeakConnectTheme(themeMode = com.peakconnect.app.core.theme.ThemeMode.DARK) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            InfoBanner(
                type = BannerType.WARNING,
                text = "You are currently offline. Showing cached information.",
                actionLabel = "Retry",
                onAction = {}
            )
            InfoBanner(
                type = BannerType.INFO,
                text = "Server is waking up. Requests may take a moment."
            )
        }
    }
}
