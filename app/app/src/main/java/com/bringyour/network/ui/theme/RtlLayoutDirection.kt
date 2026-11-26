package com.bringyour.network.ui.theme

import android.text.TextUtils
import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection

/**
 * Lays the whole app out right-to-left when the running locale is an RTL
 * language (Persian, Arabic, ...), so every screen mirrors for it: labels
 * start at the right, chevrons and values sit at the left, lists scroll from
 * the right, and Persian text aligns right. The locale follows the app
 * language choice (appcompat per-app locales) or the device's language.
 *
 * Latin and digits inside a Text stay readable as left-to-right runs by the
 * platform's bidi resolution; pure technical strings (handles, codes,
 * UUIDs, IPs) additionally pin themselves with TextDirection.Ltr where they
 * appear.
 */
@Composable
fun ProvideRtlLayoutDirection(
    content: @Composable () -> Unit,
) {
    val localeList = LocalConfiguration.current.locales
    val locale = if (localeList.isEmpty) null else localeList.get(0)
    val rtl = locale != null &&
        !locale.language.isNullOrEmpty() &&
        TextUtils.getLayoutDirectionFromLocale(locale) == View.LAYOUT_DIRECTION_RTL
    CompositionLocalProvider(
        LocalLayoutDirection provides if (rtl) LayoutDirection.Rtl else LayoutDirection.Ltr,
    ) {
        content()
    }
}
