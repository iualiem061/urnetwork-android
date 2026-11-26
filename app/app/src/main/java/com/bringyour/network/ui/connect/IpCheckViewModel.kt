package com.bringyour.network.ui.connect

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject

/**
 * The public IP and where the internet reads it from, as the ip check tab
 * shows it.
 *
 * While the VPN is connected the request travels the tunnel, so this is the
 * exit node's IP and location; while disconnected it is the device's own.
 */
@Serializable
data class IpCheckInfo(
    val ip: String,
    val country: String? = null,
    val city: String? = null,
    val isp: String? = null,
)

/** The ip check screen's read, one of: loading, ready, or unavailable. */
sealed interface IpCheckState {
    data object Loading : IpCheckState
    data class Ready(val info: IpCheckInfo) : IpCheckState
    data object Unavailable : IpCheckState
}

@HiltViewModel
class IpCheckViewModel @Inject constructor() : ViewModel() {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    private val _state = MutableStateFlow<IpCheckState>(IpCheckState.Loading)
    val state: StateFlow<IpCheckState> = _state.asStateFlow()

    init {
        refresh()
    }

    /** Reads the public IP again, from the network the device is on now. */
    fun refresh() {
        _state.value = IpCheckState.Loading
        viewModelScope.launch(Dispatchers.IO) {
            _state.value = readIpCheckInfo() ?: IpCheckState.Unavailable
        }
    }

    /** ipwho.is first (IP + where it is + the ISP), api.ipify.org as IP-only fallback. */
    private fun readIpCheckInfo(): IpCheckState? {
        readIpWho()?.let { return IpCheckState.Ready(it) }
        readIpify()?.let { return IpCheckState.Ready(IpCheckInfo(ip = it)) }
        return null
    }

    private fun readIpWho(): IpCheckInfo? = runCatching {
        val body = httpGet("https://ipwho.is/") ?: return null
        val parsed = json.decodeFromString(IpWhoResponse.serializer(), body)
        if (parsed.success != true || parsed.ip.isNullOrEmpty()) {
            return null
        }
        IpCheckInfo(
            ip = parsed.ip,
            country = parsed.country,
            city = parsed.city,
            isp = parsed.connection?.isp ?: parsed.connection?.org,
        )
    }.getOrNull()

    private fun readIpify(): String? = runCatching {
        val body = httpGet("https://api.ipify.org/?format=json") ?: return null
        val parsed = json.decodeFromString(IpifyResponse.serializer(), body)
        parsed.ip?.takeIf { it.isNotEmpty() }
    }.getOrNull()

    @Serializable
    private data class IpWhoResponse(
        val ip: String? = null,
        val success: Boolean? = null,
        val country: String? = null,
        val city: String? = null,
        val connection: Connection? = null,
    ) {
        @Serializable
        data class Connection(
            val isp: String? = null,
            val org: String? = null,
        )
    }

    @Serializable
    private data class IpifyResponse(
        val ip: String? = null,
    )

    private fun httpGet(url: String): String? = runCatching {
        val connection = URL(url).openConnection() as HttpURLConnection
        connection.connectTimeout = 10_000
        connection.readTimeout = 15_000
        connection.requestMethod = "GET"
        connection.setRequestProperty("User-Agent", "ur.network-android-ipcheck/1.0")
        try {
            if (connection.responseCode !in 200..299) {
                return null
            }
            val out = ByteArrayOutputStream()
            connection.inputStream.use { input ->
                val buffer = ByteArray(8192)
                while (true) {
                    val read = input.read(buffer)
                    if (read < 0) break
                    out.write(buffer, 0, read)
                }
            }
            out.toString("UTF-8")
        } finally {
            connection.disconnect()
        }
    }.getOrNull()
}
