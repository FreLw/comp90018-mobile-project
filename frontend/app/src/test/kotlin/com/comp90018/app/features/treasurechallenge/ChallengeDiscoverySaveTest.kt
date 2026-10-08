package com.comp90018.app.features.treasurechallenge

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ChallengeDiscoverySaveTest {
    @Test fun incompleteChallengeDoesNotRequestPersistence() {
        val save = ChallengeDiscoverySave()
        assertEquals(DiscoverySaveStatus.WAITING, save.status)
        assertEquals(false, canOpenTreasureReveal(false))
    }

    @Test fun completedChallengeRequestsOneSaveDespiteRepeatedSignals() {
        val save = ChallengeDiscoverySave()
        var calls = 0
        var finish: ((String?) -> Unit)? = null
        val persist: ((String?) -> Unit) -> Unit = { callback -> calls++; finish = callback }
        save.onChallengeCompleted(persist)
        save.onChallengeCompleted(persist)
        assertEquals(1, calls)
        assertEquals(DiscoverySaveStatus.SAVING, save.status)
        assertEquals(true, canOpenTreasureReveal(true))
        finish?.invoke(null)
        save.onChallengeCompleted(persist)
        assertEquals(1, calls)
        assertEquals(DiscoverySaveStatus.SAVED, save.status)
        assertEquals(true, canOpenTreasureReveal(true))
        assertEquals(false, canOpenTreasureReveal(false))
        assertNull(save.error)
    }

    @Test fun failureAllowsRetryWithoutResettingCompletion() {
        val save = ChallengeDiscoverySave()
        var calls = 0
        val persist: ((String?) -> Unit) -> Unit = { callback ->
            calls++
            callback(if (calls == 1) "network unavailable" else null)
        }
        save.onChallengeCompleted(persist)
        assertEquals(DiscoverySaveStatus.FAILED, save.status)
        assertEquals(true, canOpenTreasureReveal(true))
        assertEquals("network unavailable", save.error)
        save.onChallengeCompleted(persist)
        assertEquals(1, calls)
        save.retry(persist)
        assertEquals(2, calls)
        assertEquals(DiscoverySaveStatus.SAVED, save.status)
        assertEquals(true, canOpenTreasureReveal(true))
    }

    @Test fun revealIsNotifiedOnceBeforeAnUnresolvedSaveAndNeverAgainOnRetry() {
        val save = ChallengeDiscoverySave()
        val order = mutableListOf<String>()
        var finish: ((String?) -> Unit)? = null
        val persist: ((String?) -> Unit) -> Unit = { done -> order += "save"; finish = done }
        val reveal = { order.add("reveal"); Unit }
        save.onChallengeCompleted(persist, reveal)
        save.onChallengeCompleted(persist, reveal)
        assertEquals(listOf("reveal", "save"), order)
        assertEquals(DiscoverySaveStatus.SAVING, save.status)
        requireNotNull(finish)("offline")
        save.retry(persist)
        requireNotNull(finish)(null)
        requireNotNull(finish)(null)
        assertEquals(listOf("reveal", "save", "save"), order)
        assertEquals(DiscoverySaveStatus.SAVED, save.status)
    }

    @Test fun staleCallbackFromTheFailedAttemptCannotFinishTheRetry() {
        val save = ChallengeDiscoverySave()
        val callbacks = mutableListOf<(String?) -> Unit>()
        val persist: ((String?) -> Unit) -> Unit = { callbacks += it }
        save.onChallengeCompleted(persist)
        callbacks[0]("offline")
        save.retry(persist)
        callbacks[0](null)
        assertEquals(DiscoverySaveStatus.SAVING, save.status)
        callbacks[1](null)
        assertEquals(DiscoverySaveStatus.SAVED, save.status)
    }
}
