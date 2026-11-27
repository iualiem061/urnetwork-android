package com.bringyour.network.ui.settings

import android.text.format.DateUtils
import android.widget.Toast
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import com.bringyour.network.R
import com.bringyour.network.ui.components.ButtonStyle
import com.bringyour.network.ui.components.PulseDot
import com.bringyour.network.ui.components.URButton
import com.bringyour.network.ui.components.URInlineErrorText
import com.bringyour.network.ui.components.URTextInput
import com.bringyour.network.ui.components.bouncyClick
import com.bringyour.network.ui.components.entrance
import com.bringyour.network.ui.components.rememberRoundedCountryFlag
import com.bringyour.network.ui.components.RoundedFlag
import com.bringyour.network.ui.theme.AppTheme
import com.bringyour.network.ui.theme.BlueMedium
import com.bringyour.network.ui.theme.Green400
import com.bringyour.network.ui.theme.Pink
import com.bringyour.network.ui.theme.Red400
import com.bringyour.network.ui.theme.TopBarTitleTextStyle
import com.bringyour.network.ui.theme.Yellow400

/**
 * The relays of the app (Settings > Relays): one place, two sides.
 *
 * The public side is the shared relay network the app carries with it -- the
 * public pools of [RelayNetwork.kt], fetched, deduped and grouped by country,
 * every server one tap away from live. The user never adds a shared relay:
 * the sources are the app's, kept fresh on their own.
 *
 * The personal side is the user's own relays: a pasted `vless://` link, an
 * imported subscription, or the full manual form (VlessSettingsForm, the
 * editor the VLESS screen used to be).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RelayHubScreen(
    navController: NavController,
    viewModel: VlessSettingsViewModel = hiltViewModel(),
) {
    var tab by rememberSaveable { mutableStateOf(RelayHubTab.SHARED) }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        stringResource(id = R.string.relays),
                        style = TopBarTitleTextStyle,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                            contentDescription = stringResource(id = R.string.back),
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = AppTheme.bg,
                ),
            )
        },
        containerColor = AppTheme.bg,
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize(),
        ) {
            RelayHubTabRow(
                selected = tab,
                onSelect = { tab = it },
                modifier = Modifier.entrance(0),
            )

            // the two sides trade places with the app's soft slide, the
            // direction the switch travels mirrored for rtl
            val rtlLayout = LocalLayoutDirection.current == LayoutDirection.Rtl
            AnimatedContent(
                targetState = tab,
                transitionSpec = {
                    val direction = if (targetState.ordinal > initialState.ordinal) 1 else -1
                    val factor = direction * if (rtlLayout) -1 else 1
                    (
                        slideInHorizontally { it / 4 * factor } +
                            fadeIn(tween(240))
                        ) togetherWith (
                        slideOutHorizontally { it / 4 * -factor } +
                            fadeOut(tween(160))
                        )
                },
                label = "relayHubTab",
            ) { current ->
                when (current) {
                    RelayHubTab.SHARED -> RelaySharedTab(
                        viewModel = viewModel,
                        modifier = Modifier.fillMaxSize(),
                    )
                    RelayHubTab.PERSONAL -> VlessSettingsForm(
                        viewModel = viewModel,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
        }
    }
}

/** The hub's two sides, as the tab row names them. */
private enum class RelayHubTab(val labelResId: Int) {
    SHARED(R.string.relays_public),
    PERSONAL(R.string.relays_personal),
}

/**
 * The hub's tab row: a stadium of two equal sides whose colors trade
 * quietly, the chosen one lifting to the elevated ground with a soft
 * spring's scale.
 */
@Composable
private fun RelayHubTabRow(
    selected: RelayHubTab,
    onSelect: (RelayHubTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(50))
            .background(AppTheme.bgTinted)
            .padding(4.dp),
    ) {
        RelayHubTab.entries.forEach { entry ->
            val isSelected = entry == selected
            val slotColor by animateColorAsState(
                targetValue = if (isSelected) AppTheme.bgElevated else Color.Transparent,
                animationSpec = tween(200),
                label = "relayTabColor",
            )
            val slotScale by animateFloatAsState(
                targetValue = if (isSelected) 1f else 0.97f,
                animationSpec = spring(
                    dampingRatio = 0.7f,
                    stiffness = Spring.StiffnessMediumLow,
                ),
                label = "relayTabScale",
            )

            Row(
                modifier = Modifier
                    .weight(1f)
                    .graphicsLayer {
                        scaleX = slotScale
                        scaleY = slotScale
                    }
                    .clip(RoundedCornerShape(50))
                    .background(slotColor)
                    .bouncyClick(haptic = true) { onSelect(entry) }
                    .padding(vertical = 10.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                val textColor by animateColorAsState(
                    targetValue = if (isSelected) BlueMedium else AppTheme.textMuted,
                    animationSpec = tween(200),
                    label = "relayTabText",
                )
                Text(
                    stringResource(id = entry.labelResId),
                    style = MaterialTheme.typography.bodyMedium,
                    color = textColor,
                )
            }
        }
    }
}

