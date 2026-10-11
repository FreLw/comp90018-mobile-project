package com.comp90018.app.features.map.rendering

import android.graphics.Bitmap
import android.os.SystemClock
import android.view.View
import android.view.ViewGroup
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipe
import androidx.test.platform.app.InstrumentationRegistry
import com.comp90018.app.data.rooms.TeamHuntLocation
import com.comp90018.app.features.map.MapRelic
import com.comp90018.app.features.map.TeamHuntFragment
import com.comp90018.app.features.map.rendering.googlemap.GoogleMapView
import com.comp90018.app.features.map.rendering.googlemap.MapPerspective
import com.comp90018.app.sensors.location.GeoCoordinate
import com.comp90018.app.sensors.location.LocationCalculator
import com.comp90018.app.sensors.location.LocationOutput
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.model.LatLng
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

/** Runs against the real Maps SDK, including AndroidView touch dispatch and camera state. */
class GoogleMapViewUiTest {
    @get:Rule val rule = createAndroidComposeRule<ComponentActivity>()
    private val coordinate = GeoCoordinate(-37.7986, 144.9602)
    private val relic = MapRelic("map_test_relic", "Map test relic", "Campus", coordinate = coordinate)
    private val modifier = Modifier.fillMaxSize().testTag("map_surface")

    @Test fun godViewKeepsTheExistingControlsAndDispatchesMarkerAndMapClicks() {
        val selected = AtomicReference<String>()
        val mapClicks = AtomicInteger()
        rule.setContent {
            GoogleMapView(listOf(relic), null, LocationOutput(), 0f,
                onRelicSelected = { selected.set(it.id) }, onMapClick = { mapClicks.incrementAndGet() },
                modifier = modifier)
        }
        val map = awaitMap()
        waitOnMap(map) { it.cameraPosition.zoom == 17f }
        rule.runOnUiThread {
            assertTrue(map.uiSettings.isCompassEnabled)
            assertTrue(map.uiSettings.isScrollGesturesEnabled)
            assertTrue(map.uiSettings.isRotateGesturesEnabled)
            assertTrue(map.uiSettings.isZoomGesturesEnabled)
            assertTrue(map.uiSettings.isTiltGesturesEnabled)
            assertFalse(map.uiSettings.isZoomControlsEnabled)
            assertFalse(map.uiSettings.isMyLocationButtonEnabled)
            assertFalse(map.isMyLocationEnabled)
            assertTrue(map.isBuildingsEnabled)
        }
        screenshot(map, "god_view")
        tapCoordinate(map, coordinate)
        rule.waitUntil(5_000) { selected.get() == relic.id }
        rule.onNodeWithTag("map_surface").performTouchInput { click(Offset(25f, 25f)) }
        rule.waitUntil(5_000) { mapClicks.get() == 1 }
    }

    @Test fun huntViewUsesLastKnownLocationAndRecentersAfterAManualPan() {
        var location by mutableStateOf(LocationOutput(lastKnownLocation = coordinate))
        var heading by mutableStateOf(45f)
        var recenter by mutableStateOf(0)
        rule.setContent {
            GoogleMapView(emptyList(), null, location, heading,
                perspective = MapPerspective.HUNT, allowCameraGestures = true,
                allowZoomGestures = false, allowRotateGestures = false, allowTiltGestures = false,
                recenterRequestKey = recenter, onRelicSelected = {}, modifier = modifier)
        }
        val map = awaitMap()
        waitOnMap(map) { it.cameraPosition.bearing == 45f && it.cameraPosition.zoom == 18.5f }
        rule.runOnUiThread {
            assertFalse(map.uiSettings.isCompassEnabled)
            assertTrue(map.uiSettings.isScrollGesturesEnabled)
            assertFalse(map.uiSettings.isZoomGesturesEnabled)
            assertFalse(map.uiSettings.isRotateGesturesEnabled)
            assertFalse(map.uiSettings.isTiltGesturesEnabled)
            assertEquals(45f, map.cameraPosition.tilt, 0f)
            val target = map.cameraPosition.target
            assertEquals(80.0, LocationCalculator.distanceMeters(coordinate, GeoCoordinate(target.latitude, target.longitude)), 0.5)
        }
        screenshot(map, "hunt_last_known_location")
        rule.onNodeWithTag("map_surface").performTouchInput {
            swipe(center, center + Offset(180f, 80f), durationMillis = 600)
        }
        lateinit var pannedTarget: LatLng
        rule.runOnUiThread { map.stopAnimation(); pannedTarget = map.cameraPosition.target }
        rule.runOnIdle {
            heading = 135f
            location = location.copy(currentLocation = GeoCoordinate(coordinate.latitude + 0.001, coordinate.longitude))
        }
        rule.waitForIdle()
        rule.runOnUiThread { assertEquals(pannedTarget, map.cameraPosition.target) }
        rule.runOnIdle { recenter++ }
        waitOnMap(map) { it.cameraPosition.bearing == 135f }
        rule.runOnUiThread { assertNotEquals(pannedTarget, map.cameraPosition.target) }
    }

