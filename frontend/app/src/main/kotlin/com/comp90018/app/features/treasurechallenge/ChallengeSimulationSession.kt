package com.comp90018.app.features.treasurechallenge

import androidx.compose.runtime.Composable
import com.comp90018.app.contextengine.DeviceContextEngine

/** UI boundary shared by build types; the implementation exists only in debug builds. */
interface ChallengeSimulationSession {
    val engine: DeviceContextEngine

    @Composable
    fun Controls(state: TreasureChallengeUiState, onPhotoCaptured: (String) -> Unit)
}
