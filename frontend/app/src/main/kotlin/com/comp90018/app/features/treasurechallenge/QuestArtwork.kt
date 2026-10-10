package com.comp90018.app.features.treasurechallenge

/*
 * Draws shared living backgrounds and the camera, fern, glasshouse, and gramophone emblems.
 * Maps visual sensor signals and condition states to motion, gold fill, ripples, and engraved ornaments.
 */

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
import androidx.compose.ui.semantics.stateDescription
import com.comp90018.app.contextengine.challenge.ChallengeCondition
import com.comp90018.app.contextengine.challenge.RelicChallengeType
import kotlin.math.*

// Shared green-task visual tokens: active gold, parchment text, inactive grey, and forest background.
internal val QuestGold = Color(0xFFE7BF70)
internal val QuestParchment = Color(0xFFF5E5BB)
internal val QuestStone = Color(0xFF7F9285)
internal val QuestForest = Color(0xFF122F27)
private val QuestDeep = Color(0xFF0B221C)

private data class ArtworkMotion(
    val heading: Float, val roll: Float, val pitch: Float, val movement: Float,
    val instability: Float, val rotation: Float, val sound: Float, val proximity: Float,
)

/** Smooths normalized sensor inputs so drawing changes follow the device without abrupt jumps. */
@Composable
private fun artworkMotion(state: TreasureChallengeUiState): ArtworkMotion {
    val values = QuestVisualSignals.from(state)
    @Composable fun smooth(value: Float, label: String): Float =
        animateFloatAsState(value, spring(dampingRatio = 1f, stiffness = 90f), label = label).value
    return ArtworkMotion(
        smooth(values.headingError, "heading"), smooth(values.roll, "roll"), smooth(values.pitch, "pitch"),
        smooth(values.motion, "motion"), smooth(values.instability, "stability"),
        smooth(values.rotation, "rotation"), smooth(values.sound, "sound"), smooth(values.proximity, "distance"),
    )
}

/** Supplies a slow looping clock for living artwork even when device readings stay unchanged. */
@Composable
private fun ambientPhase(): Float {
    val transition = rememberInfiniteTransition(label = "living_engraving")
    return transition.animateFloat(0f, (2 * PI).toFloat(),
        infiniteRepeatable(tween(14000, easing = LinearEasing)), label = "breathing").value
}

