package com.bringyour.network.ui.connect

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.platform.LocalInputModeManager
import androidx.compose.ui.platform.SoftwareKeyboardController
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.bringyour.sdk.ConnectLocation
import com.bringyour.network.R
import com.bringyour.network.ui.components.URSearchInput
import com.bringyour.network.ui.components.bouncyClick
import com.bringyour.network.ui.components.tabletReadableColumn

import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.bringyour.network.ui.theme.AppTheme
import com.bringyour.network.ui.theme.Pink

@Composable
fun BrowseLocations(
    selectedLocation: ConnectLocation?,
    connectCountries: List<ConnectLocation>,
    cities: List<ConnectLocation>,
    regions: List<ConnectLocation>,
    devices: List<ConnectLocation>,
    bestSearchMatches: List<ConnectLocation>,
    getLocationColor: (String) -> Color,
    filterLocations: (String) -> Unit,
    connect: (ConnectLocation?) -> Unit,
    fetchLocationsState: FilterLocationsState,
    searchQueryTextFieldValue: TextFieldValue,
    setSearchQueryTextFieldValue: (TextFieldValue) -> Unit,
    currentSearchQuery: String,
    keyboardController: SoftwareKeyboardController?,
    lazyListState: LazyListState,
    refreshLocations: () -> Unit,
    networkPeers: List<com.bringyour.network.ui.stats.NetworkPeerUi> = listOf(),
    onConnectPeer: (com.bringyour.network.ui.stats.NetworkPeerUi) -> Unit = {},
    // a connection requested, and Disconnect, for the context menu of the row
    // it is on (LocationsList)
    connectRequested: Boolean = false,
    disconnect: () -> Unit = {},
) {

    val scope = rememberCoroutineScope()
    var debounceJob by remember { mutableStateOf<Job?>(null) }

    // opened from the keyboard, typing goes straight to the search, and Down
    // from there reaches the locations (Keyboard_Navigation); a tap opens it
    // without raising the on-screen keyboard, as before
    val searchFocus = remember { FocusRequester() }
    val inputModeManager = LocalInputModeManager.current
    LaunchedEffect(Unit) {
        if (inputModeManager.inputMode == InputMode.Keyboard) {
            runCatching { searchFocus.requestFocus() }
        }
    }

    Column(
        Modifier
            // a wide window centers the search and the list in the readable column
            .tabletReadableColumn()
            .fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Row (
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            URSearchInput(
                value = searchQueryTextFieldValue,
                onValueChange = { query ->
                    if (query.text != searchQueryTextFieldValue.text) {
                        setSearchQueryTextFieldValue(query)

                        debounceJob?.cancel()
                        debounceJob = scope.launch {
                            delay(250L)
                            filterLocations(query.text)
                        }
                    }
                },
                onSearch = {
                    filterLocations(searchQueryTextFieldValue.text)
                    keyboardController?.hide()
                },
                placeholder = stringResource(id = R.string.search_placeholder),
                keyboardController = keyboardController,
                onClear = {
                    setSearchQueryTextFieldValue(TextFieldValue(""))
                    filterLocations("")
                    keyboardController?.hide()
                },
                focusRequester = searchFocus,
            )
        }

        // how many locations the network carries right now -- every country,
        // region and city of the list, no searching needed to see them -- and
        // the button that asks the network for them again
        val locationCount = connectCountries.size + regions.size + cities.size
        if (locationCount > 0) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    pluralStringResource(
                        id = R.plurals.locations_count,
                        count = locationCount,
                        locationCount,
                    ),
                    style = MaterialTheme.typography.labelMedium,
                    color = AppTheme.textMuted,
                    modifier = Modifier.weight(1f),
                )

                LocationsRefreshButton(
                    isRefreshing = fetchLocationsState == FilterLocationsState.Loading,
                    onRefresh = { refreshLocations() },
                )
            }
        }

        // the search field keeps 6 dp of its own below
        Spacer(modifier = Modifier.height(12.dp))

        if (fetchLocationsState == FilterLocationsState.Error) {
            Column(
                modifier = Modifier.padding(horizontal = 16.dp)
            ) {
                FetchLocationsError(
                    onRefresh = {
                        filterLocations(currentSearchQuery)
                    }
                )
            }
        } else {
            LocationsList(
                onLocationSelect = { location ->
                    connect(location)
                },
                connectCountries = connectCountries,
                cities = cities,
                regions = regions,
                bestSearchMatches = bestSearchMatches,
                getLocationColor = getLocationColor,
                selectedLocation = selectedLocation,
                devices = devices,
                onRefresh = {
                    refreshLocations()
                },
                searchQuery = currentSearchQuery,
                listState = lazyListState,
                isRefreshing = fetchLocationsState == FilterLocationsState.Loading,
                networkPeers = networkPeers,
                onConnectPeer = onConnectPeer,
                connectRequested = connectRequested,
                onDisconnect = disconnect,
            )
        }
    }
}

/**
 * The button that asks the network for its locations again: a quiet refresh
 * glyph that spins while a fetch is away, tinted to the app's accent, and
 * lands with the app's bounce on a tap.
 */
@Composable
private fun LocationsRefreshButton(
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
) {
    val spin = rememberInfiniteTransition(label = "locationsRefreshSpin")
    val angle by spin.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900, easing = LinearEasing),
        ),
        label = "locationsRefreshAngle",
    )

    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .bouncyClick { onRefresh() },
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.Filled.Refresh,
            contentDescription = stringResource(id = R.string.refresh_servers),
            tint = if (isRefreshing) Pink else AppTheme.textMuted,
            modifier = Modifier
                .size(22.dp)
                .graphicsLayer {
                    rotationZ = if (isRefreshing) angle else 0f
                },
        )
    }
}

/**
 * The location list's scroll position, kept above the screen and the dialog
 * that show the list, so a fold or a resize that turns one into the other
 * carries the position across (apple/DUO.md). Saved with the screen.
 */
@Stable
class LocationListPosition(index: Int = 0, offset: Int = 0) {
    var index by mutableIntStateOf(index)
    var offset by mutableIntStateOf(offset)

    companion object {
        val Saver: Saver<LocationListPosition, List<Int>> = Saver(
            save = { listOf(it.index, it.offset) },
            restore = { LocationListPosition(it.getOrElse(0) { 0 }, it.getOrElse(1) { 0 }) },
        )
    }
}

@Composable
fun rememberLocationListPosition(): LocationListPosition =
    rememberSaveable(saver = LocationListPosition.Saver) { LocationListPosition() }

/** A list state that starts at [position] and keeps it up to date as the list scrolls. */
@Composable
fun rememberLocationListState(position: LocationListPosition): LazyListState {
    val state = rememberLazyListState(position.index, position.offset)
    LaunchedEffect(state, position) {
        snapshotFlow { state.firstVisibleItemIndex to state.firstVisibleItemScrollOffset }
            .collect { (index, offset) ->
                position.index = index
                position.offset = offset
            }
    }
    return state
}
