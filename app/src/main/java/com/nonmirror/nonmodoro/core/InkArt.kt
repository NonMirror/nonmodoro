package com.nonmirror.nonmodoro.core

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale

/**
 * Draws one of the ink illustrations.
 *
 * The artwork is a two-tone drawing: black ink plus opaque white knock-outs, so
 * the character is literally wearing a white shirt and black trousers. That is
 * drawn for paper, and it only behaves on paper — on the dark theme the black
 * ink (her hair, her trousers, the desk, the chair) sinks into the dark card and
 * the figure reads as a black smudge.
 *
 * The drawing is ink, and this app already has an ink colour: near-black on the
 * light theme, cream on the dark one. So the illustration's ink follows
 * [NomoColors.ink] and the picture flips with the theme.
 *
 * A plain `ColorFilter.tint` cannot do that, because tint is `SrcIn`: it paints
 * *every* opaque pixel the same colour, so the white shirt, the face and the
 * book pages are painted over too and the whole drawing collapses into a single
 * silhouette. The white knock-outs have to be dropped first, which is what
 * [KnockOutWhite] does — it converts luminance into alpha (white → clear, ink →
 * opaque) and paints everything that is left with the theme ink.
 */
@Composable
fun InkArt(
    painter: Painter,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Fit,
) {
    val c = Nomo.colors
    if (!c.isDark) {
        // On paper the drawing is already correct: black ink, white knock-outs.
        Image(painter, contentDescription, modifier, contentScale = contentScale)
        return
    }

    Box(modifier, contentAlignment = Alignment.Center) {
        Image(
            painter = painter,
            contentDescription = contentDescription,
            modifier = Modifier.fillMaxSize(),
            contentScale = contentScale,
            colorFilter = KnockOutWhite(c.ink),
        )
    }
}

/**
 * Maps the artwork to a single ink colour:
 *
 *  * alpha out = alpha in − luminance, so opaque white becomes fully clear and
 *    black ink stays opaque (anti-aliased edges keep their coverage);
 *  * rgb out = [ink], so the drawing is tinted in one pass.
 *
 * The colour matrix runs on unpremultiplied RGBA, which is what makes the
 * alpha-from-luminance row safe: fully transparent pixels have alpha 0 going in,
 * and the last column of that row is 0, so they stay transparent.
 */
private fun KnockOutWhite(ink: Color): ColorFilter {
    val m = ColorMatrix(
        floatArrayOf(
            0f, 0f, 0f, 0f, ink.red * 255f,
            0f, 0f, 0f, 0f, ink.green * 255f,
            0f, 0f, 0f, 0f, ink.blue * 255f,
            -0.299f, -0.587f, -0.114f, 1f, 0f,
        ),
    )
    return ColorFilter.colorMatrix(m)
}
