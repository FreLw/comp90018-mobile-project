package com.comp90018.app.features.rooms

import com.comp90018.app.data.rooms.TeamRoomRepository
import com.comp90018.app.data.social.Subscription
import java.lang.reflect.Proxy
import org.junit.Assert.*
import org.junit.Test

class TeamHuntClaimConfirmationTest {
    @Test fun forwardsExistingTransactionResultAndPreservesBusyGuardAndRoomErrors() {
        var callback: ((String?) -> Unit)? = null
        var claims = 0
        val repository = Proxy.newProxyInstance(TeamRoomRepository::class.java.classLoader,
            arrayOf(TeamRoomRepository::class.java)) { _, method, args ->
            when (method.name) {
                "observeRoom", "observeMessages" -> Subscription {}
                "claimCompletedHuntTreasure" -> {
                    claims++
                    assertEquals("room", args[0]); assertEquals("user", args[1])
                    @Suppress("UNCHECKED_CAST")
                    callback = args[2] as (String?) -> Unit
                    null
                }
                else -> error("Unexpected repository operation: ${method.name}")
            }
        } as TeamRoomRepository
        val model = TeamRoomChatViewModel(repository, "room", "user")
        val results = mutableListOf<String?>()
        model.claimCompletedHuntTreasureWithConfirmation(results::add)
        assertTrue(model.uiState.value.updatingTask)
        model.claimCompletedHuntTreasure() // original callers keep the same busy guard
        assertEquals(1, claims)
        requireNotNull(callback)("claim failed")
        assertFalse(model.uiState.value.updatingTask)
        assertEquals("claim failed", model.uiState.value.error)
        assertEquals(listOf("claim failed"), results)
        model.claimCompletedHuntTreasureWithConfirmation(results::add)
        requireNotNull(callback)(null)
        assertNull(model.uiState.value.error)
        assertEquals(listOf("claim failed", null), results)
    }
}
