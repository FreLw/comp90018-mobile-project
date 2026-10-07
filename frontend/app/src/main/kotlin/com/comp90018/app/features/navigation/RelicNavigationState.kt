package com.comp90018.app.features.navigation

import com.comp90018.app.sensors.DirectionProcessor
import kotlin.math.abs

/** Navigation guides the user; hunt and Context Engine own challenge eligibility. */
internal object RelicNavigationConfig {
    const val resonanceRangeMeters = 150.0
    const val faintDrawnBoundaryMeters = 80.0
    const val drawnStrongBoundaryMeters = 30.0
    const val huntArrivalEntryRadiusMeters = 10.0
    const val huntArrivalExitRadiusMeters = 15.0
    const val requiredArrivalFixes = 2
    const val directionAlignmentToleranceDegrees = 12.0
}

internal enum class RelicResonanceStage {
    ACQUIRING,
    DORMANT,
    FAINT,
    DRAWN,
    STRONG,
    // Emitted by the arrival confirmation policy, never by distance alone.
    CONFIRMING,
    ARRIVED,
}

internal enum class NavigationDirectionHint {
    UNAVAILABLE,
    TURN_LEFT,
    TURN_RIGHT,
    ALIGNED,
}

internal data class RelicNavigationUiState(
    val resonanceStage: RelicResonanceStage = RelicResonanceStage.ACQUIRING,
    val distanceMeters: Double? = null,
    val resonanceProgress: Float = 0f,
    val targetBearingDegrees: Double? = null,
    val directionHint: NavigationDirectionHint = NavigationDirectionHint.UNAVAILABLE,
    val headingErrorDegrees: Double? = null,
    val arrivalConfirmed: Boolean = false,
    val canBeginHunt: Boolean = false,
    val deviceHeadingDegrees: Double? = null,
    val locationReadiness: NavigationLocationReadiness = NavigationLocationReadiness.ACQUIRING,
)

/**
 * The caller supplies actionability from the location policy, excluding display fallbacks.
 * Boundaries belong to the stronger stage. Inside the entry radius remains STRONG:
 * a distance sample alone cannot confirm arrival or grant hunt readiness.
 */
internal fun relicResonanceStage(
    distanceMeters: Double?,
    hasActionableLocation: Boolean,
): RelicResonanceStage = when {
    !hasActionableLocation || distanceMeters == null ||
        !distanceMeters.isFinite() || distanceMeters < 0.0 -> RelicResonanceStage.ACQUIRING
    distanceMeters > RelicNavigationConfig.resonanceRangeMeters -> RelicResonanceStage.DORMANT
    distanceMeters > RelicNavigationConfig.faintDrawnBoundaryMeters -> RelicResonanceStage.FAINT
    distanceMeters > RelicNavigationConfig.drawnStrongBoundaryMeters -> RelicResonanceStage.DRAWN
    else -> RelicResonanceStage.STRONG
}

/** Use the current sample's updated confirmation state; progress alone never grants arrival. */
internal fun deriveRelicNavigationUiState(
    sample: RelicArrivalSample,
    confirmation: RelicArrivalConfirmationState,
    targetBearingDegrees: Double? = null,
    deviceHeadingDegrees: Double? = null,
    locationReadiness: NavigationLocationReadiness? = null,
): RelicNavigationUiState {
    val stage = confirmedRelicResonanceStage(sample, confirmation)
    val distance = sample.distanceMeters?.takeIf {
        sample.hasActionableLocation && it.isFinite() && it >= 0.0
    }
    val arrived = stage == RelicResonanceStage.ARRIVED
    val readiness = if (distance != null) locationReadiness ?: NavigationLocationReadiness.READY
        else locationReadiness?.takeUnless { it == NavigationLocationReadiness.READY }
            ?: NavigationLocationReadiness.ACQUIRING
    val bearing = targetBearingDegrees?.takeIf { distance != null && it.isFinite() }
    val heading = deviceHeadingDegrees?.takeIf { it.isFinite() }
    // Both inputs use true north. Negative error means left; exactly opposite means -180.
    val error = if (bearing != null && heading != null) DirectionProcessor.angularDifference(heading, bearing)
        else null
    val direction = when {
        error == null -> NavigationDirectionHint.UNAVAILABLE
        abs(error) <= RelicNavigationConfig.directionAlignmentToleranceDegrees -> NavigationDirectionHint.ALIGNED
        error < 0.0 -> NavigationDirectionHint.TURN_LEFT
        else -> NavigationDirectionHint.TURN_RIGHT
    }
    return RelicNavigationUiState(
        resonanceStage = stage,
        distanceMeters = distance,
        // Keep full resonance while confirmed arrival is retained by hysteresis.
        resonanceProgress = if (arrived) 1f else navigationEnergyProgress(distance),
        targetBearingDegrees = bearing,
        directionHint = direction,
        headingErrorDegrees = error,
        arrivalConfirmed = arrived,
        canBeginHunt = arrived,
        deviceHeadingDegrees = heading,
        locationReadiness = readiness,
    )
}
