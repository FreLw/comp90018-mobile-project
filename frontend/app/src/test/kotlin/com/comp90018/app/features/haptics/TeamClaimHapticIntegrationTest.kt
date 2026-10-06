package com.comp90018.app.features.haptics

import com.comp90018.app.data.rooms.TeamRoom
import com.comp90018.app.data.rooms.TeamRoomRepository
import com.comp90018.app.data.social.Subscription
import com.comp90018.app.features.rooms.TeamRoomChatViewModel
import java.lang.reflect.Proxy
import org.junit.Assert.*
import org.junit.Test

/** Real Map save adapter / Rooms claim entry contract -> real ViewModel -> delayed repository. */
class TeamClaimHapticIntegrationTest {
    private val events = mutableListOf<TreasureHapticEvent>()
    private val controller = TreasureHapticController(events::add).apply { enabled = true; foreground = true }
    private val coordinator = ConfirmedTeamClaimHaptics(controller)
    private val fixture = RoomFixture()
    private val map = fixture.model()
    private val rooms = fixture.model() // mirrors the existing separate AppShell and Rooms ViewModels

    @Test fun mapSaveAndRoomsClaimShareOneConfirmedCompletionIdentity() {
        coordinator.observeRoom(fixture.room)
        var save: ((String?) -> Unit)? = null
        var uiCompletions = 0
        TreasureHapticSave.collectTeam("a", controller,
            { _, callback -> save = callback },
            { attempt, complete -> coordinator.claim(map, "user", attempt, complete) },
            { uiCompletions++ })
        assertTrue(fixture.claims.isEmpty())
        requireNotNull(save)(null)
        requireNotNull(save)(null) // never dispatch a second transaction for duplicate save callbacks
        assertEquals(1, fixture.claims.size)
        assertEquals(2, uiCompletions) // original collection callback semantics retained
        coordinator.claim(rooms, "user") // stale UI snapshot observes/starts the same logical claim
        assertTrue(events.isEmpty())
        fixture.claims[0](null); fixture.claims[1](null); fixture.claims[0](null)
        assertEquals(listOf(TreasureHapticEvent.Unlocked("a")), events)
        assertFalse(map.uiState.value.updatingTask)
        assertFalse(rooms.uiState.value.updatingTask)
    }

    @Test fun failuresAndIncompleteClaimsNeverNotifyAndSuccessfulRetryWorks() {
        coordinator.observeRoom(fixture.room)
        coordinator.claim(rooms, "user")
        fixture.claims[0]("All explorers must complete the task")
        assertTrue(events.isEmpty())
        assertEquals("All explorers must complete the task", rooms.uiState.value.error)
        coordinator.claim(rooms, "user")
        fixture.claims[1](null)
        assertEquals(listOf(TreasureHapticEvent.Unlocked("a")), events)
        assertNull(rooms.uiState.value.error)
        // The original no-argument API remains functional and keeps its existing error handling.
        rooms.claimCompletedHuntTreasure()
        fixture.claims[2]("denied")
        assertEquals("denied", rooms.uiState.value.error)
        assertEquals(1, events.size)
    }

    @Test fun finalRoomResetBeforeTransactionCallbackDoesNotLoseSuccess() {
        coordinator.observeRoom(fixture.room)
        coordinator.claim(rooms, "user")
        fixture.emit(fixture.room.copy(taskId = "", taskStatus = "unassigned"))
        coordinator.observeRoom(fixture.room)
        fixture.claims[0](null)
        assertEquals(listOf(TreasureHapticEvent.Unlocked("a")), events)
    }

    @Test fun abandonedMapSaveStillClaimsButCannotTriggerFeedback() {
        coordinator.observeRoom(fixture.room)
        var save: ((String?) -> Unit)? = null
        TreasureHapticSave.collectTeam("a", controller, { _, complete -> save = complete },
            { attempt, complete -> coordinator.claim(map, "user", attempt, complete) }, {})
        controller.abandonOwner("map")
        requireNotNull(save)(null)
        assertEquals(1, fixture.claims.size) // persistence / claim operation is preserved
        fixture.claims[0](null)
        assertTrue(events.isEmpty())
    }

    @Test fun newHuntOfTheSameTreasureNotifiesAgainButProximityRemainsOneShot() {
        coordinator.observeRoom(fixture.room)
        controller.nearby("a", 20.0)
        coordinator.claim(rooms, "user"); fixture.claims[0](null)
        fixture.emit(fixture.room.copy(taskId = "", taskStatus = "unassigned"))
        coordinator.observeRoom(fixture.room)
        fixture.emit(fixture.room.copy(taskId = "a", taskStatus = "hunting", taskClaimedMemberIds = emptyList()))
        coordinator.observeRoom(fixture.room)
        coordinator.claim(rooms, "user"); fixture.claims[1](null)
        controller.nearby("a", 20.0)
        assertEquals(listOf(TreasureHapticEvent.Nearby("a"),
            TreasureHapticEvent.Unlocked("a"), TreasureHapticEvent.Unlocked("a")), events)
    }

    @Test fun newRoomRunInvalidatesOldClaimCallbackWithoutCancellingItsTransaction() {
        coordinator.observeRoom(fixture.room)
        coordinator.claim(rooms, "user")
        fixture.emit(fixture.room.copy(taskStatus = "assigned"))
        coordinator.observeRoom(fixture.room)
        fixture.emit(fixture.room.copy(taskStatus = "hunting"))
        coordinator.observeRoom(fixture.room)
        fixture.claims[0](null)
        assertTrue(events.isEmpty())
        assertFalse(rooms.uiState.value.updatingTask)
        coordinator.claim(rooms, "user"); fixture.claims[1](null)
        assertEquals(listOf(TreasureHapticEvent.Unlocked("a")), events)
    }

    @Test fun disabledConfirmedClaimIsNotReplayedWhenSettingsBecomeKnown() {
        coordinator.observeRoom(fixture.room)
        controller.enabled = false
        coordinator.claim(rooms, "user"); fixture.claims[0](null)
        controller.enabled = true
        coordinator.claim(rooms, "user"); fixture.claims[1](null)
        assertTrue(events.isEmpty())
    }

    internal class RoomFixture {
        var room = TeamRoom("room", "user", listOf("user", "friend"), "a", "A", "hunting",
            taskCompletedMemberIds = listOf("user", "friend"))
        val claims = mutableListOf<(String?) -> Unit>()
        private val observers = mutableListOf<(TeamRoom?, String?) -> Unit>()
        private val repository = Proxy.newProxyInstance(TeamRoomRepository::class.java.classLoader,
            arrayOf(TeamRoomRepository::class.java)) { _, method, args ->
            when (method.name) {
                "observeRoom" -> {
                    @Suppress("UNCHECKED_CAST")
                    val observer = args[1] as (TeamRoom?, String?) -> Unit
                    observers.add(observer); observer(room, null)
                    Subscription { observers.remove(observer) }
                }
                "observeMessages" -> Subscription {}
                "loadMembers" -> null // member profile loading is irrelevant to claim confirmation
                "claimCompletedHuntTreasure" -> {
                    assertEquals("room", args[0]); assertEquals("user", args[1])
                    @Suppress("UNCHECKED_CAST")
                    claims.add(args[2] as (String?) -> Unit)
                    null
                }
                else -> error("Unexpected operation: ${method.name}")
            }
        } as TeamRoomRepository
        fun model() = TeamRoomChatViewModel(repository, "room", "user")
        fun emit(next: TeamRoom) { room = next; observers.toList().forEach { it(room, null) } }
    }
}
