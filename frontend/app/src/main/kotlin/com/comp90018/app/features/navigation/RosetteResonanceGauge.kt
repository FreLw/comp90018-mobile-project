package com.comp90018.app.features.navigation

/*
 * Draws the flat compass-rosette instrument used by the navigation interface.
 * The ring shows distance strength; petals and the arrival sweep provide separate visual cues.
 */

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import com.comp90018.app.Brand
import com.comp90018.app.BrandSoft
import com.comp90018.app.Ink
import com.comp90018.app.RelicGold
import kotlin.math.cos
import kotlin.math.sin

/** Flat compass-rosette instrument. A full ring conveys strength, never hunt permission. */
@Composable
internal fun RosetteResonanceGauge(
    progress: Float,
    stage: RelicResonanceStage,
    arrivalSweep: Float,
    scale: Float,
    modifier: Modifier = Modifier,
    stateDescription: String = resonanceStageLabel(stage),
) {
    val safeProgress = if (progress.isFinite()) progress.coerceIn(0f, 1f) else 0f
    val displayedProgress by animateFloatAsState(safeProgress, tween(650), label = "rosette_resonance")
    val breath = if (stage == RelicResonanceStage.CONFIRMING) {
        val transition = rememberInfiniteTransition(label = "rosette_confirmation")
        val pulse by transition.animateFloat(
            initialValue = 0.65f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
            label = "rosette_breath",
        )
        pulse
    } else 1f
    val activation = rosetteActivation(displayedProgress, stage)
    Canvas(
        modifier.graphicsLayer { scaleX = scale; scaleY = scale }.semantics {
            contentDescription = "Relic resonance"
            this.stateDescription = stateDescription
            if (stage != RelicResonanceStage.ACQUIRING) {
                progressBarRangeInfo = ProgressBarRangeInfo(safeProgress, 0f..1f)
            }
        },
    ) {
        val radius = size.minDimension * 0.45f
        val centre = center
        fun point(angle: Double, length: Float): Offset = Offset(
            centre.x + cos(angle).toFloat() * length,
            centre.y + sin(angle).toFloat() * length,
        )
        drawCircle(BrandSoft, radius, centre)
        drawCircle(Ink, radius, centre, style = Stroke(1.5.dp.toPx()))
        drawCircle(Brand.copy(alpha = 0.25f), radius * 0.87f, centre, style = Stroke(0.6.dp.toPx()))
        drawCircle(Brand.copy(alpha = 0.2f), radius * 0.76f, centre, style = Stroke(1.dp.toPx()))
        if (stage != RelicResonanceStage.ACQUIRING) drawArc(
            color = RelicGold.copy(alpha = activation.arcAlpha * breath), startAngle = -90f,
            sweepAngle = displayedProgress * 360f, useCenter = false,
            topLeft = centre - Offset(radius * 0.76f, radius * 0.76f),
            size = Size(radius * 1.52f, radius * 1.52f), style = Stroke(3.dp.toPx()),
        )
        repeat(8) { index ->
            val angle = index * Math.PI / 4.0 - Math.PI / 2.0
            val tip = point(angle, radius * if (index % 2 == 0) 0.64f else 0.48f)
            val left = point(angle - 0.3, radius * 0.2f)
            val right = point(angle + 0.3, radius * 0.2f)
            val inner = point(angle, radius * 0.09f)
            val petal = Path().apply {
                moveTo(tip.x, tip.y)
                lineTo(left.x, left.y)
                lineTo(inner.x, inner.y)
                lineTo(right.x, right.y)
                close()
            }
            val active = index < activation.activePetals
            drawPath(petal, if (active) RelicGold.copy(alpha = activation.arcAlpha * breath)
                else Brand.copy(alpha = 0.06f))
            drawPath(petal, if (active) RelicGold else Brand.copy(alpha = 0.25f),
                style = Stroke(if (active) 0.8.dp.toPx() else 0.5.dp.toPx()))
        }
        drawCircle(if (activation.activePetals > 0) RelicGold.copy(alpha = activation.arcAlpha * breath)
            else Brand.copy(alpha = 0.25f), radius * 0.09f, centre)
        if (stage == RelicResonanceStage.ARRIVED) {
            val resolve = arrivalSweep.coerceIn(0f, 1f)
            drawArc(RelicGold, -90f, resolve * 360f, false,
                centre - Offset(radius * 0.87f, radius * 0.87f),
                Size(radius * 1.74f, radius * 1.74f), style = Stroke(1.6.dp.toPx()))
            // Four flat cartographic accents resolve with the same arrival sweep.
            repeat(4) { index ->
                val accent = point(index * Math.PI / 2.0 + Math.PI / 4.0, radius * 0.6f)
                val reach = radius * 0.08f
                val star = Path().apply {
                    moveTo(accent.x, accent.y - reach)
                    lineTo(accent.x + reach * 0.3f, accent.y - reach * 0.3f)
                    lineTo(accent.x + reach, accent.y)
                    lineTo(accent.x + reach * 0.3f, accent.y + reach * 0.3f)
                    lineTo(accent.x, accent.y + reach)
                    lineTo(accent.x - reach * 0.3f, accent.y + reach * 0.3f)
                    lineTo(accent.x - reach, accent.y)
                    lineTo(accent.x - reach * 0.3f, accent.y - reach * 0.3f)
                    close()
                }
                drawPath(star, RelicGold.copy(alpha = resolve))
            }
            drawCircle(Brand, radius * 0.12f, centre, style = Stroke(0.8.dp.toPx()))
        }
    }
}
