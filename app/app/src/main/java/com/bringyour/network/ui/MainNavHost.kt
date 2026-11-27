package com.bringyour.network.ui

import com.bringyour.network.ui.components.FoldSafeAlertDialog
import com.bringyour.network.ui.connection_region.CantReachRegionPrompt
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.navigation.NavDestination.Companion.hierarchy
import com.bringyour.network.ui.account.AccountDetailPlaceholder
import com.bringyour.network.ui.components.ConnectArrangement
import com.bringyour.network.ui.components.DialogAnchor
import com.bringyour.network.ui.components.LocalDialogAnchor
import com.bringyour.network.ui.components.LocalWindowLayout
import com.bringyour.network.ui.components.NavigationLayout
import com.bringyour.network.ui.components.connectArrangementFor
import com.bringyour.network.ui.components.navigationLayoutFor
import com.bringyour.network.ui.components.recordDialogAnchor
import com.bringyour.network.ui.components.rememberWindowLayout
import com.bringyour.network.ui.components.sideBySideSplit
import com.bringyour.network.ui.components.sidePaneWidth
import com.bringyour.network.ui.connect.rememberLocationListPosition
import androidx.navigation.compose.currentBackStackEntryAsState
import com.bringyour.network.ui.shared.enums.PlanType
import com.bringyour.network.ui.upgrade.rememberPlanPresentation
import com.bringyour.network.ui.upgrade.CheckoutRefusal
import com.bringyour.network.ui.upgrade.CryptoInvoiceHost
import com.bringyour.network.ui.upgrade.PaymentMethodsScope
import com.bringyour.network.ui.upgrade.PaymentPastDueNotice
import com.bringyour.network.ui.upgrade.rememberPlanMethodSlot
import com.bringyour.network.ui.upgrade.rememberPlanPurchaser
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import android.util.Log
import android.widget.Toast
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.material3.Scaffold
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.StackedLineChart
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.NavigationRailItemDefaults
import com.bringyour.network.ui.connect.InsufficientBalanceNoticeEffect
import com.bringyour.network.ui.connect.upgradeShowsFreeRefresh
import com.bringyour.network.ui.components.LocalUpgradeWaitForRefresh
import com.bringyour.network.ui.connect.ConnectDrawerState
import com.bringyour.network.ui.connect.rememberConnectDrawerState
import androidx.compose.material3.SheetValue
import androidx.compose.material3.VerticalDivider
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteDefaults
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteType
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.material3.rememberStandardBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.bringyour.network.ui.account.AccountScreen
import com.bringyour.network.ui.settings.DeveloperScreen
import com.bringyour.network.ui.settings.LicenseDetailScreen
import com.bringyour.network.ui.settings.LicensesScreen
import com.bringyour.network.ui.account.ExtendersScreen
import com.bringyour.network.ui.account.ImportExtendersScreen
import com.bringyour.network.ui.account.ProviderIdentitiesScreen
import com.bringyour.network.ui.account.SessionsScreen
import com.bringyour.network.ui.account.ManageDevicesScreen
import com.bringyour.network.ui.account.ApiKeysScreen
import com.bringyour.network.ui.account.ShareExtendersScreen
import com.bringyour.network.ui.components.ProSunglassesFlight
import com.bringyour.network.ui.components.proFlightPixelation
import com.bringyour.network.ui.components.rememberProFlightClock
import com.bringyour.network.ui.components.overlays.FullScreenOverlay
import com.bringyour.network.ui.components.overlays.WelcomeAnimatedMainOverlay
import com.bringyour.network.ui.components.referral.LocalReferralCountLoad
import com.bringyour.network.ui.components.referral.LocalReferralTerms
import com.bringyour.network.ui.components.referral.ReferralRoyalToast
import com.bringyour.network.ui.connect.ConnectScreen
import com.bringyour.network.ui.connect.ConnectViewModel
import com.bringyour.network.ui.feedback.FeedbackScreen
import com.bringyour.network.ui.settings.SettingsViewModel
import com.bringyour.network.ui.shared.viewmodels.OverlayViewModel
import com.bringyour.network.ui.shared.viewmodels.PlanViewModel
import com.bringyour.network.ui.shared.viewmodels.ReferralCelebration
import com.bringyour.network.ui.shared.viewmodels.ReferralCodeViewModel

