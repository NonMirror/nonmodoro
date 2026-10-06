package com.nonmirror.nonmodoro.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class ThemeMode { System, Light, Dark }

enum class Phase {
    Focus,
    ShortBreak,
    LongBreak;

    val isBreak: Boolean get() = this != Focus
}

enum class AlertSound(val label: String, val resName: String) {
    Glass("Glass", "tone_glass"),
    Chime("Chime", "tone_chime"),
    Marimba("Marimba", "tone_marimba"),
    Ready("Soft cue", "tone_ready"),
    None("None", ""),
}

data class Settings(
    val focusMinutes: Int = 30,
    val shortBreakMinutes: Int = 5,
    val longBreakMinutes: Int = 10,
    val sessionsBeforeLongBreak: Int = 4,
    val dailyGoalMinutes: Int = 120,
    val autoStartBreaks: Boolean = false,
    val autoStartFocus: Boolean = false,
    val sound: AlertSound = AlertSound.Glass,
    val vibrate: Boolean = true,
    val notifications: Boolean = true,
    val keepScreenOn: Boolean = false,
    val theme: ThemeMode = ThemeMode.System,
    val appIcon: AppIcon = AppIcon.default,
    val onboarded: Boolean = false,
) {
    fun minutesFor(phase: Phase): Int = when (phase) {
        Phase.Focus -> focusMinutes
        Phase.ShortBreak -> shortBreakMinutes
        Phase.LongBreak -> longBreakMinutes
    }
}

class SettingsStore(context: Context) {

    private val prefs = context.getSharedPreferences("nomo.settings", Context.MODE_PRIVATE)
    private val _state = MutableStateFlow(read())
    val state: StateFlow<Settings> = _state.asStateFlow()

    val current: Settings get() = _state.value

    private fun read() = Settings(
        focusMinutes = prefs.getInt("focusMinutes", 30),
        shortBreakMinutes = prefs.getInt("shortBreakMinutes", 5),
        longBreakMinutes = prefs.getInt("longBreakMinutes", 10),
        sessionsBeforeLongBreak = prefs.getInt("sessionsBeforeLongBreak", 4),
        dailyGoalMinutes = prefs.getInt("dailyGoalMinutes", 120),
        autoStartBreaks = prefs.getBoolean("autoStartBreaks", false),
        autoStartFocus = prefs.getBoolean("autoStartFocus", false),
        sound = runCatching { AlertSound.valueOf(prefs.getString("sound", "Glass")!!) }
            .getOrDefault(AlertSound.Glass),
        vibrate = prefs.getBoolean("vibrate", true),
        notifications = prefs.getBoolean("notifications", true),
        keepScreenOn = prefs.getBoolean("keepScreenOn", false),
        theme = runCatching { ThemeMode.valueOf(prefs.getString("theme", "System")!!) }
            .getOrDefault(ThemeMode.System),
        appIcon = AppIcon.fromName(prefs.getString("appIcon", null)),
        onboarded = prefs.getBoolean("onboarded", false),
    )

    fun update(transform: (Settings) -> Settings) {
        val next = transform(_state.value)
        _state.value = next
        prefs.edit().apply {
            putInt("focusMinutes", next.focusMinutes)
            putInt("shortBreakMinutes", next.shortBreakMinutes)
            putInt("longBreakMinutes", next.longBreakMinutes)
            putInt("sessionsBeforeLongBreak", next.sessionsBeforeLongBreak)
            putInt("dailyGoalMinutes", next.dailyGoalMinutes)
            putBoolean("autoStartBreaks", next.autoStartBreaks)
            putBoolean("autoStartFocus", next.autoStartFocus)
            putString("sound", next.sound.name)
            putBoolean("vibrate", next.vibrate)
            putBoolean("notifications", next.notifications)
            putBoolean("keepScreenOn", next.keepScreenOn)
            putString("theme", next.theme.name)
            putString("appIcon", next.appIcon.name)
            putBoolean("onboarded", next.onboarded)
        }.apply()
    }
}
