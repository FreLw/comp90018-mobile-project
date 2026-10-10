package com.comp90018.app.features.navigation

/*
 * Chooses arrival status, helper text, and resonance-stage labels for guidance panels.
 * Keeps loading/location explanations consistent with whether the hunt action is available.
 */

internal data class RelicArrivalPresentation(
    val stateDescription: String,
    val showBeginHunt: Boolean,
)

/**
 * Chooses the arrival caption/action from confirmation and location readiness rather than
 * distance alone.
 */
internal fun relicArrivalPresentation(
    state: RelicNavigationUiState,
    animationComplete: Boolean,
): RelicArrivalPresentation = RelicArrivalPresentation(
    stateDescription = when (state.locationReadiness) {
        NavigationLocationReadiness.READY -> resonanceStageLabel(state.resonanceStage)
        NavigationLocationReadiness.ACQUIRING -> "Seeking your position…"
        NavigationLocationReadiness.ACCESS_REQUIRED -> "Location access is required"
        NavigationLocationReadiness.IMPROVING_SIGNAL -> "Improving location signal…"
        NavigationLocationReadiness.UNAVAILABLE -> "Location signal unavailable"
    },
    showBeginHunt = state.canBeginHunt && animationComplete,
)

internal fun resonanceStageLabel(stage: RelicResonanceStage): String = when (stage) {
    RelicResonanceStage.ACQUIRING -> "Acquiring position"
    RelicResonanceStage.DORMANT -> "Outside resonance"
    RelicResonanceStage.FAINT -> "Faint resonance"
    RelicResonanceStage.DRAWN -> "Drawn closer"
    RelicResonanceStage.STRONG -> "Strong resonance"
    RelicResonanceStage.CONFIRMING -> "Confirming the resonance…"
    RelicResonanceStage.ARRIVED -> "Resonance complete"
}
