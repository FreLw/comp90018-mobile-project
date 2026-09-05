package com.unimelb.losttreasures.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountCircle
import androidx.compose.material.icons.rounded.Forum
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.unimelb.losttreasures.ui.data.initialSocialPosts
import com.unimelb.losttreasures.ui.data.sampleRelics
import com.unimelb.losttreasures.ui.data.sampleUserProfile
import com.unimelb.losttreasures.ui.model.SocialPost
import com.unimelb.losttreasures.ui.screens.CollectionScreen
import com.unimelb.losttreasures.ui.screens.MapScreen
import com.unimelb.losttreasures.ui.screens.ProfileScreen
import com.unimelb.losttreasures.ui.screens.SquareScreen
import com.unimelb.losttreasures.ui.screens.TeamScreen

private enum class AppTab(
    val label: String,
    val icon: ImageVector
) {
    Profile("Profile", Icons.Rounded.AccountCircle),
    Square("Square", Icons.Rounded.Forum),
    Map("Map", Icons.Rounded.LocationOn),
    Collection("Collection", Icons.Rounded.Star),
    Team("Team", Icons.Rounded.Groups)
}

@Composable
fun LostTreasuresApp() {
    var selectedTabName by rememberSaveable { mutableStateOf(AppTab.Map.name) }
    var selectedRelicId by rememberSaveable { mutableStateOf<String?>(null) }
    var showTeamChat by rememberSaveable { mutableStateOf(false) }
    val socialPosts = remember { mutableStateListOf<SocialPost>().also { it.addAll(initialSocialPosts) } }
    val selectedTab = AppTab.valueOf(selectedTabName)
    val selectedRelic = sampleRelics.firstOrNull { it.id == selectedRelicId }

    // Team chat is rendered outside the Scaffold so it can cover the bottom navigation.
    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            bottomBar = {
                NavigationBar(containerColor = Color.White) {
                    AppTab.entries.forEach { tab ->
                        NavigationBarItem(
                            selected = if (tab == AppTab.Team) showTeamChat else selectedTab == tab,
                            onClick = {
                                if (tab == AppTab.Team) {
                                    showTeamChat = true
                                } else {
                                    selectedTabName = tab.name
                                }
                            },
                            icon = { Icon(tab.icon, contentDescription = tab.label) },
                            label = { Text(tab.label) }
                        )
                    }
                }
            },
            containerColor = MaterialTheme.colorScheme.background
        ) { innerPadding ->
            when (selectedTab) {
                AppTab.Profile -> ProfileScreen(
                    profile = sampleUserProfile,
                    onLogout = { },
                    modifier = Modifier.padding(innerPadding)
                )

                AppTab.Square -> SquareScreen(
                    posts = socialPosts,
                    onPublish = { message ->
                        socialPosts.add(
                            0,
                            SocialPost(
                                author = "You",
                                location = "UniMelb Campus",
                                message = message,
                                timeAgo = "Just now",
                                likes = 0
                            )
                        )
                    },
                    modifier = Modifier.padding(innerPadding)
                )

                AppTab.Map -> MapScreen(
                    selectedRelic = selectedRelic,
                    onSelectRelic = { selectedRelicId = it.id },
                    onDismissRelic = { selectedRelicId = null },
                    modifier = Modifier.padding(innerPadding)
                )

                AppTab.Collection -> CollectionScreen(
                    modifier = Modifier.padding(innerPadding)
                )

                AppTab.Team -> Unit
            }
        }

        AnimatedVisibility(
            visible = showTeamChat,
            enter = slideInHorizontally(initialOffsetX = { it }) + fadeIn(),
            exit = slideOutHorizontally(targetOffsetX = { it }) + fadeOut(),
            modifier = Modifier.fillMaxSize()
        ) {
            TeamScreen(
                onBack = { showTeamChat = false },
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}
