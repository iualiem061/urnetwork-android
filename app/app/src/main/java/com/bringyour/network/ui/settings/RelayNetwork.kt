package com.bringyour.network.ui.settings

import android.content.Context
import kotlinx.serialization.encodeToString
import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection
import java.net.InetSocketAddress
import java.net.Socket
import java.net.URL
import java.util.UUID

/**
 * The shared relay network: a set of public relay pools the app carries with
 * it -- many servers across many countries, published and refreshed by their
 * communities, so the user never adds a shared relay by hand. The app fetches
 * the pools, reads out the `vless://` links, drops what does not parse and
 * groups the rest by the country the link's remark carries. A tap on a server
 * activates it (the same path as the personal library: link -> form -> save,
 * the space dials through it at once).
 *
 * The personal relays -- the user's own `vless://` links and subscriptions --
 * stay separate in [VlessServerStore]; both lists are [VlessServer], so they
 * share the activation machinery.
 */

/** Where the shared relays come from, in order; all are plain-text lists of links. */
val SHARED_RELAY_SOURCES = listOf(
    // the large mixed pool, refreshed about every 15 minutes (thousands of vless links)
    "https://raw.githubusercontent.com/barry-far/V2ray-Config/main/All_Configs_Sub.txt",
    // a second pool from another aggregator, so one stale list never empties the network
    "https://raw.githubusercontent.com/Argh94/Proxy-List/main/All_Config.txt",
)

/** A backstop, not a feature: plenty of relays with room for the next refresh. */
const val SHARED_RELAY_MAX = 2000

/** How long a fetched list serves before a quiet re-fetch is worth it. */
const val SHARED_RELAY_STALE_MS = 12L * 60 * 60 * 1000

/** The relay RTT map's sentinels: away / failed; a missing key means untested. */
const val RTT_TESTING = -2L
const val RTT_FAILED = -1L

/**
 * What a refresh answered: the servers that joined (deduped, capped), and how
 * many sources answered nothing at all.
 */
data class RelayFetchResult(
    val servers: List<VlessServer>,
    val failedSources: Int,
)

/**
 * Fetches every source and merges the `vless://` links it holds into one
 * library, deduped by link text and cut at the cap. Only links the sdk reads
 * become servers; the unreadable -- wrong ids, exotic transports -- drop out
 * silently, the way the import does. Runs off the main thread.
 */
fun fetchSharedRelays(): RelayFetchResult {
    val seenLinks = HashSet<String>()
    val servers = mutableListOf<VlessServer>()
    var failedSources = 0

    for (source in SHARED_RELAY_SOURCES) {
        val text = relayHttpGetString(source) ?: run {
            failedSources += 1
            null
        } ?: continue

        for (candidate in text.split(whitespaceRegex)) {
            if (servers.size >= SHARED_RELAY_MAX) break
            if (!candidate.startsWith("vless://")) continue
            if (candidate in seenLinks) continue

            val settings = parseVlessLinkOrNull(candidate) ?: continue
            val name = settings.name.ifBlank {
                "${settings.address}:${settings.port}".trimStart(':')
            }
            seenLinks += candidate
            servers += VlessServer(
                id = UUID.randomUUID().toString(),
                name = name,
                link = candidate,
                address = "${settings.address}:${settings.port}".trimStart(':'),
            )
        }
    }

    return RelayFetchResult(servers = servers, failedSources = failedSources)
}

