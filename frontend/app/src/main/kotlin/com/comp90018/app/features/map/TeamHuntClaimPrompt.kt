package com.comp90018.app.features.map

/*
 * Decides when to show the completed-team-hunt claim prompt.
 * Injected persistence remembers acknowledgement across map navigation and app restarts.
 */

/** Persistence is supplied by the map so acknowledgement survives navigation and restarts. */
internal class TeamHuntClaimPrompt(
    private val readSeen: () -> Boolean,
    private val markSeen: () -> Unit,
) {
    fun shouldShow(canClaim: Boolean): Boolean = canClaim && !readSeen()
    fun onShown() = markSeen()
}
