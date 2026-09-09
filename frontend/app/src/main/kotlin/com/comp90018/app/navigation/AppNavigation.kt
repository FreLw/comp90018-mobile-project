package com.comp90018.app.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Forum
import androidx.compose.material.icons.rounded.Group
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.Icon
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
fun AppBottomNavigation(selected: AppDestination, onSelected: (AppDestination) -> Unit) {
    NavigationBar(containerColor = Color.White, tonalElevation = 10.dp) {
        AppDestination.entries.forEach { destination ->
            val isRooms = destination == AppDestination.Rooms
            NavigationBarItem(
                selected = selected == destination,
                onClick = { onSelected(destination) },
                icon = {
                    Box(
                        modifier = if (isRooms) Modifier.size(44.dp).clip(CircleShape).background(if (selected == destination) Brand else BrandSoft) else Modifier.size(32.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(destination.icon, destination.label, tint = if (isRooms && selected == destination) Color.White else if (selected == destination) Brand else Muted)
                    }
                },
                label = { Text(destination.label) },
                alwaysShowLabel = true,
            )
        }
    }
}
