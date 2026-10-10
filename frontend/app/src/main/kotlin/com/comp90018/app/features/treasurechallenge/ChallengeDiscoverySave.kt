package com.comp90018.app.features.treasurechallenge

/*
 * Tracks one asynchronous collection save after a task completes.
 * Discovery navigation and completion feedback begin immediately; failed persistence remains retryable.
 */

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

enum class DiscoverySaveStatus { WAITING, SAVING, SAVED, FAILED }

fun canOpenTreasureReveal(challengeCompleted: Boolean): Boolean = challengeCompleted

/** Coordinates one save attempt after sensor completion, without knowing a treasure ID or Firebase. */
class ChallengeDiscoverySave {
    private var attemptGeneration = 0
    var status by mutableStateOf(DiscoverySaveStatus.WAITING)
        private set
    var error by mutableStateOf<String?>(null)
        private set

    fun onChallengeCompleted(save: ((String?) -> Unit) -> Unit) = onChallengeCompleted(save, {})

    fun onChallengeCompleted(save: ((String?) -> Unit) -> Unit, onReady: () -> Unit) {
        if (status != DiscoverySaveStatus.WAITING) return
        attempt(save, onReady)
    }

    fun retry(save: ((String?) -> Unit) -> Unit) {
        if (status != DiscoverySaveStatus.FAILED) return
        attempt(save)
    }

    private fun attempt(save: ((String?) -> Unit) -> Unit, onReady: () -> Unit = {}) {
        val generation = ++attemptGeneration
        status = DiscoverySaveStatus.SAVING
        error = null
        // Reveal and completion feedback are immediate; collection persistence continues here.
        onReady()
        save { result ->
            if (generation != attemptGeneration || status != DiscoverySaveStatus.SAVING) return@save
            error = result
            status = if (result == null) DiscoverySaveStatus.SAVED else DiscoverySaveStatus.FAILED
        }
    }
}
