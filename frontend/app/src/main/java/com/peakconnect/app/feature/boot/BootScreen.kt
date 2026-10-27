package com.peakconnect.app.feature.boot

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.WifiOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.peakconnect.app.R
import com.peakconnect.app.core.navigation.AppDestination
import com.peakconnect.app.core.theme.spacing
import com.peakconnect.app.core.ui.Logo
import com.peakconnect.app.core.ui.PrimaryButton
import com.peakconnect.app.core.ui.SecondaryButton
import com.peakconnect.app.core.ui.TertiaryTextButton

private val BootGradientBrush = Brush.verticalGradient(
    colors = listOf(
        Color(0xFF1F6B45),
        Color(0xFF0F4D31)
    )
)

@Composable
fun BootScreen(
    viewModel: BootViewModel,
    onReady: (AppDestination, Boolean) -> Unit,
    onExitApp: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    BackHandler {
        onExitApp()
    }

    when (val state = uiState) {
        is BootUiState.Ready -> {
            onReady(state.destination, state.offlineMode)
        }
        is BootUiState.Checking -> {
            CheckingContent(modifier = modifier)
        }
        is BootUiState.Waking -> {
            WakingContent(
                elapsedSeconds = state.elapsedSeconds,
                modifier = modifier
            )
        }
        is BootUiState.Timeout -> {
            TimeoutContent(
                hasCachedData = state.hasCachedData,
                onRetry = { viewModel.retry() },
                onViewSavedData = { viewModel.continueOffline() },
                onExitApp = onExitApp,
                modifier = modifier
            )
        }
        is BootUiState.Offline -> {
            OfflineContent(
                onRetry = { viewModel.retry() },
                onExitApp = onExitApp,
                modifier = modifier
            )
        }
    }
}

@Composable
private fun CheckingContent(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(BootGradientBrush),
        contentAlignment = Alignment.Center
    ) {
        Logo(size = 96.dp)
    }
}

@Composable
private fun WakingContent(
    elapsedSeconds: Long,
    modifier: Modifier = Modifier
) {
    val minutes = elapsedSeconds / 60
    val seconds = elapsedSeconds % 60
    val timeFormatted = String.format("%d:%02d", minutes, seconds)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(BootGradientBrush)
            .padding(MaterialTheme.spacing.xl),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Logo(size = 80.dp)

            Spacer(modifier = Modifier.height(MaterialTheme.spacing.lg))

            Text(
                text = stringResource(id = R.string.boot_waking_title),
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                color = Color.White,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(MaterialTheme.spacing.sm))

            Text(
                text = stringResource(id = R.string.boot_waking_body),
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White.copy(alpha = 0.85f),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(MaterialTheme.spacing.xl))

            CircularProgressIndicator(
                modifier = Modifier.size(44.dp),
                color = Color(0xFFF28C28),
                strokeWidth = 3.dp
            )

            Spacer(modifier = Modifier.height(MaterialTheme.spacing.md))

            Text(
                text = stringResource(id = R.string.boot_waiting_timer, timeFormatted),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                color = Color.White
            )

            Spacer(modifier = Modifier.height(MaterialTheme.spacing.xs))

            Text(
                text = stringResource(id = R.string.boot_automatic_entry),
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.70f),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun TimeoutContent(
    hasCachedData: Boolean,
    onRetry: () -> Unit,
    onViewSavedData: () -> Unit,
    onExitApp: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(BootGradientBrush)
            .padding(MaterialTheme.spacing.lg),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Logo(size = 72.dp)

            Spacer(modifier = Modifier.height(MaterialTheme.spacing.md))

            Text(
                text = stringResource(id = R.string.boot_timeout_title),
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                color = Color.White,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(MaterialTheme.spacing.sm))

            Text(
                text = stringResource(id = R.string.boot_timeout_body),
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.85f),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(MaterialTheme.spacing.xl))

            PrimaryButton(
                text = stringResource(id = R.string.common_retry),
                onClick = onRetry
            )

            if (hasCachedData) {
                Spacer(modifier = Modifier.height(MaterialTheme.spacing.sm))
                SecondaryButton(
                    text = stringResource(id = R.string.boot_action_view_saved),
                    onClick = onViewSavedData
                )
            }

            Spacer(modifier = Modifier.height(MaterialTheme.spacing.sm))
            TertiaryTextButton(
                text = stringResource(id = R.string.boot_action_close_app),
                onClick = onExitApp
            )
        }
    }
}

@Composable
private fun OfflineContent(
    onRetry: () -> Unit,
    onExitApp: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(BootGradientBrush)
            .padding(MaterialTheme.spacing.lg),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Outlined.WifiOff,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(64.dp)
            )

            Spacer(modifier = Modifier.height(MaterialTheme.spacing.md))

            Text(
                text = stringResource(id = R.string.boot_offline_title),
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                color = Color.White,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(MaterialTheme.spacing.sm))

            Text(
                text = stringResource(id = R.string.boot_offline_body),
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.85f),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(MaterialTheme.spacing.xl))

            PrimaryButton(
                text = stringResource(id = R.string.common_retry),
                onClick = onRetry
            )

            Spacer(modifier = Modifier.height(MaterialTheme.spacing.sm))

            TertiaryTextButton(
                text = stringResource(id = R.string.boot_action_close_app),
                onClick = onExitApp
            )
        }
    }
}
