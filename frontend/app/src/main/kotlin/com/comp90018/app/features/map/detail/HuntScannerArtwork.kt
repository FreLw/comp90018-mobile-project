package com.comp90018.app.features.map.detail

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.comp90018.app.Brand
import com.comp90018.app.BrandSoft
import com.comp90018.app.Ink
import com.comp90018.app.R
import java.util.Locale
import kotlin.math.cos
import kotlin.math.sin

/**
 * Reusable scan-and-dig hunt visual: an instruction headline, the [RelicScannerVisual] gauge (or
 * [AnimatedTreasureChest] once [ready]), a hint card, and Dig/Back actions. Callers own what decides
 * [ready] and what data feeds the gauge, so this can back either a scripted or sensor-driven hunt.
 */
@Composable
fun HuntScanPanel(
    headline: String,
    hintText: String,
    ready: Boolean,
    acceleration: Float,
    levelTilt: Float,
    heading: Float,
    primaryActionLabel: String,
    onPrimaryAction: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    soundProgress: Float? = null,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Text(
                headline,
                color = Ink,
                style = MaterialTheme.typography.titleLarge,
            )
        }
        item {
            if (ready) {
                AnimatedTreasureChest(Modifier.size(150.dp))
            } else {
                RelicScannerVisual(
                    acceleration = acceleration,
                    levelTilt = levelTilt,
                    heading = heading,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
        item {
            Card(
                Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = BrandSoft),
            ) {
                if (soundProgress != null) {
                    Column(
                        Modifier.fillMaxWidth().padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Text(
                            "Blow into the mic - the bar fills while you're loud enough.",
                            color = Ink,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        LinearProgressIndicator(
                            progress = { soundProgress.coerceIn(0f, 1f) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(10.dp)
                                .clip(RoundedCornerShape(5.dp)),
                            color = Brand,
                            trackColor = Color.White,
                        )
                    }
                } else {
                    Text(
                        hintText,
                        modifier = Modifier.padding(16.dp),
                        color = Ink,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        }
        if (ready) {
            item {
                Button(
                    onClick = onPrimaryAction,
                    modifier = Modifier.fillMaxWidth().height(54.dp),
                    shape = RoundedCornerShape(18.dp),
                ) {
                    Image(painterResource(R.drawable.nav_treasure_symbol), null, modifier = Modifier.size(30.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(primaryActionLabel, fontWeight = FontWeight.Bold)
                }
            }
        }
        item {
            OutlinedButton(
                onClick = onBack,
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = RoundedCornerShape(18.dp),
            ) {
                Text("Back", fontWeight = FontWeight.Bold)
            }
        }
    }
}

/** Draws the live scan instrument from distance, direction, motion, and posture readings. */
@Composable
internal fun RelicScannerVisual(
    acceleration: Float,
    levelTilt: Float,
    heading: Float,
    modifier: Modifier = Modifier,
) {
    val animatedHeading by animateFloatAsState(heading, tween(700), label = "scanner_heading")
    val animatedTilt by animateFloatAsState(levelTilt, tween(700), label = "scanner_level")
    val animatedMotion by animateFloatAsState(acceleration, tween(700), label = "scanner_motion")
    Card(
        modifier = modifier.height(236.dp),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF4B3024)),
    ) {
        Box(Modifier.fillMaxSize().padding(12.dp)) {
            Canvas(Modifier.fillMaxSize()) {
                val centre = Offset(size.width / 2f, size.height * 0.43f)
                val radius = size.minDimension * 0.31f
                drawCircle(
                    brush = Brush.radialGradient(
                        listOf(Color(0xFF8A5A32), Color(0xFF342319)),
                        center = centre,
                        radius = radius * 1.2f,
                    ),
                    radius = radius,
                    center = centre,
                )
                drawCircle(Color(0xFFE9B34F), radius, centre, style = Stroke(width = 5f))
                repeat(12) { index ->
                    val angle = Math.toRadians((index * 30.0) - 90.0)
                    val outer = Offset(
                        centre.x + kotlin.math.cos(angle).toFloat() * radius,
                        centre.y + kotlin.math.sin(angle).toFloat() * radius,
                    )
                    val inner = Offset(
                        centre.x + kotlin.math.cos(angle).toFloat() * radius * 0.86f,
                        centre.y + kotlin.math.sin(angle).toFloat() * radius * 0.86f,
                    )
                    drawLine(Color(0xFFFFE3A0), inner, outer, strokeWidth = if (index % 3 == 0) 5f else 2f)
                }
                val needleAngle = Math.toRadians(animatedHeading.toDouble() - 90.0)
                val needleTip = Offset(
                    centre.x + kotlin.math.cos(needleAngle).toFloat() * radius * 0.72f,
                    centre.y + kotlin.math.sin(needleAngle).toFloat() * radius * 0.72f,
                )
                drawLine(Color(0xFFF25B45), centre, needleTip, strokeWidth = 10f)
                drawCircle(Color.White, radius = 9f, center = centre)

                val levelWidth = radius * 1.15f
                val levelY = centre.y + radius * 0.48f
                drawLine(Color.White.copy(alpha = 0.38f), Offset(centre.x - levelWidth / 2f, levelY), Offset(centre.x + levelWidth / 2f, levelY), strokeWidth = 5f)
                val bubbleOffset = (animatedTilt / 15f).coerceIn(-1f, 1f) * levelWidth * 0.42f
                drawCircle(Color(0xFF72D49B), radius = 10f, center = Offset(centre.x + bubbleOffset, levelY))

                val motionFraction = (animatedMotion / 1.5f).coerceIn(0f, 1f)
                drawArc(
                    color = Color(0xFFF2B544),
                    startAngle = 145f,
                    sweepAngle = 250f * motionFraction,
                    useCenter = false,
                    topLeft = Offset(centre.x - radius * 0.78f, centre.y - radius * 0.78f),
                    size = androidx.compose.ui.geometry.Size(radius * 1.56f, radius * 1.56f),
                    style = Stroke(width = 8f),
                )
            }
            Text("N", modifier = Modifier.align(Alignment.TopCenter).padding(top = 5.dp), color = Color.White)
            Row(
                Modifier.align(Alignment.BottomCenter).fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                ScannerReading("MOTION", String.format(Locale.US, "%.2f m/s²", animatedMotion))
                ScannerReading("LEVEL", String.format(Locale.US, "%.1f°", animatedTilt))
                ScannerReading("HEADING", String.format(Locale.US, "%.0f°", animatedHeading))
            }
        }
    }
}

@Composable
private fun ScannerReading(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, color = Color(0xFFE9B34F), style = MaterialTheme.typography.labelSmall)
        Text(value, color = Color.White, style = MaterialTheme.typography.bodySmall)
    }
}

/** Draws the looping treasure-chest feedback illustration used in hunt panels. */
@Composable
private fun AnimatedTreasureChest(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "treasure_chest")
    val lift by transition.animateFloat(
        initialValue = 0f,
        targetValue = -16f,
        animationSpec = infiniteRepeatable(tween(520), RepeatMode.Reverse),
        label = "treasure_chest_lift",
    )
    val tilt by transition.animateFloat(
        initialValue = -3f,
        targetValue = 3f,
        animationSpec = infiniteRepeatable(tween(420), RepeatMode.Reverse),
        label = "treasure_chest_tilt",
    )
    Image(
        painter = painterResource(R.drawable.treasure_chest_symbol),
        contentDescription = "Treasure chest found",
        modifier = modifier.graphicsLayer {
            translationY = lift
            rotationZ = tilt
        },
    )
}
