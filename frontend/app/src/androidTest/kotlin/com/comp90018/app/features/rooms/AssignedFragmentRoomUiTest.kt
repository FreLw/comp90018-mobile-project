package com.comp90018.app.features.rooms

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.comp90018.app.data.rooms.RoomFragmentAssignments
import com.comp90018.app.data.rooms.TeamRoom
import com.comp90018.app.features.map.SouthLawnFragmentIds
import org.junit.Rule
import org.junit.Test

class AssignedFragmentRoomUiTest {
    @get:Rule val rule = createComposeRule()
    private val members = listOf("alice", "bob", "carol", "dave")
    private val fragments = SouthLawnFragmentIds.toList()

    private fun showRoom(found: List<String>, completed: List<String> = emptyList()) {
        val room = TeamRoom("room", "alice", members, "south_lawn_atlas", "Atlas", "hunting",
            foundFragmentIds = found, taskCompletedMemberIds = completed,
            fragmentAssignments = RoomFragmentAssignments.create(members))
        rule.setContent {
            MaterialTheme {
                ActiveHuntHeader(room, "alice", false, {}, {}, {},
                    memberNames = mapOf("bob" to "Bob", "carol" to "Carol", "dave" to "Dave"))
            }
        }
    }

    @Test fun fourPlayerRoomShowsEachFragmentOwner() {
        showRoom(emptyList())
        rule.onNodeWithText("Your fragment", substring = true).assertExists()
        rule.onNodeWithText("Assigned to Bob", substring = true).assertExists()
        rule.onNodeWithText("Assigned to Carol", substring = true).assertExists()
        rule.onNodeWithText("Assigned to Dave", substring = true).assertExists()
        rule.onNodeWithText("Claim Treasure").assertDoesNotExist()
    }

    @Test fun twoCompletionsDoNotExposeTheFourPlayerClaimButton() {
        showRoom(fragments.take(2), members.take(2))
        rule.onNodeWithText("Claim Treasure").assertDoesNotExist()
    }

    @Test fun allFourCompletionsExposeTheClaimButton() {
        showRoom(fragments, members)
        rule.onNodeWithText("Claim Treasure").assertExists()
    }
}
