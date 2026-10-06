package com.nonmirror.nonmodoro.core

import androidx.compose.ui.graphics.Color

/**
 * The palette is lifted from the Kofe Flow macOS app: a warm paper background,
 * near-black ink, white cards, hairline borders and a dusty terracotta accent
 * that only ever shows up in illustrations and small flourishes.
 */
data class NomoColors(
    val paper: Color,
    val card: Color,
    val ink: Color,
    val inkSoft: Color,
    val inkFaint: Color,
    val hairline: Color,
    val accent: Color,
    val glow: Color,
    val shadow: Color,
    val track: Color,
    val scrim: Color,
    val isDark: Boolean,
)

val NomoLight = NomoColors(
    paper = Color(0xFFF7F6F2),
    card = Color(0xFFFFFFFF),
    ink = Color(0xFF141414),
    inkSoft = Color(0xFF6E6A63),
    inkFaint = Color(0xFF9E998F),
    hairline = Color(0xFFE7E3DA),
    accent = Color(0xFFD8957C),
    glow = Color(0xFFF3E7E1),
    shadow = Color(0x14000000),
    track = Color(0xFFEDEAE3),
    scrim = Color(0x66000000),
    isDark = false,
)

val NomoDark = NomoColors(
    paper = Color(0xFF151412),
    card = Color(0xFF201F1C),
    ink = Color(0xFFF3F1EC),
    inkSoft = Color(0xFFA8A29A),
    inkFaint = Color(0xFF7C776F),
    hairline = Color(0xFF322F2A),
    accent = Color(0xFFD8957C),
    glow = Color(0x40D8957C),
    shadow = Color(0x33000000),
    track = Color(0xFF2C2A26),
    scrim = Color(0x99000000),
    isDark = true,
)
