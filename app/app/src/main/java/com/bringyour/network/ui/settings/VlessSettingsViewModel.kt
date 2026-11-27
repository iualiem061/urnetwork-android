package com.bringyour.network.ui.settings

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
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
 * The VLESS proxy settings of the network space: the editor form, the
 * saved servers, and the link / import machinery (VlessSettingsLogic.kt).
 * Account > Settings > Proxy mode edits it for the signed-in device's space,
 * and the login screen's network settings for the active space before there
 * is a device.
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

    /** The saved proxy servers, newest first. */
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
    private var loadJob: Job? = null

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

    // ---- the saved proxy servers (the library the link and the import feed)

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
            return
        }
        val current = form.toSettingsValues().copy(enabled = true)
        activeServerId = servers.firstOrNull { saved ->
            val parsed = parseVlessLinkOrNull(saved.link) ?: return@firstOrNull false
            vlessSettingsValues(parsed).copy(enabled = true) == current
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
