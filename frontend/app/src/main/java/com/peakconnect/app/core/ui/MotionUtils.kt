package com.peakconnect.app.core.ui

import android.content.Context
import android.provider.Settings
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext

object MotionUtils {
    fun areAnimationsDisabled(context: Context): Boolean {
        return try {
            val scale = Settings.Global.getFloat(
                context.contentResolver,
                Settings.Global.ANIMATOR_DURATION_SCALE,
                1.0f
            )
            scale == 0f
        } catch (_: Exception) {
            false
        }
    }
}

/**
 * Applies a subtle press animation (scale down to [targetScale], default 0.98f)
 * when pressed, respecting the system "remove animations" setting.
 */
@Composable
fun Modifier.pressScale(
    interactionSource: InteractionSource,
    targetScale: Float = 0.98f
): Modifier {
    val context = LocalContext.current
    val animationsDisabled = remember(context) { MotionUtils.areAnimationsDisabled(context) }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed && !animationsDisabled) targetScale else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "pressScaleAnimation"
    )

    return this.graphicsLayer {
        scaleX = scale
        scaleY = scale
    }
}
