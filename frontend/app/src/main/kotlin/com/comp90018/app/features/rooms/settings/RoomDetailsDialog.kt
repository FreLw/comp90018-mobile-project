package com.comp90018.app.features.rooms.settings

import android.content.ClipData
import android.content.ClipboardManager
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.comp90018.app.Brand
import com.comp90018.app.BrandSoft
import com.comp90018.app.Ink
import com.comp90018.app.Muted
import com.comp90018.app.RelicRed
import com.comp90018.app.data.rooms.TEAM_ROOM_CAPACITY
import com.comp90018.app.data.rooms.TeamRoom
import com.comp90018.app.data.rooms.TeamRoomMember
import com.comp90018.app.features.rooms.hunt.SouthLawnFragmentStatus
import com.comp90018.app.ui.components.ProfileAvatar

/** Displays room metadata, member management, and settings actions. */
@Composable
internal fun RoomDetailsDialog(
    roomId: String,
    room: TeamRoom?,
    members: List<TeamRoomMember>,
    isOwner: Boolean,
    onMemberClick: (TeamRoomMember) -> Unit,
    onEditSettings: () -> Unit,
    onRemoveMember: (TeamRoomMember, () -> Unit) -> Unit,
    working: Boolean,
    error: String?,
    onLeave: () -> Unit,
    onDismissRoom: () -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    var copied by remember { mutableStateOf(false) }
    var removingMember by remember { mutableStateOf<TeamRoomMember?>(null) }
    removingMember?.let { member ->
        AlertDialog(onDismissRequest = { if (!working) removingMember = null },
            title = { Text("Remove member?") }, text = { Column { Text("Remove ${member.name} from the room? They can still rejoin later.")
                error?.let { Text(it, color = RelicRed) } } },
            confirmButton = { TextButton(enabled = !working, onClick = { onRemoveMember(member) { removingMember = null } }) { Text(if (working) "Removing…" else "Remove", color = RelicRed) } },
            dismissButton = { TextButton(enabled = !working, onClick = { removingMember = null }) { Text("Cancel") } })
    }
    AlertDialog(onDismissRequest = onDismiss, title = { Text("Room details", fontWeight = FontWeight.Bold, color = Ink) }, text = {
        Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text(room?.name?.takeIf { it.isNotBlank() } ?: if (room == null) "Loading room…" else "Room ${room.id.takeLast(6)}", color = Ink, fontWeight = FontWeight.Bold)
            room?.description?.takeIf { it.isNotBlank() }?.let { Text(it, color = Muted) }
            Text(if (room?.idOnly != false) "Join by Room ID only" else "Public room · Join directly from the plaza", color = Muted)
            if (isOwner) TextButton(onClick = onEditSettings) { Text("Edit room settings") }
            Column {
                Text("ROOM ID", color = Muted, style = MaterialTheme.typography.labelMedium)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(roomId, color = Ink, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
                    TextButton(onClick = {
                        val clipboard = context.getSystemService(ClipboardManager::class.java)
                        clipboard.setPrimaryClip(ClipData.newPlainText("Room ID", roomId))
                        copied = true
                    }) { Text(if (copied) "Copied" else "Copy", color = Brand) }
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                val memberIds = room?.let { listOf(it.creatorId) + it.memberIds.filter { id -> id != it.creatorId } }
                    ?: members.map { it.uid }
                val capacity = room?.maxMembers ?: TEAM_ROOM_CAPACITY
                Text("EXPLORERS (${room?.memberIds?.size ?: members.size}/$capacity)", color = Muted, style = MaterialTheme.typography.labelMedium)
                repeat(capacity) { index ->
                    val memberId = memberIds.getOrNull(index)
                    val member = members.firstOrNull { it.uid == memberId }
                    Surface(shape = RoundedCornerShape(16.dp), color = BrandSoft.copy(alpha = 0.35f)) {
                        Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            if (memberId != null) {
                                Row(Modifier.weight(1f).clickable(enabled = member != null) { member?.let(onMemberClick) }, verticalAlignment = Alignment.CenterVertically) {
                                    ProfileAvatar(member?.avatarUrl.orEmpty(), member?.name ?: "Explorer", 40.dp)
                                    Spacer(Modifier.width(10.dp))
                                    Column {
                                        Text(member?.name ?: "Loading explorer…", color = Ink, fontWeight = FontWeight.Medium)
                                        Text(if (memberId == room?.creatorId) "Room owner" else "Teammate", color = Muted, style = MaterialTheme.typography.bodySmall)
                                    }
                                }
                                if (isOwner && member != null && member.uid != room?.creatorId) TextButton(enabled = !working, onClick = { removingMember = member }) { Text("Remove", color = RelicRed) }
                            } else {
                                Box(Modifier.size(40.dp).background(BrandSoft, CircleShape), contentAlignment = Alignment.Center) {
                                    Icon(Icons.Rounded.Person, null, tint = Muted)
                                }
                                Spacer(Modifier.width(10.dp))
                                Column {
                                    Text("Waiting for a teammate", color = Ink, fontWeight = FontWeight.Medium)
                                    Text("Share the Room ID to invite a friend.", color = Muted, style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                    }
                }
            }
            room?.takeIf { it.taskStatus == "hunting" }?.let { activeHunt ->
                Column {
                    Text("HUNT STATUS", color = Muted, style = MaterialTheme.typography.labelMedium)
                    Text(activeHunt.taskTitle, color = Ink, fontWeight = FontWeight.Medium)
                    if (activeHunt.taskId == "south_lawn_atlas") {
                        SouthLawnFragmentStatus(activeHunt.foundFragmentIds)
                        members.forEach { member ->
                            val claimed = member.uid in activeHunt.taskClaimedMemberIds
                            Text(
                                "${if (claimed) "✓" else "○"} ${member.name}: ${if (claimed) "Atlas claimed" else "Atlas not claimed"}",
                                color = if (claimed) Brand else Muted,
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                    }
                    members.forEach { member ->
                        val done = member.uid in activeHunt.taskCompletedMemberIds
                        Text("${if (done) "✓" else "○"} ${member.name}: ${if (done) "Task complete" else "Still hunting"}", color = if (done) Brand else Muted)
                    }
                }
            }
        }
    }, confirmButton = { TextButton(onClick = onDismiss) { Text("Done", color = Brand) } }, dismissButton = {
        TextButton(onClick = if (isOwner) onDismissRoom else onLeave) { Text(if (isOwner) "Dismiss room" else "Leave room", color = RelicRed) }
    })
}
