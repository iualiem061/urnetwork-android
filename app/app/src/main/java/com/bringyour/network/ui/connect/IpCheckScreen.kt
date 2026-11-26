package com.bringyour.network.ui.connect

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.bringyour.network.R
import com.bringyour.network.ui.components.RoundedFlag
import com.bringyour.network.ui.components.rememberRoundedCountryFlag
import com.bringyour.network.ui.shared.models.ConnectStatus
import com.bringyour.network.ui.theme.AppTheme
import com.bringyour.network.ui.theme.Green
import com.bringyour.network.ui.theme.Red400
import com.bringyour.network.ui.theme.Yellow

/**
 * The ip check tab: the app's own connection state on one card, and the
 * public IP -- the address and location the internet reads the device from
 * -- on another, with a re-read button. While the VPN is connected the read
 * travels the tunnel, so it shows the exit node's IP and location.
 */
@Composable
fun IpCheckScreen(
    connectViewModel: ConnectViewModel,
    viewModel: IpCheckViewModel = hiltViewModel(),
) {

    val state by viewModel.state.collectAsState()
    val connectStatus by connectViewModel.connectStatus.collectAsState()
    val selectedLocation = connectViewModel.selectedLocation

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {

        Text(
            stringResource(id = R.string.ip_check),
            style = MaterialTheme.typography.headlineSmall,
            color = AppTheme.textPrimary,
        )

        Spacer(modifier = Modifier.height(20.dp))

        // ---- the connection state card

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    AppTheme.bgElevated,
                    RoundedCornerShape(12.dp),
                )
                .padding(16.dp)
        ) {
            Column {

                Text(
                    stringResource(id = R.string.ip_check_connection_state),
                    style = MaterialTheme.typography.bodyMedium,
                    color = AppTheme.textMuted,
                )

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .background(colorForConnectStatus(connectStatus), CircleShape)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        stringResource(id = labelForConnectStatus(connectStatus)),
                        style = MaterialTheme.typography.bodyLarge,
                        color = AppTheme.textPrimary,
                    )
                }

                // the selected location while a connection stands: its name
                // beside its flag, or the best-available label
                if (connectStatus != ConnectStatus.DISCONNECTED) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        val flagResourceId = rememberRoundedCountryFlag(selectedLocation?.countryCode)
                        if (flagResourceId != null) {
                            RoundedFlag(
                                imageResourceId = flagResourceId,
                                height = 14.dp,
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        Text(
                            selectedLocation?.name
                                ?: stringResource(id = R.string.best_available_provider),
                            style = MaterialTheme.typography.bodyMedium,
                            color = AppTheme.textMuted,
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // ---- the public IP card

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    AppTheme.bgElevated,
                    RoundedCornerShape(12.dp),
                )
                .padding(16.dp)
        ) {
            Column {

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        stringResource(id = R.string.ip_check_public_ip),
                        style = MaterialTheme.typography.bodyMedium,
                        color = AppTheme.textMuted,
                    )

                    if (state is IpCheckState.Ready) {
                        TextButton(
                            onClick = { viewModel.refresh() },
                        ) {
                            Text(stringResource(id = R.string.ip_check_refresh))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                when (val current = state) {
                    is IpCheckState.Loading -> {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = AppTheme.textMuted,
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                stringResource(id = R.string.ip_check_loading),
                                style = MaterialTheme.typography.bodyLarge,
                                color = AppTheme.textMuted,
                            )
                        }
                    }

                    is IpCheckState.Unavailable -> {
                        Text(
                            stringResource(id = R.string.ip_check_unavailable),
                            style = MaterialTheme.typography.bodyLarge,
                            color = AppTheme.textPrimary,
                        )
                        TextButton(
                            onClick = { viewModel.refresh() },
                        ) {
                            Text(stringResource(id = R.string.ip_check_refresh))
                        }
                    }

                    is IpCheckState.Ready -> {
                        // the address: one clean left-to-right run even in the
                        // right-to-left layout
                        Text(
                            current.info.ip,
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontFamily = FontFamily.Monospace,
                                textDirection = TextDirection.Ltr,
                            ),
                            color = AppTheme.textPrimary,
                        )

                        current.info.country?.let { country ->
                            IpInfoRow(
                                label = stringResource(id = R.string.ip_check_location),
                                value = listOfNotNull(current.info.city, country)
                                    .joinToString(" · "),
                            )
                        }

                        current.info.isp?.let { isp ->
                            IpInfoRow(
                                label = stringResource(id = R.string.ip_check_isp),
                                value = isp,
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            stringResource(id = R.string.ip_check_note),
                            style = MaterialTheme.typography.bodySmall,
                            color = AppTheme.textFaint,
                        )
                    }
                }
            }
        }
    }
}

/** One label-value line of the public IP card; the label reads first. */
@Composable
private fun IpInfoRow(
    label: String,
    value: String,
) {
    Spacer(modifier = Modifier.height(8.dp))
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top,
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            color = AppTheme.textMuted,
        )
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium,
            color = AppTheme.textPrimary,
            modifier = Modifier.padding(start = 16.dp),
        )
    }
}

private fun labelForConnectStatus(status: ConnectStatus): Int = when (status) {
    ConnectStatus.CONNECTED -> R.string.connected
    ConnectStatus.CONNECTING,
    ConnectStatus.DESTINATION_SET,
    -> R.string.connecting_status_indicator
    ConnectStatus.DISCONNECTED -> R.string.disconnected
    ConnectStatus.CONNECT_FAILED -> R.string.disconnected
}

private fun colorForConnectStatus(status: ConnectStatus): Color = when (status) {
    ConnectStatus.CONNECTED -> Green
    ConnectStatus.CONNECTING,
    ConnectStatus.DESTINATION_SET,
    -> Yellow
    ConnectStatus.DISCONNECTED,
    ConnectStatus.CONNECT_FAILED,
    -> Red400
}
