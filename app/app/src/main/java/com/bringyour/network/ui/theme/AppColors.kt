package com.bringyour.network.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * The app's semantic palette: one set of names for the colors the whole UI
 * draws with, resolved per theme. The dark set mirrors the historic constants
 * (Color.kt); the light set is their light-mode counterpart. Screens read the
 * tokens through [AppTheme] (bg, bgElevated, textPrimary, ...), never a
 * raw dark-only constant, so both themes render.
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
    /** the floating navigation bar's translucent glass fill */
    val navGlass: Color,
    /** a top gradient's start for the glass sheen */
    val glassSheen: Color,
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
    navGlass = Black.copy(alpha = 0.86f),
    glassSheen = Color.White.copy(alpha = 0.08f),
)

val LightAppColors = AppColors(
    isDark = false,
    bg = Color(0xFFF4F5F7),
    bgElevated = Color(0xFFFFFFFF),
    bgTinted = Color(0xFFECEEF2),
    sheet = Color(0xFFFFFFFF),
    border = Color(0x14101418),
    textPrimary = Color(0xFF191C1F),
    textMuted = Color(0xFF5B6470),
    textFaint = Color(0xFF9AA3AE),
    navGlass = Color.White.copy(alpha = 0.90f),
    glassSheen = Color.White.copy(alpha = 0.45f),
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

    val navGlass: Color
        @Composable @ReadOnlyComposable get() = LocalAppColors.current.navGlass

    val glassSheen: Color
        @Composable @ReadOnlyComposable get() = LocalAppColors.current.glassSheen
}
