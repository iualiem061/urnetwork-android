package com.bringyour.network.ui

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.navigation.NavController
import com.bringyour.network.DeviceManager
import com.bringyour.network.R
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import kotlinx.serialization.Serializable

@HiltViewModel
class MainNavViewModel @Inject constructor(
    deviceManager: DeviceManager,
): ViewModel() {

    private val _currentTopLevelRoute = MutableStateFlow(TopLevelScaffoldRoutes.CONNECT_CONTAINER)
    val currentTopLevelRoute: StateFlow<TopLevelScaffoldRoutes> = _currentTopLevelRoute.asStateFlow()

    val setCurrentTopLevelRoute: (TopLevelScaffoldRoutes) -> Unit = { route ->
        _currentTopLevelRoute.value = route
    }

    private val _currentRoute = MutableStateFlow<Route?>(null)
    val currentRoute: StateFlow<Route?> = _currentRoute.asStateFlow()

    val refreshTokenOnForeground: () -> Unit = {
        deviceManager.device?.refreshToken(0)
    }

    val setCurrentRoute: (Route?) -> Unit = { route ->
        _currentRoute.value = route
    }

}

@Serializable
sealed class Route {

    // this is from https://stackoverflow.com/questions/78489838/unable-to-get-route-object-from-currentbackstackentry-in-compose-navigation-outs
    // in order to compare NavDestination.destination with our routes
    companion object {
        fun fromString(route: String): Route? {
            return Route::class.sealedSubclasses.firstOrNull {
                route.contains(it.qualifiedName.toString())
            }?.objectInstance
        }
    }

    @Serializable object ConnectContainer: Route()
    @Serializable object Connect : Route()
    @Serializable object BrowseLocations : Route()
    @Serializable object AccountContainer : Route()
    // the ip check tab: the public IP and where the internet reads the device from
    @Serializable object IpCheck : Route()

    // a legacy guest adds a sign-in method to its network (GuestConversionSheet)
    @Serializable object GuestConversion: Route()
    @Serializable object Account : Route()
    @Serializable object Profile : Route()
    // Account -> Sessions: the account's sign-ins, with sign out
    @Serializable object Sessions : Route()
    // Account -> API keys: the network's API keys, with create and delete
    @Serializable object ApiKeys : Route()
    @Serializable object Settings : Route()
    // Settings -> Manage devices, and the client limit notice's Manage devices:
    // the network's devices, each removable to free a slot (ManageDevicesScreen)
    @Serializable object ManageDevices : Route()
    @Serializable object Widgets : Route()
    @Serializable object BlockedRegions: Route()
    @Serializable object Vless: Route()
    // Settings > Relays: the shared relay network and the personal relays hub
    @Serializable object Relays: Route()
    @Serializable object Developer: Route()
    @Serializable object Licenses: Route()
    @Serializable data class LicenseDetail(val index: Int): Route()
    @Serializable data class ContractStats(val provider: Boolean) : Route()
    @Serializable object SplitRules : Route()
    @Serializable object AppSplitRules : Route()
    @Serializable object DnsSettings : Route()
    @Serializable data class TransportSettings(val provider: Boolean) : Route()

    @Serializable object ProviderLocations : Route()

    @Serializable object MockLocationGuide : Route()
    @Serializable object ProviderIdentities : Route()

    @Serializable object Extenders : Route()
    @Serializable object ShareExtenders : Route()
    @Serializable object ImportExtenders : Route()
}

enum class TopLevelScaffoldRoutes(
    val selectedIcon: Int,
    val unselectedIcon: Int,
    // the tab's name for screen readers, since the bar shows only icons
    @StringRes val descriptionId: Int,
    // how the device tests find the tab; never shown or read out
    val testTag: String,
    val route: Route
) {
    CONNECT_CONTAINER(
        R.drawable.main_nav_globe_filled,
        R.drawable.main_nav_globe,
        R.string.connect,
        "acceptance.nav.connect",
        route = Route.ConnectContainer
    ),
    ACCOUNT_CONTAINER(
        R.drawable.main_nav_user_filled,
        R.drawable.main_nav_user,
        R.string.account,
        "acceptance.nav.account",
        route = Route.AccountContainer
    ),
    // the ip check tab, third in the bar
    IP_CHECK(
        R.drawable.main_nav_ip_filled,
        R.drawable.main_nav_ip,
        R.string.ip_check,
        "acceptance.nav.ip_check",
        route = Route.IpCheck
    )
}