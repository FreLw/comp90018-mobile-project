package com.comp90018.app.features.haptics

/*
 * Coordinates treasure proximity/completion feedback and suppresses duplicate vibrations.
 * Tracks attempts across UI navigation and respects session closure, preferences, and lifecycle cancellation.
 */

/** One instance per signed-in exploration session; UI navigation never resets these sets. */
class TreasureHapticController(
    private val driver: TreasureHapticDriver,
    private val nowNanos: () -> Long = System::nanoTime,
) {
    private var successStartedAtNanos: Long? = null
    private val attempts = mutableSetOf<TreasureHapticAttempt>()
    private val nearbyIds = mutableSetOf<String>()
    private val unlockedIds = mutableSetOf<String>()
    private val completedChallengeIds = mutableSetOf<String>()
    private val challengeFeedbackTreasureIds = mutableSetOf<String>()
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
        successStartedAtNanos = null
        attempts.clear()
        nearbyIds.clear()
        unlockedIds.clear()
        completedChallengeIds.clear()
        challengeFeedbackTreasureIds.clear()
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
    fun abandonAttempt(attempt: TreasureHapticAttempt) { attempts.remove(attempt) }

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

    /** Clear protection when the driver is cancelled by settings or lifecycle. */
    @Synchronized
    fun cancelPlaybackProtection() { successStartedAtNanos = null }

    @Synchronized
    fun nearby(treasureId: String, distanceMeters: Double?) {
        if (ended || !enabled || !foreground || treasureId.isBlank() ||
            !TreasureHapticProximity.isNearby(distanceMeters)
        ) return
        // Drop this update without consuming its treasure ID. A later valid GPS event can notify.
        if (successStartedAtNanos?.let { nowNanos() - it < TreasureHapticPattern.SUCCESS_DURATION_NANOS } == true ||
            !nearbyIds.add(treasureId)
        ) return
        driver.play(TreasureHapticEvent.Nearby(treasureId))
    }

    /** A fully satisfied task gives feedback once, independently of cloud-save latency. */
    @Synchronized
    fun challengeCompleted(treasureId: String, completionId: String) {
        if (ended || treasureId.isBlank() || completionId.isBlank() || !completedChallengeIds.add(completionId)) return
        nearbyIds.add(treasureId)
        challengeFeedbackTreasureIds.add(treasureId)
        if (enabled && foreground) {
            successStartedAtNanos = nowNanos()
            driver.play(TreasureHapticEvent.Unlocked(treasureId))
        }
    }

    /** Called only by the existing discovery-save confirmation, never by sensor readiness. */
    @Synchronized
    fun discoverySaved(
        treasureId: String, error: String?, alreadyDiscovered: Boolean = false,
        completionId: String = "discovery:$treasureId",
    ) {
        if (ended || error != null || alreadyDiscovered || treasureId.isBlank() || completionId.isBlank() || !unlockedIds.add(completionId)) return
        // A background/disabled success is consumed, not replayed later on navigation/resume.
        nearbyIds.add(treasureId) // prevent a lagging collection snapshot from notifying an unlocked target
        val taskAlreadyGaveFeedback = completionId == "discovery:$treasureId" && treasureId in challengeFeedbackTreasureIds
        if (enabled && foreground && !taskAlreadyGaveFeedback) {
            successStartedAtNanos = nowNanos()
            driver.play(TreasureHapticEvent.Unlocked(treasureId))
        }
    }
}
