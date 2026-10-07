package com.comp90018.app.features.navigation

internal data class RelicArrivalPresentation(
    val stateDescription: String,
    val showBeginHunt: Boolean,
)

internal fun relicArrivalPresentation(
    state: RelicNavigationUiState,
    animationComplete: Boolean,
): RelicArrivalPresentation = RelicArrivalPresentation(
    stateDescription = resonanceStageLabel(state.resonanceStage),
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