/** Calm ambient engraving remains alive; sensor parallax is layered over it. */
@Composable
internal fun QuestBackdrop(
    state: TreasureChallengeUiState,
    modifier: Modifier = Modifier,
    emblemCenter: Offset? = null,
    drawBackground: Boolean = true,
) {
    if (state.challengeType == RelicChallengeType.WILSON_HALL_OBSERVATION) {
        WilsonHallBackdrop(state, modifier, emblemCenter)
        return
    }
    val motion = artworkMotion(state)
    val phase = ambientPhase()
    Canvas(modifier) {
        if (drawBackground) drawRect(QuestForest)
        val origin = Offset(size.width / 2f + motion.roll * .65f + motion.heading * .13f,
            size.height * .32f + motion.pitch * .7f)
        val ink = QuestGold.copy(alpha = .075f)
        withTransform({
            translate(motion.roll * .4f + sin(phase) * (5f + motion.movement * 8f) + motion.proximity * 8f,
                motion.pitch * .4f + cos(phase) * 4f - motion.proximity * 10f)
        }) {
            when (state.challengeType) {
                RelicChallengeType.UNION_LAWN_PHOTO -> repeat(12) { i ->
                    val y = size.height * (.16f + i * .067f) + sin(phase + i * .45f) * 9f
                    drawOval(ink, Offset(-size.width * .3f + motion.heading * .25f, y),
                        Size(size.width * 1.6f, 48f + i * 2f), style = Stroke(1f))
                }
                RelicChallengeType.WILSON_HALL_OBSERVATION -> Unit // Rendered by WilsonHallBackdrop.
                RelicChallengeType.OLD_QUAD_EXCAVATION -> repeat(20) { row ->
                    val path = Path()
                    for (i in 0..50) {
                        val x = size.width * i / 50f
                        val y = size.height * row / 19f + sin(i * .22f + row + phase * .4f) *
                            (10f + motion.movement * 25f) + motion.roll * (i / 50f - .5f)
                        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                    }
                    drawPath(path, ink, style = Stroke(1f))
                }
                RelicChallengeType.SOUTH_LAWN_VIEWING_ANGLE -> {
                    rotate(motion.heading * .12f + sin(phase) * 4f, origin) {
                        repeat(9) { i ->
                            drawOval(ink, origin - Offset(size.width * (.5f - i * .045f), size.height * .4f),
                                Size(size.width * (1f - i * .09f), size.height * .8f), style = Stroke(1f))
                        }
                        drawLine(ink, Offset(origin.x, 0f), Offset(origin.x, size.height), 1f)
                    }
                    repeat(8) { i ->
                        val star = Offset(size.width * (.13f + (i % 2) * .74f) + sin(phase + i) * 14f,
                            size.height * (.12f + i * .105f) + cos(phase + i) * 14f)
                        rotate(phase * 180f / PI.toFloat() + i * 20f, star) {
                            drawCrossStar(star, 11f + (i % 3) * 4f, QuestGold.copy(alpha = .11f))
                        }
                    }
                }
                RelicChallengeType.SYSTEM_GARDEN_GLASSHOUSE -> repeat(14) { i ->
                    val x = size.width * i / 13f
                    val path = Path().apply {
                        moveTo(x, size.height)
                        cubicTo(x - 45f + sin(phase + i) * 12f, size.height * .6f,
                            x + 45f + motion.roll, size.height * .3f, x + sin(phase + i) * 12f, 0f)
                    }
                    drawPath(path, ink, style = Stroke(1f))
                    repeat(4) { j ->
                        val y = size.height * (j + 1) / 5f
                        drawOval(ink.copy(alpha = .045f), Offset(x - 12f, y),
                            Size(24f, 50f), style = Stroke(1f))
                    }
                }
                RelicChallengeType.GRAINGER_MUSEUM_TONE_TOOL -> repeat(18) { row ->
                    val path = Path()
                    for (i in 0..70) {
                        val x = size.width * i / 70f
                        val y = size.height * row / 17f + sin(i * .19f + phase + row * .4f) *
                            (5f + motion.sound * 35f)
                        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                    }
                    drawPath(path, ink, style = Stroke(1f))
                }
            }
        }
        // Slowly drifting pinpricks and corner leaves keep still-phone scenes alive.
        repeat(36) { i ->
            val x = size.width * ((i * .618034f) % 1f) + sin(phase + i) * 6f + motion.roll * .4f
            val y = size.height * ((i * .381966f) % 1f) + cos(phase + i) * 8f + motion.pitch * .4f
            drawCircle(QuestGold.copy(alpha = .08f + (sin(phase + i) + 1f) * .045f), 1.6f, Offset(x, y))
        }
    }
}

/**
 * Selects the task emblem and blends evaluated conditions into gold accents and sensor-driven
 * movement.
 */
