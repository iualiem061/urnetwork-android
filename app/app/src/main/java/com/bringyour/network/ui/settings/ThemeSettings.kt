package com.bringyour.network.ui.settings

import android.app.Activity
import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import com.bringyour.network.R
import com.bringyour.network.ui.theme.BlueMedium

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** The app's appearance: follow the device, always light or always dark. */
enum class ThemeMode(val tag: String) {
    SYSTEM("system"),
    LIGHT("light"),
    DARK("dark");

    companion object {
        fun fromTag(tag: String?): ThemeMode =
            entries.firstOrNull { it.tag == tag } ?: SYSTEM
    }
}

/**
 * The chosen theme mode, kept in the app's private preferences and observed
 * as a flow; MainActivity and LoginActivity resolve it into the darkTheme
 * the URNetworkTheme takes. A mode pins the app UI and the AppCompat night
 * mode; SYSTEM follows the device.
 */
object ThemePreferences {

    private const val PREFERENCES_NAME = "app_appearance"
    private const val KEY_THEME_MODE = "theme_mode"

    private val _mode = MutableStateFlow(ThemeMode.SYSTEM)
    val mode: StateFlow<ThemeMode> = _mode.asStateFlow()

    /** Reads the stored choice once at process start. */
    fun load(context: Context) {
        val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
        val mode = ThemeMode.fromTag(preferences.getString(KEY_THEME_MODE, null))
        _mode.value = mode
        applyNightMode(mode)
    }

    /** Saves the choice and applies it to the AppCompat night mode too. */
    fun set(context: Context, mode: ThemeMode) {
        context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_THEME_MODE, mode.tag)
            .apply()
        _mode.value = mode
        applyNightMode(mode)
    }

    /**
     * The AppCompat night mode of the choice, so the AppCompat resources
     * (the launch window's background among them) follow it too.
     */
    private fun applyNightMode(mode: ThemeMode) {
        AppCompatDelegate.setDefaultNightMode(
            when (mode) {
                ThemeMode.SYSTEM -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
                ThemeMode.LIGHT -> AppCompatDelegate.MODE_NIGHT_NO
                ThemeMode.DARK -> AppCompatDelegate.MODE_NIGHT_YES
            }
        )
    }
}

/** The mode's name in the reader's language. */
@Composable
fun themeModeDisplayName(mode: ThemeMode): String =
    when (mode) {
        ThemeMode.SYSTEM -> stringResource(id = R.string.theme_system)
        ThemeMode.LIGHT -> stringResource(id = R.string.theme_light)
        ThemeMode.DARK -> stringResource(id = R.string.theme_dark)
    }

/**
 * Keeps the status bar's icon contrast right for the theme: light icons over
 * the dark theme's background, dark icons over the light theme's. Activities
 * call this from their setContent while the theme is resolved.
 */
@Composable
fun SystemBarsAppearanceEffect(darkTheme: Boolean, activity: Activity) {
    DisposableEffect(darkTheme) {
        val window = activity.window
        WindowCompat.getInsetsController(window, window.decorView)
            .isAppearanceLightStatusBars = !darkTheme
        onDispose { }
    }
}

/**
 * The appearance picker: light, dark or system default, as a radio list like
 * the language picker. A choice applies immediately and the dialog closes.
 */
@Composable
fun ThemeModePickerSheet(
    selectedMode: ThemeMode,
    onSelected: (ThemeMode) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.background,
        title = {
            Text(
                stringResource(id = R.string.appearance),
                style = MaterialTheme.typography.headlineSmall,
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                ThemeMode.entries.forEach { mode ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelected(mode) }
                            .padding(vertical = 14.dp, horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            themeModeDisplayName(mode),
                            style = MaterialTheme.typography.bodyLarge,
                            color = if (mode == selectedMode) BlueMedium else MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.weight(1f),
                        )
                        if (mode == selectedMode) {
                            Icon(
                                imageVector = Icons.Filled.Check,
                                contentDescription = null,
                                tint = BlueMedium,
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(id = R.string.close))
            }
        },
    )
}
