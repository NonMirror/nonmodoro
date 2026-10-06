package com.nonmirror.nonmodoro.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.nonmirror.nonmodoro.R
import com.nonmirror.nonmodoro.core.InkArt
import com.nonmirror.nonmodoro.core.Nomo
import com.nonmirror.nonmodoro.core.NomoCard
import com.nonmirror.nonmodoro.core.SectionLabel
import com.nonmirror.nonmodoro.core.SegmentedTabs
import com.nonmirror.nonmodoro.data.Phase
import com.nonmirror.nonmodoro.data.SessionRecord
import com.nonmirror.nonmodoro.data.computeStats
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import kotlin.math.roundToInt

private val RANGE_LABELS = listOf("7 days", "30 days", "90 days")
private val RANGE_DAYS = listOf(7, 30, 90)

@Composable
fun StatsScreen(records: List<SessionRecord>, modifier: Modifier = Modifier) {
    val c = Nomo.colors
    var range by remember { mutableIntStateOf(0) }
    val rangeDays = RANGE_DAYS[range]

    val stats = remember(records, rangeDays) { records.computeStats(days = rangeDays) }

    Column(modifier.fillMaxWidth().padding(horizontal = 18.dp)) {
        Spacer(Modifier.height(6.dp))
        Text("Your focus", style = MaterialTheme.typography.headlineMedium, color = c.ink)
        Spacer(Modifier.height(3.dp))
        Text(
            if (stats.totalSessions == 0) "Nothing logged yet. Start one clean block."
            else "See more of your focus history.",
            style = MaterialTheme.typography.bodyMedium,
            color = c.inkSoft,
        )

        Spacer(Modifier.height(16.dp))

        if (records.none { it.phase == Phase.Focus }) {
            EmptyStats()
            Spacer(Modifier.height(24.dp))
            return@Column
        }

        SegmentedTabs(
            labels = RANGE_LABELS,
            selected = range,
            onSelect = { range = it },
        )

        Spacer(Modifier.height(16.dp))

        NomoCard(shape = RoundedCornerShape(28.dp), padding = PaddingValues(22.dp)) {
            SectionLabel("Focused time")
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = formatDuration(stats.totalMinutes),
                    style = MaterialTheme.typography.headlineMedium,
                    color = c.ink,
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    "${stats.totalSessions} blocks",
                    style = MaterialTheme.typography.bodySmall,
                    color = c.inkFaint,
                    modifier = Modifier.padding(bottom = 5.dp),
                )
            }
            Spacer(Modifier.height(18.dp))
            BarChart(values = chartBuckets(stats.daily.map { it.minutes }, rangeDays))
            Spacer(Modifier.height(10.dp))
            Text(
                if (rangeDays <= 30) "Daily focus, last $rangeDays days"
                else "Weekly focus, last $rangeDays days",
                style = MaterialTheme.typography.bodySmall,
                color = c.inkFaint,
            )
        }

        Spacer(Modifier.height(12.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatCardSmall("Today", "${stats.todayMinutes}m", Modifier.weight(1f))
            StatCardSmall("Streak", "${stats.streakDays}d", Modifier.weight(1f))
            StatCardSmall("Avg block", "${stats.averageSessionMinutes}m", Modifier.weight(1f))
        }

        Spacer(Modifier.height(12.dp))

        NomoCard(shape = RoundedCornerShape(28.dp), padding = PaddingValues(22.dp)) {
            SectionLabel("When you focus")
            Spacer(Modifier.height(6.dp))
            Text(
                stats.bestHour?.let { "Best hour: %02d:00".format(it) } ?: "Not enough data yet.",
                style = MaterialTheme.typography.titleMedium,
                color = c.ink,
            )
            Spacer(Modifier.height(16.dp))
            HourStrip(hourly = stats.hourly)
        }

        Spacer(Modifier.height(12.dp))

        NomoCard(shape = RoundedCornerShape(28.dp), padding = PaddingValues(22.dp)) {
            SectionLabel("Recent blocks")
            Spacer(Modifier.height(12.dp))
            val recent = records.filter { it.phase == Phase.Focus }.takeLast(8).reversed()
            if (recent.isEmpty()) {
                Text("No blocks yet.", style = MaterialTheme.typography.bodyMedium, color = c.inkFaint)
            } else {
                recent.forEachIndexed { index, record ->
                    SessionRow(record)
                    if (index != recent.lastIndex) {
                        Spacer(Modifier.height(10.dp))
                        Box(Modifier.fillMaxWidth().height(1.dp).background(c.hairline))
                        Spacer(Modifier.height(10.dp))
                    }
                }
            }
        }

        Spacer(Modifier.height(24.dp))
    }
}

