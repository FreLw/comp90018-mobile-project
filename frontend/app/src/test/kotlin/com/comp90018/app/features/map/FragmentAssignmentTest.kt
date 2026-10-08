package com.comp90018.app.features.map

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FragmentAssignmentTest {
    private val ids = SouthLawnFragmentIds.toList()

    @Test fun twoMembersReceiveTwoFragmentsEach() {
        val assignment = FragmentAssignment.create(listOf("a", "b"))
        assertEquals(listOf("a", "b", "a", "b"), assignment.owners.values.toList())
    }

    @Test fun threeMembersReceiveOneEachAndOneShared() {
        val assignment = FragmentAssignment.create(listOf("a", "b", "c"))
        assertEquals(listOf("a", "b", "c", null), assignment.owners.values.toList())
        for (member in listOf("a", "b", "c")) {
            assertTrue(assignment.canCollect(member, ids[3], emptySet()))
        }
        assertFalse(assignment.canCollect("outsider", ids[3], emptySet()))
    }

    @Test fun fourMembersMustEachCollectTheirOwnFragment() {
        val members = listOf("a", "b", "c", "d")
        val assignment = FragmentAssignment.create(members)
        ids.forEachIndexed { index, id ->
            members.forEach { member ->
                assertEquals(member == members[index], assignment.canCollect(member, id, emptySet()))
            }
        }
        assertFalse(assignment.isComplete(ids.take(2).toSet()))
        assertTrue(assignment.isComplete(ids.toSet()))
    }

    @Test fun duplicateAndUnknownFragmentsCannotBeCollected() {
        val assignment = FragmentAssignment.create(listOf("a", "b"))
        assertFalse(assignment.canCollect("a", ids[0], setOf(ids[0])))
        assertFalse(assignment.canCollect("a", "unknown", emptySet()))
    }

    @Test fun assignmentIsRepeatableForTheSameMemberOrder() {
        val members = listOf("d", "a", "c")
        assertEquals(FragmentAssignment.create(members).owners, FragmentAssignment.create(members).owners)
    }

    @Test fun invalidMembershipIsRejected() {
        for (members in listOf(emptyList(), listOf("a"), listOf("a", "a"),
            listOf("a", " "), listOf("a", "b", "c", "d", "e"))) {
            var rejected = false
            try { FragmentAssignment.create(members) } catch (_: IllegalArgumentException) { rejected = true }
            assertTrue(rejected)
        }
    }
}
