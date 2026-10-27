package com.peakconnect.app

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.peakconnect.app.core.navigation.RootNavHost
import com.peakconnect.app.core.network.ResumeHealthChecker
import com.peakconnect.app.core.session.SessionManager
import com.peakconnect.app.core.theme.PeakConnectTheme
import com.peakconnect.app.core.theme.ThemeMode
import com.peakconnect.app.core.util.LocalIsOnline
import com.peakconnect.app.core.util.NetworkMonitor
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    @Inject
    lateinit var networkMonitor: NetworkMonitor

    @Inject
    lateinit var sessionManager: SessionManager

    @Inject
    lateinit var resumeHealthChecker: ResumeHealthChecker

    @Inject
    lateinit var themePreferences: com.peakconnect.app.core.preferences.ThemePreferences

    private var isAppReady by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        val splashStartTime = System.currentTimeMillis()

        super.onCreate(savedInstanceState)

        // Keep system splash until first frame & session check ready, max 1 second
        splashScreen.setKeepOnScreenCondition {
            val elapsed = System.currentTimeMillis() - splashStartTime
            !isAppReady && elapsed < 1000L
        }

        enableEdgeToEdge()

        setContent {
            val isOnline by networkMonitor.isOnline.collectAsStateWithLifecycle(initialValue = true)
            val themeMode by themePreferences.themeMode.collectAsStateWithLifecycle(initialValue = ThemeMode.SYSTEM)

            LaunchedEffect(Unit) {
                isAppReady = true
            }

            CompositionLocalProvider(LocalIsOnline provides isOnline) {
                PeakConnectTheme(themeMode = themeMode) {
                    RootNavHost(
                        sessionManager = sessionManager,
                        resumeHealthChecker = resumeHealthChecker,
                        onExitApp = { finish() }
                    )
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        resumeHealthChecker.onAppForegrounded(lifecycleScope)
    }

    override fun onStop() {
        super.onStop()
        resumeHealthChecker.onAppBackgrounded()
    }
}
