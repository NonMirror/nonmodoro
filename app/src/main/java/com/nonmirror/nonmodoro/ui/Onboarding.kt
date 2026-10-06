package com.nonmirror.nonmodoro.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import com.nonmirror.nonmodoro.core.PillButton
import com.nonmirror.nonmodoro.core.SectionLabel
import com.nonmirror.nonmodoro.core.clickableNoRipple
import com.nonmirror.nonmodoro.data.Settings

private data class Rhythm(
    val name: String,
    val focus: Int,
    val short: Int,
    val long: Int,
    val cadence: Int,
    val blurb: String,
)

private val Rhythms = listOf(
    Rhythm("Classic Pomodoro", 25, 5, 15, 4, "The one everybody knows. 25 on, 5 off."),
    Rhythm("Deep Work", 50, 10, 20, 3, "Longer blocks for work that needs momentum."),
    Rhythm("Sprint", 15, 3, 10, 4, "Short and sharp, good for clearing a queue."),
)

@Composable
fun OnboardingScreen(
    onFinish: (focus: Int, shortBreak: Int, longBreak: Int, cadence: Int) -> Unit,
) {
    val c = Nomo.colors
    var selected by remember { mutableIntStateOf(0) }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp),
    ) {
        Spacer(Modifier.height(28.dp))

        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            InkArt(
                painter = painterResource(R.drawable.illus_ready),
                contentDescription = null,
                modifier = Modifier.width(240.dp).height(180.dp),
            )
        }

        Spacer(Modifier.height(6.dp))

        Text("Set up Nonmodoro", style = MaterialTheme.typography.headlineMedium, color = c.ink)
        Spacer(Modifier.height(6.dp))
        Text(
            "Choose your rhythm. You can change any of it later.",
            style = MaterialTheme.typography.bodyMedium,
            color = c.inkSoft,
        )

        Spacer(Modifier.height(22.dp))
        SectionLabel("Choose your rhythm")
        Spacer(Modifier.height(10.dp))

        Rhythms.forEachIndexed { index, rhythm ->
            val isSelected = index == selected
            NomoCard(
                modifier = Modifier.clickableNoRipple { selected = index },
                shape = RoundedCornerShape(24.dp),
                padding = PaddingValues(18.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier
                            .size(20.dp)
                            .clip(CircleShape)
                            .background(if (isSelected) c.ink else c.track),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (isSelected) {
                            Box(
                                Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(if (c.isDark) c.paper else androidx.compose.ui.graphics.Color.White),
                            )
                        }
                    }
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text(rhythm.name, style = MaterialTheme.typography.headlineSmall, color = c.ink)
                        Spacer(Modifier.height(3.dp))
                        Text(rhythm.blurb, style = MaterialTheme.typography.bodySmall, color = c.inkSoft)
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "${rhythm.focus} min focus · ${rhythm.short} min break · ${rhythm.long} min long break",
                            style = MaterialTheme.typography.bodySmall,
                            color = c.inkFaint,
                        )
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
        }

        Spacer(Modifier.height(10.dp))

        PillButton(
            text = "Start focusing",
            onClick = {
                val rhythm = Rhythms[selected]
                onFinish(rhythm.focus, rhythm.short, rhythm.long, rhythm.cadence)
            },
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(Modifier.height(12.dp))
        Text(
            "The timer keeps running in the background and shows the countdown in a notification.",
            style = MaterialTheme.typography.bodySmall,
            color = c.inkFaint,
            modifier = Modifier.padding(horizontal = 4.dp),
        )

        Spacer(Modifier.height(32.dp))
    }
}

@Suppress("unused")
private fun rhythmArrangement() = Arrangement.Center
