package com.nonmirror.nonmodoro.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.nonmirror.nonmodoro.core.Nomo
import com.nonmirror.nonmodoro.core.NomoCard
import com.nonmirror.nonmodoro.core.NomoSwitch
import com.nonmirror.nonmodoro.core.SectionLabel
import com.nonmirror.nonmodoro.core.SegmentedTabs
import com.nonmirror.nonmodoro.core.SettingRow
import com.nonmirror.nonmodoro.core.Stepper
import com.nonmirror.nonmodoro.core.clickableNoRipple
import com.nonmirror.nonmodoro.data.AlertSound
import com.nonmirror.nonmodoro.data.AppIcon
import com.nonmirror.nonmodoro.data.Settings
import com.nonmirror.nonmodoro.data.ThemeMode

@Composable
fun SettingsScreen(
    settings: Settings,
    onChange: ((Settings) -> Settings) -> Unit,
    onClearHistory: () -> Unit,
    onPreviewSound: (AlertSound) -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = Nomo.colors
    val context = LocalContext.current
    var confirmClear by remember { mutableStateOf(false) }

    Column(modifier.fillMaxWidth().padding(horizontal = 18.dp)) {
        Spacer(Modifier.height(6.dp))
        Text("Settings", style = MaterialTheme.typography.headlineMedium, color = c.ink)
        Spacer(Modifier.height(3.dp))
        Text(
            "Make Nonmodoro fit your day.",
            style = MaterialTheme.typography.bodyMedium,
            color = c.inkSoft,
        )

        Spacer(Modifier.height(16.dp))

        GroupCard("Your rhythm") {
            SettingRow(
                title = "Focus length",
                trailing = {
                    Stepper(
                        value = settings.focusMinutes,
                        onChange = { v -> onChange { it.copy(focusMinutes = v) } },
                        min = 5,
                        max = 180,
                        step = 5,
                    )
                },
            )
            RowDivider()
            SettingRow(
                title = "Short break",
                trailing = {
                    Stepper(
                        value = settings.shortBreakMinutes,
                        onChange = { v -> onChange { it.copy(shortBreakMinutes = v) } },
                        min = 1,
                        max = 30,
                    )
                },
            )
            RowDivider()
            SettingRow(
                title = "Long break",
                trailing = {
                    Stepper(
                        value = settings.longBreakMinutes,
                        onChange = { v -> onChange { it.copy(longBreakMinutes = v) } },
                        min = 5,
                        max = 60,
                        step = 5,
                    )
                },
            )
            RowDivider()
            SettingRow(
                title = "Long break cadence",
                subtitle = "Focus blocks before a long break",
                trailing = {
                    Stepper(
                        value = settings.sessionsBeforeLongBreak,
                        onChange = { v -> onChange { it.copy(sessionsBeforeLongBreak = v) } },
                        min = 2,
                        max = 8,
                        suffix = "blocks",
                    )
                },
            )
            RowDivider()
            SettingRow(
                title = "Daily goal",
                trailing = {
                    Stepper(
                        value = settings.dailyGoalMinutes,
                        onChange = { v -> onChange { it.copy(dailyGoalMinutes = v) } },
                        min = 30,
                        max = 600,
                        step = 30,
                    )
                },
            )
        }

        Spacer(Modifier.height(12.dp))

        GroupCard("Alerts") {
            Column(Modifier.padding(vertical = 12.dp)) {
                Text("Completion sound", style = MaterialTheme.typography.titleMedium, color = c.ink)
                Spacer(Modifier.height(10.dp))
                SoundPicker(
                    selected = settings.sound,
                    onSelect = { sound ->
                        onChange { it.copy(sound = sound) }
                        onPreviewSound(sound)
                    },
                )
            }
            RowDivider()
            SettingRow(
                title = "Vibrate",
                subtitle = "Short buzz when a block ends",
                trailing = {
                    NomoSwitch(
                        checked = settings.vibrate,
                        onChange = { v -> onChange { it.copy(vibrate = v) } },
                    )
                },
            )
            RowDivider()
            SettingRow(
                title = "Completion alerts",
                subtitle = "Notify me when a block finishes",
                trailing = {
                    NomoSwitch(
                        checked = settings.notifications,
                        onChange = { v -> onChange { it.copy(notifications = v) } },
                    )
                },
            )
        }

        Spacer(Modifier.height(12.dp))

        GroupCard("Behaviour") {
            SettingRow(
                title = "Auto-start breaks",
                subtitle = "Slide straight into the break",
                trailing = {
                    NomoSwitch(
                        checked = settings.autoStartBreaks,
                        onChange = { v -> onChange { it.copy(autoStartBreaks = v) } },
                    )
                },
            )
            RowDivider()
            SettingRow(
                title = "Auto-start focus",
                subtitle = "Begin the next block by itself",
                trailing = {
                    NomoSwitch(
                        checked = settings.autoStartFocus,
                        onChange = { v -> onChange { it.copy(autoStartFocus = v) } },
                    )
                },
            )
            RowDivider()
            SettingRow(
                title = "Keep screen on",
                subtitle = "Only while a block is running",
                trailing = {
                    NomoSwitch(
                        checked = settings.keepScreenOn,
                        onChange = { v -> onChange { it.copy(keepScreenOn = v) } },
                    )
                },
            )
        }

        Spacer(Modifier.height(12.dp))

        GroupCard("Appearance") {
            Column(Modifier.padding(vertical = 12.dp)) {
                Text("Theme", style = MaterialTheme.typography.titleMedium, color = c.ink)
                Spacer(Modifier.height(10.dp))
                SegmentedTabs(
                    labels = listOf("System", "Light", "Dark"),
                    selected = when (settings.theme) {
                        ThemeMode.System -> 0
                        ThemeMode.Light -> 1
                        ThemeMode.Dark -> 2
                    },
                    onSelect = { index ->
                        onChange {
                            it.copy(
                                theme = when (index) {
                                    0 -> ThemeMode.System
                                    1 -> ThemeMode.Light
                                    else -> ThemeMode.Dark
                                },
                            )
                        }
                    },
                )
            }
            RowDivider()
            Column(Modifier.padding(vertical = 12.dp)) {
                Text("App icon", style = MaterialTheme.typography.titleMedium, color = c.ink)
                Spacer(Modifier.height(4.dp))
                Text(
                    "Home screen icon · ${settings.appIcon.label}",
                    style = MaterialTheme.typography.bodySmall,
                    color = c.inkSoft,
                )
                Spacer(Modifier.height(12.dp))
                IconPicker(
                    selected = settings.appIcon,
                    onSelect = { icon ->
                        onChange { it.copy(appIcon = icon) }
                        AppIcon.apply(context, icon)
                    },
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        GroupCard("Your data") {
            SettingRow(
                title = "Clear focus history",
                subtitle = "Removes every logged block",
                onClick = { confirmClear = true },
                trailing = {
                    Text("Clear", style = MaterialTheme.typography.titleMedium, color = c.accent)
                },
            )
        }

        Spacer(Modifier.height(20.dp))

        Text(
            "Nonmodoro for Android · an original build inspired by the Kofe Flow Mac app.",
            style = MaterialTheme.typography.bodySmall,
            color = c.inkFaint,
            modifier = Modifier.padding(horizontal = 4.dp),
        )

        Spacer(Modifier.height(24.dp))
    }

    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            title = { Text("Clear focus history?") },
            text = { Text("Every logged block, streak and chart will be reset. This cannot be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmClear = false
                    onClearHistory()
                }) { Text("Clear", color = c.accent) }
            },
            dismissButton = {
                TextButton(onClick = { confirmClear = false }) { Text("Cancel") }
            },
            containerColor = c.card,
            titleContentColor = c.ink,
            textContentColor = c.inkSoft,
        )
    }
}

