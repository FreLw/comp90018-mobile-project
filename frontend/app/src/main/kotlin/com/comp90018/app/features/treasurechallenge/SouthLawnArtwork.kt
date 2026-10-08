package com.comp90018.app.features.treasurechallenge

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.*
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.comp90018.app.contextengine.challenge.ChallengeCondition
import com.comp90018.app.contextengine.challenge.ChallengePoise
import kotlin.math.*

@Composable
internal fun SouthLawnEmblem(state: TreasureChallengeUiState, modifier: Modifier = Modifier) {
    val signals = QuestVisualSignals.from(state)
    @Composable fun smooth(target: Float, name: String): Float =
        animateFloatAsState(target, spring(dampingRatio = 1f, stiffness = 110f), label = name).value
    val heading = smooth(signals.headingError, "atlas_heading")
    val roll = smooth(signals.roll, "atlas_roll")
    val pitch = smooth(signals.pitch, "atlas_pitch")
    val proximity = smooth(questApproachFill(signals.distanceMeters, state.insideRadiusMeters), "atlas_vines")
    val poise = state.latestSnapshot?.let(ChallengePoise::stillnessScore)
    val turn = state.latestSnapshot?.let(ChallengePoise::turnScore)
    val movement = smooth(((poise ?: 0.0) - .4).coerceIn(0.0, 1.0).toFloat(), "atlas_movement")
    val turning = smooth(((turn ?: 0.0) - .4).coerceIn(0.0, 2.0).toFloat() / 2f, "atlas_turn")
    val transition = rememberInfiniteTransition(label = "atlas_living_ornament")
    val phase by transition.animateFloat(0f, (2 * PI).toFloat(),
        infiniteRepeatable(tween(14000, easing = LinearEasing)), label = "atlas_breath")
    val lights = listOf(ChallengeCondition.LOCATION_INSIDE, ChallengeCondition.HEADING_ALIGNED,
        ChallengeCondition.STILLNESS, ChallengeCondition.ROTATION_STILL).associateWith {
        animateFloatAsState(if (state.conditionLit(it)) 1f else 0f, tween(650), label = "atlas_" + it.name).value
    }
    Canvas(modifier.testTag("quest_emblem").semantics {
        contentDescription = "Atlas, rotating cross stars and distance-gilded vines"
    }) {
        val unit = size.minDimension / 280f
        withTransform({
            translate(center.x, center.y)
            scale(unit, unit, Offset.Zero)
        }) {
            val sway = sin(phase) * 1.5f + roll * .1f
            val pulse = sin(phase * 55f)
            val tremor = pulse * movement
            for (side in listOf(-1f, 1f)) {
                withTransform({
                    translate(side * (sin(phase) * 1.4f + proximity * 2f), pitch * .08f)
                    scale(side, 1f, Offset.Zero)
                    rotate(sway, Offset(104f, 100f))
                }) {
                    drawAtlasVine(QuestStone.copy(alpha = .34f), false)
                    clipRect(left = 65f, top = -129f, right = 137f, bottom = -129f + proximity * 256f) {
                        drawAtlasVine(QuestGold, true)
                    }
                }
                // Even at rest the stars revolve; acceleration and shake strongly disturb them.
                listOf(Offset(89f, -81f), Offset(98f, -13f), Offset(87f, 66f)).forEachIndexed { index, p ->
                    val starCenter = Offset(side * p.x + tremor * (17f - index * 2f),
                        p.y + cos(phase * 47f + index) * movement * 12f + sin(phase + index) * 1.2f)
                    val light = lights.getValue(ChallengeCondition.STILLNESS)
                    val ink = lerp(QuestStone.copy(alpha = .55f), QuestGold, light)
                    rotate(side * phase * 180f / PI.toFloat() + index * 28f + tremor * 38f, starCenter) {
                        drawCrossStar(starCenter, if (index == 1) 13f else 7f, ink, light)
                    }
                }
            }

            val alignedLight = lights.getValue(ChallengeCondition.HEADING_ALIGNED)
            val turnLight = lights.getValue(ChallengeCondition.ROTATION_STILL)
            val stillLight = lights.getValue(ChallengeCondition.STILLNESS)
            val alignedInk = lerp(QuestStone.copy(alpha = .62f), QuestGold, alignedLight)
            val turnInk = lerp(QuestStone.copy(alpha = .4f), QuestGold, turnLight)
            val stillInk = lerp(QuestStone.copy(alpha = .65f), QuestGold, stillLight)
            val globe = Offset(0f, -44f)

            // The wider armillary orbit visibly wavers with gyroscope readings.
            rotate(heading * .22f + sin(phase * 31f) * turning * 35f, globe) {
                val orbit = Rect(globe - Offset(62f, 54f), Size(124f, 108f))
                drawArc(turnInk.copy(alpha = .6f), 195f, 150f, false, orbit.topLeft, orbit.size, style = Stroke(1f))
                // Leave an opening over Atlas's face and supporting shoulders.
                for (start in listOf(30f, 112f)) {
                    drawArc(turnInk, start, 38f, false, orbit.topLeft, orbit.size, style = Stroke(1.4f))
                    if (turnLight > 0f) drawArc(QuestGold.copy(alpha = .09f * turnLight), start, 38f,
                        false, orbit.topLeft, orbit.size, style = Stroke(7f))
                }
                repeat(9) { i ->
                    if (i in 3..5) return@repeat
                    val a = (30f + i * 15f) * PI / 180
                    val axis = Offset(cos(a).toFloat(), sin(a).toFloat())
                    drawLine(turnInk.copy(alpha = .7f), globe + Offset(axis.x * 64f, axis.y * 56f),
                        globe + Offset(axis.x * 68f, axis.y * 60f), .8f)
                }
                drawCrossStar(globe + Offset(0f, -55f), 4.5f, turnInk, turnLight)
            }

            translate(roll * .12f + tremor * 9f, pitch * .1f + sin(phase * 49f) * movement * 4f) {
                rotate(heading * .32f, globe) {
                    drawCircle(alignedInk.copy(alpha = .06f), 43f, globe)
                    if (alignedLight > 0f) drawCircle(QuestGold.copy(alpha = .08f * alignedLight),
                        43f, globe, style = Stroke(7f))
                    drawCircle(alignedInk, 43f, globe, style = Stroke(1.6f))
                    drawOval(alignedInk.copy(alpha = .75f), globe - Offset(18f, 43f), Size(36f, 86f), style = Stroke(.85f))
                    drawOval(alignedInk.copy(alpha = .75f), globe - Offset(43f, 17f), Size(86f, 34f), style = Stroke(.85f))
                    drawLine(alignedInk.copy(alpha = .7f), globe - Offset(43f, 0f), globe + Offset(43f, 0f), .8f)
                    drawLine(alignedInk.copy(alpha = .7f), globe - Offset(0f, 43f), globe + Offset(0f, 43f), .8f)
                    rotate(-24f, globe) {
                        drawOval(alignedInk, globe - Offset(47f, 15f), Size(94f, 30f), style = Stroke(1.8f))
                    }
                    drawCircle(alignedInk, 2.6f, globe + Offset(heading / 180f * 30f, 0f))
                }
                rotate(tremor * 7f, Offset(0f, 68f)) { drawAtlasSilhouette(stillInk, stillLight) }
            }

            // A small, carved plinth rather than a progress track.
            val plinthInk = lerp(QuestStone.copy(alpha = .45f), QuestGold,
                lights.getValue(ChallengeCondition.LOCATION_INSIDE))
            val plinth = Path().apply {
                moveTo(-43f, 85f); lineTo(53f, 85f); lineTo(58f, 91f)
                lineTo(58f, 97f); lineTo(-48f, 97f); lineTo(-48f, 91f); close()
            }
            drawPath(plinth, plinthInk.copy(alpha = .12f))
            drawPath(plinth, plinthInk, style = Stroke(1.2f))
            drawLine(plinthInk.copy(alpha = .65f), Offset(-46f, 91f), Offset(56f, 91f), .7f)
            drawCrossStar(Offset(0f, 115f), 4f, QuestGold.copy(alpha = .55f), 0f)
            for (side in listOf(-1f, 1f)) {
                val flourish = Path().apply {
                    moveTo(side * 10f, 115f)
                    cubicTo(side * 27f, 105f, side * 39f, 124f, side * 58f, 113f)
                    cubicTo(side * 68f, 106f, side * 54f, 103f, side * 52f, 112f)
                }
                drawPath(flourish, QuestGold.copy(alpha = .4f), style = Stroke(.85f))
            }
        }
    }
}

