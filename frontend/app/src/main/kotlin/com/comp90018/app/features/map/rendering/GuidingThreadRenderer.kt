package com.comp90018.app.features.map.rendering

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.ui.graphics.Color
import com.comp90018.app.features.map.GUIDING_THREAD_GLINT_COUNT
import com.comp90018.app.features.map.GuidingThreadFrame
import com.comp90018.app.features.map.guidingThreadGlintCentre
import com.comp90018.app.features.map.guidingThreadGlintVertices
import com.comp90018.app.features.map.guidingThreadUpdates
import com.comp90018.app.features.navigation.guidingThreadStyle
import com.comp90018.app.sensors.location.GeoCoordinate
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.Polygon
import com.google.android.gms.maps.model.PolygonOptions
import com.google.android.gms.maps.model.Polyline
import com.google.android.gms.maps.model.PolylineOptions

internal class GuidingThreadRenderState {
    var overlay = GuidingThreadOverlay()
}

internal data class GuidingThreadOverlay(
    val base: Polyline? = null,
    val active: Polyline? = null,
    val glints: List<Polygon> = emptyList(),
    val frame: GuidingThreadFrame? = null,
) {
    fun remove() {
        base?.remove()
        active?.remove()
        glints.forEach(Polygon::remove)
    }
}

/** Optional Navigation overlay only; the ordinary Map/Hunt renderer has no thread target. */
internal fun updateNavigationTrail(
    map: GoogleMap,
    currentLocation: GeoCoordinate?,
    target: GeoCoordinate?,
    strength: Float,
    phase: Float,
    previous: GuidingThreadOverlay,
): GuidingThreadOverlay {
    if (currentLocation == null || target == null) {
        previous.remove()
        return GuidingThreadOverlay()
    }

    val start = currentLocation.toLatLng()
    val end = target.toLatLng()
    val resonance = if (strength.isFinite()) strength.coerceIn(0f, 1f) else 0f
    val frame = GuidingThreadFrame(currentLocation, target, resonance, phase)
    val updates = guidingThreadUpdates(previous.frame, frame)
    val style = guidingThreadStyle(resonance)
    val base = previous.base ?: map.addPolyline(
        PolylineOptions().add(start, end).geodesic(true).zIndex(1f),
    )
    val active = previous.active ?: map.addPolyline(
        PolylineOptions().add(start, end).geodesic(true).zIndex(1.1f),
    )
    if (updates.geometry) {
        base.points = listOf(start, end)
        active.points = listOf(start, end)
    }
    if (updates.style) {
        base.width = 2f
        base.color = android.graphics.Color.argb(100, 122, 75, 42)
        active.width = style.lineWidthPixels
        active.color = android.graphics.Color.argb((style.lineAlpha * 255).toInt(), 183, 121, 31)
    }

    val glints = if (previous.glints.size == GUIDING_THREAD_GLINT_COUNT) previous.glints else {
        previous.glints.forEach(Polygon::remove)
        List(GUIDING_THREAD_GLINT_COUNT) { index ->
            val centre = guidingThreadGlintCentre(target, currentLocation, phase, index)
            map.addPolygon(PolygonOptions()
                .addAll(guidingThreadGlintVertices(centre, 0.75).map { it.toLatLng() })
                .strokeWidth(0.7f).strokeColor(android.graphics.Color.rgb(52, 35, 25))
                .clickable(false).zIndex(2f))
        }
    }
    if (updates.style) glints.forEach { glint ->
        glint.fillColor = android.graphics.Color.argb((style.glintAlpha * 255).toInt(), 183, 121, 31)
        glint.strokeColor = android.graphics.Color.argb((style.glintAlpha * 200).toInt(), 52, 35, 25)
        glint.isVisible = resonance > 0f
    }
    if (updates.animation) glints.forEachIndexed { index, glint ->
        val centre = guidingThreadGlintCentre(target, currentLocation, phase, index)
        glint.points = guidingThreadGlintVertices(centre, style.glintRadiusMeters).map { it.toLatLng() }
    }
    return GuidingThreadOverlay(base, active, glints, frame)
}
