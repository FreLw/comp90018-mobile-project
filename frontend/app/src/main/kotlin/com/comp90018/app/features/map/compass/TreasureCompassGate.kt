package com.comp90018.app.features.map.compass

import android.graphics.Path
import android.hardware.Sensor
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.comp90018.app.Brand
import com.comp90018.app.BrandSoft
import com.comp90018.app.BuildConfig
import com.comp90018.app.GothicTreasureFontFamily
import com.comp90018.app.Ink
import com.comp90018.app.Muted
import com.comp90018.app.contextengine.challenge.RelicChallengeType
import com.comp90018.app.features.map.CompassGateConfig
import com.comp90018.app.features.map.HuntReadiness
import com.comp90018.app.features.map.MapRelic
import com.comp90018.app.features.map.components.DiscoveryHuntButton
import com.comp90018.app.features.map.components.formatDegrees
import com.comp90018.app.features.map.components.formatDistance
import com.comp90018.app.features.map.components.formatGateDistance
import com.comp90018.app.features.map.debug.SensorTestSlider
import com.comp90018.app.features.map.evaluateHuntReadiness
import com.comp90018.app.features.map.signedBearingDifference
import com.comp90018.app.sensors.location.LocationOutput
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

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
    val compassEntrance = remember(relic.id) { Animatable(0f) }
    val lampEntrance = remember(relic.id) { Animatable(0f) }
    val scope = rememberCoroutineScope()
    var leaving by remember(relic.id) { mutableStateOf(false) }
    LaunchedEffect(relic.id) {
        launch { compassEntrance.animateTo(1f, tween(1050, easing = FastOutSlowInEasing)) }
        delay(380)
        lampEntrance.animateTo(1f, tween(950, easing = FastOutSlowInEasing))
    }
    var signalLocked by remember(relic.id) { mutableStateOf(false) }
    var simulateSensors by remember(relic.id) { mutableStateOf(BuildConfig.DEBUG && debugSimulationEnabled) }
    var testDistance by remember(relic.id) { mutableStateOf((locationOutput.distanceToTargetMeters ?: 9.0).toFloat().coerceIn(0f, 30f)) }
    var testHeading by remember(relic.id) { mutableStateOf(deviceHeading) }
    val distance = if (BuildConfig.DEBUG && simulateSensors) testDistance.toDouble() else locationOutput.distanceToTargetMeters
    val targetBearing = locationOutput.targetBearingDegrees
        ?: if (BuildConfig.DEBUG && simulateSensors) 0.0 else null
    val heading = if (BuildConfig.DEBUG && simulateSensors) testHeading else deviceHeading
    val turnDegrees = targetBearing?.let { signedBearingDifference(it, heading.toDouble()) }
    val locationReady = (BuildConfig.DEBUG && simulateSensors) ||
        locationOutput.validity == com.comp90018.app.sensors.SensorValidity.VALID
    val readiness = if (locationReady) {
        evaluateHuntReadiness(distance, turnDegrees, relic.compassGateConfig)
    } else HuntReadiness(false, false)
    val nearTreasure = readiness.nearTreasure
    val facingTreasure = readiness.facingTreasure
    val allReady = readiness.allReady
    // Distance and heading gate entry only. The physical task remains responsible for completion and saving.
    fun continueToChallenge() {
        if (!allReady || leaving) return
        leaving = true
        scope.launch {
            launch { lampEntrance.animateTo(0f, tween(420)) }
            compassEntrance.animateTo(0f, tween(700, easing = FastOutSlowInEasing))
            onContinue()
        }
    }

    val proximityGlow = distance?.let { (((relic.compassGateConfig.huntReadyRadiusMeters + 20.0) - it) / 20.0).toFloat().coerceIn(0f, 1f) } ?: 0f
    val headingGlow = turnDegrees?.let { ((180.0 - abs(it)) / (180.0 - relic.compassGateConfig.compassAlignmentToleranceDegrees).coerceAtLeast(1.0)).toFloat().coerceIn(0f, 1f) } ?: 0f
    val stars = oracleStarCount(distance, relic.compassGateConfig.huntReadyRadiusMeters)
    val tone = remember { runCatching { android.media.ToneGenerator(android.media.AudioManager.STREAM_MUSIC, 55) }.getOrNull() }
    DisposableEffect(tone) { onDispose { tone?.release() } }
    var previousStars by remember(relic.id) { mutableIntStateOf(stars) }
    LaunchedEffect(stars, soundEffectsEnabled) {
        val newlyLit = stars - previousStars
        previousStars = stars
        if (soundEffectsEnabled && newlyLit > 0) {
            repeat(newlyLit) {
                tone?.startTone(android.media.ToneGenerator.TONE_PROP_BEEP, 85)
                delay(110)
            }
        }
    }

    LaunchedEffect(allReady, signalLocked) {
        if (!allReady) signalLocked = false
        if (allReady && !signalLocked) {
            signalLocked = true
        }
    }

    val instruction = when {
        signalLocked -> "Treasure signal locked — the hidden trial has awakened."
        !locationReady || distance == null || targetBearing == null -> "Waiting for a reliable location signal…"
        !nearTreasure -> "The signal faded. Move back within ${relic.compassGateConfig.huntReadyRadiusMeters.formatGateDistance()}."
        !facingTreasure && requireNotNull(turnDegrees) > 0 -> "Turn right ${abs(turnDegrees).formatDegrees()} toward the treasure."
        !facingTreasure -> "Turn left ${abs(requireNotNull(turnDegrees)).formatDegrees()} toward the treasure."
        else -> "Hold it there — locking onto the treasure…"
    }

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
            readiness = readiness,
            config = relic.compassGateConfig,
            turnDegrees = turnDegrees?.toFloat() ?: 0f,
            signalAvailable = turnDegrees != null,
            distanceMeters = distance,
            signalLocked = signalLocked,
            modifier = Modifier.fillMaxWidth().height(285.dp).graphicsLayer {
                alpha = compassEntrance.value
                scaleX = 0.35f + compassEntrance.value * 0.65f
                scaleY = scaleX
                rotationZ = (1f - compassEntrance.value) * -24f
                translationY = (1f - compassEntrance.value) * 80.dp.toPx()
            },
        )
        Card(
            Modifier.fillMaxWidth().graphicsLayer {
                alpha = lampEntrance.value
                scaleX = 0.7f + lampEntrance.value * 0.3f
                scaleY = scaleX
                translationY = (1f - lampEntrance.value) * 65.dp.toPx()
            },
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = if (signalLocked) BrandSoft else Color(0xFFFFF1C9)),
        ) {
            Column(
                Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 11.dp),
                verticalArrangement = Arrangement.spacedBy(7.dp),
            ) {
                Text(instruction, modifier = Modifier.fillMaxWidth(), color = Ink, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    OracleLamp("The Trail", "Draw nearer", "${distance.formatDistance()} / ${relic.compassGateConfig.huntReadyRadiusMeters.formatGateDistance()}", nearTreasure, proximityGlow, Color(0xFFBD903D), Modifier.weight(1f))
                    OracleLamp("The Bearing", "Follow the calling", turnDegrees?.let { "${abs(it).formatDegrees()} / ${relic.compassGateConfig.compassAlignmentToleranceDegrees.formatDegrees()}" } ?: "Await signal", facingTreasure, headingGlow, Color(0xFFA44736), Modifier.weight(1f))
                }
                AnimatedVisibility(visible = allReady, enter = fadeIn(tween(350)) + slideInVertically { it / 2 }) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("All lanterns are alight. The relic awaits.", color = Ink, fontFamily = GothicTreasureFontFamily, fontSize = 18.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                        if (!leaving) DiscoveryHuntButton("Hunt", ::continueToChallenge)
                    }
                }
            }
        }
        if (BuildConfig.DEBUG) {
            Surface(color = Color(0xFFE8DDC6), shape = RoundedCornerShape(16.dp)) {
                Column(Modifier.padding(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Sensor test controls", Modifier.weight(1f), color = Ink, fontWeight = FontWeight.Bold)
                        Switch(simulateSensors, { simulateSensors = it })
                    }
                    if (simulateSensors) {
                        SensorTestSlider("Distance · GPS", testDistance, 0f..30f, "m", if (nearTreasure) Color(0xFFB58A42) else Color(0xFF8D8984)) { testDistance = it }
                        SensorTestSlider("Heading · rotation", testHeading, 0f..360f, "°", if (facingTreasure) Color(0xFFA44736) else Color(0xFF8D8984)) { testHeading = it }
                    }
                }
            }
        }

    }
}

