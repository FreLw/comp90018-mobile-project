package com.comp90018.app.features.map.rendering

import android.os.Bundle
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.comp90018.app.R
import com.comp90018.app.data.rooms.TeamHuntLocation
import com.comp90018.app.features.map.DEFAULT_CAMPUS_CENTRE
import com.comp90018.app.features.map.GuidingThreadFrame
import com.comp90018.app.features.map.MapRelic
import com.comp90018.app.features.map.TeamHuntFragment
import com.comp90018.app.features.navigation.NavigationMapUpdateGate
import com.comp90018.app.sensors.location.GeoCoordinate
import com.comp90018.app.sensors.location.LocationOutput
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import kotlin.math.abs
import kotlin.math.cos
import kotlinx.coroutines.flow.collect

internal enum class MapPerspective { GOD, HUNT }

/**
 * Bridges the Android MapView into Compose, owns map lifecycle, and updates scene/marker/thread
 * overlays.
 */
@Composable
internal fun GoogleMapView(
    relics: List<MapRelic>,
    selectedRelic: MapRelic?,
    locationOutput: LocationOutput,
    deviceHeading: Float,
    markerPulse: Float = 1f,
    discoveredTreasureIds: Set<String> = emptySet(),
    activeHuntTreasureId: String? = null,
    flippingRelicId: String? = null,
    onFlipFinished: () -> Unit = {},
    huntFragments: List<TeamHuntFragment> = emptyList(),
    teammateLocations: List<TeamHuntLocation> = emptyList(),
    huntMemberNames: Map<String, String> = emptyMap(),
    huntOwnerId: String? = null,
    foundFragmentIds: Set<String> = emptySet(),
    focusSelectedRelic: Boolean = true,
    perspective: MapPerspective = MapPerspective.GOD,
    allowCameraGestures: Boolean = perspective == MapPerspective.GOD,
    allowZoomGestures: Boolean = true,
    allowRotateGestures: Boolean = allowCameraGestures,
    allowTiltGestures: Boolean = true,
    recenterRequestKey: Int = 0,
    navigationOnlyUpdates: Boolean = false,
    navigationTrailTarget: GeoCoordinate? = null,
    navigationTrailStrength: Float = 0f,
    navigationTrailPhase: Float = 0f,
    navigationTrailPhaseState: State<Float>? = null,
    onRelicSelected: (MapRelic) -> Unit,
    onFragmentSelected: (TeamHuntFragment) -> Unit = {},
    onMapClick: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val mapView = remember {
        MapView(context).apply { onCreate(Bundle()) }
    }
    var renderedMapKey by remember { mutableStateOf("__unrendered__") }
    var cameraInitialised by remember { mutableStateOf(false) }
    var cameraSelectedRelicId by remember { mutableStateOf<String?>(null) }
    var cameraRelicSignature by remember { mutableStateOf("") }
    var currentLocationMarker by remember { mutableStateOf<Marker?>(null) }
    var renderedRelicMarkers by remember { mutableStateOf<Map<String, Marker>>(emptyMap()) }
    val teammateMarkers = remember { mutableMapOf<String, Marker>() }
    var renderedPulseBucket by remember { mutableIntStateOf(-1) }
    val navigationTrail = remember { GuidingThreadRenderState() }
    var navigationMap by remember { mutableStateOf<GoogleMap?>(null) }
    var navigationMapRevision by remember { mutableIntStateOf(0) }
    val threadInputs by rememberUpdatedState(GuidingThreadFrame(
        locationOutput.currentLocation ?: locationOutput.lastKnownLocation,
        navigationTrailTarget, navigationTrailStrength, navigationTrailPhase,
    ))
    var mapStyleConfigured by remember { mutableStateOf(false) }
    var cameraPerspective by remember { mutableStateOf(perspective) }
    var cameraMovedByUser by remember { mutableStateOf(false) }
    var handledRecenterRequestKey by remember { mutableIntStateOf(recenterRequestKey) }
    val flip = remember(flippingRelicId) { Animatable(if (flippingRelicId == null) 1f else 0f) }
    LaunchedEffect(flippingRelicId, renderedRelicMarkers) {
        if (flippingRelicId != null && renderedRelicMarkers.containsKey(flippingRelicId)) {
            flip.animateTo(1f, tween(1100, easing = FastOutSlowInEasing))
            onFlipFinished()
        }
    }
    val navigationUpdates = remember(mapView) { NavigationMapUpdateGate() }
    DisposableEffect(mapView) {
        onDispose {
            navigationUpdates.dispose()
            navigationTrail.overlay.remove()
        }
    }
    MapLifecycle(mapView)

    LaunchedEffect(navigationMap, navigationOnlyUpdates, navigationTrailPhaseState) {
        val map = navigationMap ?: return@LaunchedEffect
        if (!navigationOnlyUpdates) return@LaunchedEffect
        // Observe the animation State directly: no AndroidView update or getMapAsync per tick.
        snapshotFlow {
            threadInputs.copy(phase = navigationTrailPhaseState?.value ?: threadInputs.phase) to navigationMapRevision
        }.collect { (frame, _) ->
            if (!navigationUpdates.disposed) {
                navigationTrail.overlay = updateNavigationTrail(
                    map, frame.location, frame.target, frame.strength, frame.phase, navigationTrail.overlay,
                )
            }
        }
    }

    AndroidView(
        factory = { mapView },
        modifier = modifier,
        update = { view ->
            // Thread phase/strength/visibility do not require camera or marker updates.
            val sceneInputs = listOf(
                relics, selectedRelic, locationOutput, deviceHeading, markerPulse,
                discoveredTreasureIds, activeHuntTreasureId, flippingRelicId, flip.value,
                huntFragments, teammateLocations, huntMemberNames, huntOwnerId, foundFragmentIds,
                focusSelectedRelic, perspective, allowCameraGestures, allowZoomGestures,
                allowRotateGestures, allowTiltGestures, recenterRequestKey,
            )
            view.getMapAsync { map ->
                if (navigationUpdates.disposed) return@getMapAsync
                if (navigationOnlyUpdates) navigationMap = map
                // Only standalone Navigation opts in; ordinary Map/Hunt keeps its update path.
                if (navigationOnlyUpdates && !navigationUpdates.shouldUpdateScene(sceneInputs)) {
                    return@getMapAsync
                }
                if (!mapStyleConfigured) {
                    map.setMapStyle(
                        MapStyleOptions.loadRawResourceStyle(context, R.raw.map_style_retro),
                    )
                    mapStyleConfigured = true
                }
                map.uiSettings.isZoomControlsEnabled = false
                map.uiSettings.isCompassEnabled = perspective == MapPerspective.GOD
                map.uiSettings.isScrollGesturesEnabled = allowCameraGestures
                map.uiSettings.isRotateGesturesEnabled = allowRotateGestures
                map.uiSettings.isZoomGesturesEnabled = allowZoomGestures
                map.uiSettings.isTiltGesturesEnabled = allowTiltGestures
                map.isBuildingsEnabled = true
                map.setOnMarkerClickListener { marker ->
                    relics.firstOrNull { it.id == marker.tag }?.let {
                        onRelicSelected(it)
                        true
                    } ?: huntFragments.firstOrNull { "fragment:${it.id}" == marker.tag }?.let {
                        onFragmentSelected(it)
                        true
                    } ?: false
                }
                map.setOnMapClickListener { onMapClick() }
                map.setOnCameraMoveStartedListener { reason ->
                    if (allowCameraGestures && reason == GoogleMap.OnCameraMoveStartedListener.REASON_GESTURE) {
                        cameraMovedByUser = true
                    }
                }
                // Google Maps draws its blue location layer above overlapping treasure markers,
                // which made an arrived explorer unable to tap their active hunt target. Our
                // own marker is rendered below whenever we have a usable reading, so avoid the
                // native layer and keep the target marker tappable.
                map.isMyLocationEnabled = false
                map.uiSettings.isMyLocationButtonEnabled = false
                val selectedRelicId = selectedRelic?.id ?: "__none__"
                val relicSignature = relics.joinToString("|") { relic ->
                    "${relic.id}:${relic.coordinate.latitude}:${relic.coordinate.longitude}:${relic.artworkKey}:${relic.id in discoveredTreasureIds}"
                }
                val fragmentSignature = huntFragments.joinToString("|") { fragment ->
                    "${fragment.id}:${fragment.coordinate.latitude}:${fragment.coordinate.longitude}:${fragment.id in foundFragmentIds}"
                }
                val trailTargetKey = if (navigationOnlyUpdates) "" else
                    navigationTrailTarget?.let { "${it.latitude}:${it.longitude}" }.orEmpty()
                val mapKey = "$selectedRelicId|$activeHuntTreasureId|$relicSignature|$fragmentSignature|$trailTargetKey"
                if (renderedMapKey != mapKey) {
                    map.clear()
                    teammateMarkers.clear()
                    currentLocationMarker = null
                    navigationTrail.overlay = GuidingThreadOverlay()
                    if (navigationOnlyUpdates) navigationMapRevision += 1
                    renderedRelicMarkers = renderRelics(context, map, relics, selectedRelic, activeHuntTreasureId, markerPulse, discoveredTreasureIds)
                    renderHuntFragments(context, map, huntFragments, foundFragmentIds)
                    renderedMapKey = mapKey
                }
                val pulseBucket = (markerPulse * 20).toInt()
                if (huntFragments.isEmpty() && renderedPulseBucket != pulseBucket) {
                    relics.forEach { relic ->
                        renderedRelicMarkers[relic.id]?.setIcon(questMarkerIcon(
                            context, relic, relic.id in discoveredTreasureIds,
                            selected = relic.id == selectedRelic?.id,
                            active = relic.id == activeHuntTreasureId,
                            pulseScale = markerPulse,
                        ))
                    }
                    renderedPulseBucket = pulseBucket
                }
                flippingRelicId?.let { id ->
                    relics.firstOrNull { it.id == id }?.let { relic ->
                        renderedRelicMarkers[id]?.setIcon(questMarkerIcon(
                            context, relic, discovered = flip.value >= 0.5f,
                            selected = relic.id == selectedRelic?.id,
                            active = relic.id == activeHuntTreasureId,
                            flipScale = abs(cos(Math.PI * flip.value).toFloat()).coerceAtLeast(0.025f),
                        ))
                    }
                }
                val displayLocation = locationOutput.currentLocation ?: locationOutput.lastKnownLocation
                val isStaleDisplay = locationOutput.currentLocation == null && locationOutput.lastKnownLocation != null
                currentLocationMarker = renderCurrentLocation(
                    context = context,
                    map = map,
                    currentLocationMarker = currentLocationMarker,
                    currentLocation = displayLocation,
                    headingDegrees = deviceHeading,
                    stale = isStaleDisplay,
                )
                if (!navigationOnlyUpdates) navigationTrail.overlay = updateNavigationTrail(
                    map = map,
                    currentLocation = displayLocation,
                    target = navigationTrailTarget,
                    strength = navigationTrailStrength,
                    phase = navigationTrailPhase,
                    previous = navigationTrail.overlay,
                )
                val visibleTeammateIds = teammateLocations.map { it.uid }.toSet()
                teammateMarkers.keys.filterNot { it in visibleTeammateIds }.forEach { uid ->
                    teammateMarkers.remove(uid)?.remove()
                }
                teammateLocations.forEach { teammate ->
                    val position = LatLng(teammate.coordinate.latitude, teammate.coordinate.longitude)
                    val isHost = teammate.uid == huntOwnerId
                    val name = huntMemberNames[teammate.uid]?.takeIf { it.isNotBlank() } ?: "Teammate"
                    val title = "$name (${if (isHost) "Host" else "Teammate"})"
                    val marker = teammateMarkers[teammate.uid] ?: map.addMarker(
                        MarkerOptions().position(position).title(title).snippet("Online · Shared hunt location")
                            .icon(if (isHost) {
                                BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE)
                            } else {
                                currentLocationIcon(
                                    context,
                                    dotColor = android.graphics.Color.rgb(52, 152, 219),
                                    showHeading = false,
                                )
                            })
                            .anchor(0.5f, if (isHost) 1f else 0.72f)
                            .zIndex(CURRENT_LOCATION_Z_INDEX + 1f),
                    )?.also { teammateMarkers[teammate.uid] = it; it.tag = "teammate:${teammate.uid}" }
                    marker?.position = position
                    marker?.title = title
                }
                if (cameraPerspective != perspective) {
                    cameraPerspective = perspective
                    cameraInitialised = false
                    cameraMovedByUser = false
                }
                if (handledRecenterRequestKey != recenterRequestKey) {
                    handledRecenterRequestKey = recenterRequestKey
                    cameraMovedByUser = false
                }
                if (perspective == MapPerspective.HUNT && displayLocation != null && !cameraMovedByUser) {
                    moveCameraToHuntView(map, displayLocation, deviceHeading)
                    cameraInitialised = true
                } else if (!cameraInitialised) {
                    if (focusSelectedRelic && selectedRelic != null) {
                        moveCameraToRelic(map, selectedRelic)
                    } else {
                        if (huntFragments.isNotEmpty()) {
                            moveCameraToCampus(map, huntFragments.map { it.coordinate }, displayLocation)
                        } else if (relics.isNotEmpty()) {
                            // Frame the treasure catalogue even if the user's GPS is in another city.
                            moveCameraToTreasureArea(map, relics, activeHuntTreasureId)
                        } else {
                            moveCameraToCampus(map, relics.map { it.coordinate }, displayLocation ?: DEFAULT_CAMPUS_CENTRE)
                        }
                    }
                    cameraInitialised = true
                    cameraSelectedRelicId = if (focusSelectedRelic) selectedRelic?.id else null
                    cameraRelicSignature = relicSignature
                } else if (!focusSelectedRelic && relics.isNotEmpty() && cameraRelicSignature != relicSignature) {
                    moveCameraToTreasureArea(map, relics, activeHuntTreasureId)
                    cameraRelicSignature = relicSignature
                } else if (
                    focusSelectedRelic &&
                    selectedRelic != null &&
                    (cameraSelectedRelicId != selectedRelic.id || cameraRelicSignature != relicSignature)
                ) {
                    moveCameraToRelic(map, selectedRelic)
                    cameraSelectedRelicId = selectedRelic.id
                    cameraRelicSignature = relicSignature
                }
            }
        },
    )
}

@Composable
private fun MapLifecycle(mapView: MapView) {
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lifecycle, mapView) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> mapView.onStart()
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                Lifecycle.Event.ON_STOP -> mapView.onStop()
                Lifecycle.Event.ON_DESTROY -> mapView.onDestroy()
                else -> Unit
            }
        }
        lifecycle.addObserver(observer)
        onDispose {
            lifecycle.removeObserver(observer)
            mapView.onDestroy()
        }
    }
}
