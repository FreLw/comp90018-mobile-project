package com.comp90018.app.features.map.rendering.googlemap

import com.comp90018.app.features.map.MapRelic
import com.comp90018.app.features.map.TeamHuntFragment
import com.google.android.gms.maps.GoogleMap

/** User actions do not participate in the scene-change key. */
internal data class MapInteractionActions(
    val onRelicSelected: (MapRelic) -> Unit,
    val onFragmentSelected: (TeamHuntFragment) -> Unit,
    val onMapClick: () -> Unit,
)

/** Installs the existing native-map options and tag-based click dispatch. */
internal fun bindMapInteractions(
    map: GoogleMap,
    treasures: MapTreasureInputs,
    camera: MapCameraInputs,
    actions: MapInteractionActions,
    onCameraMovedByUser: () -> Unit,
) {
    val relics = treasures.relics
    val huntFragments = treasures.huntFragments
    val perspective = camera.perspective
    val allowCameraGestures = camera.allowCameraGestures
    val allowZoomGestures = camera.allowZoomGestures
    val allowRotateGestures = camera.allowRotateGestures
    val allowTiltGestures = camera.allowTiltGestures
    val onRelicSelected = actions.onRelicSelected
    val onFragmentSelected = actions.onFragmentSelected
    val onMapClick = actions.onMapClick
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
            onCameraMovedByUser()
        }
    }
    // Google Maps draws its blue location layer above overlapping treasure markers,
    // which made an arrived explorer unable to tap their active hunt target. Our
    // own marker is rendered below whenever we have a usable reading, so avoid the
    // native layer and keep the target marker tappable.
    map.isMyLocationEnabled = false
    map.uiSettings.isMyLocationButtonEnabled = false
}
