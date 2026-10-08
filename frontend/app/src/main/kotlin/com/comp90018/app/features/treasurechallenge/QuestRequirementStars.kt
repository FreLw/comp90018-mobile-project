package com.comp90018.app.features.treasurechallenge

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.comp90018.app.GothicTreasureFontFamily
import com.comp90018.app.contextengine.challenge.ChallengeCondition
import com.comp90018.app.contextengine.challenge.RelicChallengeType
import kotlin.math.*

@Composable
internal fun ChallengeStatusCard(state: TreasureChallengeUiState) {
    val isAtlas = state.challengeType == RelicChallengeType.SOUTH_LAWN_VIEWING_ANGLE
    val isRosette = state.challengeType == RelicChallengeType.WILSON_HALL_OBSERVATION
    val compactRow = isAtlas || isRosette
    Card(Modifier.fillMaxWidth().padding(horizontal = 8.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xC91B3A2F), contentColor = QuestParchment),
        border = androidx.compose.foundation.BorderStroke(.7.dp, QuestGold.copy(alpha = .22f))) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(state.instructionText, fontFamily = GothicTreasureFontFamily, fontSize = 22.sp,
                lineHeight = 26.sp, textAlign = TextAlign.Center)
            state.conditionStates.chunked(if (compactRow) state.conditionStates.size.coerceAtLeast(1) else 3).forEach { row ->
                Row(Modifier.fillMaxWidth().testTag("quest_condition_row"), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    row.forEach { condition ->
                        val label = if (compactRow) condition.condition.observationStarLabel(isAtlas) else condition.condition.starLabel()
                        Column(Modifier.weight(1f),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(5.dp)) {
                            QuestConditionStar(condition.satisfied, label)
                            Text(label,
                                color = if (condition.satisfied) QuestParchment else QuestStone,
                                fontFamily = FontFamily.Serif, fontSize = 12.sp,
                                textAlign = TextAlign.Center)
                        }
                    }
                }
            }
            if (state.conditionStates.isEmpty()) {
                Text("Reading your surroundings…", color = QuestStone,
                    fontFamily = FontFamily.Serif, fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun QuestConditionStar(lit: Boolean, label: String) {
    val light by animateFloatAsState(if (lit) 1f else 0f, tween(650), label = "requirement_star")
    Canvas(Modifier.size(29.dp).semantics {
        contentDescription = label + " condition"
        stateDescription = if (lit) "Star lit" else "Star unlit"
    }) {
        val r = size.minDimension * .36f
        val star = Path().apply {
            repeat(10) { i ->
                val angle = -PI / 2 + i * PI / 5
                val radius = if (i % 2 == 0) r else r * .43f
                val x = center.x + cos(angle).toFloat() * radius
                val y = center.y + sin(angle).toFloat() * radius
                if (i == 0) moveTo(x, y) else lineTo(x, y)
            }
            close()
        }
        val colour = lerp(QuestStone, QuestGold, light)
        drawCircle(QuestGold.copy(alpha = .07f * light), r * 1.4f, center)
        drawCircle(QuestGold.copy(alpha = .08f * light), r * 1.1f, center)
        drawPath(star, colour.copy(alpha = .1f + .8f * light))
        drawPath(star, colour, style = Stroke(1.dp.toPx()))
        repeat(4) { i ->
            val a = i * PI / 2 + PI / 4
            val axis = Offset(cos(a).toFloat(), sin(a).toFloat())
            drawLine(QuestGold.copy(alpha = light * .7f), center + axis * r * 1.2f,
                center + axis * r * 1.4f, .6.dp.toPx())
        }
    }
}

internal fun ChallengeCondition.starLabel(): String = when (this) {
    ChallengeCondition.LOCATION_INSIDE -> "In range"
    ChallengeCondition.PHONE_HORIZONTAL -> "Phone level"
    ChallengeCondition.HEADING_ALIGNED -> "Direction aligned"
    ChallengeCondition.STATIONARY -> "Standing still"
    ChallengeCondition.STABLE -> "Steady hands"
    ChallengeCondition.STILLNESS -> "Remain still"
    ChallengeCondition.ROTATION_STILL -> "No rotation"
    ChallengeCondition.SOUND_DETECTED -> "Sound detected"
    ChallengeCondition.PHOTO_CAPTURED -> "Photograph"
}

private fun ChallengeCondition.observationStarLabel(isAtlas: Boolean): String = when (this) {
    ChallengeCondition.LOCATION_INSIDE -> if (isAtlas) "Near Atlas" else "In range"
    ChallengeCondition.HEADING_ALIGNED -> "Aligned"
    ChallengeCondition.STILLNESS -> "Stillness"
    ChallengeCondition.ROTATION_STILL -> "No turning"
    else -> starLabel()
}