/** 90-day ranges are folded into weeks so the bars stay legible. */
private fun chartBuckets(daily: List<Int>, rangeDays: Int): List<Int> {
    if (rangeDays <= 30 || daily.isEmpty()) return daily
    return daily.chunked(7).map { week -> week.sum() }
}

private fun formatDuration(totalMinutes: Int): String {
    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60
    return if (hours > 0) "${hours}h ${minutes}m" else "${minutes}m"
}

@Composable
private fun StatCardSmall(label: String, value: String, modifier: Modifier = Modifier) {
    val c = Nomo.colors
    NomoCard(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        padding = PaddingValues(horizontal = 14.dp, vertical = 14.dp),
    ) {
        SectionLabel(label)
        Spacer(Modifier.height(6.dp))
        Text(value, style = MaterialTheme.typography.headlineSmall, color = c.ink)
    }
}

@Composable
private fun BarChart(values: List<Int>) {
    val c = Nomo.colors
    val peak = (values.maxOrNull() ?: 0).coerceAtLeast(1)
    Row(
        Modifier.fillMaxWidth().height(96.dp),
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        values.forEach { minutes ->
            val fraction = (minutes.toFloat() / peak).coerceIn(0f, 1f)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .height((84 * fraction).dp.coerceAtLeast(if (minutes > 0) 5.dp else 3.dp))
                    .clip(RoundedCornerShape(5.dp))
                    .background(if (minutes > 0) c.ink else c.track),
            )
        }
    }
}

@Composable
private fun HourStrip(hourly: List<Int>) {
    val c = Nomo.colors
    val peak = (hourly.maxOrNull() ?: 0).coerceAtLeast(1)
    Column {
        Row(
            Modifier.fillMaxWidth().height(64.dp),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            hourly.forEach { seconds ->
                val fraction = (seconds.toFloat() / peak).coerceIn(0f, 1f)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height((60 * fraction).dp.coerceAtLeast(2.dp))
                        .clip(RoundedCornerShape(3.dp))
                        .background(if (seconds > 0) c.ink else c.track),
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth()) {
            listOf("00", "06", "12", "18", "23").forEach { label ->
                Text(
                    label,
                    style = MaterialTheme.typography.labelSmall,
                    color = c.inkFaint,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun SessionRow(record: SessionRecord) {
    val c = Nomo.colors
    val stamp = Instant.ofEpochMilli(record.startedAt).atZone(ZoneId.systemDefault())
    val formatter = remember { DateTimeFormatter.ofPattern("EEE HH:mm") }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(if (record.completed) c.ink else c.track),
        )
        Spacer(Modifier.width(12.dp))
        Text(
            stamp.format(formatter),
            style = MaterialTheme.typography.bodyMedium,
            color = c.ink,
            modifier = Modifier.weight(1f),
        )
        Text(
            "${(record.focusedSeconds / 60f).roundToInt()} min",
            style = MaterialTheme.typography.titleMedium,
            color = c.ink,
        )
        if (!record.completed) {
            Spacer(Modifier.width(8.dp))
            Text("partial", style = MaterialTheme.typography.bodySmall, color = c.inkFaint)
        }
    }
}

@Composable
private fun EmptyStats() {
    val c = Nomo.colors
    NomoCard(shape = RoundedCornerShape(28.dp), padding = PaddingValues(24.dp)) {
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            InkArt(
                painter = painterResource(R.drawable.illus_ready),
                contentDescription = null,
                modifier = Modifier.width(200.dp).height(150.dp),
            )
        }
        Spacer(Modifier.height(8.dp))
        Text("Ready for your first block", style = MaterialTheme.typography.headlineSmall, color = c.ink)
        Spacer(Modifier.height(6.dp))
        Text(
            "Finish one focus block and this page fills in — today's time, your streak, and the hours you work best.",
            style = MaterialTheme.typography.bodyMedium,
            color = c.inkSoft,
        )
    }
}

@Suppress("unused")
private fun todayRef(): LocalDate = LocalDate.now()

@Suppress("unused")
private fun chronoRef(): ChronoUnit = ChronoUnit.DAYS
