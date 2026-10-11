package com.comp90018.app.features.map.compass

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.comp90018.app.Brand
import com.comp90018.app.BuildConfig
import com.comp90018.app.GothicTreasureFontFamily
import com.comp90018.app.Ink
import com.comp90018.app.contextengine.challenge.RelicChallengeType
import com.comp90018.app.features.map.MapRelic
import com.comp90018.app.sensors.location.LocationOutput

/** These relics keep the local scan-and-dig hunt panel instead of the sensor-driven challenge screen. */
internal val LOCAL_HUNT_CHALLENGE_TYPES = setOf(
    RelicChallengeType.SYSTEM_GARDEN_GLASSHOUSE,
    RelicChallengeType.GRAINGER_MUSEUM_TONE_TOOL,
)

/**
 * Renders the pre-task compass and distance/heading indicators; both conditions enable the
 * departure animation and task entry.
 */
@Composable
internal fun TreasureCompassGate(
    relic: MapRelic,
    locationOutput: LocationOutput,
    deviceHeading: Float,
    soundEffectsEnabled: Boolean,
    debugSimulationEnabled: Boolean = false,
    onContinue: () -> Unit,
    onBack: () -> Unit,
) {
    var simulateSensors by remember(relic.id) { mutableStateOf(BuildConfig.DEBUG && debugSimulationEnabled) }
    var testDistance by remember(relic.id) { mutableStateOf((locationOutput.distanceToTargetMeters ?: 9.0).toFloat().coerceIn(0f, 30f)) }
    var testHeading by remember(relic.id) { mutableStateOf(deviceHeading) }
    val state = compassGateUiState(
        config = relic.compassGateConfig,
        locationOutput = locationOutput,
        deviceHeading = deviceHeading,
        simulateSensors = BuildConfig.DEBUG && simulateSensors,
        testDistance = testDistance,
        testHeading = testHeading,
    )
    val transition = rememberCompassGateTransition(relic.id, state.readiness.allReady, onContinue)
    CompassStarSoundEffects(relic.id, state.stars, soundEffectsEnabled)
    val signalLocked = rememberCompassSignalLock(relic.id, state.readiness.allReady)
    val instruction = compassGateInstruction(state, signalLocked, relic.compassGateConfig)

    Column(
        modifier = Modifier.fillMaxSize().background(Color(0xFFF5ECD9)).verticalScroll(rememberScrollState()).padding(horizontal = 18.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Back to map", tint = Ink)
            }
            Text(
                "✦ FOLLOW THE ORACLE'S SIGNAL ✦",
                modifier = Modifier.weight(1f).padding(end = 48.dp),
                color = Brand,
                fontFamily = GothicTreasureFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                textAlign = TextAlign.Center,
            )
        }
        DivineCompassVisual(
            readiness = state.readiness,
            config = relic.compassGateConfig,
            turnDegrees = state.turnDegrees?.toFloat() ?: 0f,
            signalAvailable = state.turnDegrees != null,
            distanceMeters = state.distanceMeters,
            signalLocked = signalLocked,
            modifier = Modifier.fillMaxWidth().height(285.dp).graphicsLayer {
                alpha = transition.compassEntrance.value
                scaleX = 0.35f + transition.compassEntrance.value * 0.65f
                scaleY = scaleX
                rotationZ = (1f - transition.compassEntrance.value) * -24f
                translationY = (1f - transition.compassEntrance.value) * 80.dp.toPx()
            },
        )
        CompassStatusCard(
            state = state,
            config = relic.compassGateConfig,
            signalLocked = signalLocked,
            instruction = instruction,
            leaving = transition.leaving,
            onContinue = transition.onContinue,
            modifier = Modifier.fillMaxWidth().graphicsLayer {
                alpha = transition.lampEntrance.value
                scaleX = 0.7f + transition.lampEntrance.value * 0.3f
                scaleY = scaleX
                translationY = (1f - transition.lampEntrance.value) * 65.dp.toPx()
            },
        )
        if (BuildConfig.DEBUG) {
            CompassDebugControls(
                simulateSensors = simulateSensors,
                testDistance = testDistance,
                testHeading = testHeading,
                nearTreasure = state.readiness.nearTreasure,
                facingTreasure = state.readiness.facingTreasure,
                onSimulateSensors = { simulateSensors = it },
                onTestDistance = { testDistance = it },
                onTestHeading = { testHeading = it },
            )
        }
    }
}
