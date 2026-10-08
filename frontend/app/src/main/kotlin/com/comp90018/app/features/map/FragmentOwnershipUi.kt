package com.comp90018.app.features.map

import com.comp90018.app.data.rooms.RoomFragmentAssignments

internal object FragmentOwnershipUi {
    fun nextDebugFragment(assignments: RoomFragmentAssignments?, fragmentIds: List<String>,
        currentUserId: String, foundIds: Set<String>): String? {
        val available = fragmentIds.filter { canCollect(assignments, it, currentUserId, foundIds) }
        return available.firstOrNull { assignments?.ownerByFragment?.get(it) == currentUserId }
            ?: available.firstOrNull()
    }

    fun label(
        assignments: RoomFragmentAssignments?, fragmentId: String,
        currentUserId: String, names: Map<String, String>,
    ): String {
        if (assignments == null) return "Shared"
        val owner = assignments.ownerByFragment[fragmentId] ?: return "Assignment unavailable"
        return when {
            owner.isEmpty() -> "Shared"
            owner == currentUserId -> "Your fragment"
            else -> "Assigned to ${names[owner]?.takeIf { it.isNotBlank() } ?: "another explorer"}"
        }
    }

    fun canCollect(assignments: RoomFragmentAssignments?, fragmentId: String,
        currentUserId: String, foundIds: Set<String>): Boolean =
        if (assignments == null) fragmentId in SouthLawnFragmentIds && fragmentId !in foundIds
        else assignments.canCollect(currentUserId, fragmentId, foundIds)
}
