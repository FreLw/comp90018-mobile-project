package com.comp90018.app.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.comp90018.app.*

enum class AppDestination(val label: String, val icon: ImageVector) {
    Profile("Profile", Icons.Rounded.Person),
    Friends("Friends", Icons.Rounded.Group),
    Map("Map", Icons.Rounded.LocationOn),
    Hunt("Hunt", Icons.Rounded.Explore),
    Collection("Collection", Icons.Rounded.CollectionsBookmark),
}

@Composable
fun AppBottomNavigation(selected: AppDestination, onSelected: (AppDestination) -> Unit) {
    Box(Modifier.fillMaxWidth().padding(top = 18.dp)) {
        Surface(color = Color.White, shadowElevation = 8.dp) {
            Row(Modifier.fillMaxWidth().navigationBarsPadding().height(76.dp), verticalAlignment = Alignment.CenterVertically) {
                Row(Modifier.weight(1f)) {
                    NavigationItem(AppDestination.Profile, selected, onSelected, Modifier.weight(1f))
                    NavigationItem(AppDestination.Friends, selected, onSelected, Modifier.weight(1f))
                }
                Spacer(Modifier.width(80.dp))
                Row(Modifier.weight(1f)) {
                    NavigationItem(AppDestination.Hunt, selected, onSelected, Modifier.weight(1f))
                    NavigationItem(AppDestination.Collection, selected, onSelected, Modifier.weight(1f))
                }
            }
        }
        Column(Modifier.align(Alignment.TopCenter).offset(y = (-18).dp)
            .selectable(selected == AppDestination.Map, role = Role.Tab, onClick = { onSelected(AppDestination.Map) })
            .width(80.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(Modifier.size(64.dp).shadow(8.dp, CircleShape).background(Brand, CircleShape), contentAlignment = Alignment.Center) {
                Icon(Icons.Rounded.LocationOn, "Search map", Modifier.size(34.dp), tint = Color.White)
            }
            Spacer(Modifier.height(4.dp))
            Text("MAP", color = Brand, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun NavigationItem(destination: AppDestination, selected: AppDestination, onSelected: (AppDestination) -> Unit, modifier: Modifier) {
    val active = selected == destination
    Column(modifier.height(64.dp).selectable(active, role = Role.Tab, onClick = { onSelected(destination) }),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Icon(destination.icon, null, Modifier.size(25.dp), tint = if (active) Brand else Muted)
        Spacer(Modifier.height(5.dp))
        Text(destination.label, color = if (active) Brand else Muted, style = MaterialTheme.typography.labelMedium,
            fontWeight = if (active) FontWeight.Bold else FontWeight.Normal)
    }
}
