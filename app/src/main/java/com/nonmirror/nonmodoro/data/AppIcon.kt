package com.nonmirror.nonmodoro.data

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import com.nonmirror.nonmodoro.R

/**
 * The launcher-icon variants the user can choose between.
 *
 * Android has no API to swap an app's icon directly. The trick is that the
 * launcher entry lives on an `<activity-alias>` rather than on `MainActivity`,
 * and each alias carries its own icon; switching means enabling the chosen alias
 * and disabling the other ten. See the manifest.
 *
 * [foreground] is the full-colour drawable used to draw previews inside the app;
 * [mono] is its ink-only twin (the same shape with the paper knocked out), which
 * is what the dark theme tints to preview the icon on a dark tile;
 * [launcher] is the adaptive-icon wrapper the system renders on the home screen.
 */
enum class AppIcon(
    val label: String,
    val className: String,
    val foreground: Int,
    val mono: Int,
    val launcher: Int,
) {
    Tomato("Tomato", "com.nonmirror.nonmodoro.icon.Tomato", R.drawable.ic_fg_tomato, R.drawable.ic_fg_mono_tomato, R.mipmap.ic_launcher_tomato),
    Hourglass("Hourglass", "com.nonmirror.nonmodoro.icon.Hourglass", R.drawable.ic_fg_hourglass, R.drawable.ic_fg_mono_hourglass, R.mipmap.ic_launcher_hourglass),
    Snail("Snail", "com.nonmirror.nonmodoro.icon.Snail", R.drawable.ic_fg_snail, R.drawable.ic_fg_mono_snail, R.mipmap.ic_launcher_snail),
    Alarm("Alarm clock", "com.nonmirror.nonmodoro.icon.Alarm", R.drawable.ic_fg_alarm, R.drawable.ic_fg_mono_alarm, R.mipmap.ic_launcher_alarm),
    Cat("Cat", "com.nonmirror.nonmodoro.icon.Cat", R.drawable.ic_fg_cat, R.drawable.ic_fg_mono_cat, R.mipmap.ic_launcher_cat),
    Bell("Bell", "com.nonmirror.nonmodoro.icon.Bell", R.drawable.ic_fg_bell, R.drawable.ic_fg_mono_bell, R.mipmap.ic_launcher_bell),
    Sitter("Sitting figure", "com.nonmirror.nonmodoro.icon.Sitter", R.drawable.ic_fg_sitter, R.drawable.ic_fg_mono_sitter, R.mipmap.ic_launcher_sitter),
    Target("Focus rings", "com.nonmirror.nonmodoro.icon.Target", R.drawable.ic_fg_target, R.drawable.ic_fg_mono_target, R.mipmap.ic_launcher_target),
    Sprout("Sprout", "com.nonmirror.nonmodoro.icon.Sprout", R.drawable.ic_fg_sprout, R.drawable.ic_fg_mono_sprout, R.mipmap.ic_launcher_sprout),
    Stones("Zen stones", "com.nonmirror.nonmodoro.icon.Stones", R.drawable.ic_fg_stones, R.drawable.ic_fg_mono_stones, R.mipmap.ic_launcher_stones),
    Candle("Candle", "com.nonmirror.nonmodoro.icon.Candle", R.drawable.ic_fg_candle, R.drawable.ic_fg_mono_candle, R.mipmap.ic_launcher_candle),
    ;

    companion object {
        val default: AppIcon = Tomato

        fun fromName(name: String?): AppIcon =
            entries.firstOrNull { it.name == name } ?: default

        /**
         * Enables [chosen] and disables every other alias.
         *
         * `DONT_KILL_APP` keeps the process alive so the settings screen does not
         * vanish mid-tap; the launcher picks the new icon up on its own.
         */
        fun apply(context: Context, chosen: AppIcon) {
            val pm = context.packageManager
            entries.forEach { icon ->
                val state = if (icon == chosen) {
                    PackageManager.COMPONENT_ENABLED_STATE_ENABLED
                } else {
                    PackageManager.COMPONENT_ENABLED_STATE_DISABLED
                }
                runCatching {
                    pm.setComponentEnabledSetting(
                        ComponentName(context.packageName, icon.className),
                        state,
                        PackageManager.DONT_KILL_APP,
                    )
                }
            }
        }

        /**
         * False when the manifest disagrees with the saved preference — which
         * happens on a fresh install, or if the app was restored from a backup.
         */
        fun isApplied(context: Context, chosen: AppIcon): Boolean = runCatching {
            context.packageManager.getComponentEnabledSetting(
                ComponentName(context.packageName, chosen.className),
            ) != PackageManager.COMPONENT_ENABLED_STATE_DISABLED
        }.getOrDefault(true)
    }
}
