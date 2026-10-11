package com.comp90018.app.features.map.compass

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.comp90018.app.GothicTreasureFontFamily
import com.comp90018.app.features.map.CompassGateConfig
import com.comp90018.app.features.map.HuntReadiness
import com.comp90018.app.features.map.components.formatDistance
import kotlin.math.cos
import kotlin.math.sin

/** Draws the animated compass, decorative orbit, and distance-linked stars for the gate. */
@Composable
internal fun DivineCompassVisual(
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
