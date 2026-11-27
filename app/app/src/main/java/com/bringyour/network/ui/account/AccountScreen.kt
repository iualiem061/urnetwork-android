package com.bringyour.network.ui.account

import com.bringyour.network.ui.components.rememberFoldSafeSheetState
import com.bringyour.network.ui.components.entrance
import com.bringyour.network.ui.components.pressScale
import com.bringyour.network.ui.components.tabletReadableColumn
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.ArrowOutward
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.bringyour.network.R
import com.bringyour.network.ui.Route
import com.bringyour.network.ui.components.URNavListItem
import com.bringyour.network.ui.components.AccountSwitcher
import com.bringyour.network.ui.components.LoginMode
import com.bringyour.network.ui.components.UsageBar
import com.bringyour.network.ui.shared.viewmodels.OverlayViewModel
import com.bringyour.network.ui.shared.viewmodels.PostQuantumIdentityViewModel
import com.bringyour.network.ui.shared.viewmodels.SubscriptionBalanceViewModel

import com.bringyour.network.ui.theme.BlueMedium

import com.bringyour.network.ui.theme.URNetworkTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import com.bringyour.network.ui.theme.AppTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountScreen(
    navController: NavHostController,
    accountViewModel: AccountViewModel,
    overlayViewModel: OverlayViewModel,
    subscriptionBalanceViewModel: SubscriptionBalanceViewModel,
    meanReliabilityWeight: Double,
    // where a row of the account list goes; beside a detail pane (medium width
    // and up) it replaces the chosen screen instead of stacking on it
    navigate: (Route) -> Unit = { navController.navigate(it) },
    // the screen open beside the list, highlighted in it; null with no detail pane
    chosenRoute: Route? = null,
    // kept by the caller, so moving between the list pane and the full screen
    // on a resize keeps the scroll
    scrollState: ScrollState = rememberScrollState(),
    postQuantumIdentityViewModel: PostQuantumIdentityViewModel = hiltViewModel(),
) {

    val scope = rememberCoroutineScope()

    val networkUser by accountViewModel.networkUser.collectAsState()
    val availableBalanceByteCount by subscriptionBalanceViewModel.availableBalanceByteCount.collectAsState()
    val dailyBalanceBytes by subscriptionBalanceViewModel.startBalanceByteCount.collectAsState()

    val refreshState = rememberPullToRefreshState()

    LaunchedEffect(Unit) {
        subscriptionBalanceViewModel.fetchSubscriptionBalance()
    }

    Scaffold() { innerPadding ->

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            PullToRefreshBox(
                isRefreshing = subscriptionBalanceViewModel.isRefreshingSubscriptionBalance,
                state = refreshState,
                onRefresh = subscriptionBalanceViewModel.refreshSubscriptionBalance,
            ) {

                Column(
                    modifier = Modifier
                        .tabletReadableColumn()
                        .fillMaxSize()
                        .verticalScroll(scrollState)
                        .background(AppTheme.bg)
                ) {

                    AccountScreenContent(
                        loginMode = accountViewModel.loginMode,
                        navController = navController,
                        navigate = navigate,
                        chosenRoute = chosenRoute,
                        scope = scope,
                        networkName = networkUser?.networkName,
                        usedBytes = subscriptionBalanceViewModel.usedBalanceByteCount,
                        pendingBytes = subscriptionBalanceViewModel.pendingBalanceByteCount,
                        availableBytes = availableBalanceByteCount,
                        meanReliabilityWeight = meanReliabilityWeight,
                        dailyBalanceBytes = dailyBalanceBytes,
                        postQuantumIdentityViewModel = postQuantumIdentityViewModel,
                    )
                }
            }
        }

    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountScreenContent(
    loginMode: LoginMode,
    navController: NavHostController,
    scope: CoroutineScope,
    // where the account list's rows go (AccountScreen)
    navigate: (Route) -> Unit = { navController.navigate(it) },
    // the screen open beside the list, highlighted in it
    chosenRoute: Route? = null,
    networkName: String?,
    usedBytes: Long,
    availableBytes: Long,
    pendingBytes: Long,
    meanReliabilityWeight: Double,
    dailyBalanceBytes: Long,
    // null in previews, rendering the panel's empty (no key) state
    postQuantumIdentityViewModel: PostQuantumIdentityViewModel? = null,
) {

    val uriHandler = LocalUriHandler.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppTheme.bg)
            .padding(vertical = 16.dp),
        verticalArrangement = Arrangement.Top
    ) {

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(stringResource(id = R.string.account), style = MaterialTheme.typography.headlineSmall)
            AccountSwitcher(
                loginMode = loginMode,
                networkName = networkName,
                createAccount = { navController.navigate(Route.GuestConversion) }
            )
        }

        Spacer(modifier = Modifier.height(28.dp))

        Box(
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .background(
                    AppTheme.bgElevated,
                    RoundedCornerShape(12.dp)
                )
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column() {

                UsageBar(
                    usedBytes = usedBytes,
                    pendingBytes = pendingBytes,
                    availableBytes = availableBytes,
                    meanReliabilityWeight = meanReliabilityWeight,
                    dailyByteCount = dailyBalanceBytes,
                )

                Spacer(modifier = Modifier.height(16.dp))

                HorizontalDivider()

                Spacer(modifier = Modifier.height(16.dp))
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        URNavListItem(
            iconResourceId = R.drawable.nav_list_item_user,
            text = stringResource(id = R.string.profile),
            onClick = {
                if (loginMode == LoginMode.Authenticated) {
                    navigate(Route.Profile)
                } else {
                    navigate(Route.GuestConversion)
                }
            },
            selected = chosenRoute == Route.Profile
        )
        HorizontalDivider()
        // the account's sign-ins, with sign out; always listed, and the
        // screen says when the server has no session list yet. A guest
        // network has no sign-in method to come back with, so it adds one
        // first, like Profile
        URNavListItem(
            iconResourceId = R.drawable.nav_list_item_sessions,
            text = stringResource(id = R.string.sessions_title),
            onClick = {
                if (loginMode == LoginMode.Authenticated) {
                    navigate(Route.Sessions)
                } else {
                    navigate(Route.GuestConversion)
                }
            },
            selected = chosenRoute == Route.Sessions
        )
        HorizontalDivider()
        // the network's API keys, with create and delete, beside Sessions,
        // the other credentials. A key gives full access to the network, so
        // a guest network adds a sign-in method first, like Sessions
        URNavListItem(
            iconResourceId = R.drawable.nav_list_item_api_keys,
            text = stringResource(id = R.string.api_keys_title),
            onClick = {
                if (loginMode == LoginMode.Authenticated) {
                    navigate(Route.ApiKeys)
                } else {
                    navigate(Route.GuestConversion)
                }
            },
            selected = chosenRoute == Route.ApiKeys
        )
        HorizontalDivider()
        URNavListItem(
            iconResourceId = R.drawable.nav_list_item_settings,
            text = stringResource(id = R.string.settings),
            onClick = {
                if (loginMode == LoginMode.Authenticated) {
                    navigate(Route.Settings)
                } else {
                    navigate(Route.GuestConversion)
                }
            },
            selected = chosenRoute == Route.Settings
        )
        HorizontalDivider()
        // the quick connect tile and the home screen widgets need no account,
        // so the row is open to guests too
        URNavListItem(
            iconResourceId = R.drawable.nav_list_item_widgets,
            text = stringResource(id = R.string.widgets),
            onClick = {
                navigate(Route.Widgets)
            },
            selected = chosenRoute == Route.Widgets
        )
        HorizontalDivider()
        // the extender settings of this network space, and the share and
        // import of an extender list (EXTENDER.md K6, K7)
        URNavListItem(
            iconResourceId = R.drawable.nav_list_item_extenders,
            text = stringResource(id = R.string.extenders),
            onClick = {
                if (loginMode == LoginMode.Authenticated) {
                    navigate(Route.Extenders)
                } else {
                    navigate(Route.GuestConversion)
                }
            },
            selected = chosenRoute == Route.Extenders
        )
        HorizontalDivider()

        /**
         * view IP
         */
        Row(
            modifier = Modifier.fillMaxWidth()
                .clickable {
                    uriHandler.openUri("https://ur.io/ip")
                }
                .padding(vertical = 12.dp, horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row {
                Icon(
                    Icons.Filled.LocationOn,
                    contentDescription = null,
                    tint = AppTheme.textMuted
                )
                Spacer(modifier = Modifier.width(16.dp))
                Text(stringResource(id = R.string.check_ip))
            }
            Row {
                Icon(
                    Icons.Filled.ArrowOutward,
                    contentDescription = null,
                    tint = AppTheme.textMuted,
                    modifier = Modifier
                        .size(18.dp)
                )

                Spacer(modifier = Modifier.width(4.dp))
            }
        }
        HorizontalDivider()

        /**
         * The network's social channels: Telegram and Instagram, each with
         * the platform's own glyph in its brand color
         */
        SocialLinkRow(
            iconResourceId = R.drawable.ic_telegram,
            iconTint = TelegramBrandBlue,
            title = stringResource(id = R.string.social_telegram),
            handle = "@Aliem_061",
            onClick = { uriHandler.openUri("https://t.me/Aliem_061") },
            modifier = Modifier.entrance(5),
        )
        HorizontalDivider()
        SocialLinkRow(
            iconResourceId = R.drawable.ic_instagram,
            iconTint = InstagramBrandPink,
            title = stringResource(id = R.string.social_instagram),
            handle = "@Aliem061",
            onClick = { uriHandler.openUri("https://instagram.com/Aliem061") },
            modifier = Modifier.entrance(6),
        )
        HorizontalDivider()

        Spacer(modifier = Modifier.height(16.dp))

        /**
         * Post Quantum Identity
         */
        PostQuantumIdentityPanel(
            viewModel = postQuantumIdentityViewModel,
            navigateToProviderIdentities = {
                navigate(Route.ProviderIdentities)
            }
        )

        Spacer(modifier = Modifier.height(16.dp))

    }
}

/**
 * The platforms' own brand colors, at their official values.
 */
private val TelegramBrandBlue = Color(0xFF26A5E4)
private val InstagramBrandPink = Color(0xFFE4405F)

/**
 * One social channel row: the platform's glyph in its brand color, the
 * channel's name and handle, and an outward arrow; the row opens the link.
 */
@Composable
private fun SocialLinkRow(
    iconResourceId: Int,
    iconTint: Color,
    title: String,
    handle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .pressScale(interactionSource, scaleDown = 0.97f)
            .clickable(
                interactionSource = interactionSource,
                onClick = onClick,
            )
            .padding(vertical = 12.dp, horizontal = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                painter = painterResource(id = iconResourceId),
                contentDescription = title,
                tint = iconTint,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    title,
                    style = MaterialTheme.typography.bodyLarge
                )
                Text(
                    handle,
                    style = MaterialTheme.typography.bodySmall.copy(
                        // a handle stays one clean left-to-right run in the RTL
                        // layout, its characters never reordered
                        textDirection = TextDirection.Ltr,
                    ),
                    color = AppTheme.textMuted
                )
            }
        }
        Icon(
            Icons.Filled.ArrowOutward,
            contentDescription = null,
            tint = AppTheme.textMuted,
            modifier = Modifier.size(18.dp)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview
@Composable
private fun AccountAuthenticatedPreview() {

    val navController = rememberNavController()

    val scope = rememberCoroutineScope()

    URNetworkTheme {

        Scaffold() { innerPadding ->
            Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .padding(16.dp)
            ) {

                AccountScreenContent(
                    loginMode = LoginMode.Authenticated,
                    navController = navController,
                    scope = scope,
                    networkName = "ur_network",
                    usedBytes = 30_000,
                    pendingBytes = 10_000,
                    availableBytes = 60_000,
                    meanReliabilityWeight = 0.1,
                    dailyBalanceBytes = 10_000
                )
            }
        }

    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview
@Composable
private fun AccountGuestPreview() {

    val navController = rememberNavController()

    val scope = rememberCoroutineScope()

    URNetworkTheme {
        Scaffold() { innerPadding ->
            Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .padding(16.dp)
            ) {

                AccountScreenContent(
                    loginMode = LoginMode.Guest,
                    navController = navController,
                    scope = scope,
                    networkName = "ur_network",
                    usedBytes = 30_000,
                    pendingBytes = 10_000,
                    availableBytes = 60_000,
                    meanReliabilityWeight = 0.1,
                    dailyBalanceBytes = 10_000
                )
            }
        }
    }
}
