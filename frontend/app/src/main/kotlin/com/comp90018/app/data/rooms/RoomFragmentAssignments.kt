package com.comp90018.app.data.rooms

import com.comp90018.app.features.map.FragmentAssignment
import com.comp90018.app.features.map.SouthLawnFragmentIds

/** Persisted at hunt start; an empty owner ID represents a shared fragment. */
data class RoomFragmentAssignments(
    val participantIds: List<String>,
    val ownerByFragment: Map<String, String>,
) {
    fun canCollect(userId: String, fragmentId: String, foundIds: Set<String>): Boolean =
        userId in participantIds && fragmentId in ownerByFragment && fragmentId !in foundIds &&
            (ownerByFragment[fragmentId].isNullOrEmpty() || ownerByFragment[fragmentId] == userId)

    fun isComplete(foundIds: Set<String>): Boolean = foundIds.containsAll(SouthLawnFragmentIds)

    fun toFields(): Map<String, Any> = mapOf(
        "huntParticipantIds" to participantIds,
        "fragmentOwnerIds" to ownerByFragment,
    )

    companion object {
        fun create(memberIds: List<String>): RoomFragmentAssignments {
            val assignment = FragmentAssignment.create(memberIds)
            return RoomFragmentAssignments(memberIds.toList(), assignment.owners.mapValues { it.value.orEmpty() })
        }

        fun fromFields(participants: Any?, owners: Any?): RoomFragmentAssignments? {
            val rawMembers = participants as? List<*> ?: return null
            if (rawMembers.any { it !is String }) return null
            val members = rawMembers.filterIsInstance<String>()
            val expected = try { create(members) } catch (_: IllegalArgumentException) { return null }
            val rawOwners = owners as? Map<*, *> ?: return null
            if (rawOwners != expected.ownerByFragment) return null
            return expected
        }
    }
}
