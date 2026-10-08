package com.comp90018.app.features.treasurechallenge

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.*
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import com.comp90018.app.contextengine.challenge.ChallengeCondition
import com.comp90018.app.contextengine.challenge.ChallengePoise
import com.comp90018.app.sensors.DirectionProcessor
import com.comp90018.app.sensors.SensorValidity
import kotlin.math.*

@Composable
private fun rosetteTime(): Float {
    val transition = rememberInfiniteTransition(label = "living_rosette")
    return transition.animateFloat(0f, 120f,
        infiniteRepeatable(tween(120000, easing = LinearEasing)), label = "independent_flower_paths").value
}

@Composable
private fun headingResonance(state: TreasureChallengeUiState): Float {
    val aligned = state.conditionLit(ChallengeCondition.HEADING_ALIGNED)
    val ripple = remember(state.challengeId) { Animatable(1f) }
    LaunchedEffect(state.challengeId, aligned) {
        ripple.snapTo(if (aligned) 0f else 1f)
        if (aligned) ripple.animateTo(1f, tween(2600, easing = LinearEasing))
    }
    return ripple.value
}

@Composable
internal fun WilsonHallBackdrop(
    state: TreasureChallengeUiState,
    modifier: Modifier,
    emblemCenter: Offset? = null,
) {
    val signals = QuestVisualSignals.from(state)
    val seconds = rosetteTime()
    val burst = headingResonance(state)
    val aligned = state.conditionLit(ChallengeCondition.HEADING_ALIGNED)
    val proximityTarget = questApproachFill(signals.distanceMeters, state.insideRadiusMeters)
    val proximity by animateFloatAsState(proximityTarget,
        spring(dampingRatio = 1f, stiffness = 100f), label = "fullscreen_water_level")
    val starLights = rosetteWaterStarLights(state, proximity)
    val litStars = RosetteWaterStars.count { waterStarIlluminated(proximityTarget, it) }
    val roll by animateFloatAsState(signals.roll, spring(stiffness = 100f), label = "rosette_backdrop_roll")
    val heading by animateFloatAsState(signals.headingError, spring(stiffness = 100f), label = "rosette_backdrop_heading")
    Box(modifier.testTag("wilson_fullscreen_water").semantics {
        contentDescription = "Full-screen distance-lit water and floating stars"
        stateDescription = "$litStars of 10 water stars illuminated"
    }) {
        Canvas(Modifier.matchParentSize().testTag("wilson_heading_ripple").semantics {
            contentDescription = "Living rosette water and heading resonance"
            stateDescription = if (aligned && burst < 1f) "Heading ripple expanding"
                else if (aligned) "Heading aligned" else "Awaiting viewing direction"
        }) {
            drawRect(QuestForest)
            val origin = (emblemCenter ?: Offset(center.x, size.height * .31f)) +
                Offset(roll * .7f + heading * .08f, signals.pitch)
            drawFullScreenRosetteWater(seconds, origin, QuestStone.copy(alpha = .13f))
            val waterline = size.height * (1f - proximity)
            val gildedWater = Path().apply {
                moveTo(0f, size.height)
                lineTo(0f, waterline)
                for (i in 0..60) {
                    val x = size.width * i / 60f
                    val wave = if (proximity > 0f && proximity < 1f)
                        sin(i * .17f + seconds * PI.toFloat() / 4f) * 3.dp.toPx() else 0f
                    lineTo(x, waterline + wave)
                }
                lineTo(size.width, size.height)
                close()
            }
            if (proximity > 0f) clipPath(gildedWater) {
                drawRect(Brush.verticalGradient(listOf(
                    QuestGold.copy(alpha = .015f), QuestGold.copy(alpha = .055f)), startY = waterline))
                drawFullScreenRosetteWater(seconds, origin, QuestGold.copy(alpha = .25f))
            }
            if (proximity > .005f && proximity < .995f) {
                val edge = Path().apply {
                    for (i in 0..60) {
                        val x = size.width * i / 60f
                        val y = waterline + sin(i * .17f + seconds * PI.toFloat() / 4f) * 3.dp.toPx()
                        if (i == 0) moveTo(x, y) else lineTo(x, y)
                    }
                }
                drawPath(edge, QuestGold.copy(alpha = .28f), style = Stroke(.8.dp.toPx()))
            }
            RosetteWaterStars.forEachIndexed { index, star ->
                val drift = rosetteDrift(index * 277 + 631, seconds)
                val at = Offset(size.width * (.12f + (star.x + 86f) / 172f * .76f) + drift.x * 2.dp.toPx(),
                    size.height * (1f - star.illuminationThreshold) + drift.y * 2.dp.toPx())
                val light = starLights[index]
                val radius = star.radius * 1.5.dp.toPx()
                val ink = lerp(QuestStone.copy(alpha = .35f), QuestGold.copy(alpha = .8f), light.gold)
                drawCrossStar(at, radius, ink, light.gold * .4f)
                if (light.ripple < 1f && light.gold > 0f) {
                    val flash = sin(light.ripple * PI).toFloat() * light.gold
                    drawCrossStar(at, radius * (1f + flash * .75f), QuestParchment.copy(alpha = flash), flash)
                    repeat(2) { ring ->
                        val p = ((light.ripple * 1.2f - ring * .15f) / .85f).coerceIn(0f, 1f)
                        if (p > 0f && p < 1f) drawCircle(QuestGold.copy(alpha = (1f - p) * .55f * light.gold),
                            (4f + p * 20f).dp.toPx(), at, style = Stroke(.7.dp.toPx()))
                    }
                }
            }
            repeat(9) { i ->
                val drift = rosetteDrift(i * 197 + 89, seconds)
                val radius = size.width * (.24f + i * .135f) + drift.x * 9f
                val ink = if (aligned) QuestGold.copy(alpha = .045f) else QuestStone.copy(alpha = .06f)
                drawCircle(ink, radius, origin + Offset(drift.y * 5f, 0f), style = Stroke(1f))
            }
            repeat(24) { i ->
                val a = i * PI / 12 + seconds * .002 + heading * .001
                val direction = Offset(cos(a).toFloat(), sin(a).toFloat())
                drawLine(QuestGold.copy(alpha = .035f), origin + direction * (size.width * .2f),
                    origin + direction * size.height, .7f)
            }
            // A single rising alignment edge launches a broad, layered aureole across the background.
            if (aligned && burst < 1f) {
                repeat(5) { ring ->
                    val progress = ((burst * 1.45f - ring * .11f) / .95f).coerceIn(0f, 1f)
                    if (progress > 0f && progress < 1f) {
                        val radius = size.width * .09f + progress * hypot(size.width, size.height)
                        val alpha = (1f - progress) * .3f
                        drawCircle(QuestGold.copy(alpha = alpha * .22f), radius, origin, style = Stroke(8f))
                        drawCircle(QuestGold.copy(alpha = alpha), radius, origin, style = Stroke(1.5f))
                        if (ring % 2 == 0) repeat(24) { ornament ->
                            val a = ornament * PI / 12
                            val p = origin + Offset(cos(a).toFloat(), sin(a).toFloat()) * radius
                            drawCrossStar(p, 3f + progress * 4f, QuestGold.copy(alpha = alpha))
                        }
                    }
                }
            }
            repeat(28) { i ->
                val drift = rosetteDrift(i * 127 + 61, seconds)
                val at = Offset(size.width * ((i * .618034f) % 1f) + drift.x * 7f + roll * .3f,
                    size.height * ((i * .381966f) % 1f) + drift.y * 8f)
                drawCircle(QuestGold.copy(alpha = .045f + (drift.pulse + 1f) * .025f), 1.7f, at)
            }
        }
    }
}