@Composable
internal fun QuestEmblem(state: TreasureChallengeUiState, modifier: Modifier = Modifier) {
    if (state.challengeType == RelicChallengeType.WILSON_HALL_OBSERVATION) {
        WilsonHallEmblem(state, modifier)
        return
    }
    if (state.challengeType == RelicChallengeType.SOUTH_LAWN_VIEWING_ANGLE) {
        SouthLawnEmblem(state, modifier)
        return
    }
    val motion = artworkMotion(state)
    val phase = ambientPhase()
    val isLakeCamera = state.challengeType == RelicChallengeType.UNION_LAWN_PHOTO
    val lakeGoldTarget = if (isLakeCamera) questApproachFill(QuestVisualSignals.from(state).distanceMeters,
        state.insideRadiusMeters) else 0f
    val lakeGoldWater by animateFloatAsState(lakeGoldTarget, spring(dampingRatio = 1f, stiffness = 65f),
        label = "lake_distance_gold_water")
    val distanceRipple = if (isLakeCamera) lakeConditionRipple(state, ChallengeCondition.LOCATION_INSIDE) else 1f
    val headingRipple = if (isLakeCamera) lakeConditionRipple(state, ChallengeCondition.HEADING_ALIGNED) else 1f
    val progress by animateFloatAsState(state.holdProgress.toFloat().coerceIn(0f, 1f), tween(250), label = "engraving_hold")
    val lights = ChallengeCondition.entries.associateWith { condition ->
        animateFloatAsState(if (state.conditionLit(condition)) 1f else 0f,
            tween(700), label = condition.name).value
    }
    val photo by animateFloatAsState(if (state.completed || state.capturedPhotoUri != null) 1f else 0f,
        tween(700), label = "photograph")
    Canvas(modifier.testTag("quest_emblem").semantics {
        contentDescription = "Sensor-responsive relic engraving"
        if (isLakeCamera) stateDescription = "Distance semicircle: ${if (state.conditionLit(ChallengeCondition.LOCATION_INSIDE)) "lit" else "unlit"}. " +
            "Heading semicircle: ${if (state.conditionLit(ChallengeCondition.HEADING_ALIGNED)) "lit" else "unlit"}. " +
            "Distance ripple: ${if (distanceRipple < 1f) "expanding" else "settled"}. " +
            "Heading ripple: ${if (headingRipple < 1f) "expanding" else "settled"}. " +
            "Camera gold water: ${(lakeGoldTarget * 100).roundToInt()}%."
    }) {
        val unit = size.minDimension / 264f
        withTransform({
            translate(center.x, center.y)
            scale(unit, unit, Offset.Zero)
        }) {
            drawOrnament(phase, state, lights)
            val colours = lights.mapValues { (_, light) -> lerp(QuestStone, QuestGold, light) }
            fun colour(condition: ChallengeCondition) = colours.getValue(condition)
            val nearby = colour(ChallengeCondition.LOCATION_INSIDE)
            val aligned = colour(ChallengeCondition.HEADING_ALIGNED)
            val steady = colour(ChallengeCondition.STABLE)
            val still = colour(ChallengeCondition.STATIONARY)
            val turning = colour(ChallengeCondition.ROTATION_STILL)
            val level = colour(ChallengeCondition.PHONE_HORIZONTAL)
            val audible = colour(ChallengeCondition.SOUND_DETECTED)
            val shutter = lerp(QuestStone, QuestGold, photo)
            translate(motion.roll * .16f + sin(phase) * .8f + sin(phase * 7f) * motion.movement * 3f,
                motion.pitch * .12f + cos(phase) * .8f) {
                when (state.challengeType) {
                    RelicChallengeType.UNION_LAWN_PHOTO -> {
                        drawLakeConditionRipple(distanceRipple)
                        drawLakeConditionRipple(headingRipple)
                        drawLakeCamera(motion, phase, lakeGoldWater, aligned, shutter)
                    }
                    RelicChallengeType.WILSON_HALL_OBSERVATION -> Unit // Rendered by WilsonHallEmblem.
                    RelicChallengeType.OLD_QUAD_EXCAVATION -> drawFernFossil(motion, phase, progress, nearby, level, still, steady)
                    RelicChallengeType.SOUTH_LAWN_VIEWING_ANGLE -> Unit // Rendered by SouthLawnEmblem.
                    RelicChallengeType.SYSTEM_GARDEN_GLASSHOUSE -> drawGlasshouse(motion, phase, nearby, shutter)
                    RelicChallengeType.GRAINGER_MUSEUM_TONE_TOOL -> drawGramophone(motion, phase, progress, nearby, audible,
                        (((state.soundThresholdDecibels ?: -30.0) + 80.0) / 80.0).toFloat().coerceIn(0f, 1f))
                }
            }
        }
    }
}

