package com.bringyour.network.ui.settings

import android.content.Context
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.bringyour.network.R
import com.bringyour.network.ui.theme.BlueMedium
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * The app's navigation styles, the look of the main bottom navigation. The
 * classic full-width bar is the default; the other three are floating
 * alternatives the user picks in Settings.
 */
enum class NavStyle {
    /** the current full-width Material bar */
    CLASSIC,
    /** a floating pill bar whose selection springs and stretches between tabs */
    MORPHING,
    /** a floating icon-only capsule with a circular highlight */
    CAPSULE,
    /** a vertical dock on the right edge of the screen */
    SIDE_DOCK,
}

/**
 * The chosen navigation style, stored in its own SharedPreferences (name
 * per style, "CLASSIC" the default) and published as a flow, so the nav host
 * re-renders the bar the moment a choice is made. Loaded on first read, which
 * happens in the nav host, so no start path needs to prime it.
 */
object NavStylePreferences {

    private const val PREFS_NAME = "nav_style_prefs"
    private const val KEY_NAV_STYLE = "nav_style"

    val navStyle = MutableStateFlow(NavStyle.CLASSIC)

    private var loaded = false

    @Synchronized
    fun ensureLoaded(context: Context) {
        if (loaded) return
        val stored = context
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_NAV_STYLE, null)
        navStyle.value = stored
            ?.let { name -> NavStyle.entries.firstOrNull { it.name == name } }
            ?: NavStyle.CLASSIC
        loaded = true
    }

    /** Pins [style] and publishes it; the bar switches immediately. */
    fun set(context: Context, style: NavStyle) {
        ensureLoaded(context)
        context
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_NAV_STYLE, style.name)
            .apply()
        navStyle.value = style
    }
}

/** The style's name as the settings row and the picker show it. */
@Composable
fun navStyleDisplayName(style: NavStyle): String = stringResource(
    id = when (style) {
        NavStyle.CLASSIC -> R.string.nav_style_classic
        NavStyle.MORPHING -> R.string.nav_style_morphing
        NavStyle.CAPSULE -> R.string.nav_style_capsule
        NavStyle.SIDE_DOCK -> R.string.nav_style_side_dock
    }
)

/**
 * The style the app runs with right now, as a state: the preferences are read
 * once (remember) and the flow is collected, so a choice made anywhere
 * (the picker sheet) recomposes whoever reads this.
 */
@Composable
fun currentNavStyle(): NavStyle {
    val context = LocalContext.current
    remember { NavStylePreferences.ensureLoaded(context.applicationContext) }
    return NavStylePreferences.navStyle.collectAsState().value
}

/**
 * The navigation style picker: a radio list of the four styles, each named
 * the way the settings row names it. A choice applies immediately and the
 * sheet closes.
 */
@Composable
fun NavStylePickerSheet(
    selected: NavStyle,
    onSelected: (NavStyle) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.background,
        title = {
            Text(
                stringResource(id = R.string.nav_style_picker_title),
                style = MaterialTheme.typography.headlineSmall,
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                NavStyle.entries.forEach { style ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelected(style) }
                            .padding(vertical = 14.dp, horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            navStyleDisplayName(style),
                            style = MaterialTheme.typography.bodyLarge,
                            color = if (style == selected) BlueMedium else MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.weight(1f),
                        )
                        if (style == selected) {
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
