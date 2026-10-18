package com.peakconnect.app.core.ui

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.peakconnect.app.R
import com.peakconnect.app.core.theme.PeakConnectTheme
import com.peakconnect.app.core.theme.spacing

@Composable
fun LoadingIndicator(
    modifier: Modifier = Modifier,
    message: String? = null
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(44.dp),
                color = MaterialTheme.colorScheme.primary,
                strokeWidth = 3.5.dp
            )
            if (message != null) {
                Spacer(modifier = Modifier.height(MaterialTheme.spacing.md))
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Preview(name = "Light Loading", showBackground = true)
@Composable
private fun LoadingIndicatorPreviewLight() {
    PeakConnectTheme(themeMode = com.peakconnect.app.core.theme.ThemeMode.LIGHT) {
        LoadingIndicator(message = stringResource(id = R.string.common_loading))
    }
}

@Preview(name = "Dark Loading", uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true)
@Composable
private fun LoadingIndicatorPreviewDark() {
    PeakConnectTheme(themeMode = com.peakconnect.app.core.theme.ThemeMode.DARK) {
        LoadingIndicator()
    }
}