private data class WaterStarLight(val gold: Float, val ripple: Float)

@Composable
private fun rosetteWaterStarLights(state: TreasureChallengeUiState, proximity: Float): List<WaterStarLight> =
    RosetteWaterStars.mapIndexed { index, star ->
        val illuminated = waterStarIlluminated(proximity, star)
        val gold by animateFloatAsState(if (illuminated) 1f else 0f, tween(400), label = "water_star_" + index)
        val ripple = remember(state.challengeId, index) { Animatable(1f) }
        LaunchedEffect(state.challengeId, illuminated) {
            ripple.snapTo(if (illuminated) 0f else 1f)
            if (illuminated) ripple.animateTo(1f, tween(1400, easing = LinearEasing))
        }
        WaterStarLight(gold, ripple.value)
    }

@Composable
internal fun WilsonHallEmblem(state: TreasureChallengeUiState, modifier: Modifier) {
    val signals = QuestVisualSignals.from(state)
    val seconds = rosetteTime()
    val burst = headingResonance(state)
    @Composable fun smooth(target: Float, label: String): Float =
        animateFloatAsState(target, spring(dampingRatio = 1f, stiffness = 100f), label = label).value
    val heading = smooth(signals.headingError, "rosette_heading")
    val movement = smooth(((state.latestSnapshot?.let(ChallengePoise::stillnessScore) ?: 0.0) - .4)
        .coerceIn(0.0, 1.0).toFloat(), "rosette_motion_and_shake")
    val rotation = smooth(((state.latestSnapshot?.let(ChallengePoise::turnScore) ?: 0.0) - .5)
        .coerceIn(0.0, 3.0).toFloat() / 3f, "rosette_turn")
    val direction = state.latestSnapshot?.orientation?.direction
    val headingAvailable = direction?.headingValidity == SensorValidity.VALID &&
        direction.headingDegrees?.isFinite() == true
    val aligned = state.conditionLit(ChallengeCondition.HEADING_ALIGNED)
    val pointerTarget = if (aligned) 0f else if (headingAvailable) signals.headingError else 45f
    val pointer = remember(state.challengeId) { Animatable(pointerTarget) }
    LaunchedEffect(state.challengeId, pointerTarget) {
        // Unwrap each new bearing so crossing ±180° never sends the needle around a full circle.
        val shortestTurn = DirectionProcessor.angularDifference(pointer.value.toDouble(), pointerTarget.toDouble()).toFloat()
        pointer.animateTo(pointer.value + shortestTurn, spring(dampingRatio = 1f, stiffness = 100f))
    }
    val pointerHeading = pointer.value
    val lights = listOf(ChallengeCondition.HEADING_ALIGNED,
        ChallengeCondition.STILLNESS, ChallengeCondition.ROTATION_STILL).associateWith {
        animateFloatAsState(if (state.conditionLit(it)) 1f else 0f, tween(650), label = "rosette_" + it.name).value
    }
    val turnPetals = if (state.conditionLit(ChallengeCondition.ROTATION_STILL)) 4 else 0
    val stillPetals = if (state.conditionLit(ChallengeCondition.STILLNESS)) 4 else 0
    Canvas(modifier.testTag("quest_emblem").semantics {
        contentDescription = "Eight-petal rosette with independent flowers and a heading pointer"
        stateDescription = "Turn petals: $turnPetals of 4 lit. Stillness petals: $stillPetals of 4 lit. " +
            if (aligned) "Heading pointer aligned with star." else if (headingAvailable) "Heading pointer seeking star."
            else "Heading unavailable."
    }) {
        val unit = size.minDimension / 280f
        withTransform({
            translate(center.x, center.y)
            scale(unit, unit, Offset.Zero)
        }) {
            val stillLight = lights.getValue(ChallengeCondition.STILLNESS)
            val alignedLight = lights.getValue(ChallengeCondition.HEADING_ALIGNED)
            val turnLight = lights.getValue(ChallengeCondition.ROTATION_STILL)
            val stillInk = lerp(QuestStone.copy(alpha = .62f), QuestGold, stillLight)
            val alignedInk = lerp(QuestStone.copy(alpha = .55f), QuestGold, alignedLight)
            val turnInk = lerp(QuestStone.copy(alpha = .45f), QuestGold, turnLight)
            // Separate seeds and channels keep each small flower's direction, spin, flash and size independent.
            val positions = listOf(Offset(-106f, -91f), Offset(106f, -91f),
                Offset(-117f, -28f), Offset(117f, -28f), Offset(-114f, 39f), Offset(114f, 39f),
                Offset(-91f, 102f), Offset(91f, 102f))
            positions.forEachIndexed { index, base ->
                val drift = rosetteDrift(state.challengeId.hashCode() + index * 331 + 149, seconds)
                val at = base + Offset(drift.x, drift.y) * (1.5f + movement * 13f)
                val sizeChange = 1f + movement * drift.pulse * .52f
                val opacity = (1f - movement * (drift.y + 1f) * .38f).coerceIn(.18f, 1f)
                val angle = heading * (if (index % 2 == 0) -.37f else .43f) +
                    drift.spin * (4f + rotation * 90f) + seconds * (if (index % 2 == 0) 1.2f else -1.1f)
                withTransform({
                    translate(at.x, at.y)
                    rotate(angle, Offset.Zero)
                    scale(sizeChange, sizeChange, Offset.Zero)
                }) {
                    val flowerLight = if (index % 4 < 2) turnLight else stillLight
                    val flowerInk = if (index % 4 < 2) turnInk else stillInk
                    drawRosetteFlower(if (index % 3 == 0) 11f else 8f, 5,
                        flowerInk.copy(alpha = flowerInk.alpha * opacity), flowerLight, false)
                }
            }

            val largeDrift = rosetteDrift(state.challengeId.hashCode() xor 0x4c3d, seconds)
            withTransform({
                translate(largeDrift.x * movement * 9f + signals.roll * .12f,
                    largeDrift.y * movement * 9f + signals.pitch * .1f)
                rotate(heading * .34f + largeDrift.spin * rotation * 28f, Offset.Zero)
                scale(1f + largeDrift.pulse * movement * .025f, 1f + largeDrift.pulse * movement * .025f, Offset.Zero)
            }) {
                // Cardinal leaves record turn; the four diagonal leaves record motion and shake.
                drawRosetteFlower(68f, 8, turnInk, turnLight, true,
                    alternateInk = stillInk, alternateGlow = stillLight)
                drawCircle(QuestForest.copy(alpha = .93f), 17f, Offset.Zero)
                drawCircle(alignedInk, 17f, Offset.Zero, style = Stroke(1.6f))
                drawCircle(alignedInk.copy(alpha = .5f), 12f, Offset.Zero, style = Stroke(.65f))
                drawCrossStar(Offset.Zero, 8f, alignedInk, alignedLight)
            }

            // An armillary rim records rotation independently of the petals' stillness condition.
            for (start in listOf(-76f, 14f, 104f, 194f)) {
                drawArc(turnInk, start, 52f, false, Offset(-80f, -80f), Size(160f, 160f), style = Stroke(1.2f))
            }
            if (burst < 1f && state.conditionLit(ChallengeCondition.HEADING_ALIGNED)) {
                repeat(3) { ring ->
                    val p = ((burst * 1.4f - ring * .15f) / .95f).coerceIn(0f, 1f)
                    if (p > 0f && p < 1f) drawCircle(QuestGold.copy(alpha = (1f - p) * .5f),
                        20f + p * 108f, Offset.Zero, style = Stroke(1f))
                }
            }
            // Small engraved finials frame the flowers without side vines.
            val pointerPivot = Offset(0f, -83f)
            val targetStar = Offset(0f, -122f)
            drawCrossStar(targetStar, 7f, alignedInk, alignedLight)
            drawArc(QuestStone.copy(alpha = .4f), 205f, 130f, false,
                pointerPivot - Offset(29f, 29f), Size(58f, 58f), style = Stroke(.65f))
            rotate(pointerHeading, pointerPivot) {
                val needle = Path().apply {
                    moveTo(pointerPivot.x, pointerPivot.y - 28f)
                    lineTo(pointerPivot.x - 3.2f, pointerPivot.y + 3f)
                    lineTo(pointerPivot.x, pointerPivot.y)
                    lineTo(pointerPivot.x + 3.2f, pointerPivot.y + 3f)
                    close()
                }
                if (alignedLight > 0f) drawPath(needle, QuestGold.copy(alpha = .09f * alignedLight), style = Stroke(3f))
                drawPath(needle, alignedInk.copy(alpha = .22f))
                drawPath(needle, alignedInk, style = Stroke(1f, join = StrokeJoin.Round))
            }
            drawCircle(QuestForest, 3f, pointerPivot)
            drawCircle(alignedInk, 3f, pointerPivot, style = Stroke(.8f))
            drawCrossStar(Offset(0f, 117f), 3.5f, QuestGold.copy(alpha = .4f))
            for (side in listOf(-1f, 1f)) {
                val flourish = Path().apply {
                    moveTo(side * 10f, 117f)
                    cubicTo(side * 26f, 106f, side * 36f, 126f, side * 53f, 117f)
                }
                drawPath(flourish, QuestGold.copy(alpha = .34f), style = Stroke(.8f))
            }
        }
    }
}