/**
 * The shared relay network: the header card with what the app carries and
 * when it last fetched it, a search that filters every country at once, then
 * the countries with their flags, each expanding to its servers -- a tap on
 * a server makes it the space's live relay, a tap on its test pill measures
 * the TCP RTT to the server itself.
 */
@Composable
private fun RelaySharedTab(
    viewModel: VlessSettingsViewModel,
    modifier: Modifier = Modifier,
) {
    LaunchedEffect(Unit) {
        viewModel.ensureSharedRelays()
    }

    val context = LocalContext.current
    val showToast: (Int) -> Unit = { resId ->
        Toast.makeText(context, resId, Toast.LENGTH_SHORT).show()
    }

    var search by rememberSaveable { mutableStateOf(TextFieldValue()) }
    val groups = remember(viewModel.sharedServers, search.text) {
        groupRelaysByCountry(viewModel.sharedServers, search.text)
    }
    val countryCount = remember(viewModel.sharedServers) {
        viewModel.sharedServers.mapNotNull { relayCountryCode(it) }.distinct().size
    }
    val expandedCountries = remember { mutableStateMapOf<String, Boolean>() }

    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(
            start = 16.dp,
            end = 16.dp,
            bottom = 28.dp,
        ),
    ) {

        // ---- the header card
        item(key = "shared-header") {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .animateItem()
                    .background(AppTheme.bgElevated, RoundedCornerShape(16.dp))
                    .padding(16.dp)
                    .entrance(0),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        stringResource(id = R.string.relays_public_title),
                        style = MaterialTheme.typography.titleMedium,
                        color = AppTheme.textPrimary,
                    )
                    PulseDot(color = BlueMedium, size = 8.dp)
                }

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    stringResource(id = R.string.relays_public_description),
                    style = MaterialTheme.typography.bodySmall,
                    color = AppTheme.textMuted,
                )

                Spacer(modifier = Modifier.height(12.dp))

                if (viewModel.sharedServers.isNotEmpty()) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        StatChip(
                            text = stringResource(
                                id = R.string.relays_servers_stats,
                                viewModel.sharedServers.size,
                                countryCount,
                            ),
                        )
                    }
                    if (viewModel.sharedFetchedAt > 0) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            stringResource(
                                id = R.string.relays_last_update,
                                DateUtils.getRelativeTimeSpanString(viewModel.sharedFetchedAt),
                            ),
                            style = MaterialTheme.typography.labelSmall,
                            color = AppTheme.textFaint,
                        )
                    }
                } else if (viewModel.sharedLoading) {
                    Text(
                        stringResource(id = R.string.relays_loading),
                        style = MaterialTheme.typography.labelSmall,
                        color = AppTheme.textFaint,
                    )
                }

                if (viewModel.sharedFetchFailed) {
                    Spacer(modifier = Modifier.height(10.dp))
                    URInlineErrorText(
                        message = stringResource(id = R.string.relays_refresh_failed),
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                URButton(
                    onClick = { viewModel.refreshSharedRelays() },
                    style = ButtonStyle.PRIMARY,
                    enabled = viewModel.editable,
                    isProcessing = viewModel.sharedLoading,
                ) { style ->
                    Text(stringResource(id = R.string.relays_refresh), style = style)
                }
            }
        }

        // ---- the search box
        item(key = "shared-search") {
            URTextInput(
                value = search,
                onValueChange = { search = it },
                label = stringResource(id = R.string.relays_search_label),
                placeholder = stringResource(id = R.string.relays_search_hint),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Text,
                    imeAction = ImeAction.Search,
                ),
                enabled = viewModel.sharedServers.isNotEmpty(),
                modifier = Modifier
                    .animateItem()
                    .padding(top = 14.dp),
            )
        }

        if (viewModel.sharedServers.isEmpty() && !viewModel.sharedLoading) {
            item(key = "shared-empty") {
                Text(
                    stringResource(id = R.string.relays_empty),
                    style = MaterialTheme.typography.bodySmall,
                    color = AppTheme.textMuted,
                    modifier = Modifier
                        .padding(vertical = 20.dp)
                        .entrance(1),
                )
            }
        }

        // ---- the countries
        groups.forEach { group ->
            val groupKey = group.code ?: "other"
            val expanded = expandedCountries[groupKey] == true

            item(key = "country-$groupKey") {
                RelayCountryHeader(
                    group = group,
                    expanded = expanded,
                    onToggle = { expandedCountries[groupKey] = !expanded },
                    modifier = Modifier
                        .animateItem()
                        .padding(top = 10.dp)
                        .entrance(1),
                )
            }

            if (expanded) {
                items(group.servers, key = { server -> server.id }) { server ->
                    RelaySharedServerRow(
                        server = server,
                        active = viewModel.sharedActiveId == server.id,
                        editable = viewModel.editable,
                        rtt = viewModel.relayRtts[server.id],
                        onActivate = {
                            viewModel.activate(
                                server,
                                onActivated = { showToast(R.string.vless_server_activated_toast) },
                                onFailed = { showToast(R.string.vless_server_activate_failed_toast) },
                            )
                        },
                        onTest = { viewModel.testRelayRtt(server) },
                        modifier = Modifier
                            .animateItem()
                            .padding(top = 6.dp),
                    )
                }
            }
        }
    }
}

