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
import androidx.compose.runtime.setValue
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
    var showSessionExpiredOnLogin by remember { androidx.compose.runtime.mutableStateOf(false) }

    // Global listener for SessionExpired events
    LaunchedEffect(Unit) {
        SessionEvents.events.collectLatest { event ->
            if (event is SessionEvent.SessionExpired) {
                showSessionExpiredOnLogin = true
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
                        val loginViewModel: com.peakconnect.app.feature.auth.LoginViewModel = hiltViewModel()
                        androidx.compose.runtime.LaunchedEffect(showSessionExpiredOnLogin) {
                            if (showSessionExpiredOnLogin) {
                                loginViewModel.setSessionExpiredNotice(true)
                                showSessionExpiredOnLogin = false
                            }
                        }
                        com.peakconnect.app.feature.auth.LoginScreen(
                            viewModel = loginViewModel,
                            onSuccess = { role ->
                                val targetRoute: Any = when (role) {
                                    com.peakconnect.app.domain.model.Role.TREKKER -> TrekkerGraphRoute
                                    com.peakconnect.app.domain.model.Role.GUIDE -> GuideGraphRoute
                                    com.peakconnect.app.domain.model.Role.ADMIN -> AdminUnsupportedRoute
                                }
                                navController.navigate(targetRoute) {
                                    popUpTo(0) { inclusive = true }
                                }
                            },
                            onNavigateToRegister = {
                                navController.navigate(RegisterRoute)
                            }
                        )
                    }

                    composable<RegisterRoute> {
                        val registerViewModel: com.peakconnect.app.feature.auth.RegisterViewModel = hiltViewModel()
                        com.peakconnect.app.feature.auth.RegisterScreen(
                            viewModel = registerViewModel,
                            onSuccess = { role ->
                                val targetRoute: Any = when (role) {
                                    com.peakconnect.app.domain.model.Role.TREKKER -> TrekkerGraphRoute
                                    com.peakconnect.app.domain.model.Role.GUIDE -> GuideGraphRoute
                                    com.peakconnect.app.domain.model.Role.ADMIN -> AdminUnsupportedRoute
                                }
                                navController.navigate(targetRoute) {
                                    popUpTo(0) { inclusive = true }
                                }
                            },
                            onNavigateToLogin = {
                                navController.navigate(LoginRoute) {
                                    popUpTo(RegisterRoute) { inclusive = true }
                                }
                            }
                        )
                    }

                    composable<TrekkerGraphRoute> {
                        TrekkerNavShell(
                            onNavigateToLogin = {
                                navController.navigate(LoginRoute) {
                                    popUpTo(0) { inclusive = true }
                                }
                            },
                            onNavigateToActivityDetail = { activityId ->
                                navController.navigate(ActivityDetailRoute(activityId))
                            }
                        )
                    }

                    composable<GuideGraphRoute> {
                        GuideNavShell(
                            onNavigateToLogin = {
                                navController.navigate(LoginRoute) {
                                    popUpTo(0) { inclusive = true }
                                }
                            }
                        )
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

                    composable<ActivityDetailRoute> { backStackEntry ->
                        val route = backStackEntry.toRoute<ActivityDetailRoute>()
                        com.peakconnect.app.feature.trekker.detail.ActivityDetailScreen(
                            onBack = { navController.popBackStack() },
                            onNavigateToMatches = { slotId, isWaitlist ->
                                navController.navigate(GuideMatchesRoute(slotId = slotId, isWaitlist = isWaitlist))
                            }
                        )
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
                        com.peakconnect.app.feature.profile.ProfileScreen(
                            viewModel = hiltViewModel(),
                            onNavigateToLogin = {
                                navController.navigate(LoginRoute) {
                                    popUpTo(0) { inclusive = true }
                                }
                            }
                        )
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
