package com.comp90018.app.features.map

import com.comp90018.app.data.rooms.TeamRoom
import com.comp90018.app.data.rooms.TeamHuntLocation
import com.comp90018.app.sensors.location.GeoCoordinate
import com.comp90018.app.sensors.location.LocationOutput

internal object TeamHuntLocationPolicy {
    const val HEARTBEAT_MILLIS = 10_000L
    const val EXPIRY_MILLIS = 30_000L

    fun canShare(room: TeamRoom?, uid: String): Boolean = room != null &&
        room.taskStatus == "hunting" && room.huntSessionId.isNotBlank() &&
        room.memberIds.size >= 2 && uid in room.memberIds && room.creatorId in room.memberIds

    fun liveCoordinate(location: LocationOutput, nowNanos: Long): GeoCoordinate? {
        val timestamp = location.timestampNanos ?: return null
        if (nowNanos - timestamp !in 0..EXPIRY_MILLIS * 1_000_000L) return null
        return LocationActionPolicy.actionableCoordinate(location)?.takeIf(::validCoordinate)
    }

    fun validCoordinate(coordinate: GeoCoordinate): Boolean =
        coordinate.latitude.isFinite() && coordinate.latitude in -90.0..90.0 &&
            coordinate.longitude.isFinite() && coordinate.longitude in -180.0..180.0

    fun visibleTeammates(
        room: TeamRoom?, uid: String, locations: List<TeamHuntLocation>, nowMillis: Long,
    ): List<TeamHuntLocation> {
        if (!canShare(room, uid)) return emptyList()
        val activeRoom = requireNotNull(room)
        val online = locations.filter {
            it.uid in activeRoom.memberIds && it.sessionId == activeRoom.huntSessionId &&
                validCoordinate(it.coordinate) &&
                nowMillis - it.updatedAtMillis in -5_000L..EXPIRY_MILLIS &&
                nowMillis - it.observedAtMillis in -5_000L..EXPIRY_MILLIS
        }.distinctBy { it.uid }
        // A member cannot see shared positions while the hunt owner is offline.
        if (online.none { it.uid == uid } || online.none { it.uid == activeRoom.creatorId }) return emptyList()
        return online.filter { it.uid != uid }
    }
}
