package com.nonmirror.nonmodoro.timer

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.getSystemService
import com.nonmirror.nonmodoro.R
import com.nonmirror.nonmodoro.data.AlertSound
import com.nonmirror.nonmodoro.data.Phase

/** Completion sounds, haptics and the one-shot "block finished" notification. */
object Alerts {

    const val CHANNEL_TIMER = "nomo.timer"
    const val CHANNEL_ALERTS = "nomo.alerts"
    const val ID_COMPLETION = 4711

    private var player: MediaPlayer? = null

    fun createChannels(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService<NotificationManager>() ?: return
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_TIMER,
                "Running timer",
                NotificationManager.IMPORTANCE_LOW,
            ).apply {
                description = "Shows the live countdown while a block is running."
                setShowBadge(false)
                enableVibration(false)
            },
        )
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ALERTS,
                "Block finished",
                NotificationManager.IMPORTANCE_HIGH,
            ).apply {
                description = "Tells you when a focus block or break is over."
                enableVibration(false)
            },
        )
    }

    fun playCompletion(context: Context, sound: AlertSound) {
        if (sound == AlertSound.None) return
        val resId = context.resources.getIdentifier(sound.resName, "raw", context.packageName)
        if (resId == 0) return
        runCatching {
            player?.release()
            player = MediaPlayer.create(context, resId)?.apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build(),
                )
                setOnCompletionListener {
                    it.release()
                    if (player === it) player = null
                }
                start()
            }
        }
    }

    fun stopSound() {
        runCatching {
            player?.release()
        }
        player = null
    }

    fun buzz(context: Context, pattern: LongArray = longArrayOf(0, 60, 70, 60)) {
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            context.getSystemService<VibratorManager>()?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService<Vibrator>()
        } ?: return
        if (!vibrator.hasVibrator()) return
        runCatching {
            vibrator.vibrate(VibrationEffect.createWaveform(pattern, -1))
        }
    }

    fun notifyCompletion(context: Context, finished: Phase, nextTitle: String, nextBody: String) {
        val manager = NotificationManagerCompat.from(context)
        if (!manager.areNotificationsEnabled()) return
        val title = when (finished) {
            Phase.Focus -> "Focus complete"
            Phase.ShortBreak -> "Break over"
            Phase.LongBreak -> "Long break over"
        }
        val notification = NotificationCompat.Builder(context, CHANNEL_ALERTS)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(nextBody)
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText(nextBody)
                    .setSummaryText(nextTitle),
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(NotificationTap.activity(context))
            .build()
        runCatching { manager.notify(ID_COMPLETION, notification) }
    }
}
