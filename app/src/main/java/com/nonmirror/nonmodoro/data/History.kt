package com.nonmirror.nonmodoro.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

data class SessionRecord(
    val id: Long,
    val phase: Phase,
    val startedAt: Long,
    val endedAt: Long,
    val focusedSeconds: Int,
    val completed: Boolean,
)

data class DayStat(val date: LocalDate, val focusedSeconds: Int, val sessions: Int) {
    val minutes: Int get() = focusedSeconds / 60
}

data class Stats(
    val todayMinutes: Int = 0,
    val todaySessions: Int = 0,
    val streakDays: Int = 0,
    val daily: List<DayStat> = emptyList(),
    val hourly: List<Int> = List(24) { 0 },
    val totalSessions: Int = 0,
    val totalMinutes: Int = 0,
    val bestHour: Int? = null,
    val averageSessionMinutes: Int = 0,
)

class HistoryStore(context: Context) {

    private val file = File(context.filesDir, "nomo-history.json")
    private val _records = MutableStateFlow(load())
    val records: StateFlow<List<SessionRecord>> = _records.asStateFlow()

    val current: List<SessionRecord> get() = _records.value

    private fun load(): List<SessionRecord> {
        if (!file.exists()) return emptyList()
        return runCatching {
            val array = JSONArray(file.readText())
            (0 until array.length()).map { i ->
                val o = array.getJSONObject(i)
                SessionRecord(
                    id = o.optLong("id"),
                    phase = runCatching { Phase.valueOf(o.optString("phase")) }.getOrDefault(Phase.Focus),
                    startedAt = o.optLong("startedAt"),
                    endedAt = o.optLong("endedAt"),
                    focusedSeconds = o.optInt("focusedSeconds"),
                    completed = o.optBoolean("completed"),
                )
            }
        }.getOrDefault(emptyList())
    }

    private fun persist(list: List<SessionRecord>) {
        val array = JSONArray()
        list.forEach { r ->
            array.put(
                JSONObject().apply {
                    put("id", r.id)
                    put("phase", r.phase.name)
                    put("startedAt", r.startedAt)
                    put("endedAt", r.endedAt)
                    put("focusedSeconds", r.focusedSeconds)
                    put("completed", r.completed)
                },
            )
        }
        runCatching { file.writeText(array.toString()) }
    }

    suspend fun add(record: SessionRecord) = withContext(Dispatchers.IO) {
        val next = (_records.value + record).takeLast(4000)
        persist(next)
        _records.value = next
    }

    suspend fun clear() = withContext(Dispatchers.IO) {
        persist(emptyList())
        _records.value = emptyList()
    }

    suspend fun removeLast() = withContext(Dispatchers.IO) {
        val next = _records.value.dropLast(1)
        persist(next)
        _records.value = next
    }
}

fun List<SessionRecord>.computeStats(zone: ZoneId = ZoneId.systemDefault(), days: Int = 7): Stats {
    val focuses = filter { it.phase == Phase.Focus }
    val today = LocalDate.now(zone)

    fun focusedSecondsOn(date: LocalDate): Int = focuses
        .filter { Instant.ofEpochMilli(it.startedAt).atZone(zone).toLocalDate() == date }
        .sumOf { it.focusedSeconds }

    val todaySeconds = focusedSecondsOn(today)
    val todaySessions = focuses.count {
        Instant.ofEpochMilli(it.startedAt).atZone(zone).toLocalDate() == today
    }

    // Streak: consecutive days (ending today, or yesterday if today is still empty)
    // that contain at least one completed focus block.
    val activeDays = focuses
        .filter { it.completed && it.focusedSeconds >= 60 }
        .map { Instant.ofEpochMilli(it.startedAt).atZone(zone).toLocalDate() }
        .toSet()
    var streak = 0
    var cursor = if (activeDays.contains(today)) today else today.minusDays(1)
    while (activeDays.contains(cursor)) {
        streak++
        cursor = cursor.minusDays(1)
    }

    val window = (days - 1).downTo(0).map { offset ->
        val date = today.minusDays(offset.toLong())
        val onDay = focuses.filter {
            Instant.ofEpochMilli(it.startedAt).atZone(zone).toLocalDate() == date
        }
        DayStat(date, onDay.sumOf { it.focusedSeconds }, onDay.size)
    }

    val hourly = MutableList(24) { 0 }
    focuses.forEach {
        val hour = Instant.ofEpochMilli(it.startedAt).atZone(zone).hour
        hourly[hour] = hourly[hour] + it.focusedSeconds
    }

    val bestHour = hourly.withIndex().filter { it.value > 0 }.maxByOrNull { it.value }?.index

    return Stats(
        todayMinutes = todaySeconds / 60,
        todaySessions = todaySessions,
        streakDays = streak,
        daily = window,
        hourly = hourly,
        totalSessions = focuses.size,
        totalMinutes = focuses.sumOf { it.focusedSeconds } / 60,
        bestHour = bestHour,
        averageSessionMinutes = if (focuses.isEmpty()) 0
        else (focuses.sumOf { it.focusedSeconds } / focuses.size) / 60,
    )
}
