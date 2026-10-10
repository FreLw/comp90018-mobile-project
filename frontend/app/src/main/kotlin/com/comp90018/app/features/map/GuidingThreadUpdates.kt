package com.comp90018.app.features.map

/*
 * Chooses which guiding-thread overlay parts need updating on a new animation frame.
 * Separates moving glints from line styling so animation ticks do not unnecessarily rebuild the map scene.
 */

import com.comp90018.app.sensors.location.GeoCoordinate

internal data class GuidingThreadFrame(
    val location: GeoCoordinate?,
    val target: GeoCoordinate?,
    val strength: Float,
    val phase: Float,
)

internal data class GuidingThreadUpdates(
    val geometry: Boolean,
    val style: Boolean,
    val animation: Boolean,
)

/** Phase-only ticks move existing glints, without restyling lines or touching scene/camera state. */
internal fun guidingThreadUpdates(previous: GuidingThreadFrame?, next: GuidingThreadFrame): GuidingThreadUpdates {
    val geometry = previous == null || previous.location != next.location || previous.target != next.target
    val style = previous == null || previous.strength != next.strength
    return GuidingThreadUpdates(geometry, style, geometry || style || previous.phase != next.phase)
}
