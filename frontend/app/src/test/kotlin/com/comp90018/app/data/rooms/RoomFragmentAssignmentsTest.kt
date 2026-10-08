package com.comp90018.app.data.rooms

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RoomFragmentAssignmentsTest {
    @Test fun persistedAssignmentsEnforceOwnershipAndCompletion() {
        val assignment = RoomFragmentAssignments.create(listOf("a", "b", "c", "d"))
        val ids = assignment.ownerByFragment.keys.toList()
        assertTrue(assignment.canCollect("a", ids[0], emptySet()))
        assertFalse(assignment.canCollect("a", ids[2], emptySet()))
        assertFalse(assignment.canCollect("a", ids[0], setOf(ids[0])))
        assertFalse(assignment.isComplete(ids.take(2).toSet()))
        assertTrue(assignment.isComplete(ids.toSet()))
    }

    @Test fun persistedSharedFragmentIsAvailableOnlyToParticipants() {
        val assignment = RoomFragmentAssignments.create(listOf("a", "b", "c"))
        val shared = assignment.ownerByFragment.entries.single { it.value.isEmpty() }.key
        assertTrue(assignment.canCollect("c", shared, emptySet()))
        assertFalse(assignment.canCollect("outsider", shared, emptySet()))
        assertFalse(assignment.canCollect("a", "unknown", emptySet()))
    }
    @Test fun allSupportedSizesRoundTrip() {
        for (size in 2..4) {
            val assignment = RoomFragmentAssignments.create((1..size).map { "member$it" })
            val fields = assignment.toFields()
            assertEquals(assignment, RoomFragmentAssignments.fromFields(
                fields["huntParticipantIds"], fields["fragmentOwnerIds"],
            ))
        }
    }

    @Test fun sharedFragmentUsesAnEmptyString() {
        assertEquals(1, RoomFragmentAssignments.create(listOf("a", "b", "c"))
            .ownerByFragment.values.count { it.isEmpty() })
    }

    @Test fun changingTheOriginalMemberListDoesNotChangeTheSnapshot() {
        val members = mutableListOf("a", "b", "c", "d")
        val assignment = RoomFragmentAssignments.create(members)
        members.removeAt(0)
        assertEquals(listOf("a", "b", "c", "d"), assignment.participantIds)
    }

    @Test fun legacyAndMalformedDataAreNotTreatedAsValidAssignments() {
        assertNull(RoomFragmentAssignments.fromFields(null, null))
        assertNull(RoomFragmentAssignments.fromFields(listOf("a", "a"), emptyMap<String, String>()))
        assertNull(RoomFragmentAssignments.fromFields(listOf("a", 3), emptyMap<String, String>()))
        val assignment = RoomFragmentAssignments.create(listOf("a", "b"))
        assertNull(RoomFragmentAssignments.fromFields(assignment.participantIds,
            assignment.ownerByFragment + (assignment.ownerByFragment.keys.first() to "outsider")))
        assertNull(RoomFragmentAssignments.fromFields(assignment.participantIds,
            assignment.ownerByFragment + ("unknown" to "a")))
    }
}
