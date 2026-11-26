package com.bringyour.network.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import com.bringyour.network.R

val gravityFamily = FontFamily(
    Font(R.font.abcgravity_extended, FontWeight.Normal),
)

val gravityCondensedFamily = FontFamily(
    Font(R.font.abcgravity_extra_condensed, FontWeight.Normal),
)

val ppNeueMontreal = FontFamily(
    Font(R.font.pp_neue_montreal_regular, FontWeight.Normal),
)

val ppNeueBitBold = FontFamily(
    Font(R.font.pp_neue_bit_bold, FontWeight.Bold),
)

/**
 * The shared Material typography, with the theme's text color on the styles
 * that carried a fixed white. One instance per theme; `Typography` keeps
 * naming the dark one for the previews and any legacy reader.
 */
private fun appTypography(textColor: Color): Typography = Typography(
    bodyLarge = TextStyle(
        fontFamily = ppNeueMontreal,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        color = textColor
    ),

    bodyMedium = TextStyle(
        fontFamily = ppNeueMontreal,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
    ),

    bodySmall = TextStyle(
        fontFamily = ppNeueMontreal,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
    ),

    headlineLarge = TextStyle(
        fontFamily = gravityFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 32.sp,
        lineHeight = 36.sp,
        letterSpacing = 0.sp,
        color = textColor
    ),

    headlineMedium = TextStyle(
        fontFamily = gravityCondensedFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 32.sp,
        lineHeight = 32.sp,
        letterSpacing = 0.sp,
        color = textColor
    ),

    headlineSmall = TextStyle(
        fontFamily = gravityFamily,
        fontWeight = FontWeight(900),
        fontSize = 24.sp,
        lineHeight = 32.sp,
        letterSpacing = 0.sp,
        color = textColor
    ),
)

/** The dark theme's typography (white text), as the app always shipped. */
val DarkTypography = appTypography(MainTextBase)

/** The light theme's typography (ink text). */
val LightTypography = appTypography(LightAppColors.textPrimary)

/** Legacy alias of [DarkTypography]. */
val Typography = DarkTypography

// The standalone display styles carry no color: text that uses them inherits
// the ambient content color, which the theme already sets per mode.
val TopBarTitleTextStyle = TextStyle(
    fontSize = 24.sp,
    lineHeight = 24.sp,
    fontFamily = ppNeueBitBold,
    fontWeight = FontWeight(700),
    color = Color.Unspecified,
    textAlign = TextAlign.Center,
)

val NeueBitLargeTextStyle = TextStyle(
    fontSize = 28.sp,
//    lineHeight = 24.sp,
    fontFamily = ppNeueBitBold,
    fontWeight = FontWeight(700),
    color = Color.Unspecified,
    textAlign = TextAlign.Center,
)

val NeueBitSmallTextStyle = TextStyle(
    fontSize = 18.sp,
//    lineHeight = 24.sp,
    fontFamily = ppNeueBitBold,
    fontWeight = FontWeight(700),
    color = Color.Unspecified,
    textAlign = TextAlign.Center,
)

val HeadingLargeCondensed = TextStyle(
    fontSize = 40.sp,
    fontFamily = gravityCondensedFamily,
    fontWeight = FontWeight(900),
)
