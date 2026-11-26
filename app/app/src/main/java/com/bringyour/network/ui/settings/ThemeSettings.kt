package com.bringyour.network.ui.settings

import android.app.Activity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.core.view.WindowCompat

/**
 * Keeps the status bar's icon contrast right for the app's dark theme: light
 * icons over the dark background. Activities call this from their setContent.
 */
@Composable
fun SystemBarsAppearanceEffect(activity: Activity) {
    DisposableEffect(Unit) {
        val window = activity.window
        WindowCompat.getInsetsController(window, window.decorView)
            .isAppearanceLightStatusBars = false
        onDispose { }
    }
}
