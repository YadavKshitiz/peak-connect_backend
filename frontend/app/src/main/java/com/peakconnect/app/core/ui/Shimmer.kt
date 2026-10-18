package com.peakconnect.app.core.ui

import android.content.res.Configuration
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.peakconnect.app.core.theme.AppShapes
import com.peakconnect.app.core.theme.PeakConnectTheme
import com.peakconnect.app.core.theme.spacing

fun Modifier.shimmer(): Modifier = composed {
    val transition = rememberInfiniteTransition(label = "shimmerTransition")
    val translateAnim = transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmerTranslation"
    )

    val surfaceVariant = MaterialTheme.colorScheme.surfaceVariant
    val shimmerColors = listOf(
        surfaceVariant.copy(alpha = 0.6f),
        surfaceVariant.copy(alpha = 0.2f),
        surfaceVariant.copy(alpha = 0.6f)
    )

    background(
        brush = Brush.linearGradient(
            colors = shimmerColors,
            start = Offset(translateAnim.value - 400f, translateAnim.value - 400f),
            end = Offset(translateAnim.value, translateAnim.value)
        )
    )
}

@Composable
fun ShimmerBox(
    modifier: Modifier = Modifier,
    shape: Shape = AppShapes.medium
) {
    Box(
        modifier = modifier
            .clip(shape)
            .shimmer()
    )
}

@Composable
fun SkeletonCard(
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = AppShapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column {
            ShimmerBox(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp),
                shape = AppShapes.medium
            )
            Column(modifier = Modifier.padding(MaterialTheme.spacing.lg)) {
                ShimmerBox(
                    modifier = Modifier
                        .fillMaxWidth(0.7f)
                        .height(20.dp),
                    shape = AppShapes.extraSmall
                )
                Spacer(modifier = Modifier.height(MaterialTheme.spacing.sm))
                ShimmerBox(
                    modifier = Modifier
                        .fillMaxWidth(0.9f)
                        .height(14.dp),
                    shape = AppShapes.extraSmall
                )
                Spacer(modifier = Modifier.height(MaterialTheme.spacing.xs))
                ShimmerBox(
                    modifier = Modifier
                        .fillMaxWidth(0.4f)
                        .height(14.dp),
                    shape = AppShapes.extraSmall
                )
                Spacer(modifier = Modifier.height(MaterialTheme.spacing.md))
                ShimmerBox(
                    modifier = Modifier
                        .width(100.dp)
                        .height(28.dp),
                    shape = AppShapes.small
                )
            }
        }
    }
}

@Preview(name = "Light SkeletonCard", showBackground = true)
@Composable
private fun SkeletonCardPreviewLight() {
    PeakConnectTheme(themeMode = com.peakconnect.app.core.theme.ThemeMode.LIGHT) {
        Box(modifier = Modifier.padding(16.dp)) {
            SkeletonCard()
        }
    }
}

@Preview(name = "Dark SkeletonCard", uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true)
@Composable
private fun SkeletonCardPreviewDark() {
    PeakConnectTheme(themeMode = com.peakconnect.app.core.theme.ThemeMode.DARK) {
        Box(modifier = Modifier.padding(16.dp)) {
            SkeletonCard()
        }
    }
}
