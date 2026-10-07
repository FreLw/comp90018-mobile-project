package com.comp90018.app.features.map

import com.comp90018.app.data.rooms.TeamRoom
import com.comp90018.app.data.rooms.TeamHuntLocation
import com.comp90018.app.sensors.SensorValidity
import com.comp90018.app.sensors.location.GeoCoordinate
import com.comp90018.app.sensors.location.LocationOutput
import com.comp90018.app.sensors.location.LocationPermissionState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TeamHuntLocationPolicyTest {
    private val now = 100_000L
    private val coordinate = GeoCoordinate(-37.798, 144.960)
    private val room = TeamRoom("room", "host", listOf("host", "member"), "treasure", "Treasure", "hunting", huntSessionId = "session")
    private fun fix(uid: String) = TeamHuntLocation(uid, "session", coordinate, now, now)
    private fun visible(room: TeamRoom? = this.room, uid: String = "host", fixes: List<TeamHuntLocation> = listOf(fix("host"), fix("member"))) =
        TeamHuntLocationPolicy.visibleTeammates(room, uid, fixes, now)

    @Test fun bothHostAndMemberCanSeeEachOtherDuringHunt() {
        assertEquals(listOf(fix("member")), visible())
        assertEquals(listOf(fix("host")), visible(uid = "member"))
    }

    @Test fun noSharingBeforeStartOrAfterEndOrOutsideRoom() {
        for (status in listOf("assigned", "unassigned", "completed")) {
            assertTrue(visible(room.copy(taskStatus = status)).isEmpty())
        }
        assertTrue(visible(room = null).isEmpty())
        assertTrue(visible(uid = "outsider").isEmpty())
        assertTrue(visible(room.copy(memberIds = listOf("host"))).isEmpty())
        assertTrue(visible(room.copy(huntSessionId = "")).isEmpty())
        assertTrue(visible(room.copy(memberIds = listOf("member", "other")), uid = "member").isEmpty())
    }

    @Test fun offlineHostOrLocalUserHidesAllSharedMarkers() {
        assertTrue(visible(uid = "member", fixes = listOf(fix("member"))).isEmpty())
        assertTrue(visible(fixes = listOf(fix("member"))).isEmpty())
        assertTrue(visible(fixes = listOf(fix("host"))).isEmpty())
    }

    @Test fun expiredHeartbeatAndOldSessionCannotShowOnline() {
        for (member in listOf(
            fix("member").copy(updatedAtMillis = now - 30_001),
            fix("member").copy(observedAtMillis = now - 30_001),
            fix("member").copy(sessionId = "old-session"),
            fix("member").copy(updatedAtMillis = now + 5_001),
        )) assertTrue(visible(fixes = listOf(fix("host"), member)).isEmpty())
        assertTrue(visible(room.copy(huntSessionId = "next-session")).isEmpty())
    }

    @Test fun removedMembersAndInvalidCoordinatesAreHidden() {
        assertEquals(listOf(fix("member")), visible(fixes = listOf(fix("host"), fix("member"), fix("outsider"))))
        assertTrue(visible(fixes = listOf(fix("host"), fix("member").copy(coordinate = GeoCoordinate(Double.NaN, 144.0)))).isEmpty())
        assertFalse(TeamHuntLocationPolicy.validCoordinate(GeoCoordinate(-91.0, 0.0)))
        assertFalse(TeamHuntLocationPolicy.validCoordinate(GeoCoordinate(0.0, 181.0)))
    }

    @Test fun onlyFreshPermissionGrantedGpsCanBePublished() {
        val nanos = 100_000_000_000L
        val location = LocationOutput(currentLocation = coordinate, validity = SensorValidity.VALID,
            permission = LocationPermissionState.PRECISE, timestampNanos = nanos)
        assertEquals(coordinate, TeamHuntLocationPolicy.liveCoordinate(location, nanos))
        assertNull(TeamHuntLocationPolicy.liveCoordinate(location.copy(currentLocation = null), nanos))
        assertNull(TeamHuntLocationPolicy.liveCoordinate(location.copy(validity = SensorValidity.UNRELIABLE), nanos))
        assertNull(TeamHuntLocationPolicy.liveCoordinate(location.copy(permission = LocationPermissionState.UNKNOWN), nanos))
        assertNull(TeamHuntLocationPolicy.liveCoordinate(location.copy(timestampNanos = null), nanos))
        assertNull(TeamHuntLocationPolicy.liveCoordinate(location, nanos + 30_001_000_000L))
        assertNull(TeamHuntLocationPolicy.liveCoordinate(location, nanos - 1))
    }
}
