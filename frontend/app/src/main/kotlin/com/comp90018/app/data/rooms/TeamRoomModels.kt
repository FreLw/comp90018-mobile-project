package com.comp90018.app.data.rooms

const val TEAM_ROOM_CAPACITY = 2

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
    val maxMembers: Int = TEAM_ROOM_CAPACITY,
    val description: String = "",
    val huntSessionId: String = "",
    val idOnly: Boolean = true,
)

/** Display information loaded for one room member. */
data class TeamRoomMember(
    val uid: String,
    val name: String,
    val avatarUrl: String,
)
