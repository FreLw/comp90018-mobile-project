package com.comp90018.app.features.navigation

/** Navigation guides the user; hunt and Context Engine own challenge eligibility. */
internal object RelicNavigationConfig {
    const val resonanceRangeMeters = 250.0
    const val faintDrawnBoundaryMeters = 100.0
    const val drawnStrongBoundaryMeters = 30.0
    const val huntArrivalEntryRadiusMeters = 10.0
    const val huntArrivalExitRadiusMeters = 15.0
    const val requiredArrivalFixes = 2
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
): RelicNavigationUiState {
    val stage = confirmedRelicResonanceStage(sample, confirmation)
    val distance = sample.distanceMeters?.takeIf {
        sample.hasActionableLocation && it.isFinite() && it >= 0.0
    }
    val arrived = stage == RelicResonanceStage.ARRIVED
    return RelicNavigationUiState(
        resonanceStage = stage,
        distanceMeters = distance,
        // Preserve the current full battery while confirmed arrival is retained by hysteresis.
        resonanceProgress = if (arrived) 1f else navigationEnergyProgress(distance),
        targetBearingDegrees = targetBearingDegrees?.takeIf { distance != null && it.isFinite() },
        directionHint = NavigationDirectionHint.UNAVAILABLE,
        headingErrorDegrees = null,
        arrivalConfirmed = arrived,
        canBeginHunt = arrived,
        deviceHeadingDegrees = deviceHeadingDegrees?.takeIf { it.isFinite() },
    )
}