/** A shared fetch as an HTTP GET of a plain-text body, with a byte ceiling. */
private fun relayHttpGetString(
    url: String,
    maxBytes: Int = 6 * 1024 * 1024,
): String? = runCatching {
    val connection = URL(url).openConnection() as HttpURLConnection
    connection.connectTimeout = 15_000
    connection.readTimeout = 20_000
    connection.instanceFollowRedirects = true
    connection.setRequestProperty("Accept", "text/plain, */*")
    try {
        if (connection.responseCode !in 200..299) return null
        val bytes = connection.inputStream.use { input ->
            val output = ByteArrayOutputStream()
            val chunk = ByteArray(16 * 1024)
            var total = 0
            while (total < maxBytes) {
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
 * The RTT of one relay, measured the honest way: the time a TCP handshake
 * to the server's own address takes. No relay is reachable from everywhere,
 * so an answer of null means the handshake failed or timed out, not that the
 * relay is down for the network. Runs off the main thread.
 */
fun relayRttMillis(server: VlessServer, timeoutMillis: Int = 4_000): Long? {
    val (host, port) = relayEndpoint(server.address) ?: return null
    return runCatching {
        val socket = Socket()
        try {
            val start = System.nanoTime()
            socket.connect(InetSocketAddress(host, port.toInt()), timeoutMillis)
            (System.nanoTime() - start) / 1_000_000
        } finally {
            socket.close()
        }
    }.getOrNull()
}

/** `address:port` (or `[v6]:port`) as its two parts; a missing port reads null. */
private fun relayEndpoint(address: String): Pair<String, String>? {
    val trimmed = address.trim()
    if (trimmed.isEmpty()) return null
    if (trimmed.startsWith("[")) {
        val close = trimmed.indexOf(']')
        if (close < 0) return null
        val host = trimmed.substring(1, close)
        val rest = trimmed.substring(close + 1)
        if (!rest.startsWith(":")) return null
        val port = rest.substring(1)
        if (port.isNotEmpty() && port.all { it.isDigit() }) return host to port
        return null
    }
    val colon = trimmed.lastIndexOf(':')
    if (colon < 0) return null
    val host = trimmed.substring(0, colon)
    val port = trimmed.substring(colon + 1)
    if (host.isEmpty()) return null
    if (port.isEmpty() || !port.all { it.isDigit() }) return null
    return host to port
}

// ---- the country a relay's remark carries

/**
 * The country of a relay as its ISO code (lowercase, the flag drawables'
 * key), read from the link's remark: a flag emoji first, then a standalone
 * two-letter code, then a spelled-out country name. Null when the remark
 * says nothing that reads as a country.
 */
fun relayCountryCode(server: VlessServer): String? {
    relayEmojiCountry(server.name)?.let { return it }
    relayTokenCountry(server.name)?.let { return it }
    relayNamedCountry(server.name)?.let { return it }
    return null
}

/** The first flag emoji in the remark, as `us`, `de`, ... */
private fun relayEmojiCountry(name: String): String? {
    val points = name.codePoints().toArray()
    for (index in 0 until points.size - 1) {
        val first = points[index]
        val second = points[index + 1]
        if (first in 0x1F1E6..0x1F1FF && second in 0x1F1E6..0x1F1FF) {
            val a = 'a' + (first - 0x1F1E6)
            val b = 'a' + (second - 0x1F1E6)
            return "$a$b"
        }
    }
    return null
}

/** A standalone two-letter uppercase code (`US-1.2.3.4`, `(DE)`, `FR 2`) of a known country. */
private fun relayTokenCountry(name: String): String? {
    for (match in Regex("(?<![A-Za-z])([A-Z]{2})(?![A-Za-z])").findAll(name)) {
        val code = match.groupValues[1].lowercase()
        if (code in relayCountryCodes) return code
    }
    return null
}

/** A spelled-out country name, English or Persian, of the common relay countries. */
private fun relayNamedCountry(name: String): String? {
    val lower = name.lowercase()
    for (word in lower.split(Regex("[^\\p{L}]+"))) {
        if (word.isNotEmpty()) {
            relayCountryNames[word]?.let { return it }
        }
    }
    return null
}

/**
 * One country's slice of the shared network. `code` null = the remark said
 * nothing readable, the group the app shows as "other".
 */
data class RelayCountryGroup(
    val code: String?,
    val servers: List<VlessServer>,
)

/**
 * The shared servers grouped by country, the largest groups first, the
 * unplaceable ones (`code` null) last so they never crowd the top of the
 * list. Within a group the pool's own order is kept.
 */
fun groupRelaysByCountry(
    servers: List<VlessServer>,
    filter: String = "",
): List<RelayCountryGroup> {
    val query = filter.trim().lowercase()
    val groups = servers
        .groupBy { relayCountryCode(it) }
        .map { (code, list) ->
            val filtered = if (query.isEmpty()) list else list.filter { server ->
                server.name.lowercase().contains(query) ||
                    server.address.lowercase().contains(query) ||
                    (code != null && relayCountryDisplayName(code).lowercase().contains(query))
            }
            RelayCountryGroup(code, filtered)
        }
        .filter { it.servers.isNotEmpty() }

    return groups.sortedWith(
        compareByDescending<RelayCountryGroup> { it.servers.size }
            .thenBy { it.code ?: "zz" }
    )
}

/**
 * The country's name in the language the app runs in, or its upper code
 * when the locale spells nothing for it.
 */
fun relayCountryDisplayName(code: String): String {
    val tag = code.uppercase()
    return runCatching {
        val display = java.util.Locale("", tag)
            .getDisplayCountry(java.util.Locale.getDefault())
        display.takeIf { it.isNotBlank() && !it.equals(tag, ignoreCase = true) } ?: tag
    }.getOrDefault(tag)
}

/**
 * The ISO codes the relay pools' remarks actually carry, for the token
 * match: everything the flag set covers would be fine, this set just keeps
 * the false positives (a stray `OK`, `IO`) out.
 */
private val relayCountryCodes = setOf(
    // north and south america
    "us", "ca", "mx", "br", "ar", "cl", "co", "pe", "uy",
    // europe
    "gb", "de", "nl", "fr", "es", "it", "ch", "at", "se", "no", "fi", "dk",
    "pl", "pt", "ie", "be", "lu", "cz", "sk", "hu", "ro", "bg", "gr", "hr",
    "rs", "ua", "md", "lt", "lv", "ee", "is", "al", "ba", "mk", "me", "si",
    // middle east
    "tr", "il", "ae", "sa", "kw", "qa", "bh", "om", "jo", "cy", "lb", "iq", "ir",
    // asia
    "in", "pk", "bd", "lk", "th", "vn", "ph", "sg", "my", "id", "jp", "kr",
    "hk", "tw", "cn", "kz", "uz", "kg", "am", "ge", "mn",
    // africa and oceania
    "za", "ng", "eg", "ke", "gh", "ma", "dz", "tn", "ly", "et", "ug", "tz",
    "au", "nz",
)

/** Common spellings (lowercased, English and Persian) of the relay countries. */
private val relayCountryNames: Map<String, String> = mapOf(
    // english
    "germany" to "de", "netherlands" to "nl", "holland" to "nl",
    "usa" to "us", "states" to "us", "france" to "fr", "spain" to "es",
    "italy" to "it", "switzerland" to "ch", "austria" to "at", "sweden" to "se",
    "norway" to "no", "finland" to "fi", "denmark" to "dk", "poland" to "pl",
    "portugal" to "pt", "ireland" to "ie", "belgium" to "be", "czech" to "cz",
    "slovakia" to "sk", "hungary" to "hu", "romania" to "ro", "bulgaria" to "bg",
    "greece" to "gr", "croatia" to "hr", "serbia" to "rs", "ukraine" to "ua",
    "turkey" to "tr", "turkiye" to "tr", "russia" to "ru", "israel" to "il",
    "emirates" to "ae", "dubai" to "ae", "saudi" to "sa", "qatar" to "qa",
    "kuwait" to "kw", "bahrain" to "bh", "oman" to "om", "jordan" to "jo",
    "cyprus" to "cy", "lebanon" to "lb", "india" to "in", "pakistan" to "pk",
    "bangladesh" to "bd", "thailand" to "th", "vietnam" to "vn",
    "philippines" to "ph", "singapore" to "sg", "malaysia" to "my",
    "indonesia" to "id", "japan" to "jp", "korea" to "kr", "china" to "cn",
    "taiwan" to "tw", "canada" to "ca", "mexico" to "mx", "brazil" to "br",
    "argentina" to "ar", "chile" to "cl", "colombia" to "co", "peru" to "pe",
    "australia" to "au", "zealand" to "nz", "africa" to "za", "egypt" to "eg",
    "kenya" to "ke", "ghana" to "gh", "morocco" to "ma", "algeria" to "dz",
    "tunisia" to "tn", "ethiopia" to "et", "england" to "gb", "uk" to "gb",
    "britain" to "gb", "london" to "gb", "paris" to "fr", "frankfurt" to "de",
    "amsterdam" to "nl", "vienna" to "at", "prague" to "cz", "oslo" to "no",
    "stockholm" to "se", "helsinki" to "fi", "moscow" to "ru", "istanbul" to "tr",
    "ankara" to "tr", "tokyo" to "jp", "seoul" to "kr", "delhi" to "in",
    "mumbai" to "in", "toronto" to "ca", "york" to "us", "california" to "us",
    "texas" to "us", "virginia" to "us", "florida" to "us", "seattle" to "us",
    "oregon" to "us", "ohio" to "us", "vegas" to "us",
    // persian
    "آلمان" to "de", "هلند" to "nl", "آمریکا" to "us", "فرانسه" to "fr",
    "اسپانیا" to "es", "ایتالیا" to "it", "سوئیس" to "ch", "اتریش" to "at",
    "سوئد" to "se", "نروژ" to "no", "فنلاند" to "fi", "دانمارک" to "dk",
    "لهستان" to "pl", "پرتغال" to "pt", "ایرلند" to "ie", "بلژیک" to "be",
    "ترکیه" to "tr", "روس" to "ru", "روسیه" to "ru", "اسرائیل" to "il",
    "امارات" to "ae", "عربستان" to "sa", "قطر" to "qa", "کویت" to "kw",
    "بحرین" to "bh", "عمان" to "om", "اردن" to "jo", "قبرس" to "cy",
    "لبنان" to "lb", "هند" to "in", "پاکستان" to "pk", "بنگلادش" to "bd",
    "تایلند" to "th", "ویتنام" to "vn", "فیلیپین" to "ph", "سنگاپور" to "sg",
    "مالزی" to "my", "اندونزی" to "id", "ژاپن" to "jp", "کره" to "kr",
    "چین" to "cn", "تایوان" to "tw", "کانادا" to "ca", "مکزیک" to "mx",
    "برزیل" to "br", "آرژانتین" to "ar", "شیلی" to "cl", "کلمبیا" to "co",
    "استرالیا" to "au", "نیوزیلند" to "nz", "مصر" to "eg",
    "کنیا" to "ke", "غنا" to "gh", "مراکش" to "ma", "الجزیره" to "dz",
    "تونس" to "tn", "لیبی" to "ly", "اتیوپی" to "et", "انگلیس" to "gb",
    "انگلستان" to "gb", "لندن" to "gb", "پاریس" to "fr",
)

/**
 * The shared relays' cache: what the last successful fetch brought, with its
 * time, so the tab opens instantly with the pool it last held and a refresh
 * is a tap or a stale age away.
 */
class RelaySharedStore(context: Context) {

    private val prefs =
        context.getSharedPreferences("vless_shared_relay_servers", Context.MODE_PRIVATE)

    fun load(): List<VlessServer> = runCatching {
        val raw = prefs.getString(SERVERS_KEY, null) ?: return emptyList()
        vlessServerJson.decodeFromString<List<VlessServer>>(raw)
    }.getOrDefault(emptyList())

    fun store(servers: List<VlessServer>) {
        prefs.edit()
            .putString(SERVERS_KEY, vlessServerJson.encodeToString(servers.take(SHARED_RELAY_MAX)))
            .apply()
    }

    fun fetchedAt(): Long = prefs.getLong(FETCHED_AT_KEY, 0L)

    fun storeFetchedAt(atMillis: Long) {
        prefs.edit().putLong(FETCHED_AT_KEY, atMillis).apply()
    }

    private companion object {
        const val SERVERS_KEY = "servers"
        const val FETCHED_AT_KEY = "fetched_at"
    }
}
