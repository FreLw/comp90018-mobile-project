package com.comp90018.app.features.haptics

import com.comp90018.app.data.rooms.TeamRoom
import com.comp90018.app.features.rooms.TeamRoomChatViewModel

/** Both UI entry points decorate the same existing confirmed transaction with this coordinator. */
class ConfirmedTeamClaimHaptics(private val controller: TreasureHapticController) {
    private data class Run(val target: String, val sessionId: String, val active: Boolean, val generation: Long)
    private val runs = mutableMapOf<String, Run>()
    private val pending = mutableMapOf<String, MutableSet<TreasureHapticAttempt>>()

    /** One authoritative room subscription in AppShell; other UI snapshots must not advance epochs. */
    fun observeRoom(room: TeamRoom?) {
        if (room == null) return
        val old = runs[room.id]
        val active = room.taskStatus == "hunting" && room.taskId.isNotBlank()
        val newRun = old != null && active && (!old.active || old.target != room.taskId || old.sessionId != room.huntSessionId)
        if (newRun) pending.remove(room.id)?.forEach(controller::abandonAttempt)
        runs[room.id] = Run(room.taskId, room.huntSessionId, active, (old?.generation ?: 0L) + if (old == null || newRun) 1L else 0L)
        // Do not invalidate on an unassigned snapshot: the final successful claim clears the
        // room before its transaction callback may arrive. The captured generation stays valid.
    }

    fun claim(
        viewModel: TeamRoomChatViewModel,
        userId: String,
        mapAttempt: TreasureHapticAttempt? = null,
        onComplete: (String?) -> Unit = {},
    ) {
        val room = viewModel.uiState.value.room
        if (room == null) {
            viewModel.claimCompletedHuntTreasureWithConfirmation { error ->
                if (mapAttempt != null) controller.completeAttempt(mapAttempt, error, alreadyDiscovered = true)
                onComplete(error)
            }
            return
        }
        // Bootstrap only if the authoritative observer has not delivered this room yet.
        if (room.id !in runs) observeRoom(room)
        val generation = requireNotNull(runs[room.id]).generation
        val completionId = "team:${room.id}:$generation:$userId:${room.taskId}"
        val attempt = mapAttempt ?: controller.beginAttempt(room.taskId,
            owner = "rooms:${room.id}", completionId = completionId)
        pending.getOrPut(room.id) { mutableSetOf() }.add(attempt)
        val alreadyClaimed = userId in room.taskClaimedMemberIds || room.taskStatus != "hunting"
        viewModel.claimCompletedHuntTreasureWithConfirmation { error ->
            pending[room.id]?.remove(attempt)
            controller.completeAttempt(attempt, error, alreadyClaimed, completionId)
            onComplete(error)
        }
    }
}
