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
import com.peakconnect.app.core.session.SessionState
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
    navController: NavHostController = rememberNavController(),
    networkRefreshNotifier: com.peakconnect.app.core.network.NetworkRefreshNotifier? = null,
    wakeUpRetryHelper: com.peakconnect.app.core.network.WakeUpRetryHelper? = null,
    pendingBookingId: String? = null,
    pendingIsGuide: Boolean = false,
    onClearPendingBooking: () -> Unit = {}
) {
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val isResumeWakingUp by resumeHealthChecker.isServerWakingUp.collectAsStateWithLifecycle()
    val isRetryWakingUp by (wakeUpRetryHelper?.isServerWakingUp ?: kotlinx.coroutines.flow.flowOf(false)).collectAsStateWithLifecycle(initialValue = false)
    val isServerWakingUp = isResumeWakingUp || isRetryWakingUp
    val sessionExpiredMsg = stringResource(id = R.string.session_expired_message)
    var showSessionExpiredOnLogin by remember { androidx.compose.runtime.mutableStateOf(false) }

    // Deep link navigation from local notification
    LaunchedEffect(pendingBookingId) {
        val bId = pendingBookingId
        if (bId != null && sessionManager.sessionState.value is SessionState.LoggedIn) {
            if (pendingIsGuide) {
                navController.navigate(GuideBookingDetailRoute(bookingId = bId))
            } else {
                navController.navigate(BookingStatusRoute(bookingId = bId))
            }
            onClearPendingBooking()
        }
    }

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
            // Global slim animated offline / back-online banner (Requirement 1)
            com.peakconnect.app.core.ui.GlobalOfflineBanner(
                onBackOnline = {
                    networkRefreshNotifier?.notifyBackOnline()
                }
            )

            // Non-blocking InfoBanner when server is waking up after 10+ min background resume or retry (Requirement 6)
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
                            },
                            onNavigateToBookingStatus = { bookingId ->
                                navController.navigate(BookingStatusRoute(bookingId))
                            },
                            onNavigateToPayment = { bookingId ->
                                navController.navigate(PaymentRoute(bookingId = bookingId))
                            },
                            onNotificationClick = {
                                navController.navigate(NotificationsRoute)
                            },
                            onNavigateToAbout = {
                                navController.navigate(AboutRoute)
                            },
                            onNavigateToSafetyTips = {
                                navController.navigate(SafetyTipsRoute)
                            }
                        )
                    }

                    composable<GuideGraphRoute> {
                        GuideNavShell(
                            onNavigateToBookingDetail = { bookingId ->
                                navController.navigate(GuideBookingDetailRoute(bookingId = bookingId))
                            },
                            onNavigateToProfileEdit = {
                                navController.navigate(GuideProfileEditRoute)
                            },
                            onNavigateToLogin = {
                                navController.navigate(LoginRoute) {
                                    popUpTo(0) { inclusive = true }
                                }
                            },
                            onNotificationClick = {
                                navController.navigate(NotificationsRoute)
                            },
                            onNavigateToAbout = {
                                navController.navigate(AboutRoute)
                            },
                            onNavigateToSafetyTips = {
                                navController.navigate(SafetyTipsRoute)
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
                            },
                            onNavigateToSafetyTips = {
                                navController.navigate(SafetyTipsRoute)
                            }
                        )
                    }

                    composable<GuideMatchesRoute> {
                        com.peakconnect.app.feature.trekker.matches.GuideMatchesScreen(
                            onBack = { navController.popBackStack() },
                            onNavigateToReview = { slotId, guideId ->
                                navController.navigate(BookingReviewRoute(slotId = slotId, guideId = guideId))
                            },
                            onNavigateToWaitlist = {
                                navController.navigate(WaitlistRoute) {
                                    popUpTo<GuideMatchesRoute> { inclusive = true }
                                }
                            }
                        )
                    }

                    composable<BookingReviewRoute> {
                        com.peakconnect.app.feature.trekker.review.BookingReviewScreen(
                            onBack = { navController.popBackStack() },
                            onBookingSuccess = { bookingId ->
                                navController.navigate(BookingStatusRoute(bookingId = bookingId)) {
                                    popUpTo<GuideMatchesRoute> { inclusive = true }
                                }
                            },
                            onWaitlistSuccess = {
                                navController.navigate(WaitlistRoute) {
                                    popUpTo<GuideMatchesRoute> { inclusive = true }
                                }
                            },
                            onNavigateBackToActivity = {
                                navController.popBackStack<ActivityDetailRoute>(inclusive = false)
                            },
                            onNavigateBackToMatches = {
                                navController.popBackStack()
                            }
                        )
                    }

                    composable<BookingStatusRoute> {
                        com.peakconnect.app.feature.trekker.booking.BookingStatusScreen(
                            onBack = { navController.popBackStack() },
                            onNavigateToPayment = { bookingId ->
                                navController.navigate(PaymentRoute(bookingId = bookingId))
                            },
                            onNavigateToMatches = { slotId ->
                                navController.navigate(GuideMatchesRoute(slotId = slotId))
                            }
                        )
                    }

                    composable<CancelBookingRoute> { backStackEntry ->
                        val route = backStackEntry.toRoute<CancelBookingRoute>()
                        PlaceholderScreen(title = "Cancel Booking (${route.bookingId})")
                    }

                    composable<PaymentRoute> {
                        com.peakconnect.app.feature.payment.PaymentScreen(
                            onBack = { navController.popBackStack() },
                            onViewBooking = { bookingId ->
                                navController.navigate(BookingStatusRoute(bookingId = bookingId)) {
                                    popUpTo<PaymentRoute> { inclusive = true }
                                }
                            },
                            onExplore = {
                                navController.navigate(TrekkerGraphRoute) {
                                    popUpTo(0) { inclusive = true }
                                }
                            }
                        )
                    }

                    composable<PaymentResultRoute> {
                        com.peakconnect.app.feature.payment.PaymentScreen(
                            onBack = { navController.popBackStack() },
                            onViewBooking = { bookingId ->
                                navController.navigate(BookingStatusRoute(bookingId = bookingId)) {
                                    popUpTo<PaymentResultRoute> { inclusive = true }
                                }
                            },
                            onExplore = {
                                navController.navigate(TrekkerGraphRoute) {
                                    popUpTo(0) { inclusive = true }
                                }
                            }
                        )
                    }

                    composable<WaitlistRoute> {
                        PlaceholderScreen(title = "Waitlist")
                    }

                    composable<NotificationsRoute> {
                        com.peakconnect.app.feature.notifications.NotificationsScreen(
                            onBack = { navController.popBackStack() },
                            onNavigateToBooking = { bookingId, isGuide ->
                                if (isGuide) {
                                    navController.navigate(GuideBookingDetailRoute(bookingId = bookingId))
                                } else {
                                    navController.navigate(BookingStatusRoute(bookingId = bookingId))
                                }
                            }
                        )
                    }

                    composable<SettingsRoute> {
                        com.peakconnect.app.feature.profile.ProfileScreen(
                            viewModel = hiltViewModel(),
                            onNavigateToLogin = {
                                navController.navigate(LoginRoute) {
                                    popUpTo(0) { inclusive = true }
                                }
                            },
                            onNavigateToAbout = {
                                navController.navigate(AboutRoute)
                            },
                            onNavigateToSafetyTips = {
                                navController.navigate(SafetyTipsRoute)
                            }
                        )
                    }

                    composable<AboutRoute> {
                        com.peakconnect.app.feature.about.AboutScreen(
                            onBack = { navController.popBackStack() }
                        )
                    }

                    composable<SafetyTipsRoute> {
                        com.peakconnect.app.feature.safety.SafetyTipsScreen(
                            onBack = { navController.popBackStack() }
                        )
                    }

                    composable<GuideProfileEditRoute> { backStackEntry ->
                        val editViewModel: com.peakconnect.app.feature.guide.profile.GuideProfileEditViewModel = hiltViewModel()
                        val savedStateHandle = backStackEntry.savedStateHandle
                        val pickedLat by savedStateHandle.getStateFlow<Double?>("picked_lat", null).collectAsStateWithLifecycle()
                        val pickedLng by savedStateHandle.getStateFlow<Double?>("picked_lng", null).collectAsStateWithLifecycle()
                        LaunchedEffect(pickedLat, pickedLng) {
                            val lat = pickedLat
                            val lng = pickedLng
                            if (lat != null && lng != null) {
                                editViewModel.setCoordinates(lat, lng)
                                savedStateHandle.remove<Double>("picked_lat")
                                savedStateHandle.remove<Double>("picked_lng")
                            }
                        }
                        com.peakconnect.app.feature.guide.profile.GuideProfileEditScreen(
                            onBack = { navController.popBackStack() },
                            onNavigateToMapPicker = { lat, lng ->
                                navController.navigate(MapPickerRoute(initialLat = lat, initialLng = lng))
                            },
                            viewModel = editViewModel
                        )
                    }

                    composable<MapPickerRoute> {
                        com.peakconnect.app.feature.guide.mappicker.MapPickerScreen(
                            onLocationConfirmed = { lat, lng ->
                                navController.previousBackStackEntry?.savedStateHandle?.set("picked_lat", lat)
                                navController.previousBackStackEntry?.savedStateHandle?.set("picked_lng", lng)
                                navController.popBackStack()
                            },
                            onCancel = { navController.popBackStack() }
                        )
                    }

                    composable<GuideBookingDetailRoute> {
                        com.peakconnect.app.feature.guide.detail.GuideBookingDetailScreen(
                            onBack = { navController.popBackStack() }
                        )
                    }
                }
            }
        }
    }
}
