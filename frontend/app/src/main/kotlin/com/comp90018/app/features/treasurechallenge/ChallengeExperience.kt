package com.comp90018.app.features.treasurechallenge

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.comp90018.app.GothicTreasureFontFamily
import com.comp90018.app.contextengine.challenge.RelicChallengeType
import com.comp90018.app.sensors.SensorValidity

internal fun RelicChallengeType.questTitle(): String = when (this) {
    RelicChallengeType.UNION_LAWN_PHOTO -> "Capture the lost lake"
    RelicChallengeType.WILSON_HALL_OBSERVATION -> "Awaken the rosette"
    RelicChallengeType.OLD_QUAD_EXCAVATION -> "Unearth the ancient fern"
    RelicChallengeType.SOUTH_LAWN_VIEWING_ANGLE -> "Find Atlas’s perspective"
    RelicChallengeType.SYSTEM_GARDEN_GLASSHOUSE -> "Frame the lost glasshouse"
    RelicChallengeType.GRAINGER_MUSEUM_TONE_TOOL -> "Bring the melody to life"
}

@Composable
internal fun ChallengeExperience(
    state: TreasureChallengeUiState,
    onEmblemCenterChanged: ((Offset) -> Unit)? = null,
    emblemContent: (@Composable (Modifier) -> Unit)? = null,
    afterEmblemContent: (@Composable () -> Unit)? = null,
    beforeGuideContent: (@Composable () -> Unit)? = null,
    departureProgress: Float = 0f,
) {
    val exit = (departureProgress / .42f).coerceIn(0f, 1f)
    val lowerDeparture = Modifier.graphicsLayer {
        alpha = 1f - exit
        translationY = 140.dp.toPx() * exit
    }
    val entrance = remember(state.challengeId) { Animatable(0f) }
    LaunchedEffect(state.challengeId) {
        entrance.animateTo(1f, tween(850, easing = FastOutSlowInEasing))
    }
    Column(Modifier.fillMaxWidth().padding(horizontal = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(state.challengeType.questTitle(), color = QuestParchment, fontFamily = GothicTreasureFontFamily,
            fontSize = 29.sp, lineHeight = 32.sp, textAlign = TextAlign.Center,
            modifier = Modifier.graphicsLayer {
                alpha = entrance.value * (1f - exit)
                translationY = (1f - entrance.value) * 12.dp.toPx() - 110.dp.toPx() * exit
            })
        val emblemModifier = Modifier.fillMaxWidth().height(
            if (state.challengeType == RelicChallengeType.SOUTH_LAWN_VIEWING_ANGLE ||
                state.challengeType == RelicChallengeType.WILSON_HALL_OBSERVATION) 300.dp else 280.dp
        ).graphicsLayer {
            alpha = entrance.value
            scaleX = .9f + .1f * entrance.value
            scaleY = scaleX
        }.onGloballyPositioned { coordinates ->
            onEmblemCenterChanged?.invoke(coordinates.localToRoot(
                Offset(coordinates.size.width / 2f, coordinates.size.height / 2f)))
        }
        if (emblemContent == null) QuestEmblem(state, emblemModifier) else emblemContent(emblemModifier)
        if (afterEmblemContent != null) Box(lowerDeparture.fillMaxWidth()) { afterEmblemContent() }
        val measurement = questMeasurement(state)
        val isLake = state.challengeType == RelicChallengeType.UNION_LAWN_PHOTO
        Row(modifier = if (isLake) lowerDeparture.fillMaxWidth().testTag("union_measurements") else lowerDeparture,
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            if (!isLake) Canvas(Modifier.width(24.dp).height(1.dp)) {
                drawLine(QuestGold.copy(alpha = .5f), Offset.Zero, Offset(size.width, 0f), 1f)
            }
            Column(if (isLake) Modifier.weight(1f) else Modifier, horizontalAlignment = Alignment.CenterHorizontally) {
                Text(measurement.first, color = QuestStone, fontSize = 10.sp, letterSpacing = 2.sp)
                Text(measurement.second, color = QuestParchment, fontFamily = FontFamily.Serif, fontSize = 19.sp)
            }
            if (isLake || state.challengeType == RelicChallengeType.SOUTH_LAWN_VIEWING_ANGLE ||
                state.challengeType == RelicChallengeType.WILSON_HALL_OBSERVATION) {
                if (!isLake) Spacer(Modifier.width(4.dp))
                Column(if (isLake) Modifier.weight(1f) else Modifier, horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(if (isLake) "DIRECTION TO ALIGN" else "VIEWING ANGLE", color = QuestStone,
                        fontSize = 10.sp, letterSpacing = if (isLake) 1.sp else 2.sp)
                    val angle = if (state.latestSnapshot?.orientation?.direction?.headingValidity == SensorValidity.VALID)
                        String.format(java.util.Locale.US, "%.1f°", kotlin.math.abs(QuestVisualSignals.from(state).headingError)) else "—"
                    Text(angle, color = QuestParchment, fontFamily = FontFamily.Serif, fontSize = 19.sp)
                }
            }
            if (!isLake) Canvas(Modifier.width(24.dp).height(1.dp)) {
                drawLine(QuestGold.copy(alpha = .5f), Offset.Zero, Offset(size.width, 0f), 1f)
            }
        }
        if (beforeGuideContent != null) Box(lowerDeparture.fillMaxWidth()) { beforeGuideContent() }
        Box(lowerDeparture.fillMaxWidth()) { QuestFieldGuide(state) }
    }
}
