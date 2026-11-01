package com.peakconnect.app.core.ui

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun AnimatedSuccessCheck(
    modifier: Modifier = Modifier,
    size: Dp = 96.dp,
    iconSize: Dp = 56.dp,
    performHaptic: Boolean = true
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val animationsDisabled = remember(context) { MotionUtils.areAnimationsDisabled(context) }
    var animatedStart by remember { mutableStateOf(animationsDisabled) }

    LaunchedEffect(Unit) {
        animatedStart = true
        if (performHaptic) {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        }
    }

    val checkScale by animateFloatAsState(
        targetValue = if (animatedStart || animationsDisabled) 1f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "animatedCheckScale"
    )

    Box(
        modifier = modifier
            .size(size)
            .scale(if (animationsDisabled) 1f else checkScale)
            .background(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = CircleShape
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Rounded.Check,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(iconSize)
        )
    }
}
