package com.peakconnect.app.core.ui

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Landscape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil3.compose.SubcomposeAsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.peakconnect.app.R
import com.peakconnect.app.core.theme.PeakConnectTheme
import com.peakconnect.app.core.theme.spacing

@Composable
fun ImageFallbackBox(modifier: Modifier = Modifier) {
    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                        MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Outlined.Landscape,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                modifier = Modifier.size(36.dp)
            )
            if (this@BoxWithConstraints.maxWidth >= 120.dp) {
                Spacer(modifier = Modifier.height(MaterialTheme.spacing.xs))
                Text(
                    text = stringResource(id = R.string.common_image_unavailable),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun AppAsyncImage(
    url: String?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop
) {
    if (url.isNullOrBlank()) {
        ImageFallbackBox(modifier = modifier)
    } else {
        val context = LocalContext.current
        SubcomposeAsyncImage(
            model = ImageRequest.Builder(context)
                .data(url)
                .crossfade(300)
                .build(),
            contentDescription = contentDescription,
            modifier = modifier,
            contentScale = contentScale,
            loading = {
                ShimmerBox(modifier = Modifier.fillMaxSize())
            },
            error = {
                ImageFallbackBox(modifier = Modifier.fillMaxSize())
            }
        )
    }
}

@Preview(name = "Light Fallback Box (Wide)", showBackground = true)
@Composable
private fun FallbackBoxWidePreviewLight() {
    PeakConnectTheme(themeMode = com.peakconnect.app.core.theme.ThemeMode.LIGHT) {
        Box(modifier = Modifier.size(200.dp, 150.dp)) {
            ImageFallbackBox()
        }
    }
}

@Preview(name = "Dark Fallback Box (Small)", uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true)
@Composable
private fun FallbackBoxSmallPreviewDark() {
    PeakConnectTheme(themeMode = com.peakconnect.app.core.theme.ThemeMode.DARK) {
        Box(modifier = Modifier.size(80.dp, 80.dp)) {
            ImageFallbackBox()
        }
    }
}
