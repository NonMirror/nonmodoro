package com.nonmirror.nonmodoro.core

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import com.nonmirror.nonmodoro.data.ThemeMode

val LocalNomo = staticCompositionLocalOf { NomoLight }

object Nomo {
    val colors: NomoColors
        @Composable get() = LocalNomo.current
}

@Composable
fun NomoTheme(mode: ThemeMode = ThemeMode.System, content: @Composable () -> Unit) {
    val dark = when (mode) {
        ThemeMode.System -> isSystemInDarkTheme()
        ThemeMode.Light -> false
        ThemeMode.Dark -> true
    }
    val nomo = if (dark) NomoDark else NomoLight

    CompositionLocalProvider(LocalNomo provides nomo) {
        MaterialTheme(
            colorScheme = if (dark) {
                darkColorScheme(
                    background = nomo.paper,
                    surface = nomo.card,
                    onBackground = nomo.ink,
                    onSurface = nomo.ink,
                    primary = nomo.ink,
                    onPrimary = nomo.card,
                    outline = nomo.hairline,
                )
            } else {
                lightColorScheme(
                    background = nomo.paper,
                    surface = nomo.card,
                    onBackground = nomo.ink,
                    onSurface = nomo.ink,
                    primary = nomo.ink,
                    onPrimary = nomo.card,
                    outline = nomo.hairline,
                )
            },
            typography = NomoTypography,
            content = content,
        )
    }
}
