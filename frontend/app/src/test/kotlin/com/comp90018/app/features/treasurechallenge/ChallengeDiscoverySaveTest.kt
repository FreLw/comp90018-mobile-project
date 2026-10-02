package com.comp90018.app.features.treasurechallenge

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ChallengeDiscoverySaveTest {
    @Test fun incompleteChallengeDoesNotRequestPersistence() {
        val save = ChallengeDiscoverySave()
        assertEquals(DiscoverySaveStatus.WAITING, save.status)
        assertEquals(false, canOpenTreasureReveal(false, save.status))
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
        assertEquals(false, canOpenTreasureReveal(true, save.status))
        finish?.invoke(null)
        save.onChallengeCompleted(persist)
        assertEquals(1, calls)
        assertEquals(DiscoverySaveStatus.SAVED, save.status)
        assertEquals(true, canOpenTreasureReveal(true, save.status))
        assertEquals(false, canOpenTreasureReveal(false, save.status))
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
        assertEquals(false, canOpenTreasureReveal(true, save.status))
        assertEquals("network unavailable", save.error)
        save.onChallengeCompleted(persist)
        assertEquals(1, calls)
        save.retry(persist)
        assertEquals(2, calls)
        assertEquals(DiscoverySaveStatus.SAVED, save.status)
        assertEquals(true, canOpenTreasureReveal(true, save.status))
    }
}
