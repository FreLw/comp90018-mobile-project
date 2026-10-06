package com.comp90018.app.features.map

/** Persistence is supplied by the map so acknowledgement survives navigation and restarts. */
internal class TeamHuntClaimPrompt(
    private val readSeen: () -> Boolean,
    private val markSeen: () -> Unit,
) {
    fun shouldShow(canClaim: Boolean): Boolean = canClaim && !readSeen()
    fun onShown() = markSeen()
}
