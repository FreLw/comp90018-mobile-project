package com.comp90018.app.features.map.rendering.googlemap

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.comp90018.app.R
import com.comp90018.app.features.map.DEFAULT_CAMPUS_CENTRE
import com.comp90018.app.features.map.GuidingThreadFrame
import com.comp90018.app.features.map.rendering.GuidingThreadOverlay
import com.comp90018.app.features.map.rendering.GuidingThreadRenderState
import com.comp90018.app.features.map.rendering.moveCameraToCampus
import com.comp90018.app.features.map.rendering.moveCameraToHuntView
import com.comp90018.app.features.map.rendering.moveCameraToRelic
import com.comp90018.app.features.map.rendering.moveCameraToTreasureArea
import com.comp90018.app.features.map.rendering.questMarkerIcon
import com.comp90018.app.features.map.rendering.renderCurrentLocation
import com.comp90018.app.features.map.rendering.renderHuntFragments
import com.comp90018.app.features.map.rendering.renderRelics
import com.comp90018.app.features.map.rendering.updateNavigationTrail
import com.comp90018.app.features.navigation.NavigationMapUpdateGate
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.android.gms.maps.model.Marker
import kotlin.math.abs
import kotlin.math.cos

/** Owns scene handles and camera history for the lifetime of one Compose map. */
internal class MapSceneController(initialPerspective: MapPerspective, initialRecenterRequestKey: Int) {
    private var renderedMapKey by mutableStateOf("__unrendered__")
    private var cameraInitialised by mutableStateOf(false)
    private var cameraSelectedRelicId by mutableStateOf<String?>(null)
    private var cameraRelicSignature by mutableStateOf("")
    private var currentLocationMarker by mutableStateOf<Marker?>(null)
    var renderedRelicMarkers by mutableStateOf<Map<String, Marker>>(emptyMap())
        private set
    private val teammates = TeammateMarkerRenderer()
    private var renderedPulseBucket by mutableIntStateOf(-1)
    private val navigationTrail = GuidingThreadRenderState()
    var navigationMap by mutableStateOf<GoogleMap?>(null)
        private set
    var navigationMapRevision by mutableIntStateOf(0)
        private set
    private var mapStyleConfigured by mutableStateOf(false)
    private var cameraPerspective by mutableStateOf(initialPerspective)
    private var cameraMovedByUser by mutableStateOf(false)
    private var handledRecenterRequestKey by mutableIntStateOf(initialRecenterRequestKey)
    private val navigationUpdates = NavigationMapUpdateGate()

    fun dispose() {
        navigationUpdates.dispose()
        navigationTrail.overlay.remove()
    }

    fun renderNavigationTrail(map: GoogleMap, frame: GuidingThreadFrame) {
        if (!navigationUpdates.disposed) {
            navigationTrail.overlay = updateNavigationTrail(
                map, frame.location, frame.target, frame.strength, frame.phase, navigationTrail.overlay,
            )
        }
    }

    /** Called from getMapAsync; flip progress is read here at the original callback time. */
    fun render(
        context: android.content.Context,
        map: GoogleMap,
        inputs: MapRenderInputs,
        actions: MapInteractionActions,
        sceneInputs: List<Any?>,
        readFlipProgress: () -> Float,
    ) {
        if (navigationUpdates.disposed) return
        val navigationOnlyUpdates = inputs.navigationOnlyUpdates
        if (navigationOnlyUpdates) navigationMap = map
        // Only standalone Navigation opts in; ordinary Map/Hunt keeps its update path.
        if (navigationOnlyUpdates && !navigationUpdates.shouldUpdateScene(sceneInputs)) return
        val relics = inputs.treasures.relics
        val selectedRelic = inputs.treasures.selectedRelic
        val markerPulse = inputs.treasures.markerPulse
        val discoveredTreasureIds = inputs.treasures.discoveredTreasureIds
        val activeHuntTreasureId = inputs.treasures.activeHuntTreasureId
        val flippingRelicId = inputs.treasures.flippingRelicId
        val huntFragments = inputs.treasures.huntFragments
        val foundFragmentIds = inputs.treasures.foundFragmentIds
        val locationOutput = inputs.location.output
        val deviceHeading = inputs.location.headingDegrees
        val focusSelectedRelic = inputs.camera.focusSelectedRelic
        val perspective = inputs.camera.perspective
        val recenterRequestKey = inputs.camera.recenterRequestKey
        val navigationTrailTarget = inputs.trail.target
        val navigationTrailStrength = inputs.trail.strength
        val navigationTrailPhase = inputs.trail.phase
        if (!mapStyleConfigured) {
            map.setMapStyle(
                MapStyleOptions.loadRawResourceStyle(context, R.raw.map_style_retro),
            )
            mapStyleConfigured = true
        }
        bindMapInteractions(map, inputs.treasures, inputs.camera, actions) { cameraMovedByUser = true }
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
            teammates.forgetClearedMarkers()
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
                    context, relic, discovered = readFlipProgress() >= 0.5f,
                    selected = relic.id == selectedRelic?.id,
                    active = relic.id == activeHuntTreasureId,
                    flipScale = abs(cos(Math.PI * readFlipProgress()).toFloat()).coerceAtLeast(0.025f),
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
        teammates.render(context, map, inputs.team)
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
}
