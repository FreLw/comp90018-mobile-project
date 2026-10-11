package com.comp90018.app.features.map.compass

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.comp90018.app.GothicTreasureFontFamily
import com.comp90018.app.Ink
import com.comp90018.app.Muted
import kotlin.math.cos
import kotlin.math.sin

/** Renders one compass requirement with an animated light, caption, and current reading. */
@Composable
internal fun OracleLamp(title: String, hint: String, detail: String, lit: Boolean, proximity: Float, color: Color, modifier: Modifier) {
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