/** Draws the animated compass, decorative orbit, and distance-linked stars for the gate. */
@Composable
private fun DivineCompassVisual(
    readiness: HuntReadiness,
    config: CompassGateConfig,
    turnDegrees: Float,
    signalAvailable: Boolean,
    distanceMeters: Double?,
    signalLocked: Boolean,
    modifier: Modifier = Modifier,
) {
    val animatedTurn by animateFloatAsState(turnDegrees, tween(350), label = "treasure_compass_turn")
    val orbit = rememberInfiniteTransition(label = "oracle_orbit")
    val orbitDegrees by orbit.animateFloat(0f, 360f, infiniteRepeatable(tween(16000, easing = LinearEasing)), label = "orbit_degrees")
    val shimmer by orbit.animateFloat(0.25f, 0.9f, infiniteRepeatable(tween(1400), RepeatMode.Reverse), label = "oracle_shimmer")
    val parchment = Color(0xFFF5ECD9)
    val plum = Color(0xFF382B40)
    val grey = Color(0xFF8D8984)
    val gold = if (readiness.nearTreasure) Color(0xFFB58A42) else grey
    val rust = if (readiness.facingTreasure) Color(0xFFA44736) else grey
    val compassInk = if (readiness.facingTreasure) plum else Color(0xFF777570)
    val starCount = oracleStarCount(distanceMeters, config.huntReadyRadiusMeters)
    val starFlash = remember { androidx.compose.animation.core.Animatable(0f) }
    var lastStarCount by remember { mutableIntStateOf(starCount) }
    var flashingStars by remember { mutableStateOf(0 until 0) }
    LaunchedEffect(starCount) {
        if (starCount > lastStarCount) {
            flashingStars = lastStarCount until starCount
            lastStarCount = starCount
            starFlash.snapTo(1f)
            starFlash.animateTo(0f, tween(700))
        } else lastStarCount = starCount
    }
    val distanceProgress = distanceMeters?.let { (((config.huntReadyRadiusMeters + 20.0) - it) / 20.0).toFloat().coerceIn(0f, 1f) } ?: 0f
    Box(modifier.padding(4.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize().padding(bottom = 42.dp)) {
            val centre = Offset(size.width / 2, size.height / 2)
            val radius = size.minDimension * 0.37f
            fun point(angle: Double, length: Float): Offset = centre + Offset(cos(angle).toFloat() * length, sin(angle).toFloat() * length)
            drawCircle(compassInk, radius, centre)
            drawCircle(gold, radius * 0.96f, centre, style = Stroke(3.dp.toPx()))
            drawCircle(parchment, radius * 0.86f, centre)
            drawCircle(gold, radius * 0.76f, centre, style = Stroke(1.dp.toPx()))
            repeat(48) { i ->
                val angle = Math.toRadians(i * 7.5 - 90)
                drawLine(compassInk, point(angle, radius * 0.78f), point(angle, radius * if (i % 4 == 0) 0.86f else 0.82f), if (i % 4 == 0) 3f else 1.5f)
            }
            // Eight flat heraldic points form a compass rose beneath the sensor needle.
            repeat(8) { i ->
                val angle = Math.toRadians(i * 45.0 - 90)
                val rose = androidx.compose.ui.graphics.Path().apply {
                    moveTo(centre.x, centre.y)
                    val left = point(angle - 0.22, radius * 0.23f)
                    val tip = point(angle, radius * if (i % 2 == 0) 0.70f else 0.50f)
                    val right = point(angle + 0.22, radius * 0.23f)
                    lineTo(left.x, left.y); lineTo(tip.x, tip.y); lineTo(right.x, right.y); close()
                }
                drawPath(rose, if (i % 2 == 0) gold.copy(alpha = 0.5f) else compassInk.copy(alpha = 0.18f))
            }
            val trackRadius = radius * 1.12f
            drawCircle(Color(0xFFD2CEC5), trackRadius, centre, style = Stroke(18.dp.toPx()))
            drawArc(gold, -90f, 360f * distanceProgress, false,
                Offset(centre.x - trackRadius, centre.y - trackRadius), Size(trackRadius * 2, trackRadius * 2), style = Stroke(18.dp.toPx()))
            repeat(10) { i ->
                val position = point(Math.toRadians(-72.0 + i * 36.0), trackRadius)
                val lit = i < starCount
                val flash = if (i in flashingStars && lit) starFlash.value else 0f
                if (flash > 0f) {
                    drawCircle(Color(0xFFECC86F).copy(alpha = flash * 0.6f), (10f + (1f - flash) * 16f).dp.toPx(), position, style = Stroke(2.dp.toPx()))
                    repeat(6) { ray ->
                        val angle = Math.toRadians(ray * 60.0)
                        val offset = Offset(cos(angle).toFloat(), sin(angle).toFloat())
                        drawLine(Color(0xFFFFD978).copy(alpha = flash), position + offset * 11.dp.toPx(), position + offset * (16f + (1f - flash) * 8f).dp.toPx(), 2.dp.toPx())
                    }
                }
                val star = androidx.compose.ui.graphics.Path().apply {
                    repeat(10) { vertex ->
                        val angle = Math.toRadians(vertex * 36.0 - 90)
                        val r = (if (vertex % 2 == 0) 6f else 2.7f).dp.toPx() * (1f + flash * 0.4f)
                        val x = position.x + cos(angle).toFloat() * r
                        val y = position.y + sin(angle).toFloat() * r
                        if (vertex == 0) moveTo(x, y) else lineTo(x, y)
                    }
                    close()
                }
                drawPath(star, if (lit) Color(0xFFFFE5A0) else Color(0xFF777570))
            }
            rotate(orbitDegrees, centre) {
                repeat(12) { i ->
                    val angle = Math.toRadians(i * 30.0)
                    val position = point(angle, radius * 1.34f)
                    val extent = if (signalLocked) 6.dp.toPx() else 3.dp.toPx()
                    drawLine(gold.copy(alpha = shimmer), position - Offset(extent, 0f), position + Offset(extent, 0f), 2f)
                    drawLine(gold.copy(alpha = shimmer), position - Offset(0f, extent), position + Offset(0f, extent), 2f)
                }
            }
            if (signalAvailable) rotate(animatedTurn, centre) {
                val needle = androidx.compose.ui.graphics.Path().apply {
                    moveTo(centre.x, centre.y - radius * 0.73f)
                    lineTo(centre.x + radius * 0.12f, centre.y)
                    lineTo(centre.x, centre.y + radius * 0.52f)
                    lineTo(centre.x - radius * 0.12f, centre.y); close()
                }
                drawPath(needle, compassInk)
                val tip = androidx.compose.ui.graphics.Path().apply {
                    moveTo(centre.x, centre.y - radius * 0.73f)
                    lineTo(centre.x + radius * 0.12f, centre.y)
                    lineTo(centre.x - radius * 0.12f, centre.y); close()
                }
                drawPath(tip, rust)
            }
            drawCircle(gold, radius * 0.095f, centre)
            drawCircle(plum, radius * 0.045f, centre)
        }
        Column(Modifier.align(Alignment.BottomCenter), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(if (signalLocked) "✦ SIGNAL AWAKENED ✦" else "${distanceMeters.formatDistance()} · $starCount / 10 stars", color = plum, fontFamily = GothicTreasureFontFamily, fontSize = 18.sp)
        }
    }
}

