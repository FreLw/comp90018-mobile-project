package com.comp90018.app.features.map.rendering.googlemap

import com.comp90018.app.data.rooms.TeamHuntLocation
import com.comp90018.app.features.map.MapRelic
import com.comp90018.app.features.map.TeamHuntFragment
import com.comp90018.app.features.navigation.NavigationMapUpdateGate
import com.comp90018.app.sensors.location.GeoCoordinate
import com.comp90018.app.sensors.location.LocationOutput
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Covers which independent inputs should redraw a navigation scene. */
class MapRenderInputsTest {
    private val coordinate = GeoCoordinate(-37.7986, 144.9602)
    private val relic = MapRelic("relic", "Relic", "Campus", coordinate = coordinate)
    private val inputs = MapRenderInputs(
        MapTreasureInputs(listOf(relic), null, 1f, emptySet(), null, null, emptyList(), emptySet()),
        MapLocationInputs(LocationOutput(), 0f),
        MapTeamInputs(emptyList(), emptyMap(), null),
        MapCameraInputs(true, MapPerspective.GOD, true, true, true, true, 0),
        MapTrailInputs(null, 0f, 0f),
        navigationOnlyUpdates = true,
    )

    @Test fun identicalSceneInputsDoNotRedrawTwice() {
        val gate = NavigationMapUpdateGate()
        assertTrue(gate.shouldUpdateScene(inputs.sceneChangeKey(1f)))
        assertFalse(gate.shouldUpdateScene(inputs.copy().sceneChangeKey(1f)))
    }

    @Test fun navigationThreadFramesDoNotInvalidateTheMarkerAndCameraScene() {
        val gate = NavigationMapUpdateGate()
        assertTrue(gate.shouldUpdateScene(inputs.sceneChangeKey(1f)))
        assertFalse(gate.shouldUpdateScene(inputs.copy(trail = MapTrailInputs(coordinate, 0.8f, 0.7f)).sceneChangeKey(1f)))
    }

    @Test fun gpsAndHeadingUpdatesInvalidateTheScene() {
        changesRedraw(listOf(
            inputs.copy(location = inputs.location.copy(output = LocationOutput(currentLocation = coordinate))),
            inputs.copy(location = inputs.location.copy(headingDegrees = 90f)),
        ))
    }

    @Test fun catalogueSelectionDiscoveryAndMarkerPresentationInvalidateTheScene() {
        changesRedraw(listOf(
            inputs.copy(treasures = inputs.treasures.copy(relics = listOf(relic.copy(name = "Updated")))),
            inputs.copy(treasures = inputs.treasures.copy(selectedRelic = relic)),
            inputs.copy(treasures = inputs.treasures.copy(discoveredTreasureIds = setOf(relic.id))),
            inputs.copy(treasures = inputs.treasures.copy(activeHuntTreasureId = relic.id)),
            inputs.copy(treasures = inputs.treasures.copy(markerPulse = 1.01f)),
            inputs.copy(treasures = inputs.treasures.copy(flippingRelicId = relic.id)),
        ))
    }

    @Test fun markerFlipFramesInvalidateTheScene() {
        val gate = NavigationMapUpdateGate()
        assertTrue(gate.shouldUpdateScene(inputs.sceneChangeKey(0f)))
        assertTrue(gate.shouldUpdateScene(inputs.sceneChangeKey(0.5f)))
    }

    @Test fun fragmentsAndFoundStateInvalidateTheScene() {
        changesRedraw(listOf(
            inputs.copy(treasures = inputs.treasures.copy(huntFragments = listOf(TeamHuntFragment("fragment", "Fragment", coordinate)))),
            inputs.copy(treasures = inputs.treasures.copy(foundFragmentIds = setOf("fragment"))),
        ))
    }

    @Test fun teammateLocationsNamesAndOwnerInvalidateTheScene() {
        changesRedraw(listOf(
            inputs.copy(team = inputs.team.copy(locations = listOf(TeamHuntLocation("member", "session", coordinate, 0L, 0L)))),
            inputs.copy(team = inputs.team.copy(memberNames = mapOf("member" to "Explorer"))),
            inputs.copy(team = inputs.team.copy(ownerId = "member")),
        ))
    }

    @Test fun everyCameraControlAndRecenterRequestInvalidatesTheScene() {
        changesRedraw(listOf(
            inputs.copy(camera = inputs.camera.copy(focusSelectedRelic = false)),
            inputs.copy(camera = inputs.camera.copy(perspective = MapPerspective.HUNT)),
            inputs.copy(camera = inputs.camera.copy(allowCameraGestures = false)),
            inputs.copy(camera = inputs.camera.copy(allowZoomGestures = false)),
            inputs.copy(camera = inputs.camera.copy(allowRotateGestures = false)),
            inputs.copy(camera = inputs.camera.copy(allowTiltGestures = false)),
            inputs.copy(camera = inputs.camera.copy(recenterRequestKey = 1)),
        ))
    }

    private fun changesRedraw(changes: List<MapRenderInputs>) {
        for (changed in changes) {
            val gate = NavigationMapUpdateGate()
            assertTrue(gate.shouldUpdateScene(inputs.sceneChangeKey(1f)))
            assertTrue("Changed input must redraw: $changed", gate.shouldUpdateScene(changed.sceneChangeKey(1f)))
        }
    }
}