/** Starts a new expanding camera-centred ripple when one lake prerequisite becomes satisfied. */
@Composable
private fun lakeConditionRipple(state: TreasureChallengeUiState, condition: ChallengeCondition): Float {
    val satisfied = state.conditionLit(condition)
    val ripple = remember(state.challengeId, condition) { Animatable(1f) }
    LaunchedEffect(state.challengeId, condition, satisfied) {
        ripple.snapTo(if (satisfied) 0f else 1f)
        if (satisfied) ripple.animateTo(1f, tween(2000, easing = LinearEasing))
    }
    return ripple.value
}

private fun DrawScope.drawLakeConditionRipple(progress: Float) {
    if (progress >= 1f) return
    repeat(3) { ring ->
        val p = progress * 1.3f - ring * .15f
        if (p in 0f..1f) {
            val radius = 12f + p * 142f
            drawCircle(QuestGold.copy(alpha = (1f - p) * .42f), radius, Offset(0f, 6f), style = Stroke(1.3f))
            drawCircle(QuestGold.copy(alpha = (1f - p) * .055f), radius, Offset(0f, 6f), style = Stroke(6f))
        }
    }
}

private fun DrawScope.line(x: Float, y: Float, ex: Float, ey: Float, colour: Color, width: Float = 1.4f) =
    drawLine(colour, Offset(x, y), Offset(ex, ey), width, StrokeCap.Round)

private fun DrawScope.oval(x: Float, y: Float, width: Float, height: Float, colour: Color, stroke: Float = 1.4f) =
    drawOval(colour, Offset(x, y), Size(width, height), style = Stroke(stroke))

private fun DrawScope.engrave(path: Path, colour: Color, fill: Float = .13f, width: Float = 1.5f) {
    drawPath(path, colour.copy(alpha = fill))
    if (colour.green > .6f && colour.red > .7f) drawPath(path, colour.copy(alpha = .09f), style = Stroke(width + 6f))
    drawPath(path, colour, style = Stroke(width, cap = StrokeCap.Round, join = StrokeJoin.Round))
}

private fun diamond(x: Float, y: Float, radius: Float): Path = Path().apply {
    moveTo(x, y - radius); lineTo(x + radius * .65f, y); lineTo(x, y + radius)
    lineTo(x - radius * .65f, y); close()
}

