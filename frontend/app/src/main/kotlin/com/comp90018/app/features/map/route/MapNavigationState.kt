package com.comp90018.app.features.map.route

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import com.comp90018.app.features.map.MapRelic

/** Mutable route selections shared by the map and its full-screen hunt pages.
 * Each remember key below matches the original screen; only hunt-scoped selections reset
 * when the team session changes. Atlas assembly survives the final team claim.
 */
internal class MapNavigationState(
    val selectedRelic: MutableState<MapRelic?>,
    val detailRelic: MutableState<MapRelic?>,
    val challengeRelic: MutableState<MapRelic?>,
    val compassRelic: MutableState<MapRelic?>,
    val huntRevealRelic: MutableState<MapRelic?>,
    val atlasAssemblyRelic: MutableState<MapRelic?>,
    val huntStoryVisible: MutableState<Boolean>,
    val memberQuizRelic: MutableState<MapRelic?>,
    val debugSimulationEnabled: MutableState<Boolean>,
    val debugTaskRelic: MutableState<MapRelic?>,
    val debugTaskSession: MutableState<Int>,
)

@Composable
internal fun rememberMapNavigationState(activeHuntSessionId: String?, currentUserId: String): MapNavigationState =
    MapNavigationState(
        selectedRelic = remember(activeHuntSessionId) { mutableStateOf<MapRelic?>(null) },
        detailRelic = remember(activeHuntSessionId) { mutableStateOf<MapRelic?>(null) },
        challengeRelic = remember(activeHuntSessionId) { mutableStateOf<MapRelic?>(null) },
        compassRelic = remember(activeHuntSessionId) { mutableStateOf<MapRelic?>(null) },
        huntRevealRelic = remember(activeHuntSessionId) { mutableStateOf<MapRelic?>(null) },
        atlasAssemblyRelic = remember(currentUserId) { mutableStateOf<MapRelic?>(null) },
        huntStoryVisible = remember(activeHuntSessionId) { mutableStateOf(false) },
        memberQuizRelic = remember(activeHuntSessionId) { mutableStateOf<MapRelic?>(null) },
        debugSimulationEnabled = remember { mutableStateOf(false) },
        debugTaskRelic = remember { mutableStateOf<MapRelic?>(null) },
        debugTaskSession = remember { mutableIntStateOf(0) },
    )
