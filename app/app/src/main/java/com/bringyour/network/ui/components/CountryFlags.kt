package com.bringyour.network.ui.components

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

/**
 * The drawable id of a country's flag (res/drawable/flag_<cc>.webp, the
 * standard flag art flagcdn serves at the flag's true aspect ratio; a few
 * retired non-ISO codes keep the old circle vector), or null when the app
 * ships no flag for the code -- the caller then falls back to the colored
 * circle.
 */
fun countryFlagResourceId(context: Context, countryCode: String?): Int? {
    val code = countryCode?.trim()?.lowercase() ?: return null
    if (code.length != 2 || !code.all { it in 'a'..'z' }) {
        return null
    }
    return try {
        val id = context.resources.getIdentifier("flag_$code", "drawable", context.packageName)
        if (id != 0) id else null
    } catch (_: Exception) {
        null
    }
}

/** The country flag of [countryCode], remembered per code; null keeps the colored circle. */
@Composable
fun rememberCountryFlag(countryCode: String?): Int? {
    val context = LocalContext.current
    return remember(countryCode) {
        countryFlagResourceId(context, countryCode)
    }
}

// the retired/non-ISO codes whose only bundled art is the old circle vector;
// those rows keep the colored circle instead of a rounded-rect flag
private val circleVectorFlagCodes = setOf(
    "ac", "an", "cp", "cq", "dg", "ea", "fx", "ic", "su", "ta", "xx", "yu",
)

/**
 * The flag drawable for the rounded-rectangle treatment: the standard rect
 * art only, null for the codes that never had it (their rows keep the
 * colored circle).
 */
@Composable
fun rememberRoundedCountryFlag(countryCode: String?): Int? {
    val flag = rememberCountryFlag(countryCode)
    val code = countryCode?.trim()?.lowercase()
    return if (code != null && code in circleVectorFlagCodes) null else flag
}
