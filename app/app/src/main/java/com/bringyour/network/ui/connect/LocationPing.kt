package com.bringyour.network.ui.connect

import android.net.Uri
import com.bringyour.network.utils.sdkStringListToList
import com.bringyour.sdk.Sdk
import java.net.InetSocketAddress
import java.net.Socket
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout

/**
 * The result of a location's ping test. A country's latency is measured as
 * the TCP handshake round trip to URnetwork's ingress resolver for that
 * country (the sdk's per-country DoH endpoint), which is the closest thing
 * the app can reach in that location before a connection exists. Countries
 * the ingress does not cover have no test endpoint.
 */
sealed interface LocationPing {
    /** A measurement is in flight; the row shows a spinner. */
    object Testing : LocationPing

    /** The measured round trip in milliseconds. */
    data class Measured(val millis: Long) : LocationPing

    /** No ingress endpoint covers this country, so there is nothing to test. */
    object Unavailable : LocationPing

    /** The endpoint did not answer within the timeout. */
    object Failed : LocationPing
}

object LocationPingTest {
    const val TIMEOUT_MILLIS = 4_000L
    const val PORT_HTTPS = 443
    const val PORT_HTTP = 80

    /** Whether the sdk names an ingress resolver for the country. */
    fun hasEndpoint(countryCode: String): Boolean =
        ingressHosts(countryCode).isNotEmpty()

    private fun ingressHosts(countryCode: String): List<Pair<String, Int>> =
        sdkStringListToList(Sdk.ingressDohUrls(countryCode))
            .mapNotNull { url ->
                runCatching { Uri.parse(url) }.getOrNull()?.host?.let { host ->
                    val port = if (url.startsWith("http://")) PORT_HTTP else PORT_HTTPS
                    host to port
                }
            }
            .distinct()

    /**
     * The TCP handshake round trip to the country's ingress resolver, in
     * milliseconds, or null when there is no endpoint or it did not answer.
     */
    suspend fun measure(countryCode: String): LocationPing = withContext(Dispatchers.IO) {
        val targets = ingressHosts(countryCode)
        if (targets.isEmpty()) {
            return@withContext LocationPing.Unavailable
        }
        val (host, port) = targets.first()
        val started = System.nanoTime()
        try {
            withTimeout(TIMEOUT_MILLIS) {
                Socket().use { socket ->
                    socket.connect(InetSocketAddress(host, port), TIMEOUT_MILLIS.toInt())
                }
            }
            LocationPing.Measured((System.nanoTime() - started) / 1_000_000)
        } catch (_: Exception) {
            LocationPing.Failed
        }
    }
}
