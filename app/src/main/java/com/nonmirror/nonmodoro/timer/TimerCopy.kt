package com.nonmirror.nonmodoro.timer

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.nonmirror.nonmodoro.MainActivity
import com.nonmirror.nonmodoro.R

object NotificationTap {
    fun activity(context: Context): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        return PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }
}

fun notifyCompat(context: Context): NotificationManagerCompat =
    NotificationManagerCompat.from(context)

fun smallIcon() = R.drawable.ic_notification

/** Human copy for each phase — kept in one place so the UI and notification agree. */
object TimerCopy {

    data class Copy(
        val label: String,
        val headline: String,
        val primary: String,
        val secondaries: List<String>,
        val header: String,
    )

    fun of(state: TimerState): Copy = when (state.phase) {
        com.nonmirror.nonmodoro.data.Phase.Focus -> when (state.status) {
            TimerStatus.Idle -> Copy(
                label = "Ready",
                headline = "Start one clean focus block.",
                primary = "Start Focus",
                secondaries = emptyList(),
                header = "Your focus timer is ready.",
            )
            TimerStatus.Running -> Copy(
                label = "Focus",
                headline = "In the middle of it.",
                primary = "Pause",
                secondaries = listOf("Skip Block"),
                header = "Focus running. Nothing else matters.",
            )
            TimerStatus.Paused -> Copy(
                label = "Paused",
                headline = "Paused. Pick it back up.",
                primary = "Resume",
                secondaries = listOf("Skip Block"),
                header = "Focus paused.",
            )
            TimerStatus.Finished -> Copy(
                label = "Focus complete",
                headline = "Focus closed. Break when ready.",
                primary = "Start Break",
                secondaries = listOf("Extend 5 Min", "Skip Break"),
                header = "Focus complete. Break when ready.",
            )
        }

        com.nonmirror.nonmodoro.data.Phase.ShortBreak -> when (state.status) {
            TimerStatus.Idle -> Copy(
                label = "Break",
                headline = "Short break. Step away from the desk.",
                primary = "Start Break",
                secondaries = listOf("Skip Break"),
                header = "Quick reset between focus blocks.",
            )
            TimerStatus.Running -> Copy(
                label = "Break",
                headline = "Step away from the desk.",
                primary = "Pause",
                secondaries = listOf("Skip Break"),
                header = "Short break running.",
            )
            TimerStatus.Paused -> Copy(
                label = "Break paused",
                headline = "Break paused.",
                primary = "Resume",
                secondaries = listOf("Skip Break"),
                header = "Short break paused.",
            )
            TimerStatus.Finished -> Copy(
                label = "Break over",
                headline = "Break's over. Back to it.",
                primary = "Start Focus",
                secondaries = listOf("Skip"),
                header = "Break over. Ready for the next block.",
            )
        }

        com.nonmirror.nonmodoro.data.Phase.LongBreak -> when (state.status) {
            TimerStatus.Idle -> Copy(
                label = "Long break",
                headline = "Longer recovery after several sessions.",
                primary = "Start Break",
                secondaries = listOf("Skip Break"),
                header = "You earned a longer break.",
            )
            TimerStatus.Running -> Copy(
                label = "Long break",
                headline = "Go refill your cup.",
                primary = "Pause",
                secondaries = listOf("Skip Break"),
                header = "Long break running.",
            )
            TimerStatus.Paused -> Copy(
                label = "Break paused",
                headline = "Break paused.",
                primary = "Resume",
                secondaries = listOf("Skip Break"),
                header = "Long break paused.",
            )
            TimerStatus.Finished -> Copy(
                label = "Break over",
                headline = "Break's over. Back to it.",
                primary = "Start Focus",
                secondaries = listOf("Skip"),
                header = "Break over. Ready for the next block.",
            )
        }
    }
}

fun phaseTitle(state: TimerState): String = when (state.phase) {
    com.nonmirror.nonmodoro.data.Phase.Focus -> "Focus"
    com.nonmirror.nonmodoro.data.Phase.ShortBreak -> "Short break"
    com.nonmirror.nonmodoro.data.Phase.LongBreak -> "Long break"
}

@Suppress("unused")
fun unusedBuilderRef(context: Context): NotificationCompat.Builder =
    NotificationCompat.Builder(context, Alerts.CHANNEL_TIMER)
