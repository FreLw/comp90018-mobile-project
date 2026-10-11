package com.comp90018.app.features.map.rendering.googlemap

import com.comp90018.app.data.rooms.TeamHuntLocation
import com.comp90018.app.features.map.MapRelic
import com.comp90018.app.features.map.TeamHuntFragment
import com.comp90018.app.sensors.location.GeoCoordinate
import com.comp90018.app.sensors.location.LocationOutput

internal enum class MapPerspective { GOD, HUNT }

/** Treasure content and marker presentation for one map update. */
internal data class MapTreasureInputs(
    val relics: List<MapRelic>,
    val selectedRelic: MapRelic?,
    val markerPulse: Float,
    val discoveredTreasureIds: Set<String>,
    val activeHuntTreasureId: String?,
    val flippingRelicId: String?,
    val huntFragments: List<TeamHuntFragment>,
    val foundFragmentIds: Set<String>,
)

internal data class MapLocationInputs(val output: LocationOutput, val headingDegrees: Float)

internal data class MapTeamInputs(
    val locations: List<TeamHuntLocation>,
    val memberNames: Map<String, String>,
    val ownerId: String?,
)

internal data class MapCameraInputs(
    val focusSelectedRelic: Boolean,
    val perspective: MapPerspective,
    val allowCameraGestures: Boolean,
    val allowZoomGestures: Boolean,
    val allowRotateGestures: Boolean,
    val allowTiltGestures: Boolean,
    val recenterRequestKey: Int,
)

internal data class MapTrailInputs(val target: GeoCoordinate?, val strength: Float, val phase: Float)

/** Groups inputs without changing the Compose entry point or scene-change policy. */
internal data class MapRenderInputs(
    val treasures: MapTreasureInputs,
    val location: MapLocationInputs,
    val team: MapTeamInputs,
    val camera: MapCameraInputs,
    val trail: MapTrailInputs,
    val navigationOnlyUpdates: Boolean,
) {
    // Preserve the original ordered key; navigation-thread frames have a separate update path.
    fun sceneChangeKey(flipProgress: Float): List<Any?> = listOf(
        treasures.relics, treasures.selectedRelic, location.output, location.headingDegrees, treasures.markerPulse,
        treasures.discoveredTreasureIds, treasures.activeHuntTreasureId, treasures.flippingRelicId, flipProgress,
        treasures.huntFragments, team.locations, team.memberNames, team.ownerId, treasures.foundFragmentIds,
        camera.focusSelectedRelic, camera.perspective, camera.allowCameraGestures, camera.allowZoomGestures,
        camera.allowRotateGestures, camera.allowTiltGestures, camera.recenterRequestKey,
    )
}
