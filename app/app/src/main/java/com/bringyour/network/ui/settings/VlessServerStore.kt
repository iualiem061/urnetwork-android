package com.bringyour.network.ui.settings

import android.content.Context
import androidx.annotation.VisibleForTesting
import com.bringyour.sdk.Sdk
import com.bringyour.sdk.VlessSettings
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID

/**
 * The saved VLESS relay servers: the user's own library of `vless://` links,
 * pasted one by one or imported together from a subscription (a link whose
 * body is a list of links, plain or base64), so switching between many
 * servers is a tap instead of retyping a link.
 *
 * The list and the active server's id live in one shared preferences file as
 * JSON; the sdk still owns the settings that actually dial (the active server
 * only fills and saves the form, which is what applies it to the space).
 */
@Serializable
data class VlessServer(
    val id: String,
    val name: String,
    val link: String,
    /** `address:port`, kept for the row's second line. */
    val address: String,
)

/** The most preferences will hold; a subscription pastes far more than anyone scrolls. */
val VLESS_SERVERS_MAX = 200

@VisibleForTesting
val vlessServerJson = Json { ignoreUnknownKeys = true }

/**
 * Reads and writes the server library. One instance per screen is fine: the
 * file is tiny, and every write rewrites it whole.
 */
class VlessServerStore(context: Context) {

    private val prefs =
        context.getSharedPreferences("vless_relay_servers", Context.MODE_PRIVATE)

    fun load(): List<VlessServer> = runCatching {
        val raw = prefs.getString(SERVERS_KEY, null) ?: return emptyList()
        vlessServerJson.decodeFromString<List<VlessServer>>(raw)
    }.getOrDefault(emptyList())

    fun store(servers: List<VlessServer>) {
        prefs.edit()
            .putString(SERVERS_KEY, vlessServerJson.encodeToString(servers.take(VLESS_SERVERS_MAX)))
            .apply()
    }

    private companion object {
        const val SERVERS_KEY = "servers"
    }
}

/** What one import answered with. */
sealed class VlessImportResult {
    /** The new servers that joined the library (duplicates and cap drops aside). */
    data class Added(val servers: List<VlessServer>) : VlessImportResult()

    /** The text and its base64 layer, if any, held no readable link. */
    object NoneReadable : VlessImportResult()

    /** A subscription URL was given but could not be fetched. */
    object FetchFailed : VlessImportResult()
}

private val vlessUrlRegex = Regex("^https?://\\S+$", RegexOption.IGNORE_CASE)

/** The split every link list uses, shared with the relay network fetch. */
internal val whitespaceRegex = Regex("\\s+")

/**
 * The import itself: a subscription URL is fetched, a base64 body is decoded,
 * the text is split at whitespace and every `vless://` link that parses joins
 * the library, named by its remark (`#fragment`) or its address. Existing
 * links are not added twice. Runs off the main thread.
 */
fun importVlessServers(text: String, existing: List<VlessServer>): VlessImportResult {
    var source = text.trim()
    if (source.isEmpty()) {
        return VlessImportResult.NoneReadable
    }

    // a subscription link: its body is the list (plain links or base64)
    if (!source.contains("vless://") && vlessUrlRegex.matches(source)) {
        val fetched = httpGetString(source) ?: return VlessImportResult.FetchFailed
        source = fetched.trim()
    }

    if (!source.contains("vless://")) {
        val decoded = decodeBase64Text(source)
        if (decoded != null && decoded.contains("vless://")) {
            source = decoded
        }
    }

    val knownLinks = existing.map { it.link }.toHashSet()
    val added = mutableListOf<VlessServer>()

    for (candidate in source.split(whitespaceRegex)) {
        if (!candidate.startsWith("vless://")) continue
        if (knownLinks.contains(candidate)) continue

        val values = parseVlessLinkOrNull(candidate) ?: continue
        val name = values.name.ifBlank {
            "${values.address}:${values.port}".trimStart(':')
        }
        val server = VlessServer(
            id = UUID.randomUUID().toString(),
            name = name,
            link = candidate,
            address = "${values.address}:${values.port}",
        )
        added.add(server)
        knownLinks.add(candidate)
    }

    if (added.isEmpty()) {
        return VlessImportResult.NoneReadable
    }

    // the cap is a backstop, not a feature: whatever does not fit is dropped
    val room = (VLESS_SERVERS_MAX - existing.size).coerceAtLeast(0)
    return VlessImportResult.Added(added.take(room))
}

/**
 * The library to keep when `added` joins `existing`: the new servers first
 * (newest first), then the old ones, cut at the cap.
 */
fun mergeVlessServers(existing: List<VlessServer>, added: List<VlessServer>): List<VlessServer> =
    (added + existing).take(VLESS_SERVERS_MAX)

/** One `vless://` link as plain values, or null when it does not read. */
fun parseVlessLinkOrNull(link: String): VlessSettings? {
    val result = Sdk.parseVlessLink(link) ?: return null
    if (result.error.isNotEmpty()) return null
    return result.settings ?: return null
}

private fun httpGetString(url: String): String? = runCatching {
    val connection = URL(url).openConnection() as HttpURLConnection
    connection.connectTimeout = 10_000
    connection.readTimeout = 10_000
    connection.instanceFollowRedirects = true
    connection.setRequestProperty("Accept", "text/plain, */*")
    try {
        if (connection.responseCode !in 200..299) return null
        // subscriptions are small; 4 MiB is plenty and bounds a bad server
        val bytes = connection.inputStream.use { input ->
            val output = ByteArrayOutputStream()
            val chunk = ByteArray(16 * 1024)
            var total = 0
            while (total < 4 * 1024 * 1024) {
                val read = input.read(chunk)
                if (read < 0) break
                output.write(chunk, 0, read)
                total += read
            }
            output.toByteArray()
        }
        String(bytes, Charsets.UTF_8)
    } finally {
        connection.disconnect()
    }
}.getOrNull()

/**
 * The base64 a subscription body arrives as: standard or url-safe, padded or
 * not, line-wrapped or not. Null when the text is not base64 (or decodes to
 * nothing sensible).
 */
fun decodeBase64Text(text: String): String? {
    val compact = text.replace(whitespaceRegex, "")
    if (compact.isEmpty()) return null

    val decoded = listOf(
        android.util.Base64.DEFAULT,
        android.util.Base64.URL_SAFE or android.util.Base64.NO_WRAP,
        android.util.Base64.URL_SAFE or android.util.Base64.NO_WRAP or android.util.Base64.NO_PADDING,
    ).firstNotNullOfOrNull { flags ->
        runCatching {
            android.util.Base64.decode(compact, flags)
        }.getOrNull()
    } ?: return null

    val result = runCatching { String(decoded, Charsets.UTF_8) }.getOrNull() ?: return null
    return result.ifBlank { null }
}

