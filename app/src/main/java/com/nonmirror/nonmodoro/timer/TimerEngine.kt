package com.nonmirror.nonmodoro.timer

import android.os.SystemClock
import com.nonmirror.nonmodoro.data.Phase
import com.nonmirror.nonmodoro.data.SettingsStore
import com.nonmirror.nonmodoro.data.SessionRecord
import com.nonmirror.nonmodoro.data.SessionStore
import com.nonmirror.nonmodoro.data.HistoryStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

enum class TimerStatus { Idle, Running, Paused, Finished }

data class TimerState(
    val phase: Phase = Phase.Focus,
    val status: TimerStatus = TimerStatus.Idle,
    val remainingMillis: Long = 30 * 60_000L,
    val totalMillis: Long = 30 * 60_000L,
    val cycleDone: Int = 0,
    val cycleLength: Int = 4,
) {
    val isRunning: Boolean get() = status == TimerStatus.Running
    val isBreak: Boolean get() = phase.isBreak
    val progress: Float
        get() = if (totalMillis <= 0L) 0f
        else (1f - remainingMillis.toFloat() / totalMillis.toFloat()).coerceIn(0f, 1f)
}

sealed interface TimerEvent {
    data class Completed(val phase: Phase, val focusedSeconds: Int) : TimerEvent
    data class Extended(val phase: Phase) : TimerEvent
}

/**
 * Wall-clock accurate pomodoro state machine. The remaining time is always
 * derived from [SystemClock.elapsedRealtime] rather than accumulated ticks, so
 * a paused process or a missed frame never drifts.
 *
 * The block in flight is mirrored to [SessionStore] on every transition, so the
 * countdown also survives the process being killed outright: on the way back up
 * the remaining time is recomputed from the wall clock.
 */
