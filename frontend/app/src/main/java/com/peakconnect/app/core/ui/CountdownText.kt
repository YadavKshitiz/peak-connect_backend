package com.peakconnect.app.core.ui

import android.content.res.Configuration
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import com.peakconnect.app.R
import com.peakconnect.app.core.theme.PeakConnectTheme
import java.util.Locale

fun formatCountdown(remainingMillis: Long): String {
    val totalSeconds = maxOf(0L, remainingMillis / 1000)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format(Locale.US, "%02d:%02d", minutes, seconds)
}

@Composable
fun CountdownText(
    remainingMillis: Long,
    modifier: Modifier = Modifier,
    isApprox: Boolean = false,
    style: TextStyle = MaterialTheme.typography.titleMedium
) {
    val timeString = formatCountdown(remainingMillis)
    val displayText = if (isApprox) {
        stringResource(id = R.string.common_approx, timeString)
    } else {
        timeString
    }

    Text(
        text = displayText,
        modifier = modifier,
        style = style,
        color = MaterialTheme.colorScheme.onSurface
    )
}

@Preview(name = "Light Countdown", showBackground = true)
@Composable
private fun CountdownTextPreviewLight() {
    PeakConnectTheme(themeMode = com.peakconnect.app.core.theme.ThemeMode.LIGHT) {
        CountdownText(remainingMillis = 150000L)
    }
}

@Preview(name = "Dark Approx Countdown", uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true)
@Composable
private fun CountdownTextPreviewDark() {
    PeakConnectTheme(themeMode = com.peakconnect.app.core.theme.ThemeMode.DARK) {
        CountdownText(remainingMillis = 600000L, isApprox = true)
    }
}
