package com.comp90018.app.features.map

/*
 * Converts distance estimates into map-level unknown, far, nearby, and hunt-ready stages.
 * The UI uses these stages for presentation; the challenge evaluator performs its own GPS confirmation.
 */

import com.comp90018.app.sensors.SensorValidity

enum class HuntProximityStage { UNKNOWN, FAR, NEARBY, HUNT_READY }

/** A map-level estimate; the challenge evaluator performs its own stricter GPS confirmation. */
object HuntProximityResolver {
    fun resolve(
        distanceMeters: Double?,
        locationValidity: SensorValidity,
        radarRadiusMeters: Double,
        insideRadiusMeters: Double,
    ): HuntProximityStage {
        if (locationValidity != SensorValidity.VALID || distanceMeters == null ||
            !distanceMeters.isFinite() || distanceMeters < 0.0 ||
            !radarRadiusMeters.isFinite() || radarRadiusMeters <= 0.0 ||
            !insideRadiusMeters.isFinite() || insideRadiusMeters < 0.0 ||
            insideRadiusMeters > radarRadiusMeters
        ) return HuntProximityStage.UNKNOWN

        return when {
            distanceMeters <= insideRadiusMeters -> HuntProximityStage.HUNT_READY
            distanceMeters <= radarRadiusMeters -> HuntProximityStage.NEARBY
            else -> HuntProximityStage.FAR
        }
    }
}
