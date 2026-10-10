package com.comp90018.app.features.navigation

/*
 * Maps resonance strength to rosette activation and guiding-thread visual emphasis.
 * These values style the drawing; they do not grant arrival or challenge completion.
 */

internal data class RosetteActivation(val arcAlpha: Float, val activePetals: Int)

internal fun rosetteActivation(progress: Float, stage: RelicResonanceStage): RosetteActivation {
    val strength = safeResonanceStrength(progress)
    val petals = when (stage) {
        RelicResonanceStage.ACQUIRING, RelicResonanceStage.DORMANT -> 0
        RelicResonanceStage.FAINT -> 2
        RelicResonanceStage.DRAWN -> 3 + (strength * 4f).toInt()
        RelicResonanceStage.STRONG -> 7
        RelicResonanceStage.CONFIRMING, RelicResonanceStage.ARRIVED -> 8
    }
    return RosetteActivation(0.65f + 0.35f * strength, petals)
}

internal data class GuidingThreadStyle(
    val lineAlpha: Float,
    val lineWidthPixels: Float,
    val glintAlpha: Float,
    val glintRadiusMeters: Double,
)

/** Distance sets emphasis; heading is deliberately absent from the style contract. */
internal fun guidingThreadStyle(progress: Float): GuidingThreadStyle {
    val strength = safeResonanceStrength(progress)
    return GuidingThreadStyle(
        lineAlpha = strength,
        lineWidthPixels = 1.5f + strength * 2.5f,
        glintAlpha = strength,
        glintRadiusMeters = 0.55 + strength * 0.85,
    )
}

private fun safeResonanceStrength(progress: Float): Float =
    if (progress.isFinite()) progress.coerceIn(0f, 1f) else 0f
