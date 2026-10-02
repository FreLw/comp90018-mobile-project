package com.comp90018.app.features.map

/**
 * Decides whether the explorer using this device still needs to do their part of a two-person
 * hunt. Keeping this separate from the UI ensures every entry point uses the same completion
 * check before it offers the location-triggered task.
 */
internal object TeamHuntTaskEligibility {
    fun isPendingFor(
        huntActive: Boolean,
        memberIds: List<String>,
        completedMemberIds: List<String>,
        currentUserId: String,
    ): Boolean = huntActive &&
        memberIds.size == TEAM_HUNT_MEMBER_COUNT &&
        currentUserId.isNotBlank() &&
        currentUserId in memberIds &&
        currentUserId !in completedMemberIds

    private const val TEAM_HUNT_MEMBER_COUNT = 2
}
