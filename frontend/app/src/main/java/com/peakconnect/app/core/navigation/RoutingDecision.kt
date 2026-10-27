package com.peakconnect.app.core.navigation

import com.peakconnect.app.domain.model.Role

sealed interface AppDestination {
    data object Onboarding : AppDestination
    data object Login : AppDestination
    data object TrekkerHome : AppDestination
    data object GuideHome : AppDestination
    data object AdminUnsupported : AppDestination
}

fun resolveInitialDestination(
    onboardingCompleted: Boolean,
    token: String?,
    isTokenExpired: Boolean,
    role: Role?
): AppDestination {
    if (!onboardingCompleted) {
        return AppDestination.Onboarding
    }
    if (token.isNullOrBlank() || isTokenExpired) {
        return AppDestination.Login
    }
    return when (role) {
        Role.TREKKER -> AppDestination.TrekkerHome
        Role.GUIDE -> AppDestination.GuideHome
        Role.ADMIN -> AppDestination.AdminUnsupported
        null -> AppDestination.Login
    }
}
