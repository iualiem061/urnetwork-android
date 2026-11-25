package com.bringyour.network.ui.connect

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bringyour.network.DeviceManager
import com.bringyour.network.ForegroundDeviceControllerOwner
import com.bringyour.sdk.ConnectLocation
import com.bringyour.sdk.ConnectLocationList
import com.bringyour.sdk.DeviceLocal
import com.bringyour.sdk.LocationsViewController
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class LocationsListViewModel @Inject constructor(
    private val deviceManager: DeviceManager
): ViewModel(), DefaultLifecycleObserver {

    private var locationsVc: LocationsViewController? = null
    private var filteredLocationsSub: com.bringyour.sdk.Sub? = null
    private var removeDeviceChangeListener: (() -> Unit)? = null
    private val processLifecycle = ProcessLifecycleOwner.get().lifecycle
    private val controllerOwner =
        ForegroundDeviceControllerOwner<DeviceLocal, LocationsViewController>(
            open = { openLocationsViewController(it) },
            close = { device, vc -> closeLocationsViewController(device, vc) },
        )

    private val _filterLocationsState = MutableStateFlow(FilterLocationsState.Loading)
    val filterLocationsState: StateFlow<FilterLocationsState> = _filterLocationsState.asStateFlow()

    val connectCountries = mutableStateListOf<ConnectLocation>()

    var searchQueryTextFieldValue by mutableStateOf(TextFieldValue(""))
        private set

    var currentSearchQuery by mutableStateOf("")
        private set

    val setSearchQueryTextFieldValue: (TextFieldValue) -> Unit = {
        searchQueryTextFieldValue = it
    }

    val devices = mutableStateListOf<ConnectLocation>()

    val regions = mutableStateListOf<ConnectLocation>()

    val cities = mutableStateListOf<ConnectLocation>()

    // when searching, items with matchDistance of 0
    val bestSearchMatches = mutableStateListOf<ConnectLocation>()

    /**
     * The ping test (LocationPing): one result per country code, shared by
     * every row of that country. `null` means no test has run for the code.
     */
    val locationPings = mutableStateMapOf<String, LocationPing>()

    var pingTestRunning by mutableStateOf(false)
        private set

    /**
     * Measures every listed country's ingress round trip: marks each row's
     * country as Testing, then resolves them in parallel and fills in the
     * results. Rows without a test endpoint read Unavailable.
     */
    fun testLocationPings() {
        if (pingTestRunning) {
            return
        }
        val countryCodes = listOf(bestSearchMatches, connectCountries, regions, cities, devices)
            .asSequence()
            .flatten()
            .mapNotNull { location -> location.countryCode?.takeIf { it.isNotEmpty() } }
            .distinct()
            .toList()
        if (countryCodes.isEmpty()) {
            return
        }
        pingTestRunning = true
        viewModelScope.launch {
            try {
                Snapshot.withMutableSnapshot {
                    countryCodes.forEach { code ->
                        if (LocationPingTest.hasEndpoint(code)) {
                            locationPings[code] = LocationPing.Testing
                        }
                    }
                }
                val results = countryCodes
                    .filter { LocationPingTest.hasEndpoint(it) }
                    .map { code ->
                        async(Dispatchers.IO) { code to LocationPingTest.measure(code) }
                    }
                    .awaitAll()
                Snapshot.withMutableSnapshot {
                    results.forEach { (code, ping) ->
                        locationPings[code] = ping
                    }
                }
            } finally {
                pingTestRunning = false
            }
        }
    }

    /** The measured ping of a row's country, or null while untested. */
    fun pingFor(location: ConnectLocation): LocationPing? {
        val code = location.countryCode?.takeIf { it.isNotEmpty() } ?: return null
        return locationPings[code]
    }

    val refreshLocations: () -> Unit = {
        locationsVc?.filterLocations(currentSearchQuery)
    }

    // while the home screen shows, fetch the locations again so the selected
    // location's provider count follows the network
    private val refreshTicker = LocationsRefreshTicker(
        scope = viewModelScope,
        intervalMillis = LOCATIONS_REFRESH_INTERVAL_MILLIS,
        isFetching = { _filterLocationsState.value == FilterLocationsState.Loading },
        refresh = { refreshLocations() },
    )

    val setHomeVisible: (Boolean) -> Unit = { visible ->
        refreshTicker.setVisible(visible)
    }

    val filterLocations:(String) -> Unit = { search ->

        if (
            // we don't want to re-run the same query, if we're not in an error state
            (search != currentSearchQuery && _filterLocationsState.value != FilterLocationsState.Error) ||
            // but if we're in an error state, run the query
            _filterLocationsState.value == FilterLocationsState.Error)
        {
            currentSearchQuery = search
            locationsVc?.filterLocations(search)
        }
    }

    val getLocationColor: (String) -> Color = { color ->
        val hex = locationsVc?.getColorHex(color)
        if (hex != null) {
            try {
                Color(android.graphics.Color.parseColor("#$hex"))
            } catch (_: IllegalArgumentException) {
                Color.White
            }
        } else {
            Color.White
        }
    }

    private val makeConnectLocationCollection: (ConnectLocationList) -> Collection<ConnectLocation> = { list ->
        val locations = mutableListOf<ConnectLocation>()
        val n = list.len()

        for (i in 0 until n) {
            locations.add(list.get(i))
        }

        locations
    }

    private fun addFilteredLocationsListener(vc: LocationsViewController) {
        filteredLocationsSub = vc.addFilteredLocationsListener { filteredLocation, state ->
            viewModelScope.launch {
                if (locationsVc !== vc) {
                    return@launch
                }

                val newBestSearchMatches = mutableListOf<ConnectLocation>()
                val newConnectCountries = mutableListOf<ConnectLocation>()
                val newDevices = mutableListOf<ConnectLocation>()
                val newCities = mutableListOf<ConnectLocation>()
                val newRegions = mutableListOf<ConnectLocation>()

                filteredLocation?.let {
                    newBestSearchMatches.addAll(makeConnectLocationCollection(it.bestMatches))
                    newConnectCountries.addAll(makeConnectLocationCollection(it.countries))
                    newDevices.addAll(makeConnectLocationCollection(it.devices))
                    newCities.addAll(makeConnectLocationCollection(it.cities))
                    newRegions.addAll(makeConnectLocationCollection(it.regions))
                }

                Snapshot.withMutableSnapshot {
                    bestSearchMatches.clear()
                    bestSearchMatches.addAll(newBestSearchMatches)
                    connectCountries.clear()
                    connectCountries.addAll(newConnectCountries)
                    devices.clear()
                    devices.addAll(newDevices)
                    cities.clear()
                    cities.addAll(newCities)
                    regions.clear()
                    regions.addAll(newRegions)
                }

                FilterLocationsState.fromString(state)?.let {
                    _filterLocationsState.value = it
                }
            }
        }
    }

    init {
        processLifecycle.addObserver(this)
        controllerOwner.setForeground(
            processLifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)
        )
        removeDeviceChangeListener = deviceManager.addDeviceChangeListener { device ->
            viewModelScope.launch {
                clearLocations()
                controllerOwner.setDevice(device)
            }
        }
    }

    override fun onStart(owner: LifecycleOwner) {
        controllerOwner.setForeground(true)
    }

    override fun onStop(owner: LifecycleOwner) {
        controllerOwner.setForeground(false)
    }

    private fun clearLocations() {
        _filterLocationsState.value = FilterLocationsState.Loading
        Snapshot.withMutableSnapshot {
            bestSearchMatches.clear()
            connectCountries.clear()
            devices.clear()
            cities.clear()
            regions.clear()
        }
    }

    private fun openLocationsViewController(device: DeviceLocal): LocationsViewController {
        val vc = device.openLocationsViewController()
        locationsVc = vc
        addFilteredLocationsListener(vc)
        vc.start()
        if (currentSearchQuery.isNotEmpty()) {
            vc.filterLocations(currentSearchQuery)
        }
        return vc
    }

    private fun closeLocationsViewController(
        device: DeviceLocal,
        vc: LocationsViewController,
    ) {
        filteredLocationsSub?.close()
        filteredLocationsSub = null
        vc.stop()
        device.closeViewController(vc)
        if (locationsVc === vc) {
            locationsVc = null
        }
    }

    override fun onCleared() {
        removeDeviceChangeListener?.invoke()
        removeDeviceChangeListener = null
        processLifecycle.removeObserver(this)
        controllerOwner.close()
        super.onCleared()
    }
}

enum class FilterLocationsState {
    Loading,
    Loaded,
    Error;

    companion object {
        fun fromString(value: String): FilterLocationsState? {
            return when (value.uppercase()) {
                "LOCATIONS_LOADING" -> Loading
                "LOCATIONS_LOADED" -> Loaded
                "LOCATIONS_ERROR" -> Error
                else -> null
            }
        }
    }
}

// one GET /network/provider-locations a minute while the home screen shows
const val LOCATIONS_REFRESH_INTERVAL_MILLIS = 60_000L
