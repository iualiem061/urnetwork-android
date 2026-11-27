package com.bringyour.network.ui.settings

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import android.content.Context
import com.bringyour.network.DeviceManager
import com.bringyour.network.NetworkSpaceManagerProvider
import com.bringyour.network.utils.sdkStringListToList
import com.bringyour.sdk.NetworkSpace
import com.bringyour.sdk.Sdk
import com.bringyour.sdk.VlessSettings
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID
import javax.inject.Inject

/**
 * The relays of the network space: the shared relay network the app carries
 * (the public pools of RelayNetwork.kt, fetched and cached) and the personal
 * library of `vless://` links. Account > Settings > Relays edits both for the
 * signed-in device's space, and the login screen's network settings for the
 * active space before there is a device.
 *
 * The sdk stores the settings in the space's values and applies a save in
 * place: the strategy's VLESS dialer is replaced while the space, a device
 * bound to it and this screen stay valid. The VPN service (MainService) runs
 * in the app's process -- the manifest gives it no android:process -- on the
 * device bound to this same space, so a save takes effect at once. Unlike the
 * ios tunnel extension and the desktop services, there is no "next time it
 * connects" to tell the user.
 *
 * The form logic is VlessSettingsLogic.kt; this converts between its plain
 * values and the gomobile `VlessSettings`, and keeps the space's reads and
 * writes off the main thread.
 */
@HiltViewModel
class VlessSettingsViewModel @Inject constructor(
    @ApplicationContext appContext: Context,
    private val deviceManager: DeviceManager,
    private val networkSpaceManagerProvider: NetworkSpaceManagerProvider,
) : ViewModel() {

    var form by mutableStateOf(VlessForm())
        private set

    /** Whether there is a space to edit. False until the settings load. */
    var editable by mutableStateOf(false)
        private set

    var saving by mutableStateOf(false)
        private set

    /** The form's first problem (`Sdk.validateVlessSettings`), empty when it validates. */
    var validationErrorId by mutableStateOf("")
        private set

    /** What the last save answered, until the form changes. */
    var saveErrorId by mutableStateOf("")
        private set

    /** Why the last pasted link did not read. */
    var linkErrorId by mutableStateOf("")
        private set

    /** The saved relay servers, newest first. */
    var servers by mutableStateOf<List<VlessServer>>(emptyList())
        private set

    /** The saved server the space's current settings were activated from, if any. */
    var activeServerId by mutableStateOf<String?>(null)
        private set

    /** A subscription import is running. */
    var importing by mutableStateOf(false)
        private set

    val networks = sdkStringListToList(Sdk.vlessNetworks()).ifEmpty { VLESS_NETWORKS }
    val securities = sdkStringListToList(Sdk.vlessSecurities()).ifEmpty { VLESS_SECURITIES }
    val flows = sdkStringListToList(Sdk.vlessFlows()).ifEmpty { VLESS_FLOWS }
    val fingerprints = sdkStringListToList(Sdk.vlessFingerprints()).ifEmpty { VLESS_FINGERPRINTS }

    private val serverStore = VlessServerStore(appContext)
    private val sharedStore = RelaySharedStore(appContext)
    private var loadJob: Job? = null
    private var sharedJob: Job? = null

    init {
        servers = serverStore.load()
        load()
    }

    /**
     * Fills the form with the space's settings, which are the sdk's new form
     * when the space has none. The login screen calls this each time it opens
     * the editor, since its view model outlives one opening.
     */
    fun load() {
        val space = networkSpace()
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            val values = withContext(Dispatchers.IO) {
                space?.vlessSettings?.let { vlessSettingsValues(it) }
            }
            editable = values != null
            saveErrorId = ""
            linkErrorId = ""
            replaceForm(values?.let { vlessFormFrom(it) } ?: VlessForm())
            reconcileActiveServer()
        }
    }

    /** Edits the form. The last save's answer no longer applies to it. */
    fun update(change: (VlessForm) -> VlessForm) {
        saveErrorId = ""
        replaceForm(change(form))
    }

    /**
     * Reads a `vless://` link: its settings replace the whole form, enabled,
     * or its error id is shown.
     */
    fun applyLink(link: String) {
        val result = Sdk.parseVlessLink(link)
        val outcome = vlessLinkOutcome(
            errorId = result?.error ?: VLESS_ERROR_LINK_INVALID,
            settings = result?.settings?.let { vlessSettingsValues(it) },
        )
        when (outcome) {
            is VlessLinkOutcome.Fill -> {
                linkErrorId = ""
                update { outcome.form }
            }
            is VlessLinkOutcome.Error -> linkErrorId = outcome.errorId
        }
    }

    fun clearLinkError() {
        linkErrorId = ""
    }

    /** The form's share link, empty when it does not validate. */
    fun shareLink(): String = Sdk.vlessSettingsLink(sdkVlessSettings(form.toSettingsValues()))

    /**
     * Saves the form to the space. Enabled settings must validate, otherwise
     * nothing is saved and the error id shows under Save. On success the form
     * shows what the space read back, which is what the next open loads.
     */
    fun save(onSaved: () -> Unit, onFailed: () -> Unit = {}) {
        if (saving) {
            return
        }
        val space = networkSpace() ?: return
        val values = form.toSettingsValues()
        saving = true
        viewModelScope.launch {
            try {
                val (errorId, stored) = withContext(Dispatchers.IO) {
                    val errorId = space.setVlessSettings(sdkVlessSettings(values))
                    val stored = if (errorId.isEmpty()) {
                        space.vlessSettings?.let { vlessSettingsValues(it) }
                    } else {
                        null
                    }
                    errorId to stored
                }
                saveErrorId = errorId
                if (errorId.isEmpty()) {
                    stored?.let { replaceForm(vlessFormFrom(it)) }
                    onSaved()
                } else {
                    onFailed()
                }
            } finally {
                saving = false
            }
        }
    }

    // ---- the saved relay servers (the library the subscription feeds)

    /**
     * Activates a saved server: its link fills the form and is saved to the
     * space, which applies it at once. The badge moves by matching, in
     * [reconcileActiveServer] -- the server whose settings are now the
     * space's is the active one.
     */
    fun activate(server: VlessServer, onActivated: () -> Unit, onFailed: () -> Unit) {
        if (saving) {
            return
        }
        val settings = parseVlessLinkOrNull(server.link)
        if (settings == null) {
            onFailed()
            return
        }
        saveErrorId = ""
        linkErrorId = ""
        replaceForm(vlessFormFrom(vlessSettingsValues(settings)))
        save(
            onSaved = {
                reconcileActiveServer()
                onActivated()
            },
            onFailed = onFailed,
        )
    }

    /**
     * Imports servers from pasted text: one or more `vless://` links, or a
     * subscription -- a link whose body is the list (plain or base64). The
     * answer arrives once the fetch and the parses are done.
     */
    fun importFrom(text: String, onResult: (VlessImportResult) -> Unit) {
        if (importing || text.isBlank()) {
            return
        }
        importing = true
        val existing = servers
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                importVlessServers(text, existing)
            }
            importing = false
            if (result is VlessImportResult.Added) {
                servers = mergeVlessServers(existing, result.servers)
                serverStore.store(servers)
            }
            onResult(result)
        }
    }

    /** A saved server leaves the library; the badge goes with it if it was active. */
    fun removeServer(server: VlessServer) {
        servers = servers.filterNot { it.id == server.id }
        serverStore.store(servers)
        if (activeServerId == server.id) {
            activeServerId = null
        }
    }

    /**
     * The current form joins the library under its name (or its address when
     * it has none). The form must validate: the library only holds links that
     * read back.
     */
    fun saveCurrent(onSaved: () -> Unit, onFailed: () -> Unit) {
        val link = shareLink()
        if (link.isEmpty()) {
            onFailed()
            return
        }
        val name = form.name.ifBlank { "${form.address}:${form.port}".trimStart(':') }
        val server = VlessServer(
            id = UUID.randomUUID().toString(),
            name = name,
            link = link,
            address = "${form.address}:${form.port}".trimStart(':'),
        )
        if (servers.none { it.link == link }) {
            servers = (listOf(server) + servers).take(VLESS_SERVERS_MAX)
            serverStore.store(servers)
        }
        onSaved()
    }

    // ---- the shared relay network (RelayNetwork.kt)

    /** The shared relays of the public pools, one whole fetch. */
    var sharedServers by mutableStateOf<List<VlessServer>>(emptyList())
        private set

    /** A shared fetch is running (a first open or a refresh). */
    var sharedLoading by mutableStateOf(false)
        private set

    /** When the shared list was fetched, epoch millis; 0 = never fetched. */
    var sharedFetchedAt by mutableStateOf(0L)
        private set

    /** Every source of the last refresh answered nothing; the retry note shows. */
    var sharedFetchFailed by mutableStateOf(false)
        private set

    /** The shared server the space's settings are live from, if any. */
    var sharedActiveId by mutableStateOf<String?>(null)
        private set

    /** One server's TCP RTT, keyed by id: `RTT_TESTING` while away, `RTT_FAILED` when unreachable. Missing = untested. */
    val relayRtts = mutableStateMapOf<String, Long>()

    private var sharedStarted = false

    /**
     * The shared network on the tab's first open: the cached pool serves
     * instantly, and a fetch runs when the cache is empty or stale.
     */
    fun ensureSharedRelays() {
        if (sharedStarted) return
        sharedStarted = true
        sharedServers = sharedStore.load()
        sharedFetchedAt = sharedStore.fetchedAt()
        if (sharedServers.isEmpty() ||
            System.currentTimeMillis() - sharedFetchedAt > SHARED_RELAY_STALE_MS
        ) {
            refreshSharedRelays()
        }
    }

    /**
     * Re-fetches every public pool and replaces the shared list (the pool
     * publishes fresh links; keeping the old ones beside them doubles the
     * dupes). When nothing comes back, the cached pool stays and the
     * failure note shows.
     */
    fun refreshSharedRelays() {
        if (sharedLoading) return
        sharedLoading = true
        sharedFetchFailed = false
        sharedJob?.cancel()
        sharedJob = viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                fetchSharedRelays()
            }
            sharedLoading = false
            if (result.servers.isNotEmpty()) {
                sharedServers = result.servers
                sharedFetchedAt = System.currentTimeMillis()
                sharedStore.store(result.servers)
                sharedStore.storeFetchedAt(sharedFetchedAt)
                reconcileActiveServer()
            } else {
                sharedFetchFailed = true
            }
        }
    }

    /** Tests one relay's TCP RTT; the answer lands in [relayRtts]. */
    fun testRelayRtt(server: VlessServer) {
        if (relayRtts[server.id] == RTT_TESTING) return
        relayRtts[server.id] = RTT_TESTING
        viewModelScope.launch {
            val rtt = withContext(Dispatchers.IO) {
                relayRttMillis(server)
            }
            relayRtts[server.id] = rtt ?: RTT_FAILED
        }
    }

    /**
     * The active badge is earned again from the truth: the saved server whose
     * settings equal the space's current ones. A link saved by hand and one
     * the space stored can spell the same server differently, so settings are
     * compared, not link text. The enabled bit is left out: a pasted link
     * always parses enabled, while the space may have the same server off.
     */
    private fun reconcileActiveServer() {
        if (validationErrorId.isNotEmpty()) {
            activeServerId = null
            sharedActiveId = null
            return
        }
        val current = form.toSettingsValues().copy(enabled = true)
        activeServerId = servers.firstOrNull { saved ->
            val parsed = parseVlessLinkOrNull(saved.link) ?: return@firstOrNull false
            vlessSettingsValues(parsed).copy(enabled = true) == current
        }?.id

        // the shared pool: an address match is the cheap key, the settings
        // compare only runs on it (the pool holds thousands)
        val currentAddress = "${form.address}:${form.port}".trimStart(':')
        sharedActiveId = sharedServers.firstOrNull { saved ->
            saved.address == currentAddress && run {
                val parsed = parseVlessLinkOrNull(saved.link) ?: return@run false
                vlessSettingsValues(parsed).copy(enabled = true) == current
            }
        }?.id
    }

    private fun replaceForm(next: VlessForm) {
        form = next
        validationErrorId = Sdk.validateVlessSettings(sdkVlessSettings(next.toSettingsValues()))
    }

    // the space a signed-in device is bound to, else the active space (the
    // login screen, before there is a device)
    private fun networkSpace(): NetworkSpace? =
        deviceManager.device?.networkSpace ?: networkSpaceManagerProvider.getNetworkSpace()
}

private fun vlessSettingsValues(settings: VlessSettings) = VlessSettingsValues(
    enabled = settings.enabled,
    name = settings.name,
    address = settings.address,
    port = settings.port,
    id = settings.id,
    flow = settings.flow,
    network = settings.network,
    security = settings.security,
    serverName = settings.serverName,
    fingerprint = settings.fingerprint,
    alpn = settings.alpn,
    allowInsecure = settings.allowInsecure,
    publicKey = settings.publicKey,
    shortId = settings.shortId,
    spiderX = settings.spiderX,
    path = settings.path,
    host = settings.host,
)

private fun sdkVlessSettings(values: VlessSettingsValues) = VlessSettings().apply {
    enabled = values.enabled
    name = values.name
    address = values.address
    port = values.port
    id = values.id
    flow = values.flow
    network = values.network
    security = values.security
    serverName = values.serverName
    fingerprint = values.fingerprint
    alpn = values.alpn
    allowInsecure = values.allowInsecure
    publicKey = values.publicKey
    shortId = values.shortId
    spiderX = values.spiderX
    path = values.path
    host = values.host
}
