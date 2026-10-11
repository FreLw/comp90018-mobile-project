package com.comp90018.app.features.map.rendering.googlemap

import android.os.Bundle
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.comp90018.app.data.rooms.TeamHuntLocation
import com.comp90018.app.features.map.GuidingThreadFrame
import com.comp90018.app.features.map.MapRelic
import com.comp90018.app.features.map.TeamHuntFragment
import com.comp90018.app.sensors.location.GeoCoordinate
import com.comp90018.app.sensors.location.LocationOutput
import com.google.android.gms.maps.MapView
import kotlinx.coroutines.flow.collect

/**
 * Compose entry: creates one native map, groups its inputs, and connects effects to the controller.
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
    val sceneController = remember { MapSceneController(perspective, recenterRequestKey) }
    val threadInputs by rememberUpdatedState(GuidingThreadFrame(
        locationOutput.currentLocation ?: locationOutput.lastKnownLocation,
        navigationTrailTarget, navigationTrailStrength, navigationTrailPhase,
    ))
    val flip = remember(flippingRelicId) { Animatable(if (flippingRelicId == null) 1f else 0f) }
    LaunchedEffect(flippingRelicId, sceneController.renderedRelicMarkers) {
        if (flippingRelicId != null && sceneController.renderedRelicMarkers.containsKey(flippingRelicId)) {
            flip.animateTo(1f, tween(1100, easing = FastOutSlowInEasing))
            onFlipFinished()
        }
    }
    DisposableEffect(mapView) { onDispose { sceneController.dispose() } }
    MapViewLifecycle(mapView)

    LaunchedEffect(sceneController.navigationMap, navigationOnlyUpdates, navigationTrailPhaseState) {
        val map = sceneController.navigationMap ?: return@LaunchedEffect
        if (!navigationOnlyUpdates) return@LaunchedEffect
        // Observe the animation State directly: no AndroidView update or getMapAsync per tick.
        snapshotFlow {
            threadInputs.copy(phase = navigationTrailPhaseState?.value ?: threadInputs.phase) to sceneController.navigationMapRevision
        }.collect { (frame, _) ->
            sceneController.renderNavigationTrail(map, frame)
        }
    }
    val inputs = MapRenderInputs(
        treasures = MapTreasureInputs(relics, selectedRelic, markerPulse, discoveredTreasureIds,
            activeHuntTreasureId, flippingRelicId, huntFragments, foundFragmentIds),
        location = MapLocationInputs(locationOutput, deviceHeading),
        team = MapTeamInputs(teammateLocations, huntMemberNames, huntOwnerId),
        camera = MapCameraInputs(focusSelectedRelic, perspective, allowCameraGestures, allowZoomGestures,
            allowRotateGestures, allowTiltGestures, recenterRequestKey),
        trail = MapTrailInputs(navigationTrailTarget, navigationTrailStrength, navigationTrailPhase),
        navigationOnlyUpdates = navigationOnlyUpdates,
    )
    val actions = MapInteractionActions(onRelicSelected, onFragmentSelected, onMapClick)
    AndroidView(
        factory = { mapView },
        modifier = modifier,
        update = { view ->
            // Capture the scene key before getMapAsync, as before; read flip progress in its callback.
            val sceneInputs = inputs.sceneChangeKey(flip.value)
            view.getMapAsync { map ->
                sceneController.render(context, map, inputs, actions, sceneInputs) { flip.value }
            }
        },
    )
}
