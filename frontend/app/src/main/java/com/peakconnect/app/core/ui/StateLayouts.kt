package com.peakconnect.app.core.ui

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Hiking
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.peakconnect.app.R
import com.peakconnect.app.core.theme.PeakConnectTheme
import com.peakconnect.app.core.theme.spacing

@Composable
fun ErrorState(
    message: String,
    modifier: Modifier = Modifier,
    onRetry: (() -> Unit)? = null,
    icon: ImageVector = Icons.Outlined.ErrorOutline
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(MaterialTheme.spacing.xl),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(56.dp)
            )
            Spacer(modifier = Modifier.height(MaterialTheme.spacing.md))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )
            if (onRetry != null) {
                Spacer(modifier = Modifier.height(MaterialTheme.spacing.lg))
                PrimaryButton(
                    text = stringResource(id = R.string.common_retry),
                    onClick = onRetry
                )
            }
        }
    }
}

@Composable
fun EmptyState(
    icon: ImageVector,
    title: String,
    message: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(MaterialTheme.spacing.xl),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(64.dp)
            )
            Spacer(modifier = Modifier.height(MaterialTheme.spacing.md))
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(MaterialTheme.spacing.xs))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            if (actionLabel != null && onAction != null) {
                Spacer(modifier = Modifier.height(MaterialTheme.spacing.lg))
                PrimaryButton(
                    text = actionLabel,
                    onClick = onAction
                )
            }
        }
    }
}

@Preview(name = "Light States", showBackground = true)
@Composable
private fun StatesPreviewLight() {
    PeakConnectTheme(themeMode = com.peakconnect.app.core.theme.ThemeMode.LIGHT) {
        Column(modifier = Modifier.fillMaxSize()) {
            EmptyState(
                icon = Icons.Outlined.Hiking,
                title = "No Bookings Yet",
                message = "Explore upcoming adventures and book your first trek.",
                actionLabel = "Explore Treks",
                onAction = {}
            )
        }
    }
}

@Preview(name = "Dark States", uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true)
@Composable
private fun StatesPreviewDark() {
    PeakConnectTheme(themeMode = com.peakconnect.app.core.theme.ThemeMode.DARK) {
        ErrorState(
            message = "Could not load trek details. Check your internet connection.",
            onRetry = {}
        )
    }
}
