package com.comp90018.app.features.treasurechallenge

import androidx.compose.runtime.Composable
import com.comp90018.app.contextengine.challenge.RelicChallengeConfig

/** Release builds have no simulator implementation or fake challenge entry point. */
object ChallengeSimulationFactory {
    fun create(config: RelicChallengeConfig): ChallengeSimulationSession? = null

    @Composable
    fun CalibrationPanel(treasureId: String, config: RelicChallengeConfig, radarRadiusMeters: Double,
        state: TreasureChallengeUiState, onSimulateSound: () -> Unit) = Unit
}