private fun DrawScope.drawOrnament(phase: Float, state: TreasureChallengeUiState, lights: Map<ChallengeCondition, Float>) {
    val border = QuestGold.copy(alpha = .5f)
    val arch = Path().apply {
        moveTo(-101f, 88f); lineTo(-101f, -70f)
        quadraticTo(-100f, -105f, -57f, -108f)
        quadraticTo(-20f, -108f, 0f, -121f)
        quadraticTo(20f, -108f, 57f, -108f)
        quadraticTo(100f, -105f, 101f, -70f)
        lineTo(101f, 88f)
    }
    drawPath(arch, border, style = Stroke(.9f))
    for (side in listOf(-1f, 1f)) {
        val vine = Path().apply {
            moveTo(side * 99f, 87f)
            cubicTo(side * 116f, 52f, side * 84f, 14f, side * 110f, -16f)
            cubicTo(side * 124f, -33f, side * 97f, -67f, side * 93f, -86f)
        }
        drawPath(vine, border, style = Stroke(1.1f))
        repeat(6) { i ->
            val y = 72f - i * 26f
            val leaf = Path().apply {
                moveTo(side * 101f, y)
                quadraticTo(side * 120f, y - 16f, side * 113f, y - 25f)
                quadraticTo(side * 99f, y - 16f, side * 101f, y)
            }
            drawPath(leaf, border.copy(alpha = .18f))
            drawPath(leaf, border, style = Stroke(.7f))
            line(side * 101f, y - 5f, side * 109f, y - 19f, border, .6f)
        }
        line(side * 13f, 111f, side * 84f, 111f, border, .8f)
        line(side * 34f, 115f, side * 67f, 115f, border.copy(alpha = .25f), .7f)
        drawPath(diamond(side * 90f, 111f, 4f), border)
    }
    drawPath(diamond(0f, -121f, 5f), QuestGold.copy(alpha = .7f + sin(phase) * .15f))
    drawPath(diamond(0f, 111f, 5f), border)
    // The lake camera has two halves: photo capture belongs to the shutter, never a third arc.
    val conditions = if (state.challengeType == RelicChallengeType.UNION_LAWN_PHOTO) {
        listOf(ChallengeCondition.LOCATION_INSIDE, ChallengeCondition.HEADING_ALIGNED)
    } else state.conditionStates.map { it.condition }
    val count = conditions.size.coerceAtLeast(1)
    repeat(count) { i ->
        val light = conditions.getOrNull(i)?.let { lights.getValue(it) } ?: 0f
        val c = lerp(QuestStone.copy(alpha = .25f), QuestGold, light)
        if (state.challengeType == RelicChallengeType.UNION_LAWN_PHOTO) {
            drawArc(QuestGold.copy(alpha = light * .12f), -90f + i * 360f / count, 360f / count - 5f, false,
                Offset(-88f, -88f), Size(176f, 176f), style = Stroke(7f, cap = StrokeCap.Round))
        }
        drawArc(c, -90f + i * 360f / count, 360f / count - 5f, false,
            Offset(-88f, -88f), Size(176f, 176f), style = Stroke(1.6f, cap = StrokeCap.Round))
    }
    repeat(48) { i ->
        val a = i * PI / 24
        val r = if (i % 4 == 0) 96f else 94f
        val p = Offset(cos(a).toFloat(), sin(a).toFloat())
        drawLine(border.copy(alpha = .35f), p * 92f, p * r, .7f)
    }
}

/** Draws the camera engraving and clips the rising distance-linked gold fill to its silhouette. */
private fun DrawScope.drawLakeCamera(m: ArtworkMotion, phase: Float, goldWater: Float, aligned: Color, shutter: Color) {
    rotate(m.heading * .035f + m.roll * .14f, Offset.Zero) {
        val body = Path().apply {
            moveTo(-72f, -35f); lineTo(-52f, -35f); lineTo(-46f, -49f); lineTo(-15f, -49f)
            lineTo(-9f, -35f); lineTo(72f, -35f); lineTo(72f, 50f); lineTo(-72f, 50f); close()
        }
        fun casing(colour: Color) {
            engrave(body, colour, .16f, 2f)
            drawRoundRect(QuestDeep, Offset(-65f, -28f), Size(130f, 70f), CornerRadius(5f))
            repeat(5) { i ->
                line(-60f + i * 3f, -19f, -60f + i * 3f, 33f, colour.copy(alpha = .55f), .8f)
                line(46f + i * 3f, -19f, 46f + i * 3f, 33f, colour.copy(alpha = .55f), .8f)
            }
            for (x in listOf(-62f, 62f)) for (y in listOf(-25f, 40f)) {
                drawPath(diamond(x, y, 3f), colour)
            }
            line(-42f, -43f, -21f, -43f, colour, 2f)
            drawCircle(colour.copy(alpha = .1f), 41f, Offset(0f, 6f))
            drawCircle(colour, 40f, Offset(0f, 6f), style = Stroke(2f))
            line(-66f, 58f, 66f, 58f, colour.copy(alpha = .4f), .8f)
        }
        casing(QuestStone.copy(alpha = .65f))
        val surface = Path().apply {
            repeat(57) { i ->
                val x = -78f + i * 156f / 56f
                val y = 60f - goldWater * 120f + sin(x * .095f + phase * 2f) * 2.2f
                if (i == 0) moveTo(x, y) else lineTo(x, y)
            }
        }
        val water = Path().apply {
            addPath(surface)
            lineTo(78f, 82f); lineTo(-78f, 82f); close()
        }
        if (goldWater > 0f) {
            clipPath(water) { casing(QuestGold) }
            clipPath(body) {
                drawPath(water, QuestGold.copy(alpha = .18f))
                if (goldWater < .99f) drawPath(surface, QuestGold.copy(alpha = .75f), style = Stroke(.9f))
            }
        }
        drawRoundRect(shutter, Offset(42f, -42f), Size(19f, 5f), CornerRadius(2f))
        oval(43f, -25f, 15f, 9f, shutter)
        drawCircle(aligned, 34f, Offset(0f, 6f), style = Stroke(.9f))
        rotate(m.heading * .25f, Offset(0f, 6f)) {
            repeat(8) { i -> rotate(i * 45f, Offset(0f, 6f)) {
                val blade = Path().apply {
                    moveTo(0f, -26f); lineTo(23f, -16f); lineTo(13f, 1f); lineTo(0f, -8f); close()
                }
                engrave(blade, aligned, .13f, .7f)
            } }
        }
        val lensCentre = Offset(m.heading / 180f * 12f, 6f + m.pitch * .13f)
        drawCircle(QuestDeep, 18f, lensCentre)
        drawCircle(aligned, 18f, lensCentre, style = Stroke(1.2f))
        drawCircle(shutter, 4f, lensCentre + Offset(5f, -5f), style = Stroke(1f))
        repeat(3) { i ->
            drawArc(QuestStone.copy(alpha = .6f), 5f, 170f, false, lensCentre + Offset(-14f, -3f + i * 5f),
                Size(28f, 7f), style = Stroke(.9f))
            clipPath(water) {
                drawArc(QuestGold, 5f, 170f, false, lensCentre + Offset(-14f, -3f + i * 5f),
                    Size(28f, 7f), style = Stroke(.9f))
            }
        }
        val reticle = m.heading / 180f * 54f
        drawPath(diamond(reticle, 58f, 5f), aligned)
    }
}

