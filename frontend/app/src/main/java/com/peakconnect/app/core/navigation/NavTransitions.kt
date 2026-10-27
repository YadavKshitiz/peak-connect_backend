package com.peakconnect.app.core.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.navigation.NavBackStackEntry

const val NAV_TRANSITION_DURATION_MS = 280

fun AnimatedContentTransitionScope<NavBackStackEntry>.defaultEnterTransition(): EnterTransition {
    return fadeIn(animationSpec = tween(NAV_TRANSITION_DURATION_MS)) +
            slideIntoContainer(
                towards = AnimatedContentTransitionScope.SlideDirection.Start,
                animationSpec = tween(NAV_TRANSITION_DURATION_MS)
            )
}

fun AnimatedContentTransitionScope<NavBackStackEntry>.defaultExitTransition(): ExitTransition {
    return fadeOut(animationSpec = tween(NAV_TRANSITION_DURATION_MS)) +
            slideOutOfContainer(
                towards = AnimatedContentTransitionScope.SlideDirection.Start,
                animationSpec = tween(NAV_TRANSITION_DURATION_MS)
            )
}

fun AnimatedContentTransitionScope<NavBackStackEntry>.defaultPopEnterTransition(): EnterTransition {
    return fadeIn(animationSpec = tween(NAV_TRANSITION_DURATION_MS)) +
            slideIntoContainer(
                towards = AnimatedContentTransitionScope.SlideDirection.End,
                animationSpec = tween(NAV_TRANSITION_DURATION_MS)
            )
}

fun AnimatedContentTransitionScope<NavBackStackEntry>.defaultPopExitTransition(): ExitTransition {
    return fadeOut(animationSpec = tween(NAV_TRANSITION_DURATION_MS)) +
            slideOutOfContainer(
                towards = AnimatedContentTransitionScope.SlideDirection.End,
                animationSpec = tween(NAV_TRANSITION_DURATION_MS)
            )
}
