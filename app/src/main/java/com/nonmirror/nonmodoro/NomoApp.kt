package com.nonmirror.nonmodoro

import android.app.Application
import com.nonmirror.nonmodoro.data.HistoryStore
import com.nonmirror.nonmodoro.data.SessionStore
import com.nonmirror.nonmodoro.data.SettingsStore
import com.nonmirror.nonmodoro.timer.Alerts
import com.nonmirror.nonmodoro.timer.TimerCopy
import com.nonmirror.nonmodoro.timer.TimerEngine
import com.nonmirror.nonmodoro.timer.TimerEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class NomoApp : Application() {

    lateinit var settings: SettingsStore
        private set
    lateinit var history: HistoryStore
        private set
    lateinit var sessions: SessionStore
        private set
    lateinit var engine: TimerEngine
        private set

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    fun clearHistory() {
        scope.launch { history.clear() }
    }

    override fun onCreate() {
        super.onCreate()
        settings = SettingsStore(this)
        history = HistoryStore(this)
        sessions = SessionStore(this)
        engine = TimerEngine(settings, history, sessions, scope)

        Alerts.createChannels(this)

        // Completion alerts live outside the service so they still fire when the
        // foreground notification is torn down.
        scope.launch {
            engine.events.collect { event ->
                if (event !is TimerEvent.Completed) return@collect
                val current = settings.current
                if (current.sound != com.nonmirror.nonmodoro.data.AlertSound.None) {
                    Alerts.playCompletion(this@NomoApp, current.sound)
                }
                if (current.vibrate) Alerts.buzz(this@NomoApp)
                if (current.notifications) {
                    val next = TimerCopy.of(engine.state.value)
                    Alerts.notifyCompletion(this@NomoApp, event.phase, next.label, next.headline)
                }
            }
        }
    }
}

val android.content.Context.nomo: NomoApp
    get() = applicationContext as NomoApp
