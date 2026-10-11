package com.comp90018.app.features.map.compass

import com.comp90018.app.features.map.CompassGateConfig
import com.comp90018.app.features.map.HuntReadiness
import com.comp90018.app.features.map.components.formatDegrees
import com.comp90018.app.features.map.components.formatGateDistance
import com.comp90018.app.features.map.evaluateHuntReadiness
import com.comp90018.app.features.map.signedBearingDifference
import com.comp90018.app.sensors.location.LocationOutput
import kotlin.math.abs

/** Values derived from the current location and optional sensor preview. */
internal data class CompassGateUiState(
    val distanceMeters: Double?,
    val targetBearingDegrees: Double?,
    val turnDegrees: Double?,
    val locationReady: Boolean,
    val readiness: HuntReadiness,
    val proximityGlow: Float,
    val headingGlow: Float,
    val stars: Int,
)

/** Simulation is enabled by the caller only in debug builds. */
internal fun compassGateUiState(
    config: CompassGateConfig,
    locationOutput: LocationOutput,
    deviceHeading: Float,
    simulateSensors: Boolean,
    testDistance: Float,
    testHeading: Float,
): CompassGateUiState {
    val distance = if (simulateSensors) testDistance.toDouble() else locationOutput.distanceToTargetMeters
    val targetBearing = locationOutput.targetBearingDegrees ?: if (simulateSensors) 0.0 else null
    val heading = if (simulateSensors) testHeading else deviceHeading
    val turnDegrees = targetBearing?.let { signedBearingDifference(it, heading.toDouble()) }
    val locationReady = simulateSensors ||
        locationOutput.validity == com.comp90018.app.sensors.SensorValidity.VALID
    val readiness = if (locationReady) {
        evaluateHuntReadiness(distance, turnDegrees, config)
    } else HuntReadiness(false, false)
    val proximityGlow = distance?.let { (((config.huntReadyRadiusMeters + 20.0) - it) / 20.0).toFloat().coerceIn(0f, 1f) } ?: 0f
    val headingGlow = turnDegrees?.let { ((180.0 - abs(it)) / (180.0 - config.compassAlignmentToleranceDegrees).coerceAtLeast(1.0)).toFloat().coerceIn(0f, 1f) } ?: 0f
    return CompassGateUiState(
        distance, targetBearing, turnDegrees, locationReady, readiness,
        proximityGlow, headingGlow, oracleStarCount(distance, config.huntReadyRadiusMeters),
    )
}

internal fun compassGateInstruction(
    state: CompassGateUiState,
    signalLocked: Boolean,
    config: CompassGateConfig,
): String {
    val turnDegrees = state.turnDegrees
    return when {
        signalLocked -> "Treasure signal locked — the hidden trial has awakened."
        !state.locationReady || state.distanceMeters == null || state.targetBearingDegrees == null -> "Waiting for a reliable location signal…"
        !state.readiness.nearTreasure -> "The signal faded. Move back within ${config.huntReadyRadiusMeters.formatGateDistance()}."
        !state.readiness.facingTreasure && requireNotNull(turnDegrees) > 0 -> "Turn right ${abs(turnDegrees).formatDegrees()} toward the treasure."
        !state.readiness.facingTreasure -> "Turn left ${abs(requireNotNull(turnDegrees)).formatDegrees()} toward the treasure."
        else -> "Hold it there — locking onto the treasure…"
    }
}

internal fun oracleStarCount(distanceMeters: Double?, huntReadyRadiusMeters: Double = 10.0): Int = distanceMeters?.takeIf { it.isFinite() }?.let {
    kotlin.math.floor((((huntReadyRadiusMeters + 20.0) - it) / 20.0).coerceIn(0.0, 1.0) * 10.0).toInt()
} ?: 0
