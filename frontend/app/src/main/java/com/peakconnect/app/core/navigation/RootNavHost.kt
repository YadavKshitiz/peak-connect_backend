package com.peakconnect.app.core.navigation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.peakconnect.app.R
import com.peakconnect.app.core.network.ResumeHealthChecker
import com.peakconnect.app.core.session.SessionEvent
import com.peakconnect.app.core.session.SessionEvents
import com.peakconnect.app.core.session.SessionManager
import com.peakconnect.app.core.ui.BannerType
import com.peakconnect.app.core.ui.InfoBanner
import com.peakconnect.app.feature.admin.AdminUnsupportedScreen
import com.peakconnect.app.feature.boot.BootScreen
import com.peakconnect.app.feature.onboarding.OnboardingScreen
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@Composable
fun RootNavHost(
    sessionManager: SessionManager,
    resumeHealthChecker: ResumeHealthChecker,
    onExitApp: () -> Unit,
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController()
) {
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val isServerWakingUp by resumeHealthChecker.isServerWakingUp.collectAsStateWithLifecycle()
    val sessionExpiredMsg = stringResource(id = R.string.session_expired_message)

    // Global listener for SessionExpired events
    LaunchedEffect(Unit) {
        SessionEvents.events.collectLatest { event ->
            if (event is SessionEvent.SessionExpired) {
                sessionManager.logout()
                navController.navigate(LoginRoute) {
                    popUpTo(0) { inclusive = true }
                }
                snackbarHostState.showSnackbar(sessionExpiredMsg)
            }
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Non-blocking InfoBanner when server is waking up after 10+ min background resume
            AnimatedVisibility(visible = isServerWakingUp) {
                InfoBanner(
                    type = BannerType.INFO,
                    text = stringResource(id = R.string.banner_server_waking_retry),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Box(modifier = Modifier.weight(1f)) {
                NavHost(
                    navController = navController,
                    startDestination = BootRoute,
                    modifier = Modifier.fillMaxSize(),
                    enterTransition = { defaultEnterTransition() },
                    exitTransition = { defaultExitTransition() },
                    popEnterTransition = { defaultPopEnterTransition() },
                    popExitTransition = { defaultPopExitTransition() }
                ) {
                    composable<BootRoute> {
                        BootScreen(
                            viewModel = hiltViewModel(),
                            onReady = { destination, _ ->
                                val targetRoute: Any = when (destination) {
                                    AppDestination.Onboarding -> OnboardingRoute
                                    AppDestination.Login -> LoginRoute
                                    AppDestination.TrekkerHome -> TrekkerGraphRoute
                                    AppDestination.GuideHome -> GuideGraphRoute
                                    AppDestination.AdminUnsupported -> AdminUnsupportedRoute
                                }
                                navController.navigate(targetRoute) {
                                    popUpTo(BootRoute) { inclusive = true }
                                }
                            },
                            onExitApp = onExitApp
                        )
                    }

                    composable<OnboardingRoute> {
                        OnboardingScreen(
                            viewModel = hiltViewModel(),
                            onFinish = {
                                navController.navigate(LoginRoute) {
                                    popUpTo(OnboardingRoute) { inclusive = true }
                                }
                            }
                        )
                    }

                    composable<LoginRoute> {
                        PlaceholderScreen(title = "Login")
                    }

                    composable<RegisterRoute> {
                        PlaceholderScreen(title = "Register")
                    }

                    composable<TrekkerGraphRoute> {
                        TrekkerNavShell()
                    }

                    composable<GuideGraphRoute> {
                        GuideNavShell()
                    }

                    composable<AdminUnsupportedRoute> {
                        AdminUnsupportedScreen(
                            onLogout = {
                                scope.launch {
                                    sessionManager.logout()
                                    navController.navigate(LoginRoute) {
                                        popUpTo(0) { inclusive = true }
                                    }
                                }
                            }
                        )
                    }

                    // --- Pre-defined Future Feature Routes for Later Tasks ---
                    composable<ActivityDetailRoute> { backStackEntry ->
                        val route = backStackEntry.toRoute<ActivityDetailRoute>()
                        PlaceholderScreen(title = "Activity Detail (${route.activityId})")
                    }

                    composable<GuideMatchesRoute> { backStackEntry ->
                        val route = backStackEntry.toRoute<GuideMatchesRoute>()
                        PlaceholderScreen(title = "Guide Matches (${route.slotId})")
                    }

                    composable<BookingReviewRoute> { backStackEntry ->
                        val route = backStackEntry.toRoute<BookingReviewRoute>()
                        PlaceholderScreen(title = "Booking Review (${route.slotId})")
                    }

                    composable<BookingStatusRoute> { backStackEntry ->
                        val route = backStackEntry.toRoute<BookingStatusRoute>()
                        PlaceholderScreen(title = "Booking Status (${route.bookingId})")
                    }

                    composable<CancelBookingRoute> { backStackEntry ->
                        val route = backStackEntry.toRoute<CancelBookingRoute>()
                        PlaceholderScreen(title = "Cancel Booking (${route.bookingId})")
                    }

                    composable<PaymentResultRoute> { backStackEntry ->
                        val route = backStackEntry.toRoute<PaymentResultRoute>()
                        PlaceholderScreen(title = "Payment Result (${route.bookingId})")
                    }

                    composable<WaitlistRoute> {
                        PlaceholderScreen(title = "Waitlist")
                    }

                    composable<NotificationsRoute> {
                        PlaceholderScreen(title = "Notifications")
                    }

                    composable<SettingsRoute> {
                        PlaceholderScreen(title = "Settings")
                    }

                    composable<GuideProfileEditRoute> {
                        PlaceholderScreen(title = "Guide Profile Edit")
                    }

                    composable<MapPickerRoute> {
                        PlaceholderScreen(title = "Map Picker")
                    }
                }
            }
        }
    }
}