import androidx.navigation.compose.NavHost
import androidx.navigation.compose.navigation
import androidx.navigation.toRoute
import com.bringyour.network.ui.account.AccountViewModel
import com.bringyour.network.ui.account.GuestAccount
import com.bringyour.network.ui.account.GuestConversionSheet
import com.bringyour.network.ui.account.IntroFunnel
import com.bringyour.network.ui.account.UpgradeEntry
import com.bringyour.network.ui.components.nestedLinkBottomSheet.NestedLinkBottomSheet
import com.bringyour.network.ui.connect.BrowseLocationsScreen
import com.bringyour.network.ui.connect.LocationsListViewModel
import com.bringyour.network.ui.connect.IpCheckScreen
import com.bringyour.network.ui.leaderboard.LeaderboardScreen
import com.bringyour.network.ui.referrals.ReferralsScreen
import com.bringyour.network.ui.widgets.WidgetsScreen
import com.bringyour.network.ui.profile.ProfileScreen
import com.bringyour.network.ui.profile.ProfileViewModel
import com.bringyour.network.ui.settings.SettingsScreen
import com.bringyour.network.ui.settings.NavStyle
import com.bringyour.network.ui.settings.currentNavStyle
import com.bringyour.network.ui.settings.VlessSettingsScreen
import com.bringyour.network.ui.shared.viewmodels.SubscriptionBalanceViewModel
import com.bringyour.network.ui.wallet.BittensorKeyScreenRequests
import com.bringyour.network.ui.wallet.EarningsViewModel
import com.bringyour.network.ui.wallet.Top200SignLinks
import com.bringyour.network.ui.wallet.Top200SignScreen
import com.solana.mobilewalletadapter.clientlib.ActivityResultSender
import com.bringyour.network.R
import com.bringyour.network.TAG
import com.bringyour.network.ui.api_error.ApiErrorScreen
import com.bringyour.network.ui.balance_codes.BalanceCodesScreen
import com.bringyour.network.ui.blocked_regions.BlockedRegionsScreen
import com.bringyour.network.ui.components.overlays.OverlayMode
import com.bringyour.network.ui.shared.models.BundleStore
import com.bringyour.network.ui.shared.models.ProvideControlMode
import com.bringyour.network.ui.shared.viewmodels.AccountPointsViewModel
import com.bringyour.network.ui.shared.models.SectionLoad
import com.bringyour.network.ui.shared.viewmodels.NetworkReliabilityViewModel
import com.bringyour.network.ui.shared.viewmodels.Plan
import com.bringyour.network.ui.shared.viewmodels.SolanaPaymentViewModel
import com.bringyour.network.ui.stats.AppSplitRulesScreen
import com.bringyour.network.ui.stats.ContractStatsScreen
import com.bringyour.network.ui.connect.providerlocations.MockLocationGuideScreen
import com.bringyour.network.ui.connect.providerlocations.MockLocationSection
import com.bringyour.network.ui.connect.providerlocations.ProviderLocationsScreen
import com.bringyour.network.ui.stats.DnsSettingsScreen
import com.bringyour.network.ui.stats.TransportSettingsKind
import com.bringyour.network.ui.stats.TransportSettingsScreen
import com.bringyour.network.ui.stats.SplitRulesScreen
import com.bringyour.network.ui.theme.Pink
import com.bringyour.network.ui.components.CustomNavItem
import com.bringyour.network.ui.components.MorphingNavBar
import com.bringyour.network.ui.components.CapsuleNavBar
import com.bringyour.network.ui.components.NeonGlowNavBar
import com.bringyour.network.ui.components.BubbleNavBar
import com.bringyour.network.ui.upgrade.UpgradeGuestStatusPending
import com.bringyour.network.ui.upgrade.UpgradeScreen
import com.bringyour.sdk.ReliabilityWindow
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.bringyour.network.ui.theme.AppTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainNavHost(
    settingsViewModel: SettingsViewModel,
    subscriptionBalanceViewModel: SubscriptionBalanceViewModel,
    overlayViewModel: OverlayViewModel,
    animateIn: Boolean,
    targetLink: String?,
    defaultLocation: String?,
    activityResultSender: ActivityResultSender?,
    bundleStore: BundleStore?,
    isPro: Boolean,
    mainNavViewModel: MainNavViewModel = hiltViewModel<MainNavViewModel>(),
    referralCodeViewModel: ReferralCodeViewModel = hiltViewModel<ReferralCodeViewModel>(),
    connectViewModel: ConnectViewModel = hiltViewModel<ConnectViewModel>(),
    locationsListViewModel: LocationsListViewModel = hiltViewModel<LocationsListViewModel>(),
    networkReliabilityViewModel: NetworkReliabilityViewModel = hiltViewModel<NetworkReliabilityViewModel>(),
) {

    // the referral cap and bonus come from the server with the referral code;
    // everything signed-in reads them from here instead of hardcoding numbers
    val referralTerms by referralCodeViewModel.terms.collectAsState()
    // the usage bar's referral figures wait for the same read
    val referralCountLoad by referralCodeViewModel.codeLoad.collectAsState()
    // the window's size class and fold, for every layout below, its dialogs
    // and sheets included (ui/components/AdaptiveLayout.kt)
    val windowLayout = rememberWindowLayout()
    // the touch each dialog and sheet is opened from, so beside a fold it
    // opens on that touch's side (ui/components/FoldSafeDialogs.kt)
    val dialogAnchor = remember { DialogAnchor() }

    CompositionLocalProvider(
        LocalReferralTerms provides referralTerms,
        LocalReferralCountLoad provides referralCountLoad,
        LocalWindowLayout provides windowLayout,
        LocalDialogAnchor provides dialogAnchor,
    ) {
        // over every screen, to see where each touch goes down; its children
        // measure as they did directly in the window
        Box(
            modifier = Modifier.fillMaxSize().recordDialogAnchor(dialogAnchor),
            propagateMinConstraints = true,
        ) {
        // the flavors that sell outside a store share one crypto invoice in the
        // activity (src/cryptoPay); on Play this is the content as it is
        MainNavHostContent(
        settingsViewModel = settingsViewModel,
        subscriptionBalanceViewModel = subscriptionBalanceViewModel,
        overlayViewModel = overlayViewModel,
        animateIn = animateIn,
        targetLink = targetLink,
        defaultLocation = defaultLocation,
        activityResultSender = activityResultSender,
        bundleStore = bundleStore,
        isPro = isPro,
        mainNavViewModel = mainNavViewModel,
        referralCodeViewModel = referralCodeViewModel,
        connectViewModel = connectViewModel,
        locationsListViewModel = locationsListViewModel,
        networkReliabilityViewModel = networkReliabilityViewModel,
        )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MainNavHostContent(
    settingsViewModel: SettingsViewModel,
    subscriptionBalanceViewModel: SubscriptionBalanceViewModel,
    overlayViewModel: OverlayViewModel,
    animateIn: Boolean,
    targetLink: String?,
    defaultLocation: String?,
    activityResultSender: ActivityResultSender?,
    bundleStore: BundleStore?,
    isPro: Boolean,
    mainNavViewModel: MainNavViewModel = hiltViewModel<MainNavViewModel>(),
    referralCodeViewModel: ReferralCodeViewModel = hiltViewModel<ReferralCodeViewModel>(),
    connectViewModel: ConnectViewModel = hiltViewModel<ConnectViewModel>(),
    locationsListViewModel: LocationsListViewModel = hiltViewModel<LocationsListViewModel>(),
    networkReliabilityViewModel: NetworkReliabilityViewModel = hiltViewModel<NetworkReliabilityViewModel>(),
) {

    val currentTopLevelRoute by mainNavViewModel.currentTopLevelRoute.collectAsState()
    val currentRoute by mainNavViewModel.currentRoute.collectAsState()
    val reliabilityWindow by networkReliabilityViewModel.reliabilityWindow.collectAsState()
    val referralCode by referralCodeViewModel.referralCode.collectAsState()
    // the account the purchase overlay names (support inbox 6546)
    val planAccountName by subscriptionBalanceViewModel.planAccountName.collectAsState()
    // the overlay's plan-confirmed gate reads the server's balance state
    val hasActiveSubscription by subscriptionBalanceViewModel.hasActiveSubscription.collectAsState()

    val navItemColors = NavigationSuiteDefaults.itemColors(
        navigationBarItemColors = NavigationBarItemDefaults.colors(
            indicatorColor = Color.Transparent,
            selectedIconColor = Pink
        ),
        navigationRailItemColors = NavigationRailItemDefaults.colors(
            indicatorColor = Color.Transparent,
            selectedIconColor = Pink
        ),
        navigationDrawerItemColors = NavigationDrawerItemDefaults.colors()
    )

    val customColors = NavigationSuiteDefaults.colors(
        navigationRailContainerColor = AppTheme.bg,
        navigationBarContentColor = AppTheme.bg,
        navigationBarContainerColor = AppTheme.bg,
    )

    val windowLayout = LocalWindowLayout.current

    val lifecycleOwner = LocalLifecycleOwner.current

    val scope = rememberCoroutineScope()

    // hoisted here so re-tapping the connect tab can collapse it
    val connectActionsSheetState = rememberConnectDrawerState()

    // the bar or the rail from the window size class and posture, never the
    // device: the rail from medium width in a window at least 480 dp tall,
    // the bar otherwise and in tabletop (navigationLayoutFor)
    val navigationLayout = navigationLayoutFor(windowLayout.widthDp, windowLayout.heightDp, windowLayout.tabletop)
    val navSuiteLayoutType = when (navigationLayout) {
        NavigationLayout.Rail -> NavigationSuiteType.NavigationRail
        NavigationLayout.BottomBar -> NavigationSuiteType.NavigationBar
    }

    val navController = rememberNavController()

    val nestedLinkScaffoldState = rememberBottomSheetScaffoldState(
        bottomSheetState = rememberStandardBottomSheetState(
            initialValue = if (defaultLocation != null) SheetValue.Expanded else SheetValue.Hidden,
            skipHiddenState = false
        )
    )

    DisposableEffect(Unit) {

        val listener = NavController.OnDestinationChangedListener { _, destination, _ ->

            val route = Route.fromString(destination.route ?: "")
            if (route == Route.Connect && currentTopLevelRoute.route != Route.Connect) {
                mainNavViewModel.setCurrentTopLevelRoute(TopLevelScaffoldRoutes.CONNECT_CONTAINER)
            }

            if (route == Route.AccountContainer && currentTopLevelRoute.route != Route.AccountContainer) {
                mainNavViewModel.setCurrentTopLevelRoute(TopLevelScaffoldRoutes.ACCOUNT_CONTAINER)
            }

            if (route == Route.IpCheck && currentTopLevelRoute != TopLevelScaffoldRoutes.IP_CHECK) {
                mainNavViewModel.setCurrentTopLevelRoute(TopLevelScaffoldRoutes.IP_CHECK)
            }

            mainNavViewModel.setCurrentRoute(Route.fromString(destination.route ?: ""))
        }

        navController.addOnDestinationChangedListener(listener)

        onDispose {
            navController.removeOnDestinationChangedListener(listener)
        }
    }

    /**
     * Select a bottom-bar tab. The bar's own onClick and the dashboard widget both
     * go through here, so a re-tap pops the tab to its root (and collapses the
     * connect drawer) the same way from either entry.
     */
    val selectTopLevelRoute: (TopLevelScaffoldRoutes) -> Unit = { screen ->

        if (currentTopLevelRoute.route == Route.AccountContainer
            && screen.route == Route.AccountContainer
            && currentRoute != Route.Account
        ) {
            navController.popBackStack(Route.Account, inclusive = false)
        } else if (
            currentTopLevelRoute.route == Route.ConnectContainer
            && screen.route == Route.ConnectContainer
        ) {

            if (currentRoute != Route.Connect) {
                navController.popBackStack(Route.Connect, inclusive = false)
            } else {
                // already on the connect screen: collapse the drawer
                scope.launch {
                    connectActionsSheetState.partialExpand()
                }
            }

        } else {

            navController.navigate(screen.route) {
                // from https://developer.android.com/develop/ui/compose/navigation#bottom-nav
                // Pop up to the start destination of the graph to
                // avoid building up a large stack of destinations
                // on the back stack as users select items
                popUpTo(navController.graph.findStartDestination().id) {
                    saveState = true
                }
                // Avoid multiple copies of the same destination when
                // reselecting the same item
                launchSingleTop = true
                // Restore state when reselecting a previously selected item
                restoreState = true

            }
        }
        mainNavViewModel.setCurrentTopLevelRoute(screen)
    }

    /**
     * A Home Screen widget tap asked for a screen (MainApplication.widgetRoute,
     * set by QuickConnectActivity): go to the connect tab, then push the
     * provider details or the client contract details on top. Observed as a
     * flow so it works whether the app was cold-started for the tap or was
     * already running behind the launcher.
     */
    val widgetApp = androidx.compose.ui.platform.LocalContext.current.applicationContext as? com.bringyour.network.MainApplication
    val widgetRoute by (widgetApp?.widgetRoute ?: kotlinx.coroutines.flow.MutableStateFlow<String?>(null)).collectAsState()
    LaunchedEffect(widgetRoute) {
        val route = widgetRoute ?: return@LaunchedEffect
        widgetApp?.widgetRoute?.value = null
        if (route == com.bringyour.network.QuickConnectActivity.ROUTE_CONNECT ||
            route == com.bringyour.network.analytics.OnboardingLink.ROUTE_PREFIX + com.bringyour.network.analytics.OnboardingLink.STEP_CONNECT
        ) {
            // the dashboard widget (and the "connect" email link) do exactly what a tap on the Connect tab does
            selectTopLevelRoute(TopLevelScaffoldRoutes.CONNECT_CONTAINER)
            return@LaunchedEffect
        }
        if (route == com.bringyour.network.QuickConnectActivity.ROUTE_PROVIDER_LOCATIONS ||
            route == com.bringyour.network.QuickConnectActivity.ROUTE_CONTRACT_STATS
        ) {
            selectTopLevelRoute(TopLevelScaffoldRoutes.CONNECT_CONTAINER)
        }
        com.bringyour.network.analytics.OnboardingLink.stepForRoute(route)?.let { step ->
            selectTopLevelRoute(TopLevelScaffoldRoutes.ACCOUNT_CONTAINER)
            when (step) {
                com.bringyour.network.analytics.OnboardingLink.STEP_WIDGETS -> navController.navigate(Route.Widgets)
            }
            return@LaunchedEffect
        }
        navController.navigate(TopLevelScaffoldRoutes.CONNECT_CONTAINER.route) {
            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
        mainNavViewModel.setCurrentTopLevelRoute(TopLevelScaffoldRoutes.CONNECT_CONTAINER)
        when (route) {
            com.bringyour.network.QuickConnectActivity.ROUTE_PROVIDER_LOCATIONS ->
                navController.navigate(Route.ProviderLocations)
            com.bringyour.network.QuickConnectActivity.ROUTE_CONTRACT_STATS ->
                navController.navigate(Route.ContractStats(provider = false))
        }
    }

    DisposableEffect(lifecycleOwner) {

        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                mainNavViewModel.refreshTokenOnForeground()
            }
        }

        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    NestedLinkBottomSheet(
        scaffoldState = nestedLinkScaffoldState,
        targetLink = targetLink,
        defaultLocation = defaultLocation,
        connectViewModel = connectViewModel
    ) {
                // the navigation style the user chose in Settings: classic keeps
                // the adaptive suite (the full bar on phones, the rail on wider
                // windows); the four floating styles replace the phone bar
                val navStyle = currentNavStyle()
                val customNavStyle = navStyle != NavStyle.CLASSIC &&
                        navSuiteLayoutType == NavigationSuiteType.NavigationBar

                // the floating bars reserve their own room above the bottom edge
                val barInset = when {
                    customNavStyle -> 96.dp
                    navigationLayout == NavigationLayout.BottomBar -> 1.dp
                    else -> 0.dp
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(AppTheme.bg)
                ) {

                    // one container in every layout, so the screens keep their
                    // state when the bar becomes a rail: a hairline beside the
                    // rail, a gap above the bar
                    val appContent: @Composable () -> Unit = {
                        Row(
                            modifier = Modifier.padding(bottom = barInset)
                        ) {
                            if (navigationLayout == NavigationLayout.Rail) {
                                VerticalDivider(
                                    modifier = Modifier
                                        .width(1.dp)
                                        .fillMaxHeight(),
                                    color = AppTheme.border
                                )
                            }

                            MainNavContent(
                                settingsViewModel = settingsViewModel,
                                overlayViewModel = overlayViewModel,
                                navController = navController,
                                connectViewModel = connectViewModel,
                                locationsListViewModel = locationsListViewModel,
                                activityResultSender = activityResultSender,
                                subscriptionBalanceViewModel = subscriptionBalanceViewModel,
                                referralCodeViewModel = referralCodeViewModel,
                                bundleStore = bundleStore,
                                reliabilityWindow = reliabilityWindow,
                                isPro = isPro,
                                connectActionsSheetState = connectActionsSheetState,
                            )
                        }
                    }

                    if (customNavStyle) {
                        // the content under the floating bar or the side dock
                        appContent()

                        // the tabs every custom bar shows: the same items,
                        // icons and selection the classic bar has
                        val navBarItems: List<Pair<CustomNavItem, Boolean>> =
                            TopLevelScaffoldRoutes.entries.map { screen ->
                                CustomNavItem(
                                    iconRes = screen.unselectedIcon,
                                    selectedIconRes = screen.selectedIcon,
                                    label = stringResource(id = screen.descriptionId),
                                    testTag = screen.testTag,
                                ) to (screen == currentTopLevelRoute)
                            }
                        val onTab: (Int) -> Unit = { index ->
                            selectTopLevelRoute(TopLevelScaffoldRoutes.entries[index])
                        }

                        when (navStyle) {
                            NavStyle.MORPHING -> MorphingNavBar(
                                items = navBarItems,
                                onTab = onTab,
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .navigationBarsPadding()
                                    .padding(horizontal = 16.dp, vertical = 12.dp)
                                    .fillMaxWidth(),
                            )

                            NavStyle.CAPSULE -> CapsuleNavBar(
                                items = navBarItems,
                                onTab = onTab,
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .navigationBarsPadding()
                                    .padding(vertical = 12.dp),
                            )

                            NavStyle.NEON -> NeonGlowNavBar(
                                items = navBarItems,
                                onTab = onTab,
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .navigationBarsPadding()
                                    .padding(horizontal = 16.dp, vertical = 12.dp)
                                    .fillMaxWidth(),
                            )

                            NavStyle.BUBBLE -> BubbleNavBar(
                                items = navBarItems,
                                onTab = onTab,
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .navigationBarsPadding()
                                    .padding(horizontal = 16.dp, vertical = 12.dp)
                                    .fillMaxWidth(),
                            )

                            NavStyle.CLASSIC -> Unit
                        }
                    } else {
                        NavigationSuiteScaffold(
                            containerColor = AppTheme.bg,
                            // the content's color on the black container, as a
                            // Scaffold gives every other screen: the connect
                            // screen's ripples, hover and focus layers draw in it
                            contentColor = MaterialTheme.colorScheme.onBackground,
                            navigationSuiteColors = customColors,
                            layoutType = navSuiteLayoutType,

                            navigationSuiteItems = {
                                TopLevelScaffoldRoutes.entries.forEach { screen ->
                                    item(
                                        icon = {

                                            val iconRes = if (screen == currentTopLevelRoute) {
                                                screen.selectedIcon
                                            } else {
                                                screen.unselectedIcon
                                            }

                                            Icon(
                                                painter = painterResource(id = iconRes),
                                                contentDescription = stringResource(id = screen.descriptionId),
                                                modifier = Modifier.testTag(screen.testTag)
                                            )

                                        },
                                        selected = screen == currentTopLevelRoute,
                                        onClick = { selectTopLevelRoute(screen) },
                                        colors = navItemColors,
                                    )
                                }
                            }
                        ) {
                            appContent()
                        }
                    }
                }
            }

    WelcomeAnimatedMainOverlay(
        animateIn = animateIn,
    )

    FullScreenOverlay(
        overlayViewModel,
        referralCode = referralCode,
        planUpgradeConfirmed = hasActiveSubscription,
        planAccountName = planAccountName,
    )

}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainNavContent(
    settingsViewModel: SettingsViewModel,
    overlayViewModel: OverlayViewModel,
    navController: NavHostController,
    connectViewModel: ConnectViewModel,
    locationsListViewModel: LocationsListViewModel,
    activityResultSender: ActivityResultSender?,
    subscriptionBalanceViewModel: SubscriptionBalanceViewModel,
    referralCodeViewModel: ReferralCodeViewModel,
    bundleStore: BundleStore?,
    reliabilityWindow: ReliabilityWindow?,
    isPro: Boolean,
    connectActionsSheetState: ConnectDrawerState,
    accountViewModel: AccountViewModel = hiltViewModel<AccountViewModel>(),
    profileViewModel: ProfileViewModel = hiltViewModel<ProfileViewModel>(),
) {
    val isGuestNetwork by subscriptionBalanceViewModel.isGuestNetwork.collectAsState()
    LaunchedEffect(isGuestNetwork) {
        accountViewModel.setGuestNetwork(isGuestNetwork)
    }
    // a refreshed guest is a guest only once the balance has loaded
    val guestStatusKnown by subscriptionBalanceViewModel.guestStatusKnown.collectAsState()

    val localDensityCurrent = LocalDensity.current
    val canvasSizePx =
        with(localDensityCurrent) { connectViewModel.canvasSize.times(0.4f).toPx() }

    LaunchedEffect(Unit) {
        connectViewModel.initSuccessPoints(canvasSizePx)
    }

    LifecycleResumeEffect(Unit) {
        connectViewModel.update()

        onPauseOrDispose {
        }
    }

    // the location picker open as a dialog beside the connect panes, and its
    // list's scroll position: above the screens, so a fold or a resize carries
    // both between the dialog and the picker's own screen
    var locationPickerOpen by rememberSaveable { mutableStateOf(false) }
    val locationListPosition = rememberLocationListPosition()

    // Account at medium width and up is a list-detail layout: the account list
    // stays at the start and the chosen screen opens beside it. The list lives
    // outside the NavHost, so choosing a screen moves only the detail.
    val windowLayout = LocalWindowLayout.current
    val backStack by navController.currentBackStack.collectAsState()
    val accountListDetail = windowLayout.isMediumWidthOrWider &&
        backStack.lastOrNull()?.destination?.hierarchy?.any { it.hasRoute<Route.AccountContainer>() } == true
    // the screen chosen from the list: the one right above the account root
    val accountRootIndex = backStack.indexOfLast { it.destination.hasRoute<Route.Account>() }
    val chosenAccountRoute = if (accountRootIndex < 0) null else {
        backStack.getOrNull(accountRootIndex + 1)?.destination?.route?.let { Route.fromString(it) }
    }
    // above the list's two homes (the start pane, the account destination), so
    // a resize keeps its scroll
    val accountScrollState = rememberScrollState()
    val accountScreen: @Composable (navigate: (Route) -> Unit) -> Unit = { navigate ->
        AccountScreen(
            navController,
            accountViewModel,
            overlayViewModel = overlayViewModel,
            subscriptionBalanceViewModel = subscriptionBalanceViewModel,
            meanReliabilityWeight = reliabilityWindow?.meanReliabilityWeight ?: 0.0,
            navigate = navigate,
            chosenRoute = if (accountListDetail) chosenAccountRoute else null,
            scrollState = accountScrollState,
        )
    }

    // where the account panes sit in the window, to split them at a fold
    var paneRowInWindow by remember { mutableStateOf(Rect.Zero) }
    val density = LocalDensity.current
    val listOnLeft = LocalLayoutDirection.current == LayoutDirection.Ltr
    val paneRowWidth = paneRowInWindow.width / density.density
    val accountListWidth = sidePaneWidth(
        split = sideBySideSplit(
            containerStart = paneRowInWindow.left / density.density,
            width = paneRowWidth,
            fold = windowLayout.hinge,
            sidePaneOnLeft = listOnLeft,
        ),
        width = paneRowWidth,
        sidePaneOnLeft = listOnLeft,
    ).dp

    Row(
        modifier = Modifier
            .fillMaxSize()
            .onGloballyPositioned { paneRowInWindow = it.boundsInWindow() }
    ) {

        if (accountListDetail) {
            Box(modifier = Modifier.width(accountListWidth.coerceAtLeast(0.dp))) {
                // a choice replaces the screen beside the list instead of
                // stacking on it
                accountScreen { route ->
                    navController.navigate(route) {
                        popUpTo(Route.Account) { inclusive = false }
                        launchSingleTop = true
                    }
                }
            }
            VerticalDivider(
                modifier = Modifier
                    .width(1.dp)
                    .fillMaxHeight(),
                color = AppTheme.border
            )
        }

        // the page transitions need the reading direction to slide pages
        // the way the language travels (forward arrives from the trailing
        // edge), mirrored for rtl
        val rtlLayout = LocalLayoutDirection.current == LayoutDirection.Rtl
        NavHost(
            navController = navController,
            startDestination = Route.ConnectContainer,
            modifier = Modifier.weight(1f),
        ) {

            navigation<Route.ConnectContainer>(
                startDestination = Route.Connect,
                enterTransition = NavigationAnimations.tabEnterTransition(),
                exitTransition = NavigationAnimations.tabExitTransition(),
                popEnterTransition = NavigationAnimations.tabPopEnterTransition(),
                popExitTransition = NavigationAnimations.tabPopExitTransition()
            ) {
                composable<Route.Connect> {
                    ConnectScreen(
                        connectViewModel,
                        overlayViewModel,
                        locationsListViewModel,
                        navController,
                        subscriptionBalanceViewModel,
                        bundleStore,
                        meanReliabilityWeight = reliabilityWindow?.meanReliabilityWeight ?: 0.0,
                        isPro = isPro,
                        connectActionsSheetState = connectActionsSheetState,
                        locationPickerOpen = locationPickerOpen,
                        setLocationPickerOpen = { locationPickerOpen = it },
                        locationListPosition = locationListPosition,
                    )
                }

                composable<Route.BrowseLocations>(
                    enterTransition = NavigationAnimations.enterTransition(rtlLayout),
                    exitTransition = NavigationAnimations.exitTransition(rtlLayout),
                    popEnterTransition = NavigationAnimations.popEnterTransition(rtlLayout),
                    popExitTransition = NavigationAnimations.popExitTransition(rtlLayout)
                ) {
                    // unfolded or widened onto the connect panes: the picker
                    // continues as their location dialog, search and scroll kept
                    if (connectArrangementFor(windowLayout.widthDp, windowLayout.heightDp, windowLayout.fold) != ConnectArrangement.Drawer) {
                        LaunchedEffect(Unit) {
                            locationPickerOpen = true
                            navController.popBackStack(Route.BrowseLocations, inclusive = true)
                        }
                    }
                    BrowseLocationsScreen(
                        navController = navController,
                        connectViewModel = connectViewModel,
                        locationsListViewModel = locationsListViewModel,
                        connectActionsSheetState = connectActionsSheetState,
                        listPosition = locationListPosition,
                    )
                }

                composable<Route.ContractStats>(
                    enterTransition = NavigationAnimations.enterTransition(rtlLayout),
                    exitTransition = NavigationAnimations.exitTransition(rtlLayout),
                    popEnterTransition = NavigationAnimations.popEnterTransition(rtlLayout),
                    popExitTransition = NavigationAnimations.popExitTransition(rtlLayout)
                ) { backStackEntry ->
                    val route: Route.ContractStats = backStackEntry.toRoute()
                    ContractStatsScreen(
                        navController = navController,
                        provider = route.provider,
                    )
                }

                composable<Route.SplitRules>(
                    enterTransition = NavigationAnimations.enterTransition(rtlLayout),
                    exitTransition = NavigationAnimations.exitTransition(rtlLayout),
                    popEnterTransition = NavigationAnimations.popEnterTransition(rtlLayout),
                    popExitTransition = NavigationAnimations.popExitTransition(rtlLayout)
                ) {
                    SplitRulesScreen(
                        navController = navController,
                    )
                }

                composable<Route.AppSplitRules>(
                    enterTransition = NavigationAnimations.enterTransition(rtlLayout),
                    exitTransition = NavigationAnimations.exitTransition(rtlLayout),
                    popEnterTransition = NavigationAnimations.popEnterTransition(rtlLayout),
                    popExitTransition = NavigationAnimations.popExitTransition(rtlLayout)
                ) {
                    AppSplitRulesScreen(
                        navController = navController,
                    )
                }

                composable<Route.DnsSettings>(
                    enterTransition = NavigationAnimations.enterTransition(rtlLayout),
                    exitTransition = NavigationAnimations.exitTransition(rtlLayout),
                    popEnterTransition = NavigationAnimations.popEnterTransition(rtlLayout),
                    popExitTransition = NavigationAnimations.popExitTransition(rtlLayout)
                ) {
                    DnsSettingsScreen(
                        navController = navController,
                    )
                }

                composable<Route.TransportSettings>(
                    enterTransition = NavigationAnimations.enterTransition(rtlLayout),
                    exitTransition = NavigationAnimations.exitTransition(rtlLayout),
                    popEnterTransition = NavigationAnimations.popEnterTransition(rtlLayout),
                    popExitTransition = NavigationAnimations.popExitTransition(rtlLayout)
                ) { backStackEntry ->
                    val route: Route.TransportSettings = backStackEntry.toRoute()
                    TransportSettingsScreen(
                        navController = navController,
                        kind = if (route.provider) TransportSettingsKind.PROVIDER else TransportSettingsKind.CLIENT,
                    )
                }

                composable<Route.ProviderLocations>(
                    enterTransition = NavigationAnimations.enterTransition(rtlLayout),
                    exitTransition = NavigationAnimations.exitTransition(rtlLayout),
                    popEnterTransition = NavigationAnimations.popEnterTransition(rtlLayout),
                    popExitTransition = NavigationAnimations.popExitTransition(rtlLayout)
                ) {
                    ProviderLocationsScreen(
                        navController = navController,
                        getLocationColor = locationsListViewModel.getLocationColor,
                        mockLocationSection = {
                            MockLocationSection(navController = navController)
                        },
                        stayingClientId = connectViewModel.selectedLocation?.connectLocationId?.clientId?.idStr,
                        // Stay on this exit reconnects to the one provider, through the
                        // same connect gate as a location pick, and returns to the grid
                        onStayOnExit = { location ->
                            connectViewModel.connect(location)
                            navController.popBackStack()
                        },
                    )
                }

                composable<Route.MockLocationGuide>(
                    enterTransition = NavigationAnimations.enterTransition(rtlLayout),
                    exitTransition = NavigationAnimations.exitTransition(rtlLayout),
                    popEnterTransition = NavigationAnimations.popEnterTransition(rtlLayout),
                    popExitTransition = NavigationAnimations.popExitTransition(rtlLayout)
                ) {
                    MockLocationGuideScreen(
                        navController = navController,
                    )
                }
            }

            composable<Route.GuestConversion>(
                enterTransition = NavigationAnimations.enterTransition(rtlLayout),
                exitTransition = NavigationAnimations.exitTransition(rtlLayout),
                popEnterTransition = NavigationAnimations.popEnterTransition(rtlLayout),
                popExitTransition = NavigationAnimations.popExitTransition(rtlLayout)
            ) {
                GuestConversionSheet(
                    settingsViewModel = settingsViewModel,
                    activityResultSender = activityResultSender,
                    refreshJwt = subscriptionBalanceViewModel.refreshJwt,
                    onAdded = {
                        // the server stops reporting a guest once the method exists
                        subscriptionBalanceViewModel.fetchSubscriptionBalance()
                        navController.popBackStack()
                    },
                    onDismiss = { navController.popBackStack() }
                )
            }

            // the device list, top level like the upgrade: the client limit
            // notice on the connect screen opens it, and so does Settings
            composable<Route.ManageDevices>(
                enterTransition = NavigationAnimations.enterTransition(rtlLayout),
                exitTransition = NavigationAnimations.exitTransition(rtlLayout),
                popEnterTransition = NavigationAnimations.popEnterTransition(rtlLayout),
                popExitTransition = NavigationAnimations.popExitTransition(rtlLayout)
            ) {
                ManageDevicesScreen(navController = navController)
            }

            // the ip check tab: one top-level leaf screen, so the tab's state
            // (the last read) is kept like the other tabs' screens
            composable<Route.IpCheck>(
                enterTransition = NavigationAnimations.tabEnterTransition(),
                exitTransition = NavigationAnimations.tabExitTransition(),
                popEnterTransition = NavigationAnimations.tabPopEnterTransition(),
                popExitTransition = NavigationAnimations.tabPopExitTransition()
            ) {
                IpCheckScreen(
                    connectViewModel = connectViewModel,
                )
            }

            navigation<Route.AccountContainer>(
                startDestination = Route.Account,
                enterTransition = NavigationAnimations.tabEnterTransition(),
                exitTransition = NavigationAnimations.tabExitTransition(),
                popEnterTransition = NavigationAnimations.tabPopEnterTransition(),
                popExitTransition = NavigationAnimations.tabPopExitTransition()
            ) {
                composable<Route.Account> {
                    if (accountListDetail) {
                        // the list is beside it: no screen chosen yet
                        AccountDetailPlaceholder()
                    } else {
                        accountScreen { route -> navController.navigate(route) }
                    }
                }
                composable<Route.Profile>(
                    enterTransition = NavigationAnimations.enterTransition(rtlLayout),
                    exitTransition = NavigationAnimations.exitTransition(rtlLayout),
                    popEnterTransition = NavigationAnimations.popEnterTransition(rtlLayout),
                    popExitTransition = NavigationAnimations.popExitTransition(rtlLayout)
                ) { ProfileScreen(
                    navController,
                    accountViewModel,
                    profileViewModel,
                    overlayViewModel
                ) }
                composable<Route.Sessions>(
                    enterTransition = NavigationAnimations.enterTransition(rtlLayout),
                    exitTransition = NavigationAnimations.exitTransition(rtlLayout),
                    popEnterTransition = NavigationAnimations.popEnterTransition(rtlLayout),
                    popExitTransition = NavigationAnimations.popExitTransition(rtlLayout)
                ) {
                    SessionsScreen(navController = navController)
                }
                composable<Route.ApiKeys>(
                    enterTransition = NavigationAnimations.enterTransition(rtlLayout),
                    exitTransition = NavigationAnimations.exitTransition(rtlLayout),
                    popEnterTransition = NavigationAnimations.popEnterTransition(rtlLayout),
                    popExitTransition = NavigationAnimations.popExitTransition(rtlLayout)
                ) {
                    ApiKeysScreen(navController = navController)
                }
                composable<Route.Settings>(
                    enterTransition = NavigationAnimations.enterTransition(rtlLayout),
                    exitTransition = NavigationAnimations.exitTransition(rtlLayout),
                    popEnterTransition = NavigationAnimations.popEnterTransition(rtlLayout),
                    popExitTransition = NavigationAnimations.popExitTransition(rtlLayout)
                ) { SettingsScreen(
                    navController,
                    accountViewModel,
                    settingsViewModel,
                    overlayViewModel,
                    activityResultSender,
                ) }

                // Settings > Proxy mode: the VLESS proxy settings editor
                composable<Route.Vless>(
                    enterTransition = NavigationAnimations.enterTransition(rtlLayout),
                    exitTransition = NavigationAnimations.exitTransition(rtlLayout),
                    popEnterTransition = NavigationAnimations.popEnterTransition(rtlLayout),
                    popExitTransition = NavigationAnimations.popExitTransition(rtlLayout)
                ) {
                    VlessSettingsScreen(navController = navController)
                }

                composable<Route.ProviderIdentities>(
                    enterTransition = NavigationAnimations.enterTransition(rtlLayout),
                    exitTransition = NavigationAnimations.exitTransition(rtlLayout),
                    popEnterTransition = NavigationAnimations.popEnterTransition(rtlLayout),
                    popExitTransition = NavigationAnimations.popExitTransition(rtlLayout)
                ) {
                    ProviderIdentitiesScreen(
                        navController = navController,
                    )
                }

                composable<Route.Extenders>(
                    enterTransition = NavigationAnimations.enterTransition(rtlLayout),
                    exitTransition = NavigationAnimations.exitTransition(rtlLayout),
                    popEnterTransition = NavigationAnimations.popEnterTransition(rtlLayout),
                    popExitTransition = NavigationAnimations.popExitTransition(rtlLayout)
                ) {
                    ExtendersScreen(
                        navController = navController,
                    )
                }

                composable<Route.ShareExtenders>(
                    enterTransition = NavigationAnimations.enterTransition(rtlLayout),
                    exitTransition = NavigationAnimations.exitTransition(rtlLayout),
                    popEnterTransition = NavigationAnimations.popEnterTransition(rtlLayout),
                    popExitTransition = NavigationAnimations.popExitTransition(rtlLayout)
                ) {
                    ShareExtendersScreen(
                        navController = navController,
                    )
                }

                composable<Route.ImportExtenders>(
                    enterTransition = NavigationAnimations.enterTransition(rtlLayout),
                    exitTransition = NavigationAnimations.exitTransition(rtlLayout),
                    popEnterTransition = NavigationAnimations.popEnterTransition(rtlLayout),
                    popExitTransition = NavigationAnimations.popExitTransition(rtlLayout)
                ) {
                    ImportExtendersScreen(
                        navController = navController,
                    )
                }

                composable<Route.Developer>(
                    enterTransition = NavigationAnimations.enterTransition(rtlLayout),
                    exitTransition = NavigationAnimations.exitTransition(rtlLayout),
                    popEnterTransition = NavigationAnimations.popEnterTransition(rtlLayout),
                    popExitTransition = NavigationAnimations.popExitTransition(rtlLayout)
                ) {
                    DeveloperScreen(navController = navController)
                }

                composable<Route.Licenses>(
                    enterTransition = NavigationAnimations.enterTransition(rtlLayout),
                    exitTransition = NavigationAnimations.exitTransition(rtlLayout),
                    popEnterTransition = NavigationAnimations.popEnterTransition(rtlLayout),
                    popExitTransition = NavigationAnimations.popExitTransition(rtlLayout)
                ) {
                    LicensesScreen(navController = navController)
                }

                composable<Route.LicenseDetail>(
                    enterTransition = NavigationAnimations.enterTransition(rtlLayout),
                    exitTransition = NavigationAnimations.exitTransition(rtlLayout),
                    popEnterTransition = NavigationAnimations.popEnterTransition(rtlLayout),
                    popExitTransition = NavigationAnimations.popExitTransition(rtlLayout)
                ) { backStackEntry ->
                    val route: Route.LicenseDetail = backStackEntry.toRoute()
                    LicenseDetailScreen(navController = navController, index = route.index)
                }

                composable<Route.BlockedRegions>(
                    enterTransition = NavigationAnimations.enterTransition(rtlLayout),
                    exitTransition = NavigationAnimations.exitTransition(rtlLayout),
                    popEnterTransition = NavigationAnimations.popEnterTransition(rtlLayout),
                    popExitTransition = NavigationAnimations.popExitTransition(rtlLayout)
                ) {
                    BlockedRegionsScreen(
                        navController = navController,
                        countries = locationsListViewModel.connectCountries,
                        getLocationColor = locationsListViewModel.getLocationColor
                    )
                }

                composable<Route.Widgets>(
                    enterTransition = NavigationAnimations.enterTransition(rtlLayout),
                    exitTransition = NavigationAnimations.exitTransition(rtlLayout),
                    popEnterTransition = NavigationAnimations.popEnterTransition(rtlLayout),
                    popExitTransition = NavigationAnimations.popExitTransition(rtlLayout)
                ) {
                    WidgetsScreen(navController = navController)
                }
            }
        }
    }

}

// The page-change rhythm: how far a page travels and how long the fades
// breathe. The travels are fractions of the width; the enters settle on
// springs, the leaves run on short tweens, so a page arrives softly and
// clears quickly.
private const val ENTER_TRAVEL_FRACTION = 0.22f
private const val EXIT_TRAVEL_FRACTION = 0.10f
private const val POP_TRAVEL_FRACTION = 0.14f
private const val ENTER_FADE_MILLIS = 300
private const val EXIT_FADE_MILLIS = 200
private const val POP_FADE_MILLIS = 260

private const val ENTER_SLIDE_STIFFNESS = 350f
private const val ENTER_SLIDE_DAMPING = 0.9f

object NavigationAnimations {

    /**
     * Forward navigation (a new page opens): the page arrives from the
     * trailing edge -- the edge the reading direction travels toward, the
     * right in ltr, the left in rtl -- gliding in with a soft spring settle
     * and a quiet fade.
     */
    fun enterTransition(rtl: Boolean): AnimatedContentTransitionScope<*>.() -> EnterTransition = {
        val direction = if (rtl) -1 else 1
        slideInHorizontally(
            initialOffsetX = { fullWidth -> (fullWidth * ENTER_TRAVEL_FRACTION).toInt() * direction },
            animationSpec = spring(
                dampingRatio = ENTER_SLIDE_DAMPING,
                stiffness = ENTER_SLIDE_STIFFNESS,
            ),
        ) + fadeIn(
            animationSpec = tween(
                durationMillis = ENTER_FADE_MILLIS,
                easing = LinearOutSlowInEasing,
            ),
        )
    }

    /**
     * Forward navigation (the page being covered): it slips a little toward
     * the leading edge and dims away, so the new page reads as arriving over
     * it rather than blinking in its place.
     */
    fun exitTransition(rtl: Boolean): AnimatedContentTransitionScope<*>.() -> ExitTransition = {
        val direction = if (rtl) -1 else 1
        slideOutHorizontally(
            targetOffsetX = { fullWidth -> -(fullWidth * EXIT_TRAVEL_FRACTION).toInt() * direction },
            animationSpec = tween(
                durationMillis = EXIT_FADE_MILLIS,
                easing = FastOutLinearInEasing,
            ),
        ) + fadeOut(
            animationSpec = tween(
                durationMillis = EXIT_FADE_MILLIS,
                easing = FastOutLinearInEasing,
            ),
        )
    }

    /**
     * Back navigation (the page underneath returns): it slides back in from
     * the leading edge, the mirror of the forward arrival, a touch shorter
     * and a touch slower to read as settling home.
     */
    fun popEnterTransition(rtl: Boolean): AnimatedContentTransitionScope<*>.() -> EnterTransition = {
        val direction = if (rtl) -1 else 1
        slideInHorizontally(
            initialOffsetX = { fullWidth -> -(fullWidth * POP_TRAVEL_FRACTION).toInt() * direction },
            animationSpec = spring(
                dampingRatio = ENTER_SLIDE_DAMPING,
                stiffness = ENTER_SLIDE_STIFFNESS,
            ),
        ) + fadeIn(
            animationSpec = tween(
                durationMillis = POP_FADE_MILLIS,
                easing = LinearOutSlowInEasing,
            ),
        )
    }

    /**
     * Back navigation (the closing page leaves): it glides out toward the
     * trailing edge, the direction it arrived from, and fades on the way.
     */
    fun popExitTransition(rtl: Boolean): AnimatedContentTransitionScope<*>.() -> ExitTransition = {
        val direction = if (rtl) -1 else 1
        slideOutHorizontally(
            targetOffsetX = { fullWidth -> (fullWidth * ENTER_TRAVEL_FRACTION).toInt() * direction },
            animationSpec = tween(
                durationMillis = POP_FADE_MILLIS,
                easing = FastOutLinearInEasing,
            ),
        ) + fadeOut(
            animationSpec = tween(
                durationMillis = POP_FADE_MILLIS,
                easing = FastOutLinearInEasing,
            ),
        )
    }

    // Tab switches (the bottom bar): a quiet crossfade with a whisper of
    // scale, so the three top screens read as trading places softly instead
    // of blinking on. Both directions are the same -- no tab is "forward".
    fun tabEnterTransition(): AnimatedContentTransitionScope<*>.() -> EnterTransition = {
        fadeIn(
            animationSpec = tween(
                durationMillis = 220,
                easing = LinearOutSlowInEasing
            )
        ) + scaleIn(
            initialScale = 0.985f,
            animationSpec = tween(
                durationMillis = 220,
                easing = LinearOutSlowInEasing
            )
        )
    }

    fun tabExitTransition(): AnimatedContentTransitionScope<*>.() -> ExitTransition = {
        fadeOut(
            animationSpec = tween(
                durationMillis = 160,
                easing = FastOutLinearInEasing
            )
        )
    }

    fun tabPopEnterTransition(): AnimatedContentTransitionScope<*>.() -> EnterTransition = tabEnterTransition()

    fun tabPopExitTransition(): AnimatedContentTransitionScope<*>.() -> ExitTransition = tabExitTransition()
}
