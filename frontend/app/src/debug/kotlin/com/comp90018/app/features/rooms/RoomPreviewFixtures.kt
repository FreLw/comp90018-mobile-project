package com.comp90018.app.features.rooms

/*
 * Supplies local room fixtures for development-only room previews.
 * Keep preview content here so it is excluded from the release implementation.
 */

import com.comp90018.app.data.rooms.TeamRoom

/** Local fixtures for previewing the plaza without creating cloud rooms. */
internal fun roomPreviewFixtures(): List<TeamRoom> {
    val names = listOf("Campus Explorers", "Lost Lake Hunters", "Wilson Hall Duo", "Old Quad Adventurers",
        "South Lawn Seekers", "Garden Wanderers", "Museum Detectives", "After Class Hunt",
        "Weekend Treasure Team", "First Time Explorers")
    val destinations = listOf("Union Lawn", "Wilson Hall", "Old Quad", "South Lawn", "System Garden", "Grainger Museum")
    return names.mapIndexed { index, name ->
        val owner = "preview-owner-$index"
        TeamRoom(
            id = "preview-room-$index",
            creatorId = owner,
            memberIds = if (index % 3 == 1) listOf(owner, "preview-teammate-$index") else listOf(owner),
            taskId = "",
            taskTitle = destinations[index % destinations.size],
            taskStatus = if (index % 3 == 1) "hunting" else "idle",
            name = name,
            description = "Explore ${destinations[index % destinations.size]} together and uncover its hidden treasure.",
            idOnly = false,
        )
    }
}
