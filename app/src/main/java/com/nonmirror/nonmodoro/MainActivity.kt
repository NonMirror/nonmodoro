package com.nonmirror.nonmodoro

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nonmirror.nonmodoro.core.Nomo
import com.nonmirror.nonmodoro.core.NomoTheme
import com.nonmirror.nonmodoro.data.AppIcon
import com.nonmirror.nonmodoro.timer.Alerts
import com.nonmirror.nonmodoro.timer.TimerService
import com.nonmirror.nonmodoro.timer.TimerStatus
import com.nonmirror.nonmodoro.ui.OnboardingScreen
import com.nonmirror.nonmodoro.ui.RootScreen
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {

    private val permissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        maybeAskForNotifications()

        val app = application as NomoApp

        // Fresh installs (and restored backups) can disagree with the saved
        // preference, so make the manifest match before anything is drawn.
        val savedIcon = app.settings.current.appIcon
        if (!AppIcon.isApplied(this, savedIcon)) AppIcon.apply(this, savedIcon)

        setContent {
            val settings by app.settings.state.collectAsStateWithLifecycle()
            val timer by app.engine.state.collectAsStateWithLifecycle()
            val records by app.history.records.collectAsStateWithLifecycle()

            NomoTheme(mode = settings.theme) {
                val colors = Nomo.colors

                KeepScreenOn(enabled = settings.keepScreenOn && timer.isRunning)
                ServiceBinding(status = timer.status)

                Surface(color = colors.paper, modifier = Modifier.fillMaxSize()) {
                    Column(
                        Modifier
                            .fillMaxSize()
                            .background(colors.paper)
                            .systemBarsPadding(),
                    ) {
                        if (!settings.onboarded) {
                            OnboardingScreen(
                                onFinish = { focus, short, long, cadence ->
                                    app.settings.update {
                                        it.copy(
                                            focusMinutes = focus,
                                            shortBreakMinutes = short,
                                            longBreakMinutes = long,
                                            sessionsBeforeLongBreak = cadence,
                                            onboarded = true,
                                        )
                                    }
                                },
                            )
                        } else {
                            RootScreen(
                                settings = settings,
                                timer = timer,
                                records = records,
                                onToggle = { app.engine.toggle() },
                                onSkip = { app.engine.skip() },
                                onExtend = { app.engine.extend(5) },
                                onReset = { app.engine.reset() },
                                onSettingsChange = { transform -> app.settings.update(transform) },
                                onClearHistory = { app.clearHistory() },
                                onPreviewSound = { sound ->
                                    Alerts.playCompletion(this@MainActivity, sound)
                                },
                            )
                        }
                    }
                }
            }
        }
    }

    private fun maybeAskForNotifications() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        val granted = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.POST_NOTIFICATIONS,
        ) == PackageManager.PERMISSION_GRANTED
        if (!granted) permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
    }
}

@Composable
private fun KeepScreenOn(enabled: Boolean) {
    val activity = LocalContext.current as? Activity
    DisposableEffect(enabled) {
        if (enabled) {
            activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
            activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
        onDispose { activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) }
    }
}

/**
 * The foreground service only lives while a block is actually in flight, so the
 * notification never lingers after the user stops the timer.
 */
@Composable
private fun ServiceBinding(status: TimerStatus) {
    val context = LocalContext.current
    LaunchedEffect(status) {
        when (status) {
            TimerStatus.Running, TimerStatus.Paused -> TimerService.ensureRunning(context)
            TimerStatus.Finished -> {
                // Give an auto-start a moment to kick in before tearing down.
                delay(1200)
                TimerService.shutdown(context)
            }
            TimerStatus.Idle -> TimerService.shutdown(context)
        }
    }
}
