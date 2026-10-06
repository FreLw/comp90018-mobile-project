package com.comp90018.app.features.map

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

enum class PostChallengePage { PHOTO_HISTORY, TREASURE, STORY }

/** Created only after discovery persistence succeeds. Contains no save action. */
class PostChallengeRevealSession(
    val relic: MapRelic,
    val capturedPhotoUri: String?,
) {
    // Simplified photo tasks open the normal treasure reveal immediately after a confirmed save.
    // The reusable historical-image panel remains available but is not part of task completion.
    var page by mutableStateOf(PostChallengePage.TREASURE)
        private set
    var historicalShown by mutableStateOf(false)
        private set

    val hasHistoricalImage: Boolean
        get() = relic.historicalImageUrl.startsWith("https://") || relic.historicalImageResId != null

    fun showHistorical() { historicalShown = true }
    fun continueToTreasure() { page = PostChallengePage.TREASURE }
    fun viewStory() { page = PostChallengePage.STORY }
    fun backToTreasure() { page = PostChallengePage.TREASURE }
}

/** Map-owned handoff; duplicate successful callbacks keep the original photo and session. */
class PostChallengeRevealCoordinator {
    var session by mutableStateOf<PostChallengeRevealSession?>(null)
        private set

    fun openAfterSave(relic: MapRelic, capturedPhotoUri: String?) {
        if (session == null) session = PostChallengeRevealSession(relic, capturedPhotoUri)
    }

    fun clear() { session = null }
}
