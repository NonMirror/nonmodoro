package com.nonmirror.nonmodoro.data

import android.content.Context

/**
 * Remembers the block that is currently in flight so that a process death — an
 * aggressive OEM battery manager, a swipe out of recents, a crash — does not
 * silently reset the clock back to a full focus block.
 *
 * Only *transitions* are written (start, pause, resume, skip, extend, advance),
 * never the per-tick countdown: while a block runs the authoritative value is
 * [Snapshot.endAtWall], so one write at the start is enough to reconstruct the
 * exact remaining time later.
 *
 * The status is kept as a plain string so this package stays independent of the
 * timer state machine that consumes it.
 */
class SessionStore(context: Context) {

    private val prefs = context.getSharedPreferences("nomo.session", Context.MODE_PRIVATE)

    data class Snapshot(
        val phase: Phase,
        val status: String,
        val totalMillis: Long,
        val remainingMillis: Long,
        val endAtWall: Long,
        val cycleDone: Int,
        val phaseStartWall: Long,
    )

    fun save(snapshot: Snapshot) {
        // commit(), not apply(): a block can be killed at any moment and a
        // queued write would be lost. These writes are rare, so the cost is nil.
        prefs.edit()
            .putString(KEY_PHASE, snapshot.phase.name)
            .putString(KEY_STATUS, snapshot.status)
            .putLong(KEY_TOTAL, snapshot.totalMillis)
            .putLong(KEY_REMAINING, snapshot.remainingMillis)
            .putLong(KEY_END_AT_WALL, snapshot.endAtWall)
            .putInt(KEY_CYCLE_DONE, snapshot.cycleDone)
            .putLong(KEY_PHASE_START_WALL, snapshot.phaseStartWall)
            .commit()
    }

    fun clear() {
        prefs.edit().clear().commit()
    }

    fun load(): Snapshot? {
        val phaseName = prefs.getString(KEY_PHASE, null) ?: return null
        val status = prefs.getString(KEY_STATUS, null) ?: return null
        val phase = runCatching { Phase.valueOf(phaseName) }.getOrNull() ?: return null
        return Snapshot(
            phase = phase,
            status = status,
            totalMillis = prefs.getLong(KEY_TOTAL, 0L),
            remainingMillis = prefs.getLong(KEY_REMAINING, 0L),
            endAtWall = prefs.getLong(KEY_END_AT_WALL, 0L),
            cycleDone = prefs.getInt(KEY_CYCLE_DONE, 0),
            phaseStartWall = prefs.getLong(KEY_PHASE_START_WALL, 0L),
        )
    }

    private companion object {
        const val KEY_PHASE = "phase"
        const val KEY_STATUS = "status"
        const val KEY_TOTAL = "totalMillis"
        const val KEY_REMAINING = "remainingMillis"
        const val KEY_END_AT_WALL = "endAtWall"
        const val KEY_CYCLE_DONE = "cycleDone"
        const val KEY_PHASE_START_WALL = "phaseStartWall"
    }
}
