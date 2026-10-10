package com.comp90018.app.features.map

/*
 * Publishes and observes teammate positions during a foreground cooperative hunt.
 * Lives in the signed-in shell so shared locations remain available when the user changes tabs.
 */

import android.os.SystemClock
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.comp90018.app.data.rooms.TeamHuntLocation
import com.comp90018.app.data.rooms.TeamHuntLocationRepository
import com.comp90018.app.data.rooms.TeamRoom
import com.comp90018.app.sensors.location.LocationOutput
import kotlinx.coroutines.delay

/** Shares live GPS during a foreground team hunt, independently of the selected tab. */
@Composable
internal fun rememberTeamHuntLocations(
    repository: TeamHuntLocationRepository, room: TeamRoom?, uid: String, location: LocationOutput,
): List<TeamHuntLocation> {
    val activeRoom = room?.takeIf { TeamHuntLocationPolicy.canShare(it, uid) }
    val roomId = activeRoom?.id
    val sessionId = activeRoom?.huntSessionId
    val lifecycleOwner = LocalLifecycleOwner.current
    val latestLocation by rememberUpdatedState(location)
    val latestRoom by rememberUpdatedState(activeRoom)
    var visible by remember(roomId, sessionId, uid) { mutableStateOf(emptyList<TeamHuntLocation>()) }

    LaunchedEffect(repository, roomId, sessionId, uid, lifecycleOwner) {
        if (roomId == null || sessionId == null) return@LaunchedEffect
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            var confirmed = emptyList<TeamHuntLocation>()
            val subscription = repository.observe(roomId, sessionId) { positions, error ->
                confirmed = positions
                if (positions.isEmpty()) visible = emptyList()
                if (error != null) Log.w("TeamHuntLocations", "Unable to read shared positions: $error")
            }
            var writePending = false
            var lastPublishNanos = Long.MIN_VALUE
            var hasPublished = false
            fun clearPosition() {
                repository.clear(roomId, uid) { error ->
                    if (error != null) Log.w("TeamHuntLocations", "Unable to clear shared position: $error")
                }
            }
            try {
                while (true) {
                    val nowNanos = SystemClock.elapsedRealtimeNanos()
                    val current = TeamHuntLocationPolicy.liveCoordinate(latestLocation, nowNanos)
                    if (current == null) {
                        visible = emptyList()
                        if (hasPublished) {
                            clearPosition()
                            hasPublished = false
                        }
                        lastPublishNanos = Long.MIN_VALUE
                    } else {
                        if (!writePending && (lastPublishNanos == Long.MIN_VALUE ||
                                nowNanos - lastPublishNanos >= TeamHuntLocationPolicy.HEARTBEAT_MILLIS * 1_000_000L)) {
                            val fixAgeMillis = (nowNanos - requireNotNull(latestLocation.timestampNanos)) / 1_000_000L
                            writePending = true
                            repository.publish(roomId, sessionId, uid, current, System.currentTimeMillis() - fixAgeMillis) { error ->
                                writePending = false
                                if (error != null) Log.w("TeamHuntLocations", "Unable to publish position: $error")
                            }
                            hasPublished = true
                            lastPublishNanos = nowNanos
                        }
                        visible = TeamHuntLocationPolicy.visibleTeammates(latestRoom, uid, confirmed, System.currentTimeMillis())
                    }
                    delay(1_000L)
                }
            } finally {
                subscription.cancel()
                visible = emptyList()
                // Sudden network/process loss is handled by expiry on the other device.
                clearPosition()
            }
        }
    }
    return visible.takeIf { TeamHuntLocationPolicy.canShare(activeRoom, uid) }.orEmpty()
}