/** Mirrored acanthus scrollwork; no repeated leaf icons. Coordinates are in emblem units. */
private fun DrawScope.drawAtlasVine(ink: Color, glowing: Boolean) {
    val paths = listOf(
        Path().apply {
            moveTo(92f, -123f)
            cubicTo(129f, -114f, 115f, -85f, 107f, -56f)
            cubicTo(95f, -16f, 127f, 3f, 118f, 37f)
            cubicTo(108f, 73f, 92f, 83f, 101f, 113f)
            cubicTo(105f, 127f, 126f, 117f, 115f, 109f)
            cubicTo(108f, 105f, 105f, 113f, 112f, 115f)
        },
        Path().apply {
            moveTo(103f, -92f)
            cubicTo(76f, -108f, 81f, -124f, 94f, -117f)
            cubicTo(103f, -111f, 93f, -102f, 88f, -109f)
        },
        Path().apply {
            moveTo(108f, -55f)
            cubicTo(137f, -81f, 130f, -101f, 119f, -95f)
            cubicTo(110f, -91f, 113f, -78f, 122f, -85f)
        },
        Path().apply {
            moveTo(109f, -22f)
            cubicTo(78f, -38f, 77f, -56f, 92f, -53f)
            cubicTo(102f, -51f, 98f, -40f, 89f, -44f)
        },
        Path().apply {
            moveTo(119f, 18f)
            cubicTo(139f, -8f, 135f, -28f, 123f, -20f)
            cubicTo(115f, -15f, 121f, -5f, 126f, -12f)
        },
        Path().apply {
            moveTo(111f, 59f)
            cubicTo(81f, 43f, 75f, 24f, 88f, 26f)
            cubicTo(99f, 28f, 93f, 39f, 85f, 33f)
        },
        Path().apply {
            moveTo(98f, 88f)
            cubicTo(125f, 71f, 135f, 43f, 124f, 44f)
            cubicTo(113f, 45f, 116f, 59f, 123f, 52f)
        },
    )
    paths.forEachIndexed { index, path ->
        if (glowing) drawPath(path, ink.copy(alpha = .08f), style = Stroke(5f))
        drawPath(path, ink.copy(alpha = ink.alpha * if (index == 0) .95f else .8f),
            style = Stroke(if (index == 0) 1.6f else 1.05f, cap = StrokeCap.Round))
    }
    // Fine parallel strokes recall engraved botanical ornaments without adding depth.
    val fine = Path().apply {
        moveTo(98f, -119f)
        cubicTo(119f, -108f, 109f, -80f, 102f, -58f)
        moveTo(116f, 22f)
        cubicTo(115f, 49f, 94f, 80f, 98f, 105f)
    }
    drawPath(fine, ink.copy(alpha = ink.alpha * .42f), style = Stroke(.6f))
}

