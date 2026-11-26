package com.bringyour.network.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * The app's semantic palette: one set of names for the colors the whole UI
 * draws with. The set mirrors the historic constants (Color.kt), and the app
 * resolves it to that single dark palette. Screens read the tokens through
 * [AppTheme] (bg, bgElevated, textPrimary, ...).
 */
@Immutable
data class AppColors(
    val isDark: Boolean,
    /** the screens' background */
    val bg: Color,
    /** cards and rows on the background */
    val bgElevated: Color,
    /** tinted containers (the location row's fill, settings wells) */
    val bgTinted: Color,
    /** bottom sheets' and dialogs' surface */
    val sheet: Color,
    /** hairlines and card outlines */
    val border: Color,
    /** primary text on bg/bgElevated */
    val textPrimary: Color,
    /** secondary text */
    val textMuted: Color,
    /** tertiary text (notes, resolver urls) */
    val textFaint: Color,
)

val DarkAppColors = AppColors(
    isDark = true,
    bg = Black,
    bgElevated = OffBlack,
    bgTinted = MainTintedBackgroundBase,
    sheet = SheetBlack,
    border = MainBorderBase,
    textPrimary = MainTextBase,
    textMuted = TextMuted,
    textFaint = TextFaint,
)

val LocalAppColors = staticCompositionLocalOf { DarkAppColors }

/**
 * The semantic tokens of the running theme, as `MaterialTheme` exposes the M3
 * scheme: `AppTheme.bg`, `AppTheme.textPrimary`, `AppTheme.border`, ...
 * Reads the [LocalAppColors] the theme provides.
 */
object AppTheme {
    val isDark: Boolean
        @Composable @ReadOnlyComposable get() = LocalAppColors.current.isDark

    val bg: Color
        @Composable @ReadOnlyComposable get() = LocalAppColors.current.bg

    val bgElevated: Color
        @Composable @ReadOnlyComposable get() = LocalAppColors.current.bgElevated

    val bgTinted: Color
        @Composable @ReadOnlyComposable get() = LocalAppColors.current.bgTinted

    val sheet: Color
        @Composable @ReadOnlyComposable get() = LocalAppColors.current.sheet

    val border: Color
        @Composable @ReadOnlyComposable get() = LocalAppColors.current.border

    val textPrimary: Color
        @Composable @ReadOnlyComposable get() = LocalAppColors.current.textPrimary

    val textMuted: Color
        @Composable @ReadOnlyComposable get() = LocalAppColors.current.textMuted

    val textFaint: Color
        @Composable @ReadOnlyComposable get() = LocalAppColors.current.textFaint
}
