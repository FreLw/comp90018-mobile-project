package com.comp90018.app.features.navigation

/** Navigation guides the user; hunt and Context Engine own challenge eligibility. */
internal object RelicNavigationConfig {
    const val resonanceRangeMeters = 250.0
    const val faintDrawnBoundaryMeters = 100.0
    const val drawnStrongBoundaryMeters = 30.0
    const val huntArrivalEntryRadiusMeters = 10.0
    const val huntArrivalExitRadiusMeters = 15.0
}

internal enum class RelicResonanceStage {
    ACQUIRING,
    DORMANT,
    FAINT,
    DRAWN,
    STRONG,
    // Reserved for a future reliable arrival confirmation policy.
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
