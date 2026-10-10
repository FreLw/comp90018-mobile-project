package com.comp90018.app.features.rooms.hunt

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.comp90018.app.Brand
import com.comp90018.app.BrandSoft
import com.comp90018.app.Ink
import com.comp90018.app.Muted
import com.comp90018.app.RelicRed
import com.comp90018.app.data.rooms.TeamRoom
import com.comp90018.app.features.map.MapRelic

internal fun MapRelic.roomDestinationName(): String =
    locationName.substringBefore('/').trim().ifBlank { name }

/** Summarizes the active destination and teammate completion while offering hunt navigation. */
@Composable
internal fun ActiveHuntHeader(
    room: TeamRoom,
    currentUserId: String,
    updating: Boolean,
    onContinue: () -> Unit,
    onTerminate: () -> Unit,
    onClaimAtlas: () -> Unit,
) {
    val isFragmentHunt = room.taskId == "south_lawn_atlas"
    val allFragmentsFound = listOf(
        "south_lawn_north_west", "south_lawn_north_east",
        "south_lawn_south_west", "south_lawn_south_east",
    ).all { it in room.foundFragmentIds }
    val completed = currentUserId in room.taskCompletedMemberIds
    val allCompleted = room.memberIds.isNotEmpty() && room.memberIds.all { it in room.taskCompletedMemberIds }
    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = BrandSoft)) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
            Text("Targeting ${room.taskTitle}", color = Ink, fontWeight = FontWeight.Bold)
            Text(
                when {
                    isFragmentHunt && currentUserId in room.taskClaimedMemberIds -> "Your Atlas is claimed. Waiting for your teammate to claim theirs."
                    currentUserId in room.taskClaimedMemberIds -> "Your treasure is claimed. Waiting for your teammate to claim theirs."
                    isFragmentHunt && allFragmentsFound -> "All four fragments are found. Turn to the map to reconstruct the Atlas."
                    isFragmentHunt -> "Find all four Atlas fragments together. Found fragments are shared with your teammate."
                    allCompleted -> "All explorers are ready. Turn to the map to dig the treasure."
                    completed -> "Waiting for your teammate to finish. You can help them."
                    else -> "Go and hunt for the treasure."
                },
                color = Muted,
                style = MaterialTheme.typography.bodySmall,
            )
            if (isFragmentHunt) SouthLawnFragmentStatus(room.foundFragmentIds)
            if (allCompleted && currentUserId !in room.taskClaimedMemberIds) {
                Button(
                    onClick = onClaimAtlas,
                    enabled = !updating,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Brand),
                ) { Text(if (updating) "Claiming..." else "Claim Treasure") }
            }
            Button(onClick = onContinue, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = Brand)) {
                Text(if (isFragmentHunt && currentUserId in room.taskClaimedMemberIds) "View hunt status" else if (isFragmentHunt && allFragmentsFound) "Turn to map and reconstruct" else if (isFragmentHunt) "Turn to map to find fragments" else if (allCompleted) "Turn to map and dig" else "Turn to map to continue your hunt")
            }
            if (room.creatorId == currentUserId) {
                OutlinedButton(onClick = onTerminate, enabled = !updating, modifier = Modifier.fillMaxWidth()) {
                    Text(if (updating) "Terminating..." else "Terminate Hunt", color = RelicRed)
                }
            }
        }
    }
}

/** Displays the four shared Atlas fragment states reported by the room. */
@Composable
internal fun SouthLawnFragmentStatus(foundFragmentIds: List<String>) {
    val fragments = listOf(
        "south_lawn_north_west" to "North-west fragment",
        "south_lawn_north_east" to "North-east fragment",
        "south_lawn_south_west" to "South-west fragment",
        "south_lawn_south_east" to "South-east fragment",
    )
    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Text("ROOM TASK STATUS", color = Muted, style = MaterialTheme.typography.labelMedium)
        fragments.forEach { (id, label) ->
            val found = id in foundFragmentIds
            Text(
                "${if (found) "✓" else "○"} $label: ${if (found) "is found" else "not found"}",
                color = if (found) Brand else Muted,
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

/** Shows destination/start/termination controls according to the room and current user. */
@Composable
internal fun RoomHuntControls(
    room: TeamRoom?,
    treasures: List<MapRelic>,
    isOwner: Boolean,
    memberCount: Int,
    treasuresLoading: Boolean,
    treasuresError: String?,
    updating: Boolean,
    onChooseDestination: () -> Unit,
    onStartHunt: () -> Unit,
) {
    val hasDestination = !room?.taskId.isNullOrBlank()
    val hasEnoughMembers = memberCount >= 2
    val destinationName = room?.taskTitle?.takeIf { it.isNotBlank() } ?: "Choose destination"
    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(
                onClick = onChooseDestination,
                enabled = !updating,
                modifier = Modifier.weight(1f),
            ) {
                Icon(Icons.Rounded.LocationOn, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(5.dp))
                Text(destinationName, maxLines = 1)
            }
            if (isOwner) {
                Button(
                    onClick = onStartHunt,
                    enabled = hasEnoughMembers && hasDestination && !updating,
                    colors = ButtonDefaults.buttonColors(containerColor = Brand),
                ) {
                    Icon(Icons.Rounded.PlayArrow, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(3.dp))
                    Text(if (updating) "Starting..." else "Start Hunt")
                }
            }
        }
        if (!isOwner || !hasEnoughMembers || !hasDestination) {
            Text(
                when {
                    !isOwner -> "Only the room owner can start a hunt."
                    !hasEnoughMembers -> "Start Hunt unlocks when 2 explorers are in the room."
                    treasuresLoading -> "Loading the six treasure destinations…"
                    treasuresError != null -> "Treasure destinations are unavailable right now."
                    else -> "Choose one of the six treasures to unlock Start Hunt."
                },
                color = Muted,
                style = MaterialTheme.typography.bodySmall,
            )
        } else if (room.taskStatus == "hunting") {
            Text("Hunt in progress: $destinationName", color = Brand, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
        }
    }
}