/** Draws the fossil/fern emblem with posture and stillness-linked visual accents. */
private fun DrawScope.drawFernFossil(m: ArtworkMotion, phase: Float, progress: Float,
    nearby: Color, level: Color, still: Color, steady: Color) {
    val slab = Path().apply {
        moveTo(-65f, -65f); lineTo(-33f, -80f); lineTo(58f, -75f); lineTo(71f, -44f)
        lineTo(66f, 73f); lineTo(-53f, 79f); lineTo(-72f, 49f); close()
    }
    engrave(slab, nearby, .06f, .8f)
    repeat(28) { i ->
        val x = (i * 37 % 120) - 60f
        val y = (i * 61 % 140) - 70f
        drawCircle(nearby.copy(alpha = .22f), .8f, Offset(x, y))
    }
    rotate(m.roll * .65f, Offset.Zero) {
        scale(1f, 1f - abs(m.pitch) / 300f, Offset.Zero) {
            val stem = Path().apply {
                moveTo(-6f, 69f); cubicTo(8f, 19f, -16f, -33f, 6f, -68f)
            }
            drawPath(stem, level, style = Stroke(2.2f, cap = StrokeCap.Round))
            repeat(9) { i ->
                val y = 49f - i * 13f
                val width = 42f - i * 3.6f
                for (side in listOf(-1f, 1f)) {
                    val sway = sin(phase + i) * m.instability * 5f
                    val leaf = Path().apply {
                        moveTo(0f, y)
                        cubicTo(side * width * .6f, y - 3f, side * width, y - 13f + sway,
                            side * (width + 2f), y - 25f + sway)
                        cubicTo(side * width * .4f, y - 28f, side * 7f, y - 10f, 0f, y); close()
                    }
                    val colour = if (i % 2 == 0) steady else still
                    engrave(leaf, colour, .2f, 1f)
                    line(0f, y, side * (width - 5f), y - 19f + sway, colour.copy(alpha = .7f), .7f)
                    repeat(3) { vein ->
                        val x = side * (10f + vein * 7f)
                        line(x, y - 5f - vein * 4f, x + side * 5f, y - 13f - vein * 4f,
                            colour.copy(alpha = .5f), .5f)
                    }
                }
            }
        }
    }
    // A live spirit level is part of the fossil's ornamental pedestal.
    oval(-49f, 84f, 98f, 10f, level, .8f)
    drawCircle(level.copy(alpha = .22f), 4f, Offset(m.roll / 45f * 43f, 89f))
    drawCircle(level, 4f, Offset(m.roll / 45f * 43f, 89f), style = Stroke(1f))
    line(-6f, 82f, -6f, 96f, level, .7f); line(6f, 82f, 6f, 96f, level, .7f)
    drawArc(QuestGold, -90f, progress * 360f, false, Offset(-80f, -80f), Size(160f, 160f), style = Stroke(1.2f))
}

