package com.comp90018.app.features.map

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.comp90018.app.GothicTreasureFontFamily
import com.comp90018.app.features.treasure.TreasurePrototypeImage
import kotlin.math.cos
import kotlin.math.sin

/** A visual discovery only; collection is still saved by the existing challenge flow. */
@Composable
internal fun TreasureDiscoveryReveal(relic: MapRelic, onViewTreasure: () -> Unit, onReturnToMap: () -> Unit) {
    val entrance = remember(relic.id) { Animatable(0f) }
    LaunchedEffect(relic.id) { entrance.animateTo(1f, tween(1000)) }
    val transition = rememberInfiniteTransition(label = "relic_radiance")
    val glow by transition.animateFloat(0.4f, 0.85f, infiniteRepeatable(tween(1700), RepeatMode.Reverse), label = "relic_glow")
    val orbit by transition.animateFloat(0f, 360f, infiniteRepeatable(tween(28000, easing = LinearEasing)), label = "relic_sparks")
    val gold = Color(0xFFE8BC65)
    val ink = Color(0xFF382B40)
    Column(
        Modifier.fillMaxSize().background(Color(0xFFF5ECD9)).verticalScroll(rememberScrollState()).padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Spacer(Modifier.height(20.dp))
        Text("✦ A HIDDEN WONDER AWAKENS ✦", color = ink, fontFamily = GothicTreasureFontFamily, fontSize = 20.sp, textAlign = TextAlign.Center)
        Box(Modifier.fillMaxWidth().height(340.dp), contentAlignment = Alignment.Center) {
            Canvas(Modifier.fillMaxSize()) {
                val c = Offset(size.width / 2f, size.height / 2f)
                val r = size.minDimension * 0.43f
                drawCircle(Brush.radialGradient(listOf(gold.copy(alpha = glow), gold.copy(alpha = glow * 0.25f), Color.Transparent), c, r), r, c)
                drawCircle(gold.copy(alpha = (1f - entrance.value) * 0.8f), r * (0.3f + entrance.value * 0.8f), c, style = Stroke(3.dp.toPx()))
                repeat(18) { i ->
                    val angle = Math.toRadians(i * 20.0 + orbit)
                    val radius = r * (0.72f + (i % 3) * 0.1f)
                    val p = c + Offset(cos(angle).toFloat(), sin(angle).toFloat()) * radius
                    val extent = (if (i % 3 == 0) 5f else 3f).dp.toPx() * entrance.value
                    val color = gold.copy(alpha = glow)
                    drawLine(color, p - Offset(extent, 0f), p + Offset(extent, 0f), 2.dp.toPx())
                    drawLine(color, p - Offset(0f, extent), p + Offset(0f, extent), 2.dp.toPx())
                }
            }
            // The original full-resolution prototype is rendered in colour, without silhouette tint.
            TreasurePrototypeImage(relic, discovered = true,
                modifier = Modifier.fillMaxWidth(0.82f).height(290.dp).graphicsLayer {
                    alpha = entrance.value
                    scaleX = 0.65f + entrance.value * 0.35f
                    scaleY = scaleX
                    translationY = (1f - entrance.value) * 36.dp.toPx()
                })
        }
        Text("Congratulations!\nYou found a treasure.", color = ink, fontFamily = GothicTreasureFontFamily, fontSize = 30.sp, lineHeight = 35.sp, textAlign = TextAlign.Center)
        Text(relic.name, color = ink, fontWeight = FontWeight.Bold, fontSize = 21.sp, textAlign = TextAlign.Center)
        Text("The trail has revealed its secret. Discover the story within.", color = Color(0xFF786B60), textAlign = TextAlign.Center, fontSize = 14.sp)
        Button(onClick = onViewTreasure, modifier = Modifier.fillMaxWidth().height(56.dp), shape = RoundedCornerShape(14.dp), colors = ButtonDefaults.buttonColors(containerColor = ink)) {
            Text("View treasure", fontFamily = GothicTreasureFontFamily, color = Color(0xFFFFE1A0), fontSize = 22.sp)
        }
        TextButton(onClick = onReturnToMap) { Text("Return to map", color = ink) }
    }
}
