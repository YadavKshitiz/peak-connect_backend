package com.peakconnect.app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.peakconnect.app.core.navigation.RootNavHost
import com.peakconnect.app.core.network.ResumeHealthChecker
import com.peakconnect.app.core.notifications.LocalNotificationPoster
import com.peakconnect.app.core.notifications.NotificationPermissionState
import com.peakconnect.app.core.notifications.NotificationRationaleDialog
import com.peakconnect.app.core.session.SessionManager
import com.peakconnect.app.core.session.SessionState
import com.peakconnect.app.core.theme.PeakConnectTheme
import com.peakconnect.app.core.theme.ThemeMode
import com.peakconnect.app.core.util.LocalIsOnline
import com.peakconnect.app.core.util.NetworkMonitor
import com.peakconnect.app.feature.notifications.sync.NotificationWorkScheduler
import com.peakconnect.app.feature.payment.PaymentEvent
import com.peakconnect.app.feature.payment.PaymentEventBus
import com.razorpay.ExternalWalletListener
import com.razorpay.PaymentData
import com.razorpay.PaymentResultWithDataListener
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity(), PaymentResultWithDataListener, ExternalWalletListener {

    @Inject
    lateinit var networkMonitor: NetworkMonitor

    @Inject
    lateinit var sessionManager: SessionManager

    @Inject
    lateinit var resumeHealthChecker: ResumeHealthChecker

    @Inject
    lateinit var themePreferences: com.peakconnect.app.core.preferences.ThemePreferences

    @Inject
    lateinit var paymentEventBus: PaymentEventBus

    @Inject
    lateinit var notificationWorkScheduler: NotificationWorkScheduler

    @Inject
    lateinit var networkRefreshNotifier: com.peakconnect.app.core.network.NetworkRefreshNotifier

    @Inject
    lateinit var wakeUpRetryHelper: com.peakconnect.app.core.network.WakeUpRetryHelper

    private var isAppReady by mutableStateOf(false)
    private var pendingBookingId by mutableStateOf<String?>(null)
    private var pendingIsGuide by mutableStateOf(false)

    override fun onPaymentSuccess(razorpayPaymentId: String?, paymentData: PaymentData?) {
        val paymentId = razorpayPaymentId ?: paymentData?.paymentId.orEmpty()
        val orderId = paymentData?.orderId
        if (BuildConfig.DEBUG) {
            Log.d("MainActivity", "Razorpay payment success: paymentId=$paymentId, orderId=$orderId")
        }
        paymentEventBus.post(PaymentEvent.Success(paymentId = paymentId, orderId = orderId))
    }

    override fun onPaymentError(errorCode: Int, response: String?, paymentData: PaymentData?) {
        if (BuildConfig.DEBUG) {
            Log.d("MainActivity", "Razorpay payment error: code=$errorCode, response=$response")
        }
        paymentEventBus.post(PaymentEvent.Failure(code = errorCode, message = response))
    }

    override fun onExternalWalletSelected(walletName: String?, paymentData: PaymentData?) {
        if (BuildConfig.DEBUG) {
            Log.d("MainActivity", "Razorpay external wallet selected: $walletName")
        }
        paymentEventBus.post(PaymentEvent.ExternalWallet(walletName = walletName))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        val splashStartTime = System.currentTimeMillis()

        super.onCreate(savedInstanceState)

        // Read intent extras from notification deep links
        pendingBookingId = intent?.getStringExtra(LocalNotificationPoster.EXTRA_BOOKING_ID)
        pendingIsGuide = intent?.getBooleanExtra(LocalNotificationPoster.EXTRA_IS_GUIDE, false) ?: false

        // Keep system splash until first frame & session check ready, max 1 second
        splashScreen.setKeepOnScreenCondition {
            val elapsed = System.currentTimeMillis() - splashStartTime
            !isAppReady && elapsed < 1000L
        }

        enableEdgeToEdge()

        // Debug-only slow frames logger (Requirement 8)
        if (BuildConfig.DEBUG && Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            window.addOnFrameMetricsAvailableListener(
                { _, frameMetrics, _ ->
                    val durationNs = frameMetrics.getMetric(android.view.FrameMetrics.TOTAL_DURATION)
                    if (durationNs > 33_333_333L) { // > 33ms (dropped 2+ frames @ 60fps)
                        Log.d("PeakConnect_Perf", "Slow frame detected: ${durationNs / 1_000_000}ms")
                    }
                },
                android.os.Handler(android.os.Looper.getMainLooper())
            )
        }

        setContent {
            val isOnline by networkMonitor.isOnline.collectAsStateWithLifecycle(initialValue = true)
            val themeMode by themePreferences.themeMode.collectAsStateWithLifecycle(initialValue = ThemeMode.SYSTEM)
            val sessionState by sessionManager.sessionState.collectAsStateWithLifecycle()
            val context = LocalContext.current

            var showRationaleDialog by remember { mutableStateOf(false) }

            val permissionLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.RequestPermission(),
                onResult = { isGranted ->
                    if (isGranted) {
                        notificationWorkScheduler.schedulePeriodicSync()
                    }
                }
            )

            // Contextual notification permission prompt after login (Android 13+)
            LaunchedEffect(sessionState) {
                if (sessionState is SessionState.LoggedIn && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    if (!NotificationPermissionState.hasPromptedThisLaunch) {
                        val isGranted = ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.POST_NOTIFICATIONS
                        ) == PackageManager.PERMISSION_GRANTED
                        if (!isGranted) {
                            showRationaleDialog = true
                        }
                    }
                }
            }

            if (showRationaleDialog) {
                NotificationRationaleDialog(
                    onAllow = {
                        showRationaleDialog = false
                        NotificationPermissionState.hasPromptedThisLaunch = true
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                    },
                    onDismiss = {
                        showRationaleDialog = false
                        NotificationPermissionState.hasPromptedThisLaunch = true
                    }
                )
            }

            LaunchedEffect(Unit) {
                isAppReady = true
            }

            CompositionLocalProvider(LocalIsOnline provides isOnline) {
                PeakConnectTheme(themeMode = themeMode) {
                    RootNavHost(
                        sessionManager = sessionManager,
                        resumeHealthChecker = resumeHealthChecker,
                        networkRefreshNotifier = networkRefreshNotifier,
                        wakeUpRetryHelper = wakeUpRetryHelper,
                        onExitApp = { finish() },
                        pendingBookingId = pendingBookingId,
                        pendingIsGuide = pendingIsGuide,
                        onClearPendingBooking = { pendingBookingId = null }
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        pendingBookingId = intent.getStringExtra(LocalNotificationPoster.EXTRA_BOOKING_ID)
        pendingIsGuide = intent.getBooleanExtra(LocalNotificationPoster.EXTRA_IS_GUIDE, false)
    }

    override fun onStart() {
        super.onStart()
        resumeHealthChecker.onAppForegrounded(lifecycleScope)
        notificationWorkScheduler.scheduleOneTimeSync()
    }

    override fun onStop() {
        super.onStop()
        resumeHealthChecker.onAppBackgrounded()
    }
}
