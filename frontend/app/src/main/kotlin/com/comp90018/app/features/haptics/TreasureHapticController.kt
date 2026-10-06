package com.comp90018.app.features.haptics

/** One instance per signed-in exploration session; UI navigation never resets these sets. */
class TreasureHapticController(private val driver: TreasureHapticDriver) {
    private val attempts = mutableSetOf<TreasureHapticAttempt>()
    private val nearbyIds = mutableSetOf<String>()
    private val unlockedIds = mutableSetOf<String>()
    var ended = false
        private set
    var enabled = false
    var foreground = false

    /** Closed controllers stay closed: late callbacks cannot cross into the next sign-in. */
    @Synchronized
    fun endSession() {
        ended = true
        enabled = false
        foreground = false
        attempts.clear()
        nearbyIds.clear()
        unlockedIds.clear()
    }

    @Synchronized
    fun beginAttempt(
        treasureId: String,
        owner: String = "map",
        completionId: String = "discovery:$treasureId",
    ): TreasureHapticAttempt {
        abandonTreasure(owner, treasureId)
        return TreasureHapticAttempt(treasureId, completionId, owner).also {
            if (!ended) attempts.add(it)
        }
    }

    @Synchronized
    fun isCurrent(attempt: TreasureHapticAttempt): Boolean = !ended && attempt in attempts

    @Synchronized
    fun abandonTreasure(owner: String, treasureId: String) {
        attempts.removeAll { it.owner == owner && it.treasureId == treasureId }
    }

    @Synchronized
    fun abandonOwner(owner: String) { attempts.removeAll { it.owner == owner } }

    @Synchronized
    fun completeAttempt(
        attempt: TreasureHapticAttempt,
        error: String?,
        alreadyDiscovered: Boolean = false,
        completionId: String = attempt.completionId,
    ) {
        if (ended || !attempts.remove(attempt)) return
        discoverySaved(attempt.treasureId, error, alreadyDiscovered, completionId)
    }

    @Synchronized
    fun nearby(treasureId: String, distanceMeters: Double?) {
        if (ended || !enabled || !foreground || treasureId.isBlank() ||
            !TreasureHapticProximity.isNearby(distanceMeters) || !nearbyIds.add(treasureId)
        ) return
        driver.play(TreasureHapticEvent.Nearby(treasureId))
    }

    /** Called only by the existing discovery-save confirmation, never by sensor readiness. */
    @Synchronized
    fun discoverySaved(
        treasureId: String, error: String?, alreadyDiscovered: Boolean = false,
        completionId: String = "discovery:$treasureId",
    ) {
        if (ended || error != null || alreadyDiscovered || treasureId.isBlank() || completionId.isBlank() || !unlockedIds.add(completionId)) return
        // A background/disabled success is consumed, not replayed later on navigation/resume.
        if (enabled && foreground) driver.play(TreasureHapticEvent.Unlocked(treasureId))
    }
}
