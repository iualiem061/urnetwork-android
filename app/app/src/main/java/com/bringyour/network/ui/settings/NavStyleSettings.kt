package com.bringyour.network.ui.settings

import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.bringyour.network.R
import com.bringyour.network.ui.components.bouncyClick
import com.bringyour.network.ui.components.entrance
import com.bringyour.network.ui.theme.AppTheme
import com.bringyour.network.ui.theme.BlueMedium
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * The app's navigation styles, the look of the main bottom navigation. The
 * classic full-width bar is the default; the other two are floating
 * alternatives the user picks in Settings.
 */
enum class NavStyle {
    /** the current full-width Material bar */
    CLASSIC,
    /** a floating pill bar whose highlight glides between tabs */
    MORPHING,
    /** a floating icon-only capsule with a circular highlight */
    CAPSULE,
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
 * The navigation style picker: a radio list of the three styles, each named
 * the way the settings row names it, each arriving with the soft cascade and
 * answering a press with the app's bounce. A choice applies immediately and
 * the sheet closes.
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
        // no title: the settings row it opens from carries the description
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                NavStyle.entries.forEachIndexed { styleIndex, style ->
                    val isChosen = style == selected
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (isChosen) AppTheme.bgTinted else Color.Transparent)
                            .bouncyClick { onSelected(style) }
                            .padding(vertical = 14.dp, horizontal = 12.dp)
                            .entrance(styleIndex),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            navStyleDisplayName(style),
                            style = MaterialTheme.typography.bodyLarge,
                            color = if (isChosen) BlueMedium else MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.weight(1f),
                        )
                        AnimatedVisibility(visible = isChosen) {
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
