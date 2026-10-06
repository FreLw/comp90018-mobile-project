package com.comp90018.app.features.haptics

import com.comp90018.app.data.social.Subscription
import com.comp90018.app.data.treasure.TreasureCollectionRepository
import com.comp90018.app.features.treasure.TreasureCollectionViewModel
import org.junit.Assert.*
import org.junit.Test

/** Exercises the real collection ViewModel and haptic save adapter together. */
class HapticCollectionIntegrationTest {
    private val events = mutableListOf<TreasureHapticEvent>()
    private val session = TreasureHapticSessionViewModel(events::add).apply {
        setPreference(true); setForeground(true)
    }
    private val repository = DelayedCollectionRepository()
    private val collection = TreasureCollectionViewModel(repository, "user")
    private val results = mutableListOf<String?>()

    private fun collect() = TreasureHapticSave.collect("a", false, session.controller,
        collection::addDiscoveredTreasure, results::add)

    @Test fun abandoningChallengeSuppressesLateFeedbackButPreservesCollectionCompletion() {
        collect()
        assertEquals("a", collection.uiState.value.savingTreasureId)
        session.controller.abandonTreasure("map", "a")
        repository.finish(null)
        assertTrue(events.isEmpty())
        assertEquals(listOf<String?>(null), results)
        assertNull(collection.uiState.value.savingTreasureId)
        assertEquals(setOf("a"), collection.uiState.value.discoveredIds)
    }

    @Test fun failedSaveCanRetryAndDuplicateConfirmationCannotRepeatFeedback() {
        collect(); repository.finish("failed")
        assertTrue(events.isEmpty())
        assertEquals("failed", collection.uiState.value.error)
        collect(); repository.finish(null); repository.finish(null)
        assertEquals(listOf(TreasureHapticEvent.Unlocked("a")), events)
        assertEquals(listOf("failed", null, null), results)
    }

    @Test fun signOutInvalidatesDelayedSaveEvenAfterSameUserStartsNewSession() {
        collect()
        session.endSession(); session.beginSession()
        session.setPreference(true); session.setForeground(true)
        repository.finish(null)
        assertTrue(events.isEmpty())
        assertEquals(setOf("a"), collection.uiState.value.discoveredIds)
    }

    @Test fun rotationRetainsAttemptButLeavingMapInvalidatesIt() {
        collect()
        session.detach(changingConfigurations = true)
        session.beginSession(); session.setForeground(true)
        repository.finish(null)
        assertEquals(listOf(TreasureHapticEvent.Unlocked("a")), events)
        val attempt = session.controller.beginAttempt("b")
        session.controller.abandonOwner("map")
        session.controller.completeAttempt(attempt, null)
        assertEquals(1, events.size)
    }

    private class DelayedCollectionRepository : TreasureCollectionRepository {
        private var observer: ((Set<String>, String?) -> Unit)? = null
        private var callback: ((String?) -> Unit)? = null
        override fun observeDiscoveredTreasureIds(userId: String, onChange: (Set<String>, String?) -> Unit): Subscription {
            observer = onChange; onChange(emptySet(), null)
            return Subscription { observer = null }
        }
        override fun addDiscoveredTreasure(userId: String, treasureId: String, onComplete: (String?) -> Unit) {
            assertEquals("user", userId); assertEquals("a", treasureId)
            callback = onComplete
        }
        fun finish(error: String?) {
            if (error == null) observer?.invoke(setOf("a"), null)
            requireNotNull(callback)(error)
        }
    }
}
