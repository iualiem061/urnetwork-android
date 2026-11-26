package com.bringyour.network.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.ripple.RippleAlpha
import androidx.compose.material3.LocalRippleConfiguration
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RippleConfiguration
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = BlueMedium,
    secondary = BlueLight,
    tertiary = Pink,
    background = Black,
    onBackground = OffWhite,
    onPrimary = OffWhite,
)

/** The light counterpart: the same brand hues on the light palette's neutrals. */
private val LightColorScheme = lightColorScheme(
    primary = Blue500,
    secondary = Blue300,
    tertiary = Pink500,
    background = LightAppColors.bg,
    onBackground = LightAppColors.textPrimary,
    surface = LightAppColors.bgElevated,
    onSurface = LightAppColors.textPrimary,
    onPrimary = Color.White,
    surfaceVariant = LightAppColors.bgTinted,
    onSurfaceVariant = LightAppColors.textMuted,
    outlineVariant = LightAppColors.border,
)

/**
 * The state layers every clickable draws in its content color. Hover and
 * keyboard focus are stronger than Material's 8% and 10%, which barely show
 * on the app's near-black surfaces, so a pointer over a control and the
 * keyboard's focus are always apparent (adaptive app quality, Hover_States
 * and Drawable_Focus). Neither shows for touch; a press keeps Material's.
 */
private val URRippleConfiguration = RippleConfiguration(
    rippleAlpha = RippleAlpha(
        pressedAlpha = 0.10f,
        focusedAlpha = 0.20f,
        draggedAlpha = 0.16f,
        hoveredAlpha = 0.12f,
    ),
)

/**
 * The app's theme. [darkTheme] picks the palette: the device's setting by
 * default, or the user's choice from Settings -> Appearance (ThemePreferences)
 * when the app pins one.
 */
@Composable
fun URNetworkTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val appColors = if (darkTheme) DarkAppColors else LightAppColors
    val typography = if (darkTheme) DarkTypography else LightTypography

    MaterialTheme(
        colorScheme = colorScheme,
        typography = typography,
    ) {
        CompositionLocalProvider(
            LocalAppColors provides appColors,
            LocalRippleConfiguration provides URRippleConfiguration,
            content = content,
        )
    }
}
