package com.comp90018.app.features.map

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.comp90018.app.features.treasurechallenge.ChallengeDiscoverySave

enum class PostChallengePage { PHOTO_HISTORY, TREASURE, STORY }

/** Opens on task completion while the collection save continues independently. */
class PostChallengeRevealSession(
    val relic: MapRelic,
    val capturedPhotoUri: String?,
    val discoverySave: ChallengeDiscoverySave? = null,
    val onRetrySave: (() -> Unit)? = null,
) {
    // Completed tasks open the normal treasure reveal immediately, including photo tasks.
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

/** Map-owned handoff; duplicate completion callbacks keep the original photo and session. */
class PostChallengeRevealCoordinator {
    var session by mutableStateOf<PostChallengeRevealSession?>(null)
        private set

    fun openOnCompletion(
        relic: MapRelic, capturedPhotoUri: String?,
        discoverySave: ChallengeDiscoverySave? = null, onRetrySave: (() -> Unit)? = null,
    ) {
        if (session == null) session = PostChallengeRevealSession(relic, capturedPhotoUri, discoverySave, onRetrySave)
    }

    fun clear() { session = null }
}
