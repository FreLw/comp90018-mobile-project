package com.comp90018.app.features.map

/** Member order must be captured once when the hunt starts. Null means shared. */
internal class FragmentAssignment private constructor(
    val owners: Map<String, String?>,
    private val members: Set<String>,
) {
    fun canCollect(userId: String, fragmentId: String, foundIds: Set<String>): Boolean =
        userId in members && fragmentId in owners && fragmentId !in foundIds &&
            (owners[fragmentId] == null || owners[fragmentId] == userId)

    fun isComplete(foundIds: Set<String>): Boolean = foundIds.containsAll(owners.keys)

    companion object {
        fun create(memberIds: List<String>): FragmentAssignment {
            require(memberIds.size in 2..4 && memberIds.all { it.isNotBlank() } &&
                memberIds.distinct().size == memberIds.size) { "A hunt requires 2 to 4 distinct members" }
            val owners = SouthLawnFragmentIds.toList().mapIndexed { index, id ->
                id to if (memberIds.size == 3 && index == 3) null
                    else memberIds[index % memberIds.size]
            }.toMap()
            return FragmentAssignment(owners, memberIds.toSet())
        }
    }
}