    @Test fun fragmentsRemainClickableWhenTheirFoundStateAndTeamOverlayChange() {
        val fragment = TeamHuntFragment("map_test_fragment", "Fragment", coordinate)
        var found by mutableStateOf(emptySet<String>())
        var teammates by mutableStateOf(emptyList<TeamHuntLocation>())
        val selected = AtomicReference<String>()
        rule.setContent {
            GoogleMapView(emptyList(), null, LocationOutput(), 0f,
                huntFragments = listOf(fragment), foundFragmentIds = found,
                teammateLocations = teammates, huntMemberNames = mapOf("host" to "Host explorer"),
                huntOwnerId = "host", onRelicSelected = {}, onFragmentSelected = { selected.set(it.id) },
                modifier = modifier)
        }
        val map = awaitMap()
        waitOnMap(map) { it.cameraPosition.zoom == 17f }
        screenshot(map, "fragment_unfound")
        tapCoordinate(map, coordinate)
        rule.waitUntil(5_000) { selected.get() == fragment.id }
        rule.runOnIdle {
            found = setOf(fragment.id)
            teammates = listOf(TeamHuntLocation("host", "map_test_session",
                GeoCoordinate(coordinate.latitude + 0.0003, coordinate.longitude + 0.0003), 0L, 0L))
            selected.set(null)
        }
        rule.waitForIdle()
        tapCoordinate(map, coordinate)
        rule.waitUntil(5_000) { selected.get() == fragment.id }
        screenshot(map, "fragment_found")
        rule.runOnIdle { teammates = emptyList(); selected.set(null) }
        rule.waitForIdle()
        tapCoordinate(map, coordinate)
        rule.waitUntil(5_000) { selected.get() == fragment.id }
    }

    private fun awaitMap(): GoogleMap {
        val map = AtomicReference<GoogleMap>()
        rule.waitForIdle()
        rule.runOnUiThread {
            requireNotNull(findMapView(rule.activity.window.decorView)).getMapAsync { map.set(it) }
        }
        rule.waitUntil(30_000) { map.get() != null }
        return map.get()
    }

    private fun findMapView(view: View): MapView? {
        if (view is MapView) return view
        if (view is ViewGroup) for (index in 0 until view.childCount) {
            findMapView(view.getChildAt(index))?.let { return it }
        }
        return null
    }

    private fun waitOnMap(map: GoogleMap, predicate: (GoogleMap) -> Boolean) {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        rule.waitUntil(10_000) {
            var ready = false
            instrumentation.runOnMainSync { ready = predicate(map) }
            ready
        }
    }

    private fun tapCoordinate(map: GoogleMap, location: GeoCoordinate) {
        var point = Offset.Zero
        rule.runOnUiThread {
            val screen = map.projection.toScreenLocation(LatLng(location.latitude, location.longitude))
            point = Offset(screen.x.toFloat(), screen.y.toFloat())
        }
        rule.onNodeWithTag("map_surface").performTouchInput {
            // Maps also recognises double taps; each assertion here exercises an independent single tap.
            advanceEventTime(500)
            click(point)
        }
    }

    private fun screenshot(map: GoogleMap, name: String) {
        val saved = AtomicBoolean(false)
        val pending = AtomicBoolean(false)
        var previousFrame: Bitmap? = null
        var stableSince = 0L
        // Camera updates complete before the native renderer draws its first marker frame.
        rule.waitUntil(10_000) {
            if (!saved.get() && pending.compareAndSet(false, true)) {
                rule.runOnUiThread {
                    val anchor = map.projection.toScreenLocation(LatLng(coordinate.latitude, coordinate.longitude))
                    map.snapshot { bitmap ->
                        requireNotNull(bitmap)
                        if (hasVisibleMarker(bitmap, anchor.x, anchor.y)) {
                            val now = SystemClock.uptimeMillis()
                            if (previousFrame?.sameAs(bitmap) != true) {
                                previousFrame = bitmap
                                stableSince = now
                            } else if (now - stableSince >= 200L) {
                                val directory = File(rule.activity.getExternalFilesDir(null), "googlemap-preview").apply { mkdirs() }
                                File(directory, "$name.png").outputStream().use {
                                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
                                }
                                saved.set(true)
                            }
                        }
                        pending.set(false)
                    }
                }
            }
            saved.get()
        }
    }

    private fun hasVisibleMarker(bitmap: Bitmap, anchorX: Int, anchorY: Int): Boolean {
        val background = bitmap.getPixel(0, 0)
        var visiblePixels = 0
        for (y in (anchorY - 48).coerceAtLeast(0)..(anchorY + 48).coerceAtMost(bitmap.height - 1)) {
            for (x in (anchorX - 48).coerceAtLeast(0)..(anchorX + 48).coerceAtMost(bitmap.width - 1)) {
                if (bitmap.getPixel(x, y) != background) visiblePixels++
            }
        }
        return visiblePixels > 20
    }
}
