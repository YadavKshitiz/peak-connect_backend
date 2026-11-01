package com.peakconnect.app.core.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.peakconnect.app.R
import com.peakconnect.app.core.util.LocalIsOnline
import kotlinx.coroutines.delay

private val BackOnlineGreen = Color(0xFF1F6B45)

/**
 * Slim animated banner shown at the top of the main scaffold.
 * Displays "You're offline. Showing saved data." when offline.
 * Displays a brief 2-second green "Back online" banner when network connection returns.
 */
@Composable
fun GlobalOfflineBanner(
    modifier: Modifier = Modifier,
    isOnline: Boolean = LocalIsOnline.current,
    onBackOnline: () -> Unit = {}
) {
    val currentOnBackOnline by rememberUpdatedState(onBackOnline)
    var wasOffline by remember { mutableStateOf(false) }
    var showBackOnline by remember { mutableStateOf(false) }

    LaunchedEffect(isOnline) {
        if (!isOnline) {
            wasOffline = true
            showBackOnline = false
        } else if (wasOffline) {
            wasOffline = false
            showBackOnline = true
            currentOnBackOnline()
            delay(2000L)
            showBackOnline = false
        }
    }

    val isVisible = !isOnline || showBackOnline

    AnimatedVisibility(
        visible = isVisible,
        enter = expandVertically() + fadeIn(),
        exit = shrinkVertically() + fadeOut(),
        modifier = modifier
    ) {
        val (backgroundColor, contentColor, icon, textRes) = if (!isOnline) {
            FourValues(
                MaterialTheme.colorScheme.surfaceVariant,
                MaterialTheme.colorScheme.onSurfaceVariant,
                Icons.Default.CloudOff,
                R.string.global_offline_banner
            )
        } else {
            FourValues(
                BackOnlineGreen,
                Color.White,
                Icons.Default.CloudDone,
                R.string.global_back_online_banner
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(backgroundColor)
                .padding(horizontal = 16.dp, vertical = 6.dp)
                .testTag(if (!isOnline) "global_offline_banner" else "global_back_online_banner"),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = stringResource(id = textRes),
                style = MaterialTheme.typography.labelMedium,
                color = contentColor
            )
        }
    }
}

private data class FourValues<A, B, C, D>(
    val first: A,
    val second: B,
    val third: C,
    val fourth: D
)