/** Draws the tower/glasshouse emblem and its proximity/photo accents. */
private fun DrawScope.drawGlasshouse(m: ArtworkMotion, phase: Float, nearby: Color, shutter: Color) {
    val building = Path().apply {
        moveTo(-71f, 72f); lineTo(-71f, -11f); lineTo(-43f, -35f); lineTo(-43f, -45f)
        quadraticTo(-25f, -71f, 0f, -75f); quadraticTo(25f, -71f, 43f, -45f)
        lineTo(43f, -35f); lineTo(71f, -11f); lineTo(71f, 72f); close()
    }
    engrave(building, nearby, .13f, 1.8f)
    line(-76f, 75f, 76f, 75f, nearby, 2f)
    line(-76f, 81f, 76f, 81f, nearby.copy(alpha = .6f), .8f)
    line(-70f, -10f, 70f, -10f, nearby, 1.5f)
    line(-43f, -35f, 43f, -35f, nearby, 1.2f)
    line(-43f, -44f, 43f, -44f, nearby, .8f)
    for (x in listOf(-59f, -43f, -27f, 27f, 43f, 59f)) {
        line(x, -10f, x, 70f, nearby, .9f)
        line(x, -10f, x * .62f, -35f, nearby, .8f)
    }
    for (x in listOf(-28f, -14f, 0f, 14f, 28f)) {
        val rib = Path().apply {
            moveTo(x, -44f); quadraticTo(x * .7f, -65f, 0f, -75f)
        }
        drawPath(rib, nearby, style = Stroke(.8f))
    }
    line(-70f, 17f, -17f, 17f, nearby, .7f); line(17f, 17f, 70f, 17f, nearby, .7f)
    line(-70f, 43f, -17f, 43f, nearby, .7f); line(17f, 43f, 70f, 43f, nearby, .7f)
    val door = Path().apply {
        moveTo(-17f, 71f); lineTo(-17f, 9f)
        quadraticTo(-17f, -7f, 0f, -8f); quadraticTo(17f, -7f, 17f, 9f)
        lineTo(17f, 71f)
    }
    drawPath(door, shutter, style = Stroke(1.3f))
    line(0f, 2f, 0f, 71f, shutter, .8f)
    drawCircle(shutter, 1.4f, Offset(-4f, 38f)); drawCircle(shutter, 1.4f, Offset(4f, 38f))
    drawPath(diamond(0f, -86f, 7f), nearby)
    line(0f, -79f, 0f, -73f, nearby, 1f)
    for (side in listOf(-1f, 1f)) {
        val x = side * 43f
        val sway = sin(phase + side) * 2f + m.roll * .1f + m.pitch * .05f
        val vine = Path().apply {
            moveTo(x, 65f); cubicTo(x - 8f, 44f, x + sway + 8f, 33f, x + sway, 12f)
        }
        drawPath(vine, nearby, style = Stroke(1.1f))
        repeat(4) { i ->
            val y = 60f - i * 11f
            val s = if (i % 2 == 0) -1f else 1f
            val leaf = Path().apply {
                moveTo(x, y); quadraticTo(x + s * 17f, y - 3f, x + s * 13f + sway, y - 14f)
                quadraticTo(x + s * 3f, y - 14f, x, y)
            }
            engrave(leaf, nearby, .18f, .7f)
        }
    }
    val marker = -64f + m.proximity * 128f
    line(-64f, 93f, 64f, 93f, nearby.copy(alpha = .5f), .8f)
    drawPath(diamond(marker, 93f, 4f), nearby)
}

