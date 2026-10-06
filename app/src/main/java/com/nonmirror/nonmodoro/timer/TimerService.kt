package com.nonmirror.nonmodoro.timer

import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.nonmirror.nonmodoro.NomoApp
import com.nonmirror.nonmodoro.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * Keeps the countdown alive while the app is in the background and mirrors it
 * into an ongoing notification with pause / skip / stop actions.
 */
class TimerService : Service() {

    companion object {
        private const val ACTION_SYNC = "com.nonmirror.nonmodoro.action.SYNC"
        private const val ACTION_TOGGLE = "com.nonmirror.nonmodoro.action.TOGGLE"
        private const val ACTION_SKIP = "com.nonmirror.nonmodoro.action.SKIP"
        private const val ACTION_STOP = "com.nonmirror.nonmodoro.action.STOP"
        private const val NOTIFICATION_ID = 1001

        fun ensureRunning(context: Context) {
            val intent = Intent(context, TimerService::class.java).setAction(ACTION_SYNC)
            ContextCompat.startForegroundService(context, intent)
        }

        fun shutdown(context: Context) {
            context.stopService(Intent(context, TimerService::class.java))
        }

        private fun command(context: Context, action: String) {
            val intent = Intent(context, TimerService::class.java).setAction(action)
            runCatching { context.startService(intent) }
        }

        fun toggle(context: Context) = command(context, ACTION_TOGGLE)
        fun skip(context: Context) = command(context, ACTION_SKIP)
        fun stopTimer(context: Context) = command(context, ACTION_STOP)
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private lateinit var app: NomoApp
    private var watcher: Job? = null
    private var foregroundStarted = false

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        app = application as NomoApp
        // Promote immediately: Android requires foreground within a few seconds.
        promote(app.engine.state.value)
        watcher = scope.launch {
            app.engine.state.collectLatest { state ->
                if (state.status == TimerStatus.Idle) {
                    demote()
                    stopSelf()
                } else {
                    promote(state)
                }
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_TOGGLE -> app.engine.toggle()
            ACTION_SKIP -> app.engine.skip()
            ACTION_STOP -> app.engine.reset()
            else -> Unit
        }
        promote(app.engine.state.value)
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        watcher?.cancel()
        scope.cancel()
        super.onDestroy()
    }

    private fun promote(state: TimerState) {
        val notification = build(state)
        if (!foregroundStarted) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ServiceCompat.startForeground(
                    this,
                    NOTIFICATION_ID,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE,
                )
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }
            foregroundStarted = true
        } else {
            notifyCompat(this).notify(NOTIFICATION_ID, notification)
        }
    }

    private fun demote() {
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        foregroundStarted = false
    }

    private fun build(state: TimerState): android.app.Notification {
        val running = state.status == TimerStatus.Running
        val minutes = state.remainingMillis / 60_000L
        val seconds = (state.remainingMillis / 1000L) % 60L
        val clock = "%02d:%02d".format(minutes, seconds)

        val builder = NotificationCompat.Builder(this, Alerts.CHANNEL_TIMER)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(phaseTitle(state))
            .setContentText(
                when (state.status) {
                    TimerStatus.Paused -> "Paused · $clock left"
                    TimerStatus.Finished -> "Block complete"
                    else -> "Focus on this block"
                },
            )
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setShowWhen(false)
            .setContentIntent(NotificationTap.activity(this))
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .addAction(0, if (running) "Pause" else "Resume", serviceIntent(ACTION_TOGGLE, 11))
            .addAction(0, "Skip", serviceIntent(ACTION_SKIP, 12))
            .addAction(0, "Stop", serviceIntent(ACTION_STOP, 13))

        if (running) {
            builder
                .setUsesChronometer(true)
                .setChronometerCountDown(true)
                .setShowWhen(true)
                .setWhen(System.currentTimeMillis() + state.remainingMillis)
        }
        return builder.build()
    }

    private fun serviceIntent(action: String, requestCode: Int): PendingIntent {
        val intent = Intent(this, TimerService::class.java).setAction(action)
        return PendingIntent.getService(
            this,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }
}
