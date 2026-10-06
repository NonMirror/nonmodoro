package com.nonmirror.nonmodoro.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nonmirror.nonmodoro.R
import com.nonmirror.nonmodoro.core.DotRow
import com.nonmirror.nonmodoro.core.InkArt
import com.nonmirror.nonmodoro.core.Nomo
import com.nonmirror.nonmodoro.core.NomoCard
import com.nonmirror.nonmodoro.core.PillButton
import com.nonmirror.nonmodoro.core.SectionLabel
import com.nonmirror.nonmodoro.core.StatCard
import com.nonmirror.nonmodoro.core.clickableNoRipple
import com.nonmirror.nonmodoro.data.Phase
import com.nonmirror.nonmodoro.data.Settings
import com.nonmirror.nonmodoro.data.Stats
import com.nonmirror.nonmodoro.timer.TimerCopy
import com.nonmirror.nonmodoro.timer.TimerState
import com.nonmirror.nonmodoro.timer.TimerStatus

@Composable
fun TimerScreen(
    state: TimerState,
    settings: Settings,
    stats: Stats,
    onToggle: () -> Unit,
    onSkip: () -> Unit,
    onExtend: () -> Unit,
    onReset: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = Nomo.colors
    val copy = TimerCopy.of(state)

    Column(modifier.fillMaxWidth().padding(horizontal = 18.dp)) {
        Spacer(Modifier.height(6.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Nonmodoro", style = MaterialTheme.typography.headlineMedium, color = c.ink)
                Spacer(Modifier.height(3.dp))
                Text(
                    copy.header,
                    style = MaterialTheme.typography.bodyMedium,
                    color = c.inkSoft,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (state.status != TimerStatus.Idle) {
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(c.card)
                        .clickableNoRipple(onReset)
                        .padding(horizontal = 14.dp, vertical = 9.dp),
                ) {
                    Text("Reset", style = MaterialTheme.typography.bodySmall, color = c.inkSoft)
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        BoxWithConstraints {
            val compact = maxWidth < 340.dp
            val clock = formatClock(state.remainingMillis)
            // Long blocks (h:mm:ss) get a smaller face so the card never overflows.
            val base = if (compact) 52f else 62f
            val timerSize = (base - (clock.length - 5).coerceAtLeast(0) * 6.5f).sp
            val artWidth = if (compact) 104.dp else 124.dp

            NomoCard(shape = RoundedCornerShape(30.dp), padding = PaddingValues(22.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = clock,
                            style = MaterialTheme.typography.displayLarge.copy(fontSize = timerSize),
                            color = c.ink,
                            maxLines = 1,
                            softWrap = false,
                        )
                        Spacer(Modifier.height(8.dp))
                        SectionLabel(copy.label)
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = copy.headline,
                            style = MaterialTheme.typography.headlineSmall,
                            color = c.ink,
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    IllustrationBlock(state = state, width = artWidth)
                }

                Spacer(Modifier.height(16.dp))

                ProgressLine(progress = state.progress, visible = state.status != TimerStatus.Idle)

                Spacer(Modifier.height(18.dp))

                PillButton(
                    text = copy.primary,
                    onClick = onToggle,
                    modifier = Modifier.fillMaxWidth(),
                )

                if (copy.secondaries.isNotEmpty()) {
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        copy.secondaries.forEach { label ->
                            PillButton(
                                text = label,
                                onClick = if (label.startsWith("Extend")) onExtend else onSkip,
                                primary = false,
                                height = 42.dp,
                                horizontalPadding = 20.dp,
                            )
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    DotRow(
                        count = state.cycleLength.coerceAtLeast(1),
                        filled = state.cycleDone % state.cycleLength.coerceAtLeast(1),
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        "Round ${state.cycleDone % state.cycleLength.coerceAtLeast(1) + 1} of ${state.cycleLength}",
                        style = MaterialTheme.typography.bodySmall,
                        color = c.inkFaint,
                    )
                }
            }
        }

        Spacer(Modifier.height(14.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatCard(
                label = "Today",
                value = "${stats.todayMinutes}m",
                sub = if (stats.todaySessions == 1) "1 session" else "${stats.todaySessions} sessions",
                modifier = Modifier.weight(1f),
            )
            StatCard(
                label = "Streak",
                value = "${stats.streakDays}",
                sub = if (stats.streakDays == 1) "day" else "days",
                modifier = Modifier.weight(1f),
            )
        }
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatCard(label = "Last 7 days", value = "", modifier = Modifier.weight(1f)) {
                MiniWeek(stats = stats)
            }
            StatCard(
                label = "Daily goal",
                value = "${goalPercent(stats.todayMinutes, settings.dailyGoalMinutes)}%",
                sub = "${stats.todayMinutes} of ${settings.dailyGoalMinutes} min",
                modifier = Modifier.weight(1f),
            )
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun ProgressLine(progress: Float, visible: Boolean) {
    val c = Nomo.colors
    Box(
        Modifier
            .fillMaxWidth()
            .height(4.dp)
            .clip(RoundedCornerShape(3.dp))
            .background(c.track),
    ) {
        if (visible) {
            Box(
                Modifier
                    .fillMaxWidth(progress.coerceIn(0.008f, 1f))
                    .height(4.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(c.ink),
            )
        }
    }
}

/**
 * Draws the illustration that matches the current state over the timer card's
 * soft peach glow and ground shadow. The artwork stays transparent so the ink
 * can sit naturally on both the light and dark paper themes.
 */
@Composable
private fun IllustrationBlock(state: TimerState, width: Dp) {
    val c = Nomo.colors
    val height = width * 0.75f                       // the artwork is 1024 x 768
    val art = when {
        state.status == TimerStatus.Idle && state.cycleDone == 0 -> R.drawable.illus_ready
        state.phase == Phase.Focus -> R.drawable.illus_focus
        else -> R.drawable.illus_break
    }
    val notesVisible = state.phase == Phase.Focus && state.status != TimerStatus.Idle

    Box(Modifier.size(width, height)) {
        Box(
            Modifier
                .matchParentSize()
                .drawBehind {
                    val glowR = size.height * 0.80f
                    val glowC = Offset(size.width * 0.46f, size.height * 0.44f)
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(c.glow, Color.Transparent),
                            center = glowC,
                            radius = glowR,
                        ),
                        radius = glowR,
                        center = glowC,
                    )
                    val shadowC = Offset(size.width * 0.42f, size.height * 0.94f)
                    drawOval(
                        brush = Brush.radialGradient(
                            colors = listOf(c.shadow, Color.Transparent),
                            center = shadowC,
                            radius = size.width * 0.34f,
                        ),
                        topLeft = Offset(size.width * 0.08f, size.height * 0.89f),
                        size = Size(size.width * 0.68f, size.height * 0.10f),
                    )
                },
        )
        InkArt(
            painter = painterResource(art),
            contentDescription = null,
            modifier = Modifier.matchParentSize(),
        )
        if (notesVisible) {
            FloatingNotes(modifier = Modifier.matchParentSize())
        }
    }
}

@Composable
private fun MiniWeek(stats: Stats) {
    val c = Nomo.colors
    val days = stats.daily
    val peak = (days.maxOfOrNull { it.focusedSeconds } ?: 0).coerceAtLeast(1)
    Row(
        Modifier.fillMaxWidth().height(46.dp),
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        days.forEach { day ->
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Bottom,
            ) {
                val fraction = (day.focusedSeconds.toFloat() / peak).coerceIn(0f, 1f)
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height((24 * fraction).dp.coerceAtLeast(if (day.focusedSeconds > 0) 4.dp else 3.dp))
                        .clip(RoundedCornerShape(4.dp))
                        .background(if (day.focusedSeconds > 0) c.ink else c.track),
                )
                Spacer(Modifier.height(5.dp))
                Text(
                    day.date.dayOfWeek.name.take(1),
                    style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.sp),
                    color = c.inkFaint,
                )
            }
        }
    }
}

private fun goalPercent(done: Int, goal: Int): Int =
    if (goal <= 0) 0 else ((done * 100f) / goal).toInt().coerceIn(0, 999)

fun formatClock(millis: Long): String {
    val total = (millis / 1000L).coerceAtLeast(0L)
    val hours = total / 3600L
    val minutes = (total % 3600L) / 60L
    val seconds = total % 60L
    return if (hours > 0L) {
        "%d:%02d:%02d".format(hours, minutes, seconds)
    } else {
        "%02d:%02d".format(minutes, seconds)
    }
}