/** Draws the gramophone with sound-linked motion and the configured audible threshold cue. */
private fun DrawScope.drawGramophone(m: ArtworkMotion, phase: Float, progress: Float, nearby: Color, audible: Color, threshold: Float) {
    val horn = Path().apply {
        moveTo(-18f, 28f); cubicTo(-31f, -2f, -37f, -16f, -65f, -41f)
        cubicTo(-37f, -67f, 29f, -70f, 66f, -44f)
        cubicTo(44f, -11f, 22f, 9f, 9f, 28f); close()
    }
    engrave(horn, audible, .2f, 1.8f)
    oval(-65f, -66f, 131f, 43f, audible, 1.8f)
    oval(-52f, -60f, 105f, 31f, audible.copy(alpha = .7f), .8f)
    oval(-21f, -53f, 43f, 17f, audible, 1f)
    repeat(6) { i ->
        val rib = Path().apply {
            moveTo(-13f + i * 4f, 26f)
            quadraticTo(-15f + i * 9f, -6f, -50f + i * 20f, -41f)
        }
        drawPath(rib, audible.copy(alpha = .5f), style = Stroke(.8f))
    }
    val pipe = Path().apply {
        moveTo(-15f, 26f); lineTo(-15f, 43f); quadraticTo(-15f, 50f, -3f, 50f)
        lineTo(23f, 50f); lineTo(23f, 43f); lineTo(-3f, 43f); lineTo(-3f, 27f)
    }
    engrave(pipe, audible, .25f, 1.3f)
    oval(-56f, 50f, 112f, 13f, nearby, 1.6f)
    val cabinet = Path().apply {
        moveTo(-58f, 58f); lineTo(58f, 58f); lineTo(58f, 86f); lineTo(-58f, 86f); close()
    }
    engrave(cabinet, nearby, .17f)
    line(-52f, 64f, 52f, 64f, nearby, .7f)
    line(-52f, 80f, 52f, 80f, nearby, .7f)
    for (side in listOf(-1f, 1f)) {
        drawPath(diamond(side * 42f, 72f, 5f), nearby)
        line(side * 48f, 86f, side * 48f, 92f, nearby, 2f)
    }
    val waveform = Path()
    for (i in 0..60) {
        val x = -31f + i * 62f / 60f
        val y = 72f + sin(i * .55f - phase * 4f) * m.sound * 6f
        if (i == 0) waveform.moveTo(x, y) else waveform.lineTo(x, y)
    }
    drawPath(waveform, audible, style = Stroke(1.1f))
    repeat(4) { i ->
        val radius = 68f + i * 8f + sin(phase * 2f) * m.sound * 3f
        drawArc(audible.copy(alpha = .12f + m.sound * (.5f - i * .09f)), 202f, 136f, false,
            Offset(-radius, -42f - radius * .5f), Size(radius * 2f, radius),
            style = Stroke(.8f + m.sound * .5f))
    }
    repeat(24) { i ->
        val x = -56f + i * 4.8f
        line(x, 100f, x, 96f, if (i / 24f <= m.sound) audible else QuestStone.copy(alpha = .35f), 1.3f)
    }
    drawPath(diamond(-56f + threshold * 112f, 103f, 2.5f), QuestParchment.copy(alpha = .8f))
    drawArc(QuestGold, -90f, progress * 360f, false, Offset(-83f, -83f), Size(166f, 166f), style = Stroke(1.3f))
}
