package com.comp90018.app.features.treasurechallenge

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import com.comp90018.app.GothicTreasureFontFamily
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
    RelicChallengeType.GRAINGER_MUSEUM_TONE_TOOL -> QuestDesign("SOUND EXPLORATION", "Bring the melody to life", "Enable your microphone and hum a sustained note. Keep the sound going to awaken the engraved sound waves.")
}

/** Artwork follows sensor readings and evaluator progress without changing completion rules. */
@Composable
internal fun ChallengeExperience(state: TreasureChallengeUiState) {
    val design = state.challengeType.design()
    val entrance = remember(state.challengeId) { Animatable(0f) }
    LaunchedEffect(state.challengeId) { entrance.animateTo(1f, tween(1100, easing = FastOutSlowInEasing)) }
    Column(Modifier.fillMaxWidth().padding(horizontal = 18.dp),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Column(Modifier.graphicsLayer {
            alpha = entrance.value
            translationY = (1f - entrance.value) * 28.dp.toPx()
            rotationX = (1f - entrance.value) * -18f
        }, horizontalAlignment = Alignment.CenterHorizontally) {
            Text("✦ ${design.label} ✦", color = QuestGold, style = MaterialTheme.typography.labelMedium)
            Text(design.title, color = Color(0xFFFFE6B1), fontFamily = GothicTreasureFontFamily,
                style = MaterialTheme.typography.headlineLarge, textAlign = TextAlign.Center)
        }
        QuestEmblem(state, Modifier.fillMaxWidth().height(230.dp).graphicsLayer {
            alpha = entrance.value
            scaleX = .7f + .3f * entrance.value
            scaleY = scaleX
            rotationZ = (1f - entrance.value) * -12f
        })
        key(state.instructionText) {
            var visible by remember { mutableStateOf(false) }
            LaunchedEffect(Unit) { visible = true }
            AnimatedVisibility(visible, enter = fadeIn(tween(650)) + expandVertically(tween(650))) {
                Text(if (state.completed) "Discovery unlocked" else if (state.actionReady) "Ready for your photo" else state.instructionText,
                    color = Color(0xFFFFE6B1), fontFamily = GothicTreasureFontFamily,
                    style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
            }
        }
        Text(design.hint, color = Color.White.copy(alpha = .8f), style = MaterialTheme.typography.bodyMedium,
            fontFamily = FontFamily.Serif, textAlign = TextAlign.Center)
        var showGuide by remember(state.challengeId) { mutableStateOf(false) }
        TextButton(onClick = { showGuide = !showGuide }, colors = ButtonDefaults.textButtonColors(contentColor = QuestGold)) {
            Text(if (showGuide) "Close field guide" else "How to play", fontFamily = GothicTreasureFontFamily)
        }
        if (showGuide) Text(state.challengeType.taskInstructions(), color = Color.White, fontFamily = FontFamily.Serif)
    }
}

@Composable
private fun questPhase(): Float {
    val transition = rememberInfiniteTransition(label = "engraving_motion")
    val phase by transition.animateFloat(0f, (2 * PI).toFloat(),
        infiniteRepeatable(tween(7000, easing = LinearEasing), RepeatMode.Restart), label = "engraving_phase")
    return phase
}

@Composable
internal fun QuestBackdrop(state: TreasureChallengeUiState, modifier: Modifier = Modifier) {
    val phase = questPhase()
    val progress by animateFloatAsState(state.holdProgress.toFloat(), label = "backdrop_progress")
    val sound = state.conditionStates.any { it.condition == ChallengeCondition.SOUND_DETECTED && it.satisfied }
    val response by animateFloatAsState(
        if (state.completed || state.actionReady) 1f else
            state.conditionStates.count { it.satisfied }.toFloat() / state.conditionStates.size.coerceAtLeast(1),
        label = "sensor_response")
    val soundLevel by animateFloatAsState(
        (((state.latestSnapshot?.sound?.decibels ?: -80.0) + 80.0) / 80.0).toFloat().coerceIn(0f, 1f),
        label = "sound_wave_strength")
    val heading by animateFloatAsState((state.angularErrorDegrees ?: 0.0).toFloat(), label = "backdrop_heading")
    val roll by animateFloatAsState((state.latestSnapshot?.orientation?.attitude?.rollDegrees ?: 0.0).toFloat(), label = "backdrop_balance")
    Canvas(modifier) {
        drawRect(Brush.verticalGradient(listOf(Color(0xFF142D25), QuestInk, Color(0xFF10251E))))
        val ink = QuestGold.copy(alpha = .06f + response * .08f + progress * .1f)
        when (state.challengeType) {
            RelicChallengeType.GRAINGER_MUSEUM_TONE_TOOL -> repeat(16) { row ->
                val path = Path()
                for (i in 0..80) {
                    val x = size.width * i / 80
                    val y = size.height * row / 15 + sin(i * .23f + phase * (if (sound) 3 else 1) + row) * (3 + soundLevel * 30).dp.toPx()
                    if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                }
                drawPath(path, ink, style = Stroke(1.dp.toPx()))
            }
            RelicChallengeType.OLD_QUAD_EXCAVATION -> repeat(32) { i ->
                val y = (size.height * i / 31 + sin(phase + i) * 8 - progress * 100) % size.height
                drawLine(ink, Offset(0f, y), Offset(size.width, y + roll * 2), 1.dp.toPx())
            }
            RelicChallengeType.WILSON_HALL_OBSERVATION -> repeat(24) { i ->
                val a = i * PI / 12 + phase * .04
                val origin = Offset(size.width / 2, size.height * .38f)
                drawLine(ink, origin, origin + Offset(cos(a).toFloat(), sin(a).toFloat()) * size.height, 1.dp.toPx())
            }
            RelicChallengeType.SOUTH_LAWN_VIEWING_ANGLE -> rotate(heading * .15f, center) {
                repeat(12) { i -> drawOval(ink, Offset(-size.width * .3f + i * 18, size.height * .12f),
                    Size(size.width * 1.6f - i * 36, size.height * .65f), style = Stroke(1.dp.toPx())) }
            }
            RelicChallengeType.SYSTEM_GARDEN_GLASSHOUSE -> repeat(20) { i ->
                val x = size.width * i / 19
                drawLine(ink, Offset(x, 0f), Offset(x + sin(phase + i) * 15, size.height), 1.dp.toPx())
                drawLine(ink, Offset(0f, size.height * i / 19), Offset(size.width, size.height * i / 19), 1.dp.toPx())
            }
            RelicChallengeType.UNION_LAWN_PHOTO -> repeat(20) { i ->
                val y = size.height * (.2f + i * .04f)
                drawOval(ink, Offset(-size.width * .2f + sin(phase + i * .3f) * 12 + heading * .2f, y),
                    Size(size.width * 1.4f, 32.dp.toPx()), style = Stroke(1.dp.toPx()))
            }
        }
        repeat(42) { i ->
            val x = size.width * ((i * .618f) % 1f)
            val y = size.height * ((i * .381f + phase * .006f) % 1f)
            drawCircle(QuestGold.copy(alpha = .12f + progress * .2f), 1.5.dp.toPx(), Offset(x, y))
        }
    }
}

@Composable
private fun QuestEmblem(state: TreasureChallengeUiState, modifier: Modifier) {
    val phase = questPhase()
    val progress by animateFloatAsState(state.holdProgress.toFloat(), label = "emblem_progress")
    val heading by animateFloatAsState((state.angularErrorDegrees ?: 0.0).toFloat(), label = "emblem_heading")
    val roll by animateFloatAsState((state.latestSnapshot?.orientation?.attitude?.rollDegrees ?: 0.0).toFloat(), label = "emblem_roll")
    val sound = state.conditionStates.any { it.condition == ChallengeCondition.SOUND_DETECTED && it.satisfied }
    Canvas(modifier) {
        val unit = size.minDimension / 220f
        val gold = QuestGold
        val light = Color(0xFFFFE6B1)
        fun pt(x: Float, y: Float) = center + Offset(x * unit, y * unit)
        fun line(x: Float, y: Float, ex: Float, ey: Float, color: Color = gold, width: Float = 2f) =
            drawLine(color, pt(x, y), pt(ex, ey), width * unit, cap = StrokeCap.Round)
        fun oval(x: Float, y: Float, w: Float, h: Float, color: Color = gold) =
            drawOval(color, pt(x, y), Size(w * unit, h * unit), style = Stroke(2 * unit))
        // Engraved corner flourishes rather than a shared circular enclosure.
        for (side in listOf(-1f, 1f)) {
            val flourish = Path().apply {
                moveTo(pt(side * 96, 65f).x, pt(side * 96, 65f).y)
                cubicTo(pt(side * 120, 15f).x, pt(side * 120, 15f).y, pt(side * 65, 20f).x, pt(side * 65, 20f).y, pt(side * 92, -65f).x, pt(side * 92, -65f).y)
            }
            drawPath(flourish, gold.copy(alpha = .55f), style = Stroke(unit))
            repeat(5) { i -> oval(side * (88 + sin(i.toFloat()) * 7), -55 + i * 24f, 9f, 16f, gold.copy(alpha = .4f)) }
        }
        when (state.challengeType) {
            RelicChallengeType.UNION_LAWN_PHOTO -> rotate(heading * .08f) {
                drawRoundRect(gold.copy(alpha = .14f), pt(-64f, -35f), Size(128 * unit, 80 * unit), CornerRadius(8 * unit))
                drawRoundRect(gold, pt(-64f, -35f), Size(128 * unit, 80 * unit), CornerRadius(8 * unit), style = Stroke(3 * unit))
                oval(-33f, -31f, 66f, 66f); oval(-25f, -23f, 50f, 50f, light)
                repeat(8) { i -> val a = i * PI / 4 + phase * .08; line(cos(a).toFloat() * 24, sin(a).toFloat() * 24 + 2, cos(a + .5).toFloat() * 12, sin(a + .5).toFloat() * 12 + 2) }
                line(-42f, -35f, -42f, -52f); line(-42f, -52f, -12f, -52f); line(-12f, -52f, -12f, -35f)
                oval(38f, -23f, 13f, 10f)
                repeat(5) { i -> line(-58f + i * 3, -24f, -58f + i * 3, 33f, gold.copy(alpha = .4f), 1f) }
                line(-72f, 60f, 72f, 60f, light.copy(alpha = if (state.actionReady) 1f else .3f))
            }
            RelicChallengeType.WILSON_HALL_OBSERVATION -> rotate(sin(phase) * (1 - progress) * 3) {
                repeat(12) { i -> rotate(i * 30f) {
                    val petal = Path().apply { moveTo(pt(0f, 0f).x, pt(0f, 0f).y); cubicTo(pt(-34f, -44f).x, pt(-34f, -44f).y, pt(-15f, -83f).x, pt(-15f, -83f).y, pt(0f, -87f).x, pt(0f, -87f).y); cubicTo(pt(15f, -83f).x, pt(15f, -83f).y, pt(34f, -44f).x, pt(34f, -44f).y, center.x, center.y) }
                    drawPath(petal, gold.copy(alpha = if (i < progress * 12) .65f else .08f))
                    drawPath(petal, gold, style = Stroke(1.5f * unit)); line(0f, -20f, 0f, -73f, light.copy(alpha = .6f), 1f)
                } }
                oval(-16f, -16f, 32f, 32f, light)
            }
            RelicChallengeType.OLD_QUAD_EXCAVATION -> rotate(roll.coerceIn(-25f, 25f)) {
                line(0f, 82f, 0f, -78f, light, 3f)
                repeat(9) { i -> val y = 55f - i * 15; val w = 48f - i * 4
                    for (side in listOf(-1f, 1f)) {
                        val leaf = Path().apply { moveTo(center.x, pt(0f, y).y); quadraticTo(pt(side * w, y - 42).x, pt(side * w, y - 42).y, pt(side * w, y - 18).x, pt(side * w, y - 18).y); quadraticTo(pt(side * w, y + 5).x, pt(side * w, y + 5).y, center.x, pt(0f, y).y) }
                        drawPath(leaf, gold.copy(alpha = .15f + progress * .6f)); drawPath(leaf, gold, style = Stroke(unit))
                    }
                }
                repeat(22) { i -> val y = -90f + progress * 180 + i * 8; if (y < 90) line(-65f, y, 65f, y, QuestInk.copy(alpha = .8f), 4f) }
            }
            RelicChallengeType.SOUTH_LAWN_VIEWING_ANGLE -> {
                rotate(heading * .3f) {
                    oval(-54f, -82f, 108f, 108f); oval(-23f, -82f, 46f, 108f, light)
                    rotate(35f) { oval(-54f, -59f, 108f, 62f) }
                    line(-62f, -28f, 62f, -28f); line(0f, -88f, 0f, 30f, light)
                }
                oval(-10f, 28f, 20f, 20f, light)
                line(-28f, 62f, 0f, 46f, light, 4f); line(0f, 46f, 28f, 62f, light, 4f)
                line(0f, 48f, 0f, 82f, light, 4f); line(-28f, 94f, 0f, 80f); line(0f, 80f, 28f, 94f)
            }
            RelicChallengeType.SYSTEM_GARDEN_GLASSHOUSE -> {
                line(-65f, -18f, 0f, -76f, light); line(0f, -76f, 65f, -18f, light)
                line(-65f, -18f, 65f, -18f); line(-65f, -18f, -65f, 76f); line(65f, -18f, 65f, 76f); line(-65f, 76f, 65f, 76f)
                for (i in -2..2) { line(i * 22f, -18f, i * 22f, 76f); line(0f, -76f, i * 22f, -18f) }
                line(-65f, 15f, 65f, 15f); line(-65f, 45f, 65f, 45f)
                repeat(5) { i -> val x = -48f + i * 24; val sway = sin(phase + i) * 5
                    line(x, 71f, x + sway, 20f, light.copy(alpha = .7f))
                    oval(x - 13 + sway, 26f, 13f, 24f); oval(x + sway, 40f, 13f, 20f)
                }
                oval(-7f, -93f, 14f, 14f)
            }
            RelicChallengeType.GRAINGER_MUSEUM_TONE_TOOL -> {
                val horn = Path().apply { moveTo(pt(-15f, 25f).x, pt(-15f, 25f).y); lineTo(pt(-42f, -53f).x, pt(-42f, -53f).y); quadraticTo(pt(10f, -83f).x, pt(10f, -83f).y, pt(62f, -48f).x, pt(62f, -48f).y); lineTo(pt(8f, 28f).x, pt(8f, 28f).y); close() }
                drawPath(horn, gold.copy(alpha = .2f)); drawPath(horn, gold, style = Stroke(3 * unit))
                oval(-42f, -72f, 104f, 40f, light)
                repeat(5) { i -> line(-15f, 25f, -32f + i * 20, -51f, gold.copy(alpha = .6f), 1f) }
                line(-15f, 25f, -15f, 51f); line(-15f, 51f, 28f, 51f)
                oval(-55f, 48f, 110f, 19f); line(-58f, 65f, -58f, 85f); line(-58f, 85f, 58f, 85f); line(58f, 85f, 58f, 65f)
                repeat(4) { i -> val radius = (65 + i * 12 + sin(phase * 3) * (if (sound) 5 else 0)) * unit
                    drawArc(gold.copy(alpha = if (sound) .65f - i * .12f else .12f), -42f, 60f, false,
                        pt(10f, -50f) - Offset(radius, radius), Size(radius * 2, radius * 2), style = Stroke(2 * unit))
                }
            }
        }
    }
}
