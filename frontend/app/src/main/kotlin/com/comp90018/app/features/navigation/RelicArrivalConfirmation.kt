package com.comp90018.app.features.navigation

/*
 * Confirms navigation arrival using distinct reliable location samples.
 * Arrival enables starting a hunt; it does not complete the subsequent treasure challenge.
 */

internal data class RelicArrivalSample(
    val hasActionableLocation: Boolean = false,
    val distanceMeters: Double? = null,
    val accuracyMeters: Double? = null,
    val timestampNanos: Long? = null,
)

/** Reaching the search area permits beginning a hunt, not completing its challenge. */
internal data class RelicArrivalConfirmationState(
    val consecutiveInsideFixes: Int = 0,
    val lastProcessedTimestampNanos: Long? = null,
) {
    val arrivalConfirmed: Boolean
        get() = consecutiveInsideFixes >= RelicNavigationConfig.requiredArrivalFixes
}

/**
 * Use one state per navigation target/session. No clock or challenge-domain dependency.
 * Invalid/unavailable evidence immediately clears arrival, retaining the timestamp watermark
 * so a previously counted fix cannot re-confirm it. Reliable repeated/older fixes are ignored.
 * Accuracy remains bounded by the entry radius even after arrival; only distance uses hysteresis.
 */
internal fun updateRelicArrivalConfirmation(
    previousState: RelicArrivalConfirmationState,
    sample: RelicArrivalSample,
): RelicArrivalConfirmationState {
    val timestamp = sample.timestampNanos
    val lastTimestamp = previousState.lastProcessedTimestampNanos
    val isNewFix = timestamp != null && (lastTimestamp == null || timestamp > lastTimestamp)
    val processedTimestamp = if (isNewFix) timestamp else lastTimestamp
    val distance = sample.distanceMeters
    val accuracy = sample.accuracyMeters
    val reliable = sample.hasActionableLocation && timestamp != null &&
        distance != null && distance.isFinite() && distance >= 0.0 &&
        (accuracy == null || (accuracy.isFinite() && accuracy >= 0.0 &&
            accuracy <= RelicNavigationConfig.huntArrivalEntryRadiusMeters))

    if (!reliable) return RelicArrivalConfirmationState(lastProcessedTimestampNanos = processedTimestamp)
    if (!isNewFix) return previousState

    val radius = if (previousState.arrivalConfirmed) RelicNavigationConfig.huntArrivalExitRadiusMeters
        else RelicNavigationConfig.huntArrivalEntryRadiusMeters
    val count = if (requireNotNull(distance) <= radius) {
        (previousState.consecutiveInsideFixes + 1).coerceAtMost(RelicNavigationConfig.requiredArrivalFixes)
    } else 0
    return RelicArrivalConfirmationState(count, processedTimestamp)
}

/** Combine the current sample with its updated confirmation state before presenting guidance. */
internal fun confirmedRelicResonanceStage(
    sample: RelicArrivalSample,
    confirmation: RelicArrivalConfirmationState,
): RelicResonanceStage {
    val stage = relicResonanceStage(sample.distanceMeters, sample.hasActionableLocation)
    return when {
        stage == RelicResonanceStage.ACQUIRING -> stage
        confirmation.arrivalConfirmed -> RelicResonanceStage.ARRIVED
        confirmation.consecutiveInsideFixes > 0 -> RelicResonanceStage.CONFIRMING
        else -> stage
    }
}
