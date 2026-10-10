package com.comp90018.app.features.navigation

/*
 * Converts magnetic device headings into the true-north reference used by GPS bearings.
 * Caches geographic declination so heading animation does not repeatedly recreate the geomagnetic model.
 */

import android.hardware.GeomagneticField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.comp90018.app.sensors.DirectionProcessor
import com.comp90018.app.sensors.location.GeoCoordinate
import kotlin.math.floor

/** GPS bearings are true north; rotation-vector azimuth is magnetic north. */
internal fun navigationTrueHeading(
    magneticHeadingDegrees: Double?,
    declinationDegrees: Double?,
    simulatedTrueHeadingDegrees: Double? = null,
): Double? {
    if (simulatedTrueHeadingDegrees != null) {
        return simulatedTrueHeadingDegrees.takeIf { it.isFinite() }?.let(DirectionProcessor::normalize)
    }
    if (magneticHeadingDegrees == null || !magneticHeadingDegrees.isFinite() ||
        declinationDegrees == null || !declinationDegrees.isFinite()
    ) return null
    return DirectionProcessor.normalize(magneticHeadingDegrees + declinationDegrees)
}

/** Cache within a roughly kilometre-wide cell, independent of heading/animation updates. */
@Composable
internal fun rememberNavigationDeclination(coordinate: GeoCoordinate?): Double? {
    val latitudeCell = coordinate?.latitude?.let { floor(it * 100.0) }
    val longitudeCell = coordinate?.longitude?.let { floor(it * 100.0) }
    return remember(latitudeCell, longitudeCell) {
        coordinate?.takeIf {
            it.latitude.isFinite() && it.latitude in -90.0..90.0 &&
                it.longitude.isFinite() && it.longitude in -180.0..180.0
        }?.let {
            // LocationOutput has no altitude/epoch fix time. Sea level is adequate for guidance;
            // elapsed GPS timestamps remain exclusively arrival evidence, never model epoch time.
            GeomagneticField(it.latitude.toFloat(), it.longitude.toFloat(), 0f, System.currentTimeMillis())
                .declination.toDouble().takeIf { declination -> declination.isFinite() }
        }
    }
}
