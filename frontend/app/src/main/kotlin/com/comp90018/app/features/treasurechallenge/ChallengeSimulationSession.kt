package com.comp90018.app.features.treasurechallenge

/*
 * Defines the debug-control UI and fake context-engine contract shared by build types.
 * The real simulator is supplied by src/debug; production rule evaluation still determines completion.
 */

import androidx.compose.runtime.Composable
import com.comp90018.app.contextengine.DeviceContextSnapshot
import com.comp90018.app.contextengine.DeviceContextEngine

/** UI boundary shared by build types; the implementation exists only in debug builds. */
interface ChallengeSimulationSession {
    val engine: DeviceContextEngine

    @Composable
    fun Controls(
        state: TreasureChallengeUiState,
        onPhotoCaptured: (String) -> Unit,
        onCompleteWithDebugSnapshot: (DeviceContextSnapshot) -> Unit,
    )
}
