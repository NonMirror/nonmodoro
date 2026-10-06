package com.nonmirror.nonmodoro.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.nonmirror.nonmodoro.core.Nomo
import com.nonmirror.nonmodoro.core.SegmentedTabs
import com.nonmirror.nonmodoro.data.AlertSound
import com.nonmirror.nonmodoro.data.SessionRecord
import com.nonmirror.nonmodoro.data.Settings
import com.nonmirror.nonmodoro.data.computeStats
import com.nonmirror.nonmodoro.timer.TimerState

@Composable
fun RootScreen(
    settings: Settings,
    timer: TimerState,
    records: List<SessionRecord>,
    onToggle: () -> Unit,
    onSkip: () -> Unit,
    onExtend: () -> Unit,
    onReset: () -> Unit,
    onSettingsChange: ((Settings) -> Settings) -> Unit,
    onClearHistory: () -> Unit,
    onPreviewSound: (AlertSound) -> Unit,
) {
    val c = Nomo.colors
    var tab by remember { mutableIntStateOf(0) }
    val stats = remember(records) { records.computeStats(days = 7) }

    Column(Modifier.fillMaxSize().background(c.paper)) {
        Spacer(Modifier.height(8.dp))
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            SegmentedTabs(
                labels = listOf("Timer", "Stats", "Settings"),
                selected = tab,
                onSelect = { tab = it },
            )
        }
        Spacer(Modifier.height(12.dp))

        val scroll = rememberScrollState()
        when (tab) {
            0 -> TimerScreen(
                state = timer,
                settings = settings,
                stats = stats,
                onToggle = onToggle,
                onSkip = onSkip,
                onExtend = onExtend,
                onReset = onReset,
                modifier = Modifier.verticalScroll(scroll),
            )
            1 -> StatsScreen(
                records = records,
                modifier = Modifier.verticalScroll(scroll),
            )
            else -> SettingsScreen(
                settings = settings,
                onChange = onSettingsChange,
                onClearHistory = onClearHistory,
                onPreviewSound = onPreviewSound,
                modifier = Modifier.verticalScroll(scroll),
            )
        }
    }
}