internal fun oracleStarCount(distanceMeters: Double?, huntReadyRadiusMeters: Double = 10.0): Int = distanceMeters?.takeIf { it.isFinite() }?.let {
    kotlin.math.floor((((huntReadyRadiusMeters + 20.0) - it) / 20.0).coerceIn(0.0, 1.0) * 10.0).toInt()
} ?: 0

/** Renders one compass requirement with an animated light, caption, and current reading. */
@Composable
private fun OracleLamp(title: String, hint: String, detail: String, lit: Boolean, proximity: Float, color: Color, modifier: Modifier) {
    val glow by animateFloatAsState(if (lit) 1f else proximity * 0.55f, tween(450), label = "lantern_brightness")
    val flameTransition = rememberInfiniteTransition(label = "living_flame")
    val flicker by flameTransition.animateFloat(0.82f, 1.08f, infiniteRepeatable(tween(620), RepeatMode.Reverse), label = "flame_flicker")
    val sway by flameTransition.animateFloat(-2f, 2f, infiniteRepeatable(tween(1600), RepeatMode.Reverse), label = "lantern_sway")
    val burst = remember { Animatable(0f) }
    LaunchedEffect(lit) {
        if (lit) { burst.snapTo(1f); burst.animateTo(0f, tween(950)) } else burst.snapTo(0f)
    }
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Canvas(Modifier.size(82.dp).graphicsLayer { rotationZ = if (lit) sway else 0f }) {
            val w = size.width
            val h = size.height
            val c = Offset(w / 2f, h * 0.53f)
            val ink = Color(0xFF382B40)
            val brass = Color(0xFFC69A52)
            if (glow > 0f) drawCircle(Brush.radialGradient(listOf(color.copy(alpha = glow * flicker * 0.32f), Color.Transparent), c, w * 0.48f), w * 0.48f, c)
            if (burst.value > 0f) {
                repeat(8) { i ->
                    val angle = Math.toRadians(i * 45.0)
                    val ray = Offset(cos(angle).toFloat(), sin(angle).toFloat())
                    val radius = w * (0.29f + (1f - burst.value) * 0.19f)
                    drawLine(brass.copy(alpha = burst.value), c + ray * radius, c + ray * (radius + w * 0.06f), 2.dp.toPx())
                }
            }
            // Ring handle, sloping roof, glass chamber, side rails and broad pedestal.
            drawOval(ink, Offset(w * 0.43f, h * 0.05f), Size(w * 0.14f, h * 0.18f), style = Stroke(2.5.dp.toPx()))
            val roof = androidx.compose.ui.graphics.Path().apply {
                moveTo(w * 0.5f, h * 0.19f); lineTo(w * 0.74f, h * 0.34f)
                lineTo(w * 0.26f, h * 0.34f); close()
            }
            drawPath(roof, ink)
            drawLine(brass, Offset(w * 0.3f, h * 0.33f), Offset(w * 0.7f, h * 0.33f), 2.dp.toPx())
            drawRoundRect(androidx.compose.ui.graphics.lerp(Color(0xFFE2D7C6), Color(0xFFFFE4A0), glow), Offset(w * 0.33f, h * 0.36f), Size(w * 0.34f, h * 0.36f), androidx.compose.ui.geometry.CornerRadius(w * 0.04f))
            drawLine(Color.White.copy(alpha = 0.65f), Offset(w * 0.39f, h * 0.4f), Offset(w * 0.39f, h * 0.6f), 2.dp.toPx())
            for (x in listOf(0.3f, 0.7f)) drawLine(ink, Offset(w * x, h * 0.34f), Offset(w * x, h * 0.75f), 3.dp.toPx())
            drawRoundRect(ink, Offset(w * 0.25f, h * 0.73f), Size(w * 0.5f, h * 0.07f), androidx.compose.ui.geometry.CornerRadius(3.dp.toPx()))
            drawRoundRect(brass, Offset(w * 0.32f, h * 0.81f), Size(w * 0.36f, h * 0.05f), androidx.compose.ui.geometry.CornerRadius(2.dp.toPx()))
            drawLine(ink, Offset(w * 0.46f, h * 0.69f), Offset(w * 0.54f, h * 0.69f), 3.dp.toPx())
            if (glow > 0f) {
                val flame = androidx.compose.ui.graphics.Path().apply {
                    moveTo(c.x, h * (0.66f - 0.24f * flicker))
                    cubicTo(w * 0.44f, h * 0.54f, w * 0.37f, h * 0.65f, c.x, h * 0.68f)
                    cubicTo(w * 0.64f, h * 0.64f, w * 0.54f, h * 0.53f, c.x, h * (0.66f - 0.24f * flicker))
                    close()
                }
                drawPath(flame, Color(0xFFE89A36).copy(alpha = glow))
                drawOval(Color(0xFFFFF3CA).copy(alpha = glow), Offset(w * 0.47f, h * 0.57f), Size(w * 0.06f, h * 0.1f))
                if (lit) repeat(3) { i ->
                    val phase = (flicker + i * 0.33f) % 1f
                    drawCircle(brass.copy(alpha = (1f - phase) * 0.7f), 1.5.dp.toPx(), Offset(w * (0.43f + i * 0.07f), h * (0.52f - phase * 0.18f)))
                }
            }
        }
        Text(title, color = if (lit) color else Ink, fontFamily = GothicTreasureFontFamily, fontSize = 17.sp, textAlign = TextAlign.Center)
        Text(if (lit) "FLAME AWAKENED" else hint, color = Muted, fontSize = 9.sp, textAlign = TextAlign.Center)
        Text(detail, color = Muted, fontSize = 10.sp, textAlign = TextAlign.Center)
    }
}