@Composable
private fun GroupCard(title: String, content: @Composable () -> Unit) {
    Column {
        SectionLabel(title, modifier = Modifier.padding(start = 6.dp, bottom = 8.dp))
        NomoCard(shape = RoundedCornerShape(26.dp), padding = PaddingValues(horizontal = 20.dp, vertical = 6.dp)) {
            content()
        }
    }
}

@Composable
private fun RowDivider() {
    Box(
        Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(Nomo.colors.hairline),
    )
}

@Composable
private fun SoundPicker(selected: AlertSound, onSelect: (AlertSound) -> Unit) {    val c = Nomo.colors
    val options = AlertSound.entries
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        options.chunked(3).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { sound ->
                    val isSelected = sound == selected
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(50))
                            .background(if (isSelected) c.ink else c.paper)
                            .clickableNoRipple { onSelect(sound) }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            sound.label,
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isSelected) {
                                if (c.isDark) c.paper else androidx.compose.ui.graphics.Color.White
                            } else {
                                c.inkSoft
                            },
                        )
                    }
                }
                repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

/**
 * Grid of launcher-icon choices.
 *
 * Each tile reproduces what the home screen actually shows: the adaptive-icon
 * canvas is 108dp but the mask only covers the central 72dp, so the artwork is
 * drawn at 1.5x the tile and centred, then clipped.
 *
 * On the dark theme the tile flips to dark paper and the drawing is tinted with
 * [NomoColors.ink], which is already cream at night. That is the same picture as
 * the light tile, just inverted — and it is why the picker uses each icon's
 * ink-only [AppIcon.mono] twin rather than its full-colour [AppIcon.foreground]:
 * the foreground paints its own paper fill, which would sit on the dark tile as
 * a cream blob.
 */
@Composable
private fun IconPicker(selected: AppIcon, onSelect: (AppIcon) -> Unit) {
    val c = Nomo.colors
    val shape = RoundedCornerShape(percent = 24)
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        AppIcon.entries.chunked(4).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                row.forEach { icon ->
                    val isSelected = icon == selected
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1f)
                            .clip(shape)
                            .background(if (c.isDark) ICON_PAPER_DARK else ICON_PAPER)
                            .then(
                                if (isSelected) {
                                    Modifier.border(2.dp, c.ink, shape)
                                } else {
                                    Modifier
                                },
                            )
                            .clickableNoRipple { onSelect(icon) },
                        contentAlignment = Alignment.Center,
                    ) {
                        Image(
                            painter = painterResource(if (c.isDark) icon.mono else icon.foreground),
                            contentDescription = icon.label,
                            modifier = Modifier.fillMaxSize(1.5f),
                            colorFilter = if (c.isDark) ColorFilter.tint(c.ink) else null,
                        )
                    }
                }
                repeat(4 - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

/** Matches `ic_launcher_background.xml` exactly. */
private val ICON_PAPER = Color(0xFFF8F7F2)

/** The dark twin of [ICON_PAPER]: the same tile drawn on the dark theme's paper. */
private val ICON_PAPER_DARK = Color(0xFF151412)

@Suppress("unused")
private fun settingsRef(s: Settings) = s
