package com.comp90018.app.features.map

import com.comp90018.app.data.rooms.RoomFragmentAssignments
import org.junit.Assert.*
import org.junit.Test

class FragmentOwnershipUiTest {
    private val ids = SouthLawnFragmentIds.toList()

    @Test fun debugCollectsOwnFragmentBeforeSharedEvenWhenSharedIsListedFirst() {
        val assignment = RoomFragmentAssignments.create(listOf("a", "b", "c"))
        val reordered = listOf(ids[3], ids[0], ids[1], ids[2])
        assertEquals(ids[0], FragmentOwnershipUi.nextDebugFragment(assignment, reordered, "a", emptySet()))
        assertEquals(ids[3], FragmentOwnershipUi.nextDebugFragment(assignment, reordered, "a", setOf(ids[0])))
        assertNull(FragmentOwnershipUi.nextDebugFragment(assignment, reordered, "a", setOf(ids[0], ids[3])))
    }

    @Test fun labelsDistinguishOwnOtherAndSharedFragments() {
        val assignment = RoomFragmentAssignments.create(listOf("a", "b", "c"))
        assertEquals("Your fragment", FragmentOwnershipUi.label(assignment, ids[0], "a", emptyMap()))
        assertEquals("Assigned to Bob", FragmentOwnershipUi.label(assignment, ids[1], "a", mapOf("b" to "Bob")))
        assertEquals("Shared", FragmentOwnershipUi.label(assignment, ids[3], "a", emptyMap()))
        assertEquals("Assigned to another explorer", FragmentOwnershipUi.label(assignment, ids[1], "a", emptyMap()))
    }

    @Test fun fourPlayerDebugCollectionCannotUnlockEveryoneElse() {
        val assignment = RoomFragmentAssignments.create(listOf("a", "b", "c", "d"))
        assertEquals(listOf(ids[0]), ids.filter { FragmentOwnershipUi.canCollect(assignment, it, "a", emptySet()) })
        assertFalse(FragmentOwnershipUi.canCollect(assignment, ids[0], "a", setOf(ids[0])))
        assertFalse(FragmentOwnershipUi.canCollect(assignment, ids[0], "outsider", emptySet()))
    }

    @Test fun oldRoomsRemainSharedButUnknownFragmentsAreNotCollectible() {
        assertEquals("Shared", FragmentOwnershipUi.label(null, ids[0], "a", emptyMap()))
        assertTrue(FragmentOwnershipUi.canCollect(null, ids[0], "a", emptySet()))
        assertFalse(FragmentOwnershipUi.canCollect(null, "unknown", "a", emptySet()))
    }
}
