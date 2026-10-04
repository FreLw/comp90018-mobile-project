package com.comp90018.app.data.rooms

/** Shared hunt state stored in `teamRooms/{roomId}`. */
data class TeamRoom(
    val id: String,
    val creatorId: String,
    val memberIds: List<String>,
    val taskId: String,
    val taskTitle: String,
    val taskStatus: String,
    val taskCompletedMemberIds: List<String> = emptyList(),
    val foundFragmentIds: List<String> = emptyList(),
    val taskClaimedMemberIds: List<String> = emptyList(),
    val name: String = "",
    val maxMembers: Int = 4,
    val description: String = "",
)

/** Display information loaded for one room member. */
data class TeamRoomMember(
    val uid: String,
    val name: String,
    val avatarUrl: String,
)
