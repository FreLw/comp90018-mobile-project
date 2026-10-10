package com.comp90018.app.features.map

/*
 * Determines whether the current teammate should perform a task, wait, or claim the treasure.
 * One policy keeps map prompts and hunt entry points aligned with shared completion state.
 */

/**
 * Decides whether the explorer using this device still needs to do their part of a team
 * hunt. Keeping this separate from the UI ensures every entry point uses the same completion
 * check before it offers the location-triggered task.
 */
internal object TeamHuntTaskEligibility {
    fun canClaim(
        huntActive: Boolean,
        memberIds: List<String>,
        completedMemberIds: List<String>,
        claimedMemberIds: List<String>,
        currentUserId: String,
    ): Boolean = huntActive && memberIds.size in 2..4 &&
        currentUserId in memberIds && currentUserId !in claimedMemberIds &&
        memberIds.all { it in completedMemberIds }

    fun promptKey(userId: String, sessionId: String): String = "claim_${userId.length}:${userId}_$sessionId"

    fun isPendingFor(
        huntActive: Boolean,
        memberIds: List<String>,
        completedMemberIds: List<String>,
        currentUserId: String,
    ): Boolean = huntActive &&
        memberIds.size in 2..4 &&
        currentUserId.isNotBlank() &&
        currentUserId in memberIds &&
        currentUserId !in completedMemberIds

}
