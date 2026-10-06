package com.nonmirror.nonmodoro.core

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** White card with the soft paper shadow used everywhere in the app. */
@Composable
fun NomoCard(
    modifier: Modifier = Modifier,
    shape: RoundedCornerShape = RoundedCornerShape(28.dp),
    padding: PaddingValues = PaddingValues(22.dp),
    content: @Composable ColumnScope.() -> Unit,
) {
    val c = Nomo.colors
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .shadow(12.dp, shape, clip = false, ambientColor = c.shadow, spotColor = c.shadow),
        shape = shape,
        color = c.card,
    ) {
        Column(modifier = Modifier.padding(padding), content = content)
    }
}

/** Small uppercase label used above values and headlines. */
@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier, color: Color? = null) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        color = color ?: Nomo.colors.inkFaint,
        modifier = modifier,
    )
}

@Composable
fun PillButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    primary: Boolean = true,
    enabled: Boolean = true,
    height: Dp = 54.dp,
    horizontalPadding: Dp = 28.dp,
) {
    val c = Nomo.colors
    val shape = RoundedCornerShape(height / 2)
    val bg = when {
        !enabled -> c.track
        primary -> c.ink
        else -> c.card
    }
    val fg = when {
        !enabled -> c.inkFaint
        primary -> if (c.isDark) c.paper else Color.White
        else -> c.ink
    }
    Box(
        modifier = modifier
            .height(height)
            .clip(shape)
            .background(bg, shape)
            .then(if (!primary && enabled) Modifier.border(BorderStroke(1.4.dp, c.hairline), shape) else Modifier)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = horizontalPadding),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = text, style = MaterialTheme.typography.labelLarge, color = fg, maxLines = 1)
    }
}

/** The pill segmented control that sits at the top of the window. */
@Composable
fun SegmentedTabs(
    labels: List<String>,
    selected: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = Nomo.colors
    val outer = RoundedCornerShape(50)
    Row(
        modifier = modifier
            .shadow(10.dp, outer, clip = false, ambientColor = c.shadow, spotColor = c.shadow)
            .clip(outer)
            .background(c.card)
            .padding(5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        labels.forEachIndexed { index, label ->
            val isSelected = index == selected
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(if (isSelected) c.track else Color.Transparent)
                    .clickable { onSelect(index) }
                    .padding(horizontal = 16.dp, vertical = 9.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.titleMedium,
                    color = if (isSelected) c.ink else c.inkSoft,
                )
            }
        }
    }
}

@Composable
fun StatCard(
    label: String,
    value: String,
    sub: String? = null,
    modifier: Modifier = Modifier,
    content: (@Composable () -> Unit)? = null,
) {
    val c = Nomo.colors
    NomoCard(
        modifier = modifier,
        shape = RoundedCornerShape(22.dp),
        padding = PaddingValues(horizontal = 18.dp, vertical = 16.dp),
    ) {
        SectionLabel(label)
        Spacer(Modifier.height(8.dp))
        if (content != null) {
            content()
        } else {
            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium,
                color = c.ink,
            )
        }
        if (sub != null) {
            Spacer(Modifier.height(2.dp))
            Text(sub, style = MaterialTheme.typography.bodySmall, color = c.inkFaint)
        }
    }
}

/** Minimal ink switch — a pill track with a sliding knob. */
@Composable
fun NomoSwitch(checked: Boolean, onChange: (Boolean) -> Unit, modifier: Modifier = Modifier) {
    val c = Nomo.colors
    val trackW = 46.dp
    val trackH = 27.dp
    Box(
        modifier = modifier
            .size(width = trackW, height = trackH)
            .clip(RoundedCornerShape(50))
            .background(if (checked) c.ink else c.track)
            .clickable { onChange(!checked) }
            .padding(3.dp),
        contentAlignment = if (checked) Alignment.CenterEnd else Alignment.CenterStart,
    ) {
        Box(
            Modifier
                .size(trackH - 6.dp)
                .clip(CircleShape)
                .background(if (checked) (if (c.isDark) c.paper else Color.White) else c.card),
        )
    }
}

@Composable
fun SettingRow(
    title: String,
    subtitle: String? = null,
    trailing: @Composable () -> Unit,
    onClick: (() -> Unit)? = null,
) {
    val c = Nomo.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = c.ink)
            if (subtitle != null) {
                Spacer(Modifier.height(2.dp))
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = c.inkFaint)
            }
        }
        Spacer(Modifier.width(14.dp))
        trailing()
    }
}

@Composable
fun Hairline(modifier: Modifier = Modifier) {
    Box(
        modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(Nomo.colors.hairline),
    )
}

/** Clickable without the Material ripple — the flat paper look reads better. */
@Composable
fun Modifier.clickableNoRipple(onClick: () -> Unit): Modifier {
    val source = remember { MutableInteractionSource() }
    return this.clickable(
        interactionSource = source,
        indication = null,
        onClick = onClick,
    )
}

/** Tiny +/- stepper used for minute values. */
@Composable
fun Stepper(
    value: Int,
    onChange: (Int) -> Unit,
    min: Int,
    max: Int,
    step: Int = 1,
    suffix: String = "min",
) {
    val c = Nomo.colors
    Row(verticalAlignment = Alignment.CenterVertically) {
        StepperButton(enabled = value > min, onClick = { onChange((value - step).coerceAtLeast(min)) }) { minusGlyph(c) }
        Text(
            text = "$value $suffix",
            style = MaterialTheme.typography.titleMedium,
            color = c.ink,
            textAlign = TextAlign.Center,
            modifier = Modifier.width(74.dp),
        )
        StepperButton(enabled = value < max, onClick = { onChange((value + step).coerceAtMost(max)) }) { plusGlyph(c) }
    }
}

@Composable
private fun StepperButton(enabled: Boolean, onClick: () -> Unit, content: @Composable () -> Unit) {
    val c = Nomo.colors
    Box(
        modifier = Modifier
            .size(34.dp)
            .clip(CircleShape)
            .background(if (enabled) c.card else c.track)
            .border(BorderStroke(1.4.dp, if (enabled) c.hairline else Color.Transparent), CircleShape)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { content() }
}

@Composable
private fun minusGlyph(c: NomoColors) {
    Box(Modifier.width(13.dp).height(2.dp).background(if (c.isDark) c.ink else c.ink, RoundedCornerShape(2.dp)))
}

@Composable
private fun plusGlyph(c: NomoColors) {
    Box(contentAlignment = Alignment.Center) {
        Box(Modifier.width(13.dp).height(2.dp).background(c.ink, RoundedCornerShape(2.dp)))
        Box(Modifier.width(2.dp).height(13.dp).background(c.ink, RoundedCornerShape(2.dp)))
    }
}

/** Row of small filled/hollow dots — used for cycle progress and the week strip. */
@Composable
fun DotRow(
    count: Int,
    filled: Int,
    modifier: Modifier = Modifier,
    size: Dp = 7.dp,
    spacing: Dp = 6.dp,
    fillColor: Color? = null,
    emptyColor: Color? = null,
) {
    val c = Nomo.colors
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(spacing)) {
        repeat(count) { i ->
            Box(
                Modifier
                    .size(size)
                    .clip(CircleShape)
                    .background(if (i < filled) (fillColor ?: c.ink) else (emptyColor ?: c.track)),
            )
        }
    }
}
