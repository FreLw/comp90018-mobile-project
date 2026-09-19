package com.comp90018.app.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Forum
import androidx.compose.material.icons.rounded.Group
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.Badge
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.comp90018.app.Brand
import com.comp90018.app.BrandSoft
import com.comp90018.app.Muted

enum class AppDestination(val label: String, val icon: ImageVector) {
    Treasure("treasure", Icons.Rounded.Star),
    Rooms("Rooms", Icons.Rounded.Forum),
    Map("Map", Icons.Rounded.LocationOn),
    Friends("friend", Icons.Rounded.Group),
    Profile("profile", Icons.Rounded.Person),
}

@Composable
fun AppBottomNavigation(
    selected: AppDestination,
    unreadFriendMessages: Int,
    unreadRoomMessages: Int,
    onSelected: (AppDestination) -> Unit,
) {
    NavigationBar(containerColor = Color.White, tonalElevation = 10.dp) {
        AppDestination.entries.forEach { destination ->
            val isRooms = destination == AppDestination.Rooms
            val isFriends = destination == AppDestination.Friends
            NavigationBarItem(
                selected = selected == destination,
                onClick = { onSelected(destination) },
                icon = {
                    Box(
                        modifier = if (isRooms) Modifier.size(44.dp) else Modifier.size(32.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (isRooms) {
                            Box(
                                modifier = Modifier.size(44.dp).clip(CircleShape)
                                    .background(if (selected == destination) Brand else BrandSoft),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(destination.icon, destination.label, tint = if (selected == destination) Color.White else Muted)
                            }
                        } else {
                            Icon(destination.icon, destination.label, tint = if (selected == destination) Brand else Muted)
                        }
                        if (isRooms && unreadRoomMessages > 0) {
                            Badge(
                                modifier = Modifier.align(Alignment.TopEnd).offset(x = 4.dp, y = (-4).dp),
                            ) {
                                Text(if (unreadRoomMessages > 99) "99+" else unreadRoomMessages.toString())
                            }
                        }
                        if (isFriends && unreadFriendMessages > 0) {
                            Badge(
                                modifier = Modifier.align(Alignment.TopEnd).offset(x = 6.dp, y = (-6).dp),
                            ) {
                                Text(if (unreadFriendMessages > 99) "99+" else unreadFriendMessages.toString())
                            }
                        }
                    }
                },
                label = { Text(destination.label) },
                alwaysShowLabel = true,
            )
        }
    }
}
