package com.comp90018.app.features.treasurechallenge

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.comp90018.app.contextengine.challenge.ChallengeCondition
import com.comp90018.app.contextengine.challenge.RelicChallengeType
import kotlin.math.*

private val QuestInk = Color(0xFF344C3D)
private val QuestGold = Color(0xFFE8B75B)

private data class QuestDesign(val label: String, val title: String, val hint: String)
private fun RelicChallengeType.design() = when (this) {
    RelicChallengeType.UNION_LAWN_PHOTO -> QuestDesign("FIELD PHOTOGRAPHY", "Capture the lost lake", "Turn toward the lake site. When the direction locks, take your discovery photo.")
    RelicChallengeType.WILSON_HALL_OBSERVATION -> QuestDesign("STONE OBSERVATORY", "Awaken the rosette", "Face the stone rosette and hold your phone still. Watch its petals light up.")
    RelicChallengeType.OLD_QUAD_EXCAVATION -> QuestDesign("FOSSIL EXCAVATION", "Unearth the ancient fern", "Hold your phone flat and steady. Each moment of balance clears another layer of soil.")
    RelicChallengeType.SOUTH_LAWN_VIEWING_ANGLE -> QuestDesign("SCULPTURE ALIGNMENT", "Find Atlas’s perspective", "Turn slowly to find the viewing angle, then hold steady to lock the sculpture in place.")
    RelicChallengeType.SYSTEM_GARDEN_GLASSHOUSE -> QuestDesign("BOTANICAL EXPEDITION", "Frame the lost glasshouse", "Explore the glasshouse site and take a photo to preserve this piece of garden history.")
    RelicChallengeType.GRAINGER_MUSEUM_TONE_TOOL -> QuestDesign("SOUND EXPLORATION", "Bring the melody to life", "Enable your microphone and hum a sustained note. Keep the sound going to fill the melody ring.")
}

/** Artwork follows actual challenge progress; it never completes or bypasses sensor rules. */
@Composable
internal fun ChallengeExperience(state: TreasureChallengeUiState) {
    val design = state.challengeType.design()
    val progress by animateFloatAsState(state.holdProgress.toFloat().coerceIn(0f, 1f), label = "quest_art_progress")
    val angle by animateFloatAsState((state.angularErrorDegrees ?: 0.0).toFloat(), label = "quest_art_heading")
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = QuestInk),
    ) {
        Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(design.label, color = QuestGold, style = MaterialTheme.typography.labelMedium)
            Text(design.title, color = Color.White, style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold)
            Box(Modifier.fillMaxWidth().height(210.dp), contentAlignment = Alignment.Center) {
                Canvas(Modifier.size(200.dp)) {
                    val c = center
                    val r = size.minDimension * .42f
                    drawCircle(Color.White.copy(alpha = .06f), r)
                    drawCircle(QuestGold.copy(alpha = .22f), r, style = Stroke(3.dp.toPx()))
                    drawArc(QuestGold, -90f, 360f * progress, false,
                        Offset(c.x-r, c.y-r), Size(r*2, r*2), style = Stroke(6.dp.toPx()))
                    when (state.challengeType) {
                        RelicChallengeType.GRAINGER_MUSEUM_TONE_TOOL -> {
                            val sound = state.conditionStates.any { it.condition == ChallengeCondition.SOUND_DETECTED && it.satisfied }
                            for (i in -5..5) {
                                val h = (if (sound) 16f + 35f * abs(sin(i * 1.7f)) else 8f) * density
                                val x = c.x + i * 10.dp.toPx()
                                drawLine(QuestGold, Offset(x, c.y-h/2), Offset(x, c.y+h/2), 5.dp.toPx())
                            }
                        }
                        RelicChallengeType.OLD_QUAD_EXCAVATION -> {
                            drawLine(QuestGold, Offset(c.x, c.y+45.dp.toPx()), Offset(c.x, c.y-45.dp.toPx()), 3.dp.toPx())
                            for (i in 0..5) {
                                val y = c.y + (30-i*12).dp.toPx()
                                val w = (30-i*3).dp.toPx()
                                drawLine(QuestGold, Offset(c.x,y), Offset(c.x-w,y-15.dp.toPx()), 3.dp.toPx())
                                drawLine(QuestGold, Offset(c.x,y), Offset(c.x+w,y-15.dp.toPx()), 3.dp.toPx())
                            }
                            drawRect(QuestInk.copy(alpha = .85f), Offset(c.x-r, c.y-r), Size(r*2, r*2*(1-progress)))
                        }
                        RelicChallengeType.WILSON_HALL_OBSERVATION -> {
                            for (i in 0..7) {
                                val a = i * PI / 4
                                drawCircle(if (i < progress*8) QuestGold else QuestGold.copy(alpha=.25f),
                                    13.dp.toPx(), Offset(c.x+cos(a).toFloat()*42.dp.toPx(), c.y+sin(a).toFloat()*42.dp.toPx()))
                            }
                            drawCircle(QuestGold, 15.dp.toPx())
                        }
                        RelicChallengeType.SOUTH_LAWN_VIEWING_ANGLE -> {
                            drawCircle(QuestGold.copy(alpha=.6f), 40.dp.toPx(), style=Stroke(2.dp.toPx()))
                            drawOval(QuestGold, Offset(c.x-18.dp.toPx(),c.y-40.dp.toPx()), Size(36.dp.toPx(),80.dp.toPx()), style=Stroke(2.dp.toPx()))
                            val a = (angle-90) * PI / 180
                            drawLine(Color.White, c, Offset(c.x+cos(a).toFloat()*55.dp.toPx(),c.y+sin(a).toFloat()*55.dp.toPx()), 4.dp.toPx())
                        }
                        else -> {
                            val w = 52.dp.toPx()
                            val h = 36.dp.toPx()
                            drawRoundRect(QuestGold, Offset(c.x-w,c.y-h), Size(w*2,h*2), cornerRadius=androidx.compose.ui.geometry.CornerRadius(10.dp.toPx()), style=Stroke(3.dp.toPx()))
                            drawCircle(QuestGold, 22.dp.toPx(), style=Stroke(3.dp.toPx()))
                            drawCircle(Color.White, 5.dp.toPx(), Offset(c.x+37.dp.toPx(),c.y-22.dp.toPx()))
                        }
                    }
                }
            }
            Text(if (state.completed) "Discovery unlocked" else if (state.actionReady) "Ready for your photo" else state.instructionText,
                color = Color.White, style = MaterialTheme.typography.titleMedium)
            Text(design.hint, color = Color.White.copy(alpha=.75f), style = MaterialTheme.typography.bodyMedium)
            var showGuide by remember(state.challengeId) { mutableStateOf(false) }
            TextButton(onClick = { showGuide = !showGuide },
                colors = ButtonDefaults.textButtonColors(contentColor = QuestGold)) {
                Text(if (showGuide) "Close field guide" else "How to play")
            }
            if (showGuide) {
                Text(state.challengeType.taskInstructions(), color = Color.White,
                    style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}