/** The header's small stat pill. */
@Composable
private fun StatChip(text: String) {
    Surface(
        shape = RoundedCornerShape(50),
        color = AppTheme.bgTinted,
    ) {
        Text(
            text,
            style = MaterialTheme.typography.labelSmall,
            color = AppTheme.textMuted,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
        )
    }
}

/**
 * One country of the network: its flag, its name in the app's language, how
 * many relays it holds, and a chevron that rolls over softly when the group
 * opens and closes.
 */
@Composable
private fun RelayCountryHeader(
    group: RelayCountryGroup,
    expanded: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val chevronRotation by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        animationSpec = spring(
            dampingRatio = 0.75f,
            stiffness = Spring.StiffnessMediumLow,
        ),
        label = "countryChevron",
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(AppTheme.bgElevated)
            .bouncyClick(haptic = true) { onToggle() }
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val flag = group.code?.let { rememberRoundedCountryFlag(it) }
        if (flag != null) {
            RoundedFlag(imageResourceId = flag, height = 20.dp)
        } else {
            Icon(
                painter = painterResource(id = R.drawable.main_nav_globe),
                contentDescription = stringResource(id = R.string.relays_other),
                tint = AppTheme.textMuted,
                modifier = Modifier.size(width = 30.dp, height = 20.dp),
            )
        }

        Spacer(modifier = Modifier.width(10.dp))

        Text(
            text = group.code?.let { relayCountryDisplayName(it) }
                ?: stringResource(id = R.string.relays_other),
            style = MaterialTheme.typography.bodyMedium,
            color = AppTheme.textPrimary,
            modifier = Modifier.weight(1f),
        )

        Surface(
            shape = RoundedCornerShape(50),
            color = AppTheme.bgTinted,
        ) {
            Text(
                group.servers.size.toString(),
                style = MaterialTheme.typography.labelSmall,
                color = AppTheme.textMuted,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp),
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        Icon(
            imageVector = Icons.Filled.KeyboardArrowDown,
            contentDescription = null,
            tint = AppTheme.textMuted,
            modifier = Modifier.graphicsLayer {
                rotationZ = chevronRotation
            },
        )
    }
}

/**
 * One relay of the shared network: its name and address on the tap that
 * activates it (the same live path the personal library uses), the active
 * badge riding in beside the name, and the test pill that measures the
 * server's own TCP RTT and carries the answer in the app's green-amber-red.
 */
@Composable
private fun RelaySharedServerRow(
    server: VlessServer,
    active: Boolean,
    editable: Boolean,
    rtt: Long?,
    onActivate: () -> Unit,
    onTest: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(if (active) AppTheme.bgTinted else AppTheme.bgElevated)
            .bouncyClick { onActivate() }
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    server.name,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (active) Pink else AppTheme.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                AnimatedVisibility(visible = active) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = Pink.copy(alpha = 0.16f),
                        ) {
                            Text(
                                stringResource(id = R.string.vless_server_active),
                                style = MaterialTheme.typography.labelSmall,
                                color = Pink,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                            )
                        }
                    }
                }
            }
            Text(
                server.address,
                style = MaterialTheme.typography.bodySmall.copy(
                    textDirection = TextDirection.Ltr,
                ),
                color = AppTheme.textFaint,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        RelayRttPill(
            rtt = rtt,
            onTest = onTest,
            enabled = editable,
        )
    }
}

/**
 * The row's test pill: «تست» before anything is measured, a quiet ellipsis
 * while the handshake is away, the RTT in the app's traffic colors once it
 * lands, and a dash when the server does not answer. The answer retests on
 * a tap of its own.
 */
@Composable
private fun RelayRttPill(
    rtt: Long?,
    onTest: () -> Unit,
    enabled: Boolean,
) {
    val (label, color) = when (rtt) {
        null -> stringResource(id = R.string.relays_test) to AppTheme.textMuted
        RTT_TESTING -> stringResource(id = R.string.relays_testing) to AppTheme.textFaint
        RTT_FAILED -> stringResource(id = R.string.relays_rtt_failed) to AppTheme.textFaint
        else -> when {
            rtt < 120 -> stringResource(id = R.string.relays_rtt_ms, rtt) to Green400
            rtt < 250 -> stringResource(id = R.string.relays_rtt_ms, rtt) to Yellow400
            else -> stringResource(id = R.string.relays_rtt_ms, rtt) to Red400
        }
    }

    Surface(
        shape = RoundedCornerShape(50),
        color = AppTheme.bgTinted,
        // bouncyClick has no enabled of its own: a disabled pill is the
        // plain surface, so an uneditable space still shows the answers
        modifier = if (enabled) {
            Modifier
                .clip(RoundedCornerShape(50))
                .bouncyClick { onTest() }
        } else {
            Modifier
        },
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelSmall.copy(
                textDirection = TextDirection.Ltr,
            ),
            color = color,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
        )
    }
}