private fun DrawScope.drawFullScreenRosetteWater(seconds: Float, origin: Offset, ink: Color) {
    val spacing = 25.dp.toPx()
    repeat(ceil(size.height / spacing).toInt() + 1) { row ->
        val path = Path()
        for (i in 0..50) {
            val x = size.width * i / 50f
            val y = row * spacing + sin(i * .17f + seconds * PI.toFloat() / 4f + row * .7f) * 3.dp.toPx()
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        drawPath(path, ink, style = Stroke(.6.dp.toPx()))
    }
    repeat(6) { ring ->
        val p = ((seconds * .025f + ring / 6f) % 1f)
        drawCircle(ink.copy(alpha = ink.alpha * (1f - p) * .8f),
            18.dp.toPx() + p * hypot(size.width, size.height), origin, style = Stroke(.7.dp.toPx()))
    }
}

private fun DrawScope.drawRosetteFlower(
    radius: Float, petals: Int, ink: Color, glow: Float, ornate: Boolean,
    alternateInk: Color = ink, alternateGlow: Float = glow,
) {
    repeat(petals) { index ->
        val petalInk = if (index % 2 == 0) ink else alternateInk
        val petalGlow = if (index % 2 == 0) glow else alternateGlow
        rotate(index * 360f / petals, Offset.Zero) {
            val petal = Path().apply {
                moveTo(0f, -radius * .13f)
                cubicTo(-radius * .32f, -radius * .38f, -radius * .31f, -radius * .74f, 0f, -radius)
                cubicTo(radius * .31f, -radius * .74f, radius * .32f, -radius * .38f, 0f, -radius * .13f)
                close()
            }
            if (petalGlow > 0f) drawPath(petal, QuestGold.copy(alpha = .055f * petalGlow),
                style = Stroke(if (ornate) 4f else 2f))
            drawPath(petal, petalInk.copy(alpha = petalInk.alpha * if (ornate) .07f else .18f))
            drawPath(petal, petalInk, style = Stroke(if (ornate) 1.1f else .8f, join = StrokeJoin.Round))
            if (ornate) {
                val carving = Path().apply {
                    moveTo(0f, -radius * .34f)
                    quadraticTo(-radius * .14f, -radius * .6f, 0f, -radius * .82f)
                    quadraticTo(radius * .14f, -radius * .6f, 0f, -radius * .34f)
                }
                drawPath(carving, petalInk.copy(alpha = petalInk.alpha * .65f), style = Stroke(.65f))
                drawCrossStar(Offset(0f, -radius * 1.07f), 2.2f, petalInk.copy(alpha = petalInk.alpha * .75f))
            }
        }
    }
    if (!ornate) {
        drawCircle(QuestForest, radius * .19f, Offset.Zero)
        drawCircle(ink, radius * .19f, Offset.Zero, style = Stroke(.7f))
    }
}
