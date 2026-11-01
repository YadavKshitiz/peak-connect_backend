package com.peakconnect.app.feature.guide.mappicker

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.peakconnect.app.R
import com.peakconnect.app.core.theme.AppShapes
import com.peakconnect.app.core.theme.spacing
import com.peakconnect.app.core.ui.AppTopBar
import com.peakconnect.app.core.ui.map.OsmMapView
import com.peakconnect.app.core.util.LocalIsOnline
import org.osmdroid.util.GeoPoint
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapPickerScreen(
    onLocationConfirmed: (Double, Double) -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: MapPickerViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val isOnline = LocalIsOnline.current
    val context = LocalContext.current

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            obtainCurrentLocation(context) { loc ->
                if (loc != null) {
                    viewModel.onMyLocationObtained(loc.latitude, loc.longitude)
                } else {
                    Toast.makeText(context, "Could not determine location", Toast.LENGTH_SHORT).show()
                }
            }
        } else {
            Toast.makeText(context, context.getString(R.string.map_picker_location_permission_rationale), Toast.LENGTH_SHORT).show()
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            AppTopBar(
                title = stringResource(id = R.string.map_picker_title),
                onBackClick = onCancel
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            val centerGeo = remember(uiState.currentLat, uiState.currentLng) {
                GeoPoint(uiState.currentLat, uiState.currentLng)
            }
            val markerList = remember(uiState.hasPin, uiState.currentLat, uiState.currentLng) {
                if (uiState.hasPin) listOf(GeoPoint(uiState.currentLat, uiState.currentLng)) else emptyList()
            }

            // Interactive OSM map view
            OsmMapView(
                center = centerGeo,
                zoom = uiState.zoom,
                markerPositions = markerList,
                interactive = true,
                onMapTap = { geo ->
                    viewModel.onMapTapped(geo.latitude, geo.longitude)
                },
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("osm_map_picker_view")
            )

            // Search Bar & Dropdown Results at the Top
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(MaterialTheme.spacing.md)
                    .align(Alignment.TopCenter)
            ) {
                OutlinedTextField(
                    value = uiState.searchQuery,
                    onValueChange = { viewModel.onSearchQueryChanged(it) },
                    placeholder = { Text(text = stringResource(id = R.string.map_picker_search_hint)) },
                    leadingIcon = {
                        Icon(imageVector = Icons.Default.Search, contentDescription = null)
                    },
                    trailingIcon = {
                        if (uiState.isSearching) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp
                            )
                        } else if (uiState.searchQuery.isNotBlank()) {
                            IconButton(onClick = { viewModel.clearSearch() }) {
                                Icon(imageVector = Icons.Default.Close, contentDescription = stringResource(id = R.string.common_clear_text))
                            }
                        }
                    },
                    enabled = isOnline,
                    singleLine = true,
                    shape = AppShapes.small,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                        disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("map_search_field")
                )

                // Search Results Dropdown
                if (uiState.searchResults.isNotEmpty()) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = MaterialTheme.spacing.xs),
                        shape = AppShapes.small,
                        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
                    ) {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height((uiState.searchResults.size * 56).coerceAtMost(220).dp)
                        ) {
                            items(uiState.searchResults, key = { it.placeId }) { place ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { viewModel.onPlaceSelected(place) }
                                        .padding(MaterialTheme.spacing.md),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = place.displayName,
                                        style = MaterialTheme.typography.bodyMedium,
                                        maxLines = 2
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // "Use My Location" FAB
            FloatingActionButton(
                onClick = {
                    val coarseGranted = ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    ) == PackageManager.PERMISSION_GRANTED

                    if (coarseGranted) {
                        obtainCurrentLocation(context) { loc ->
                            if (loc != null) {
                                viewModel.onMyLocationObtained(loc.latitude, loc.longitude)
                            } else {
                                Toast.makeText(context, "Could not determine location", Toast.LENGTH_SHORT).show()
                            }
                        }
                    } else {
                        permissionLauncher.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
                    }
                },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(bottom = 140.dp, end = MaterialTheme.spacing.md)
                    .testTag("use_my_location_btn"),
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(
                    imageVector = Icons.Default.MyLocation,
                    contentDescription = stringResource(id = R.string.map_picker_action_use_my_location)
                )
            }

            // Bottom Confirmation Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(MaterialTheme.spacing.md)
                    .align(Alignment.BottomCenter)
                    .testTag("map_picker_bottom_card"),
                shape = AppShapes.medium,
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(MaterialTheme.spacing.md),
                    verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.sm)
                ) {
                    val coordsText = stringResource(
                        id = R.string.map_picker_coords_format,
                        uiState.currentLat,
                        uiState.currentLng
                    )

                    Text(
                        text = coordsText,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.testTag("map_coordinates_text")
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.sm)
                    ) {
                        OutlinedButton(
                            onClick = onCancel,
                            modifier = Modifier.weight(1f),
                            shape = AppShapes.small
                        ) {
                            Text(text = stringResource(id = R.string.map_picker_action_cancel))
                        }

                        Button(
                            onClick = {
                                onLocationConfirmed(uiState.currentLat, uiState.currentLng)
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("confirm_location_button"),
                            shape = AppShapes.small,
                            enabled = uiState.hasPin
                        ) {
                            Text(text = stringResource(id = R.string.map_picker_action_confirm))
                        }
                    }
                }
            }
        }
    }
}

private fun obtainCurrentLocation(context: Context, onLocation: (Location?) -> Unit) {
    val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
    if (locationManager == null) {
        onLocation(null)
        return
    }

    try {
        val providers = locationManager.getProviders(true)
        var bestLocation: Location? = null
        for (provider in providers) {
            val loc = locationManager.getLastKnownLocation(provider) ?: continue
            if (bestLocation == null || loc.accuracy < bestLocation.accuracy) {
                bestLocation = loc
            }
        }
        onLocation(bestLocation)
    } catch (e: SecurityException) {
        onLocation(null)
    }
}
