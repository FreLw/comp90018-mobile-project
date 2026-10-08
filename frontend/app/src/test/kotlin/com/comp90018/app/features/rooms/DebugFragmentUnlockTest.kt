package com.comp90018.app.features.rooms

import com.comp90018.app.data.rooms.TeamRoomRepository
import com.comp90018.app.data.social.Subscription
import com.comp90018.app.features.map.SouthLawnFragmentIds
import java.lang.reflect.Proxy
import org.junit.Assert.*
import org.junit.Test

class DebugFragmentUnlockTest {
    @Test fun singleAssignedFragmentIsAccepted() {
        val stub = RepositoryStub()
        val model = TeamRoomChatViewModel(stub.repository, "room", "user")
        val id = SouthLawnFragmentIds.first()
        var completed = false
        model.debugUnlockHuntFragments(listOf(id)) { error -> assertNull(error); completed = true }
        assertEquals(listOf(id), stub.writes)
        stub.complete()
        assertTrue(completed)
        assertFalse(model.uiState.value.updatingTask)
    }

    @Test fun invalidSubsetsAreRejectedWithoutWriting() {
        val stub = RepositoryStub()
        val model = TeamRoomChatViewModel(stub.repository, "room", "user")
        val id = SouthLawnFragmentIds.first()
        for (ids in listOf(emptyList(), listOf("unknown"), listOf(id, id))) {
            model.debugUnlockHuntFragments(ids) { assertEquals("Invalid fragment configuration", it) }
        }
        assertTrue(stub.writes.isEmpty())
    }

    private class RepositoryStub {
        val writes = mutableListOf<String>()
        var pending: ((String?) -> Unit)? = null
        @Suppress("UNCHECKED_CAST")
        val repository = Proxy.newProxyInstance(
            TeamRoomRepository::class.java.classLoader,
            arrayOf(TeamRoomRepository::class.java),
        ) { _, method, args ->
            when (method.name) {
                "observeRoom", "observeMessages" -> Subscription {}
                "findHuntFragment" -> {
                    check(pending == null) { "Overlapping fragment writes" }
                    writes.add(args[2] as String)
                    pending = args[3] as (String?) -> Unit
                    Unit
                }
                else -> error("Unexpected repository call: ${method.name}")
            }
        } as TeamRoomRepository

        fun complete(error: String? = null) {
            val callback = requireNotNull(pending)
            pending = null
            callback(error)
        }
    }

    @Test fun unlockWaitsForEachWriteAndRejectsOverlappingRequests() {
        val stub = RepositoryStub()
        val model = TeamRoomChatViewModel(stub.repository, "room", "user")
        val ids = SouthLawnFragmentIds.toList()
        var completed = false
        model.debugUnlockHuntFragments(ids) { error -> assertNull(error); completed = true }
        assertEquals(listOf(ids.first()), stub.writes)
        assertTrue(model.uiState.value.updatingTask)
        model.debugUnlockHuntFragments(ids) { assertNotNull(it) }
        assertEquals(1, stub.writes.size)
        repeat(3) {
            stub.complete()
            assertFalse(completed)
        }
        stub.complete()
        assertEquals(ids, stub.writes)
        assertTrue(completed)
        assertFalse(model.uiState.value.updatingTask)
    }

    @Test fun saveFailureStopsTheSequenceAndAllowsRetry() {
        val stub = RepositoryStub()
        val model = TeamRoomChatViewModel(stub.repository, "room", "user")
        val ids = SouthLawnFragmentIds.toList()
        model.debugUnlockHuntFragments(ids) { assertEquals("Save failed", it) }
        stub.complete("Save failed")
        assertEquals(1, stub.writes.size)
        assertFalse(model.uiState.value.updatingTask)
        assertEquals("Save failed", model.uiState.value.error)
        model.debugUnlockHuntFragments(ids) {}
        assertEquals(2, stub.writes.size)
        assertTrue(model.uiState.value.updatingTask)
    }
}
