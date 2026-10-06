package com.nonmirror.nonmodoro.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import com.nonmirror.nonmodoro.core.Nomo
import kotlin.math.PI
import kotlin.math.sin

/**
 * Music notes that drift upwards and fade, staggered — the same flourish the
 * reference app floats over its focus illustration.
 *
 * Everything here was measured off a screen recording (10 frames over 5.53 s):
 * a note is 11.8% of the illustration height, the three notes sit at x 67% / 78%
 * / 89% and travel 64.8% of the height per cycle, and they climb at ~35 px/s,
 * which works out to an 8.2 s cycle. The leftmost note leads the others and
 * carries the short staff lines. The canvas is expected to be the artwork box.
 */
@Composable
fun FloatingNotes(modifier: Modifier = Modifier, tint: Color? = null) {
    val color = tint ?: Nomo.colors.accent
    val transition = rememberInfiniteTransition(label = "notes")
    val progress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(8200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "noteProgress",
    )

    Canvas(modifier) {
        // xFraction, phase offset, hasStaffLines
        val slots = listOf(
            floatArrayOf(0.885f, 0.00f, 0f),
            floatArrayOf(0.775f, 0.34f, 0f),
            floatArrayOf(0.670f, 0.67f, 1f),
        )
        slots.forEach { slot ->
            val t = (progress + slot[1]) % 1f
            val alpha = when {
                t < 0.12f -> t / 0.12f
                t > 0.78f -> ((1f - t) / 0.22f).coerceIn(0f, 1f)
                else -> 1f
            } * 0.95f
            val s = size.minDimension / 334f
            val sway = sin(t * PI * 4).toFloat()
            val cx = size.width * slot[0] + sway * size.width * 0.022f
            val cy = size.height * (0.46f - t * 0.648f)
            drawNote(cx, cy, s, color, alpha, slot[2] > 0.5f)
        }
    }
}

private fun DrawScope.drawNote(
    cx: Float,
    cy: Float,
    s: Float,
    color: Color,
    alpha: Float,
    staffLines: Boolean,
) {
    val headW = 15f * s
    val headH = 11f * s
    val stemTop = cy - 34f * s

    rotate(degrees = -22f, pivot = Offset(cx, cy)) {
        drawOval(
            color = color.copy(alpha = alpha),
            topLeft = Offset(cx - headW / 2f, cy - headH / 2f),
            size = Size(headW, headH),
        )
    }
    val stemX = cx + headW * 0.36f
    drawLine(
        color = color.copy(alpha = alpha),
        start = Offset(stemX, cy - headH * 0.15f),
        end = Offset(stemX, stemTop),
        strokeWidth = 2.9f * s,
        cap = StrokeCap.Round,
    )
    // Flag
    val flag = Path().apply {
        moveTo(stemX, stemTop)
        cubicTo(
            stemX + 13f * s, stemTop + 7f * s,
            stemX + 14f * s, stemTop + 18f * s,
            stemX + 4f * s, stemTop + 23f * s,
        )
    }
    drawPath(flag, color.copy(alpha = alpha), style = Stroke(width = 2.7f * s, cap = StrokeCap.Round))

    if (staffLines) {
        repeat(3) { i ->
            val y = stemTop + (2f + i * 6f) * s
            drawLine(
                color = color.copy(alpha = alpha * 0.85f),
                start = Offset(stemX - 26f * s, y),
                end = Offset(stemX - 6f * s, y),
                strokeWidth = 2.4f * s,
                cap = StrokeCap.Round,
            )
        }
    }
}
