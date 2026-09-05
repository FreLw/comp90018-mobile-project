package com.unimelb.losttreasures.ui.data

import com.unimelb.losttreasures.ui.model.ChatMessage
import com.unimelb.losttreasures.ui.model.HuntType
import com.unimelb.losttreasures.ui.model.Relic
import com.unimelb.losttreasures.ui.model.RelicTone
import com.unimelb.losttreasures.ui.model.SocialPost
import com.unimelb.losttreasures.ui.model.TeamMember
import com.unimelb.losttreasures.ui.model.UserProfile

const val SampleTeamCode = "UOM-427"

// Temporary UI fixtures. Replace this file with repositories or ViewModels when real data arrives.
val sampleUserProfile = UserProfile(
    displayName = "Ruoning Ren",
    studentId = "1234567",
    email = "ruoningr@student.unimelb.edu.au",
    level = 6,
    discoveredCount = 2,
    teamCode = SampleTeamCode,
    currentTitle = "Campus Relic Seeker"
)

val sampleRelics = listOf(
    Relic(
        id = "old-quad",
        name = "Old Quad Compass",
        place = "Old Quadrangle",
        distance = "210 m",
        type = HuntType.Solo,
        condition = "Stand still, face east, hold the phone steady.",
        progress = 0.68f,
        mapX = 0.18f,
        mapY = 0.38f,
        tone = RelicTone.Gold,
        story = "A brass compass linked to early campus wayfinding stories."
    ),
    Relic(
        id = "baillieu",
        name = "Baillieu Lantern",
        place = "Baillieu Library",
        distance = "480 m",
        type = HuntType.Collaborative,
        condition = "One teammate must stay still while another faces north.",
        progress = 0.42f,
        mapX = 0.62f,
        mapY = 0.28f,
        tone = RelicTone.Red,
        story = "A study lantern revealed through coordinated team signals."
    ),
    Relic(
        id = "south-lawn",
        name = "South Lawn Sundial",
        place = "South Lawn",
        distance = "95 m",
        type = HuntType.Solo,
        condition = "Enter the search area and slowly rotate to the marker.",
        progress = 0.83f,
        mapX = 0.46f,
        mapY = 0.62f,
        tone = RelicTone.Gold,
        story = "A hidden sundial that reacts to direction and stillness."
    ),
    Relic(
        id = "wilson",
        name = "Wilson Hall Bell",
        place = "Wilson Hall",
        distance = "330 m",
        type = HuntType.Collaborative,
        condition = "Coordinate with your team near the hall.",
        progress = 0.34f,
        mapX = 0.82f,
        mapY = 0.52f,
        tone = RelicTone.Red,
        story = "A ceremonial bell recovered through a shared team hunt."
    )
)

val discoveredRelics = listOf(
    sampleRelics[2],
    sampleRelics[0],
    sampleRelics[1].copy(progress = 0.0f),
    sampleRelics[3].copy(progress = 0.0f)
)

val teamMembers = listOf(
    TeamMember("Zhuoer", "Map UI and treasure feedback", 1f, true),
    TeamMember("Fan", "Arrive near Baillieu Library", 0.64f, false),
    TeamMember("Jiayi", "Keep device stable for 8 sec", 1f, true),
    TeamMember("Yunxiao", "Share final clue in team chat", 0.48f, false)
)

val initialChatMessages = listOf(
    ChatMessage("Yunxiao", "I am near Baillieu. Need one stable scan.", false),
    ChatMessage("You", "I can hold the phone steady now.", true),
    ChatMessage("Fan", "Bearing is still off by around 20 deg.", false)
)

val initialSocialPosts = listOf(
    SocialPost(
        author = "Zhuoer",
        location = "Old Quadrangle",
        message = "Found a clue near the archway. The compass clue points east.",
        timeAgo = "4 min ago",
        likes = 8
    ),
    SocialPost(
        author = "Fan",
        location = "South Lawn",
        message = "The Sundial marker is active today. Move slowly before checking direction.",
        timeAgo = "12 min ago",
        likes = 13
    ),
    SocialPost(
        author = "Yunxiao",
        location = "Baillieu Library",
        message = "Looking for teammates for the collaborative lantern relic.",
        timeAgo = "21 min ago",
        likes = 5
    )
)
