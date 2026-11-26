package com.bringyour.network.ui.settings

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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.os.LocaleListCompat
import com.bringyour.network.R
import com.bringyour.network.ui.theme.BlueMedium

/** The tag that means "follow the device's language settings". */
const val LANGUAGE_TAG_SYSTEM = "system"

/**
 * The app's language offering: Persian plus the app's own translations.
 * Each entry names its tag and the language in its own endonym, so every
 * reader finds theirs written the way they write it.
 */
val appLanguages: List<Pair<String, String>> = listOf(
    LANGUAGE_TAG_SYSTEM to "",
    "fa" to "فارسی",
    "ar" to "العربية",
    "cs" to "Čeština",
    "de" to "Deutsch",
    "el" to "Ελληνικά",
    "en" to "English",
    "es" to "Español",
    "es-MX" to "Español (México)",
    "fr" to "Français",
    "hi" to "हिन्दी",
    "ja" to "日本語",
    "ko" to "한국어",
    "nl" to "Nederlands",
    "pl" to "Polski",
    "pt" to "Português",
    "pt-BR" to "Português (Brasil)",
    "ru" to "Русский",
    "sw" to "Kiswahili",
    "th" to "ไทย",
    "vi" to "Tiếng Việt",
    "zh" to "中文",
)

/** The tag the app currently runs with ("system" when it follows the device). */
fun currentAppLanguageTag(): String {
    val locales = AppCompatDelegate.getApplicationLocales()
    if (locales.isEmpty) {
        return LANGUAGE_TAG_SYSTEM
    }
    return locales.toLanguageTags()
}

/**
 * Pins the app's language, or returns it to the device's setting with the
 * empty list. The appcompat storage keeps the choice across restarts, and on
 * Android 13 and up the system's per-app language settings read it too.
 */
fun applyAppLanguage(tag: String) {
    val locales = if (tag == LANGUAGE_TAG_SYSTEM) {
        LocaleListCompat.getEmptyLocaleList()
    } else {
        LocaleListCompat.forLanguageTags(tag)
    }
    AppCompatDelegate.setApplicationLocales(locales)
}

/** The name of a language as its own speakers write it; the system entry reads in the reader's language. */
@Composable
fun languageDisplayName(tag: String): String {
    if (tag == LANGUAGE_TAG_SYSTEM) {
        return stringResource(id = R.string.system_default_language)
    }
    return appLanguages
        .firstOrNull { it.first == tag }
        ?.second
        ?: tag
}

/**
 * The language picker: a radio list of every language the app ships, each
 * named in its own endonym, with "System default" first. A choice applies
 * immediately and the sheet closes.
 */
@Composable
fun LanguagePickerSheet(
    selectedTag: String,
    onSelected: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.background,
        title = {
            Text(
                stringResource(id = R.string.language),
                style = MaterialTheme.typography.headlineSmall,
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                appLanguages.forEach { (tag, endonym) ->
                    val label = if (tag == LANGUAGE_TAG_SYSTEM) {
                        stringResource(id = R.string.system_default_language)
                    } else {
                        endonym
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelected(tag) }
                            .padding(vertical = 14.dp, horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            label,
                            style = MaterialTheme.typography.bodyLarge,
                            color = if (tag == selectedTag) BlueMedium else MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.weight(1f),
                        )
                        if (tag == selectedTag) {
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
