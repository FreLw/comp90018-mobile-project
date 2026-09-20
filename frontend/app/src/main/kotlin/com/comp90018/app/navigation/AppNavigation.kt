package com.comp90018.app.navigation

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Badge
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.comp90018.app.R

enum class AppDestination(val label: String, @param:DrawableRes val iconRes: Int) {
    Friends("Friends", R.drawable.nav_friends_symbol),
    Rooms("Rooms", R.drawable.nav_rooms_symbol),
    Map("Map", R.drawable.nav_map_symbol),
    Treasure("Treasure", R.drawable.nav_treasure_symbol),
    Profile("Profile", R.drawable.nav_profile_symbol),
}

@Composable
fun AppBottomNavigation(
    selected: AppDestination,
    unreadFriendMessages: Int,
    unreadRoomMessages: Int,
    onSelected: (AppDestination) -> Unit,
) {
    NavigationBar(containerColor = MaterialTheme.colorScheme.surface, tonalElevation = 10.dp) {
        AppDestination.entries.forEach { destination ->
            val isRooms = destination == AppDestination.Rooms
            val isFriends = destination == AppDestination.Friends
            NavigationBarItem(
                selected = selected == destination,
                onClick = { onSelected(destination) },
                icon = {
                    Box(
                        modifier = Modifier.size(38.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Image(
                            painter = painterResource(destination.iconRes),
                            contentDescription = destination.label,
                            modifier = Modifier
                                .size(34.dp)
                                .alpha(if (selected == destination) 1f else 0.88f),
                        )
                        if (isRooms && unreadRoomMessages > 0) {
                            Badge(
                                modifier = Modifier.align(Alignment.TopEnd).offset(x = 6.dp, y = (-6).dp),
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
                label = { Text(destination.label, maxLines = 1) },
                alwaysShowLabel = true,
            )
        }
    }
}