class TimerEngine(
    private val settings: SettingsStore,
    private val history: HistoryStore,
    private val sessions: SessionStore,
    private val scope: CoroutineScope,
) {

    private val _state = MutableStateFlow(initialState())
    val state: StateFlow<TimerState> = _state.asStateFlow()

    private val _events = MutableSharedFlow<TimerEvent>(extraBufferCapacity = 8)
    val events: SharedFlow<TimerEvent> = _events.asSharedFlow()

    private var endAtElapsed = 0L

    /** Same instant as [endAtElapsed] but on the wall clock, so it stays valid
     *  across a process restart. Only meaningful while running. */
    private var endAtWall = 0L
    private var ticker: Job? = null
    private var startedAtWall = 0L
    private var phaseStartWall = 0L

    private fun initialState(): TimerState {
        val s = settings.current
        val total = s.focusMinutes * 60_000L
        return TimerState(
            phase = Phase.Focus,
            status = TimerStatus.Idle,
            remainingMillis = total,
            totalMillis = total,
            cycleDone = 0,
            cycleLength = s.sessionsBeforeLongBreak,
        )
    }

    init {
        // Must run before the settings collector below, which would otherwise
        // flatten a restored block back to a full, idle one.
        restore()

        scope.launch {
            settings.state.collect { s ->
                _state.update { st ->
                    if (st.status == TimerStatus.Idle) {
                        val total = s.minutesFor(st.phase) * 60_000L
                        st.copy(
                            totalMillis = total,
                            remainingMillis = total,
                            cycleLength = s.sessionsBeforeLongBreak,
                        )
                    } else {
                        st.copy(cycleLength = s.sessionsBeforeLongBreak)
                    }
                }
            }
        }
    }

    /**
     * Rebuilds the block that was in flight when the process died, if any.
     *
     * A running block that has already expired while we were away is banked as a
     * completed session and parked on the "block closed" screen rather than
     * alerting at launch — the moment to sound the cue has passed.
     */
    private fun restore() {
        val snap = sessions.load() ?: return
        val total = if (snap.totalMillis > 0L) snap.totalMillis else 0L
        val cycleLength = settings.current.sessionsBeforeLongBreak

        when (snap.status) {
            TimerStatus.Paused.name -> {
                phaseStartWall = snap.phaseStartWall
                _state.value = TimerState(
                    phase = snap.phase,
                    status = TimerStatus.Paused,
                    remainingMillis = snap.remainingMillis.coerceIn(0L, total),
                    totalMillis = total,
                    cycleDone = snap.cycleDone,
                    cycleLength = cycleLength,
                )
            }

            TimerStatus.Running.name -> {
                phaseStartWall = snap.phaseStartWall
                val remaining = (snap.endAtWall - System.currentTimeMillis()).coerceAtLeast(0L)
                if (remaining > 0L) {
                    endAtWall = snap.endAtWall
                    endAtElapsed = SystemClock.elapsedRealtime() + remaining
                    _state.value = TimerState(
                        phase = snap.phase,
                        status = TimerStatus.Running,
                        remainingMillis = remaining,
                        totalMillis = total,
                        cycleDone = snap.cycleDone,
                        cycleLength = cycleLength,
                    )
                    startTicker()
                } else {
                    if (snap.phase == Phase.Focus) {
                        recordSession(
                            seconds = (total / 1000L).toInt(),
                            completed = true,
                            endedAt = snap.endAtWall,
                        )
                    }
                    _state.value = TimerState(
                        phase = snap.phase,
                        status = TimerStatus.Finished,
                        remainingMillis = 0L,
                        totalMillis = total,
                        cycleDone = snap.cycleDone,
                        cycleLength = cycleLength,
                    )
                    // Rewrite the snapshot as finished. Without this every later
                    // launch would find the same expired running block and bank
                    // the very same session again, inflating the user's stats.
                    persist()
                }
            }

            TimerStatus.Finished.name -> {
                _state.value = TimerState(
                    phase = snap.phase,
                    status = TimerStatus.Finished,
                    remainingMillis = 0L,
                    totalMillis = total,
                    cycleDone = snap.cycleDone,
                    cycleLength = cycleLength,
                )
            }

            TimerStatus.Idle.name -> {
                // Nothing is in flight, but the position in the cycle still
                // matters: a queued-up break, or round 3 of 4. Keep the phase and
                // counter and let the settings collector fill in the duration.
                _state.value = _state.value.copy(
                    phase = snap.phase,
                    cycleDone = snap.cycleDone,
                    cycleLength = cycleLength,
                )
            }

            else -> Unit // Unknown status: fall back to a fresh block.
        }
    }

    /** Mirrors the current block to disk. Called on transitions only. */
    private fun persist() {
        val st = _state.value
        if (st.status == TimerStatus.Idle && st.phase == Phase.Focus && st.cycleDone == 0) {
            sessions.clear()
            return
        }
        sessions.save(
            SessionStore.Snapshot(
                phase = st.phase,
                status = st.status.name,
                totalMillis = st.totalMillis,
                remainingMillis = st.remainingMillis,
                endAtWall = endAtWall,
                cycleDone = st.cycleDone,
                phaseStartWall = phaseStartWall,
            ),
        )
    }

    fun start() {
        val st = _state.value
        if (st.status == TimerStatus.Running) return
        val total = if (st.remainingMillis > 0L) st.remainingMillis else st.totalMillis
        if (st.status != TimerStatus.Paused) {
            phaseStartWall = System.currentTimeMillis()
            startedAtWall = phaseStartWall
        }
        endAtElapsed = SystemClock.elapsedRealtime() + total
        endAtWall = System.currentTimeMillis() + total
        _state.value = st.copy(
            status = TimerStatus.Running,
            remainingMillis = total,
            totalMillis = if (st.totalMillis > 0L) st.totalMillis else total,
        )
        persist()
        startTicker()
    }

    fun pause() {
        val st = _state.value
        if (st.status != TimerStatus.Running) return
        ticker?.cancel()
        val remaining = (endAtElapsed - SystemClock.elapsedRealtime()).coerceAtLeast(0L)
        _state.value = st.copy(status = TimerStatus.Paused, remainingMillis = remaining)
        persist()
    }

    fun toggle() {
        when (_state.value.status) {
            TimerStatus.Running -> pause()
            TimerStatus.Idle, TimerStatus.Paused, TimerStatus.Finished -> {
                if (_state.value.status == TimerStatus.Finished) {
                    advance(autoStart = true)
                } else {
                    start()
                }
            }
        }
    }

    /** Ends the current phase early and moves on. Partial focus time is kept. */
    fun skip() {
        val st = _state.value
        ticker?.cancel()
        if (st.phase == Phase.Focus && st.status != TimerStatus.Idle) {
            val elapsed = ((st.totalMillis - st.remainingMillis) / 1000L).toInt()
            if (elapsed >= 60) recordSession(elapsed, completed = false)
        }
        advance(autoStart = false)
    }

    /** Adds time to the phase in flight, or re-opens it after it finished. */
    fun extend(minutes: Int = 5) {
        val st = _state.value
        val add = minutes * 60_000L
        when (st.status) {
            TimerStatus.Finished -> {
                _state.value = st.copy(
                    status = TimerStatus.Running,
                    totalMillis = st.totalMillis + add,
                    remainingMillis = add,
                )
                endAtElapsed = SystemClock.elapsedRealtime() + add
                endAtWall = System.currentTimeMillis() + add
                persist()
                startTicker()
            }
            TimerStatus.Running -> {
                endAtElapsed += add
                endAtWall += add
                _state.update {
                    it.copy(
                        totalMillis = it.totalMillis + add,
                        remainingMillis = it.remainingMillis + add,
                    )
                }
                persist()
            }
            TimerStatus.Paused -> {
                _state.update {
                    it.copy(
                        totalMillis = it.totalMillis + add,
                        remainingMillis = it.remainingMillis + add,
                    )
                }
                persist()
            }
            TimerStatus.Idle -> {
                _state.update {
                    it.copy(
                        totalMillis = it.totalMillis + add,
                        remainingMillis = it.remainingMillis + add,
                    )
                }
                persist()
            }
        }
        _events.tryEmit(TimerEvent.Extended(st.phase))
    }

    /** Back to a fresh focus block, cycle counter cleared. */
    fun reset() {
        ticker?.cancel()
        endAtWall = 0L
        _state.value = initialState()
        persist()
    }

    fun setPhase(phase: Phase) {
        ticker?.cancel()
        endAtWall = 0L
        val s = settings.current
        val total = s.minutesFor(phase) * 60_000L
        _state.value = _state.value.copy(
            phase = phase,
            status = TimerStatus.Idle,
            remainingMillis = total,
            totalMillis = total,
        )
        persist()
    }

    private fun startTicker() {
        ticker?.cancel()
        ticker = scope.launch {
            while (isActive) {
                val remaining = (endAtElapsed - SystemClock.elapsedRealtime()).coerceAtLeast(0L)
                _state.update { it.copy(remainingMillis = remaining) }
                if (remaining <= 0L) {
                    finishPhase()
                    break
                }
                delay(200)
            }
        }
    }

    private suspend fun finishPhase() {
        val st = _state.value
        val plannedSeconds = (st.totalMillis / 1000L).toInt()
        if (st.phase == Phase.Focus) recordSession(plannedSeconds, completed = true)
        endAtWall = 0L
        _state.value = st.copy(status = TimerStatus.Finished, remainingMillis = 0L)
        persist()
        _events.emit(TimerEvent.Completed(st.phase, plannedSeconds))

        val s = settings.current
        val auto = if (st.phase == Phase.Focus) s.autoStartBreaks else s.autoStartFocus
        if (auto) advance(autoStart = true)
    }

    private fun advance(autoStart: Boolean) {
        val st = _state.value
        val s = settings.current
        var cycleDone = st.cycleDone
        val next: Phase
        if (st.phase == Phase.Focus) {
            cycleDone += 1
            next = if (cycleDone >= s.sessionsBeforeLongBreak) Phase.LongBreak else Phase.ShortBreak
        } else {
            if (st.phase == Phase.LongBreak) cycleDone = 0
            next = Phase.Focus
        }
        val total = s.minutesFor(next) * 60_000L
        endAtWall = 0L
        _state.value = TimerState(
            phase = next,
            status = TimerStatus.Idle,
            remainingMillis = total,
            totalMillis = total,
            cycleDone = cycleDone,
            cycleLength = s.sessionsBeforeLongBreak,
        )
        persist()
        if (autoStart) start()
    }

    /**
     * @param endedAt when the block actually finished. Defaults to now, but a
     *   block that ran out while the process was dead finished back then — not
     *   at the moment we noticed.
     */
    private fun recordSession(
        seconds: Int,
        completed: Boolean,
        endedAt: Long = System.currentTimeMillis(),
    ) {
        if (seconds < 30) return
        scope.launch {
            history.add(
                SessionRecord(
                    id = System.currentTimeMillis(),
                    phase = Phase.Focus,
                    startedAt = if (phaseStartWall > 0L) phaseStartWall else endedAt - seconds * 1000L,
                    endedAt = endedAt,
                    focusedSeconds = seconds,
                    completed = completed,
                ),
            )
        }
    }
}
