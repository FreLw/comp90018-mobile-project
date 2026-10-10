package com.comp90018.app.features.rooms

import com.comp90018.app.features.rooms.hunt.ActiveHuntHeader

/*
 * Checks feedback for confirmed room treasure claims through the real UI entry point.
 * Run these device/Compose checks when changing the corresponding interface or interaction contract.
 */

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.comp90018.app.data.rooms.TeamRoom
import com.comp90018.app.data.rooms.TeamRoomRepository
import com.comp90018.app.data.social.Subscription
import com.comp90018.app.features.haptics.ConfirmedTeamClaimHaptics
import com.comp90018.app.features.haptics.TreasureHapticController
import com.comp90018.app.features.haptics.TreasureHapticEvent
import java.lang.reflect.Proxy
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class RoomClaimHapticUiTest {
    @get:Rule val rule = createComposeRule()

    @Test fun realRoomsClaimButtonWaitsForTransactionAndAllowsFailureRetry() {
        val room = TeamRoom("room", "user", listOf("user", "friend"), "a", "A", "hunting",
            taskCompletedMemberIds = listOf("user", "friend"))
        val callbacks = mutableListOf<(String?) -> Unit>()
        val repository = Proxy.newProxyInstance(TeamRoomRepository::class.java.classLoader,
            arrayOf(TeamRoomRepository::class.java)) { _, method, args ->
            when (method.name) {
                "observeRoom" -> {
                    @Suppress("UNCHECKED_CAST")
                    (args[1] as (TeamRoom?, String?) -> Unit)(room, null)
                    Subscription {}
                }
                "observeMessages" -> Subscription {}
                "loadMembers" -> null
                "claimCompletedHuntTreasure" -> {
                    @Suppress("UNCHECKED_CAST")
                    callbacks.add(args[2] as (String?) -> Unit)
                    null
                }
                else -> error("Unexpected repository method: ${method.name}")
            }
        } as TeamRoomRepository
        val model = TeamRoomChatViewModel(repository, "room", "user")
        val events = mutableListOf<TreasureHapticEvent>()
        val controller = TreasureHapticController(events::add).apply { enabled = true; foreground = true }
        val coordinator = ConfirmedTeamClaimHaptics(controller).apply { observeRoom(room) }
        rule.setContent {
            MaterialTheme {
                val state by model.uiState.collectAsState()
                ActiveHuntHeader(requireNotNull(state.room), "user", state.updatingTask,
                    onContinue = {}, onTerminate = {}, onClaimAtlas = { coordinator.claim(model, "user") })
            }
        }
        rule.onNodeWithText("Claim Treasure").performClick()
        rule.runOnIdle { assertTrue(events.isEmpty()); assertEquals(1, callbacks.size); callbacks[0]("denied") }
        rule.onNodeWithText("Claim Treasure").performClick()
        rule.runOnIdle {
            assertTrue(events.isEmpty()); assertEquals(2, callbacks.size)
            callbacks[1](null); callbacks[1](null)
            assertEquals(listOf(TreasureHapticEvent.Unlocked("a")), events)
            assertNull(model.uiState.value.error)
        }
    }
}
