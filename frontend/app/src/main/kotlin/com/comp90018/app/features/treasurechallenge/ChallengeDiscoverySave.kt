package com.comp90018.app.features.treasurechallenge

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

enum class DiscoverySaveStatus { WAITING, SAVING, SAVED, FAILED }

fun canOpenTreasureReveal(challengeCompleted: Boolean, saveStatus: DiscoverySaveStatus): Boolean =
    challengeCompleted && saveStatus == DiscoverySaveStatus.SAVED

/** Coordinates one save attempt after sensor completion, without knowing a treasure ID or Firebase. */
class ChallengeDiscoverySave {
    var status by mutableStateOf(DiscoverySaveStatus.WAITING)
        private set
    var error by mutableStateOf<String?>(null)
        private set

    fun onChallengeCompleted(save: ((String?) -> Unit) -> Unit) {
        if (status != DiscoverySaveStatus.WAITING) return
        attempt(save)
    }

    fun retry(save: ((String?) -> Unit) -> Unit) {
        if (status != DiscoverySaveStatus.FAILED) return
        attempt(save)
    }

    private fun attempt(save: ((String?) -> Unit) -> Unit) {
        status = DiscoverySaveStatus.SAVING
        error = null
        save { result ->
            if (status != DiscoverySaveStatus.SAVING) return@save
            error = result
            status = if (result == null) DiscoverySaveStatus.SAVED else DiscoverySaveStatus.FAILED
        }
    }
}
