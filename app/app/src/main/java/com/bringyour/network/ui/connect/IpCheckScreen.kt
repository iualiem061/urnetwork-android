package com.bringyour.network.ui.connect

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.bringyour.network.R
import com.bringyour.network.ui.components.PulseDot
import com.bringyour.network.ui.components.RoundedFlag
import com.bringyour.network.ui.components.countryDisplayName
import com.bringyour.network.ui.components.rememberRoundedCountryFlag
import com.bringyour.network.ui.components.entrance
import com.bringyour.network.ui.components.pressScale
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
 *
 * The layout keeps the app's standard rhythm: the content clears the system
 * bars, one 16 dp gutter, a 24 dp step from the title, 16 dp between cards,
 * 16 dp padding inside them; the cards sit in the middle of the leftover
 * space rather than hugging the top, and every state change -- connecting,
 * a fresh read, a new address -- lands as a soft crossfade, the live dot
 * breathing quietly while a connection stands.
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
            .windowInsetsPadding(
                WindowInsets.safeDrawing.only(WindowInsetsSides.Vertical)
            )
            .padding(horizontal = 16.dp)
    ) {

        Text(
            stringResource(id = R.string.ip_check),
            style = MaterialTheme.typography.headlineSmall,
            color = AppTheme.textPrimary,
            modifier = Modifier
                .padding(top = 12.dp)
                .entrance(0),
        )

        // the cards ride in the middle of the room left under the title, so
        // the page reads as one balanced group instead of clinging to the top
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f, fill = false),
            verticalArrangement = Arrangement.Center,
        ) {

            // ---- the connection state card

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        AppTheme.bgElevated,
                        RoundedCornerShape(16.dp),
                    )
                    .padding(16.dp)
                    .entrance(1)
            ) {
                Column {

                    Text(
                        stringResource(id = R.string.ip_check_connection_state),
                        style = MaterialTheme.typography.bodyMedium,
                        color = AppTheme.textMuted,
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // the state swaps as a soft crossfade, the dot breathing
                    // while a connection stands or is coming up
                    AnimatedContent(
                        targetState = connectStatus,
                        transitionSpec = {
                            (fadeIn(androidx.compose.animation.core.tween(220)) +
                                    slideInVertically(
                                        animationSpec = spring(
                                            dampingRatio = 0.8f,
                                            stiffness = Spring.StiffnessMediumLow,
                                        ),
                                        initialOffsetY = { it / 3 },
                                    )) togetherWith
                                    (fadeOut(androidx.compose.animation.core.tween(160)) +
                                            slideOutVertically(
                                                animationSpec = spring(
                                                    dampingRatio = 0.8f,
                                                    stiffness = Spring.StiffnessMediumLow,
                                                ),
                                                targetOffsetY = { -it / 3 },
                                            ))
                        },
                        label = "connectionState",
                    ) { status ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            PulseDot(
                                color = colorForConnectStatus(status),
                                pulsing = status != ConnectStatus.DISCONNECTED,
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                stringResource(id = labelForConnectStatus(status)),
                                style = MaterialTheme.typography.bodyLarge,
                                color = AppTheme.textPrimary,
                            )
                        }
                    }

                    // the selected location while a connection stands: its name
                    // beside its flag, or the best-available label
                    if (connectStatus != ConnectStatus.DISCONNECTED) {
                        Spacer(modifier = Modifier.height(12.dp))
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
                                selectedLocation?.let(::locationDisplayName)
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
                        RoundedCornerShape(16.dp),
                    )
                    .padding(16.dp)
                    .animateContentSize(
                        animationSpec = spring(
                            dampingRatio = 0.9f,
                            stiffness = Spring.StiffnessMediumLow,
                        )
                    )
                    .entrance(2)
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

                        CheckAgainPill(
                            loading = state is IpCheckState.Loading,
                            onClick = { viewModel.refresh() },
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

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
                        }

                        is IpCheckState.Ready -> {
                            // the address: one clean left-to-right run even in
                            // the right-to-left layout, sliding in softly on
                            // every fresh read
                            AnimatedContent(
                                targetState = current.info.ip,
                                transitionSpec = {
                                    (fadeIn(androidx.compose.animation.core.tween(240)) +
                                            slideInVertically(
                                                animationSpec = spring(
                                                    dampingRatio = 0.8f,
                                                    stiffness = Spring.StiffnessMediumLow,
                                                ),
                                                initialOffsetY = { it / 4 },
                                            )) togetherWith fadeOut(androidx.compose.animation.core.tween(120))
                                },
                                label = "ipAddress",
                            ) { ip ->
                                Text(
                                    ip,
                                    style = MaterialTheme.typography.titleLarge.copy(
                                        fontFamily = FontFamily.Monospace,
                                        textDirection = TextDirection.Ltr,
                                    ),
                                    color = AppTheme.textPrimary,
                                )
                            }

                            current.info.country?.let { country ->
                                IpInfoRow(
                                    label = stringResource(id = R.string.ip_check_location),
                                    value = listOfNotNull(
                                        current.info.city,
                                        countryDisplayName(current.info.countryCode, country),
                                    ).joinToString(" · "),
                                )
                            }

                            current.info.isp?.let { isp ->
                                IpInfoRow(
                                    label = stringResource(id = R.string.ip_check_isp),
                                    value = isp,
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))
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

        // the breathing room under the cards
        Spacer(modifier = Modifier.height(24.dp))
    }
}

/**
 * The re-read control: a proper pill button with the app's press bounce, the
 * spinner taking the place of the label while a read is running.
 */
@Composable
private fun CheckAgainPill(
    loading: Boolean,
    onClick: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    Surface(
        onClick = onClick,
        enabled = !loading,
        interactionSource = interactionSource,
        shape = RoundedCornerShape(50),
        color = AppTheme.bgTinted,
        border = BorderStroke(1.dp, AppTheme.border),
        modifier = Modifier.pressScale(interactionSource, scaleDown = 0.93f),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (loading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(14.dp),
                    strokeWidth = 2.dp,
                    color = AppTheme.textMuted,
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(
                stringResource(id = R.string.ip_check_refresh),
                style = MaterialTheme.typography.labelMedium,
                color = AppTheme.textPrimary,
            )
        }
    }
}

/** One label-value line of the public IP card; the label reads first. */
@Composable
private fun IpInfoRow(
    label: String,
    value: String,
) {
    Spacer(modifier = Modifier.height(10.dp))
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