internal fun DrawScope.drawCrossStar(at: Offset, radius: Float, ink: Color, glow: Float = 0f) {
    val star = Path().apply {
        moveTo(at.x, at.y - radius)
        quadraticTo(at.x + radius * .13f, at.y - radius * .13f, at.x + radius, at.y)
        quadraticTo(at.x + radius * .13f, at.y + radius * .13f, at.x, at.y + radius)
        quadraticTo(at.x - radius * .13f, at.y + radius * .13f, at.x - radius, at.y)
        quadraticTo(at.x - radius * .13f, at.y - radius * .13f, at.x, at.y - radius)
        close()
    }
    if (glow > 0f) drawCircle(QuestGold.copy(alpha = .075f * glow), radius * 1.6f, at)
    drawPath(star, ink.copy(alpha = ink.alpha * .35f))
    drawPath(star, ink, style = Stroke(.9f))
    drawCircle(ink, 1f, at)
}

/** A bowed head, swept arms and a kneeling, draped figure echo Atlas supporting the heavens. */
private fun DrawScope.drawAtlasSilhouette(ink: Color, light: Float) {
    val body = Path().apply {
        moveTo(-10f, 22f)
        cubicTo(-18f, 20f, -24f, 12f, -29f, 3f)
        cubicTo(-32f, -2f, -38f, -9f, -34f, -13f)
        cubicTo(-31f, -15f, -30f, -9f, -27f, -5f)
        cubicTo(-22f, 2f, -15f, 8f, -6f, 15f)
        cubicTo(1f, 20f, 9f, 15f, 16f, 9f)
        cubicTo(23f, 4f, 28f, -5f, 30f, -11f)
        cubicTo(31f, -17f, 36f, -15f, 35f, -9f)
        cubicTo(35f, 1f, 29f, 13f, 20f, 21f)
        cubicTo(10f, 29f, 7f, 38f, 11f, 49f)
        cubicTo(20f, 45f, 30f, 41f, 37f, 46f)
        cubicTo(45f, 52f, 40f, 63f, 39f, 73f)
        cubicTo(39f, 78f, 47f, 78f, 49f, 82f)
        cubicTo(45f, 86f, 32f, 83f, 29f, 80f)
        cubicTo(29f, 74f, 34f, 64f, 31f, 56f)
        cubicTo(20f, 60f, 10f, 63f, -2f, 57f)
        cubicTo(-7f, 67f, -11f, 75f, -18f, 79f)
        cubicTo(-24f, 82f, -35f, 81f, -38f, 78f)
        cubicTo(-39f, 75f, -33f, 73f, -27f, 75f)
        cubicTo(-18f, 74f, -18f, 62f, -15f, 53f)
        cubicTo(-11f, 43f, -17f, 32f, -10f, 22f)
        close()
    }
    if (light > 0f) drawPath(body, QuestGold.copy(alpha = .08f * light), style = Stroke(6f))
    drawPath(body, ink.copy(alpha = ink.alpha * .32f))
    drawPath(body, ink, style = Stroke(1.4f, cap = StrokeCap.Round, join = StrokeJoin.Round))
    // Bowed, bearded profile echoes the relic's sculpture, with one knee raised beneath it.
    val head = Path().apply {
        moveTo(-8f, 17f)
        cubicTo(-14f, 14f, -13f, 5f, -8f, 2f)
        cubicTo(-2f, -2f, 6f, 1f, 5f, 9f)
        lineTo(7f, 13f); lineTo(3f, 14f)
        cubicTo(3f, 20f, -1f, 24f, -5f, 21f)
        quadraticTo(-8f, 20f, -8f, 17f); close()
    }
    drawPath(head, ink.copy(alpha = ink.alpha * .55f))
    drawPath(head, ink, style = Stroke(1.2f))
    val carving = Path().apply {
        moveTo(-10f, 8f); quadraticTo(-8f, 2f, -5f, 5f)
        quadraticTo(-2f, 0f, 1f, 4f); quadraticTo(4f, 2f, 4f, 7f)
        moveTo(-4f, 17f); quadraticTo(-1f, 20f, 1f, 17f)
        moveTo(-1f, 10f); lineTo(2f, 11f)
        moveTo(-6f, 27f); cubicTo(-12f, 36f, 0f, 43f, 3f, 48f)
        moveTo(-13f, 49f); quadraticTo(-4f, 54f, 7f, 51f)
        moveTo(-10f, 55f); quadraticTo(-12f, 69f, -20f, 76f)
        moveTo(0f, 55f); quadraticTo(14f, 59f, 33f, 51f)
        moveTo(34f, 58f); quadraticTo(38f, 62f, 35f, 75f)
        moveTo(-20f, 13f); quadraticTo(-17f, 19f, -12f, 21f)
        moveTo(15f, 18f); quadraticTo(22f, 13f, 25f, 5f)
    }
    drawPath(carving, ink.copy(alpha = ink.alpha * .65f), style = Stroke(.75f, cap = StrokeCap.Round))
}
