package com.comp90018.app.features.navigation

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
import com.comp90018.app.Muted
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
    val emphasis = when (stage) {
        RelicResonanceStage.ACQUIRING -> 0.18f
        RelicResonanceStage.DORMANT -> 0.3f
        RelicResonanceStage.FAINT -> 0.45f
        RelicResonanceStage.DRAWN -> 0.65f
        RelicResonanceStage.STRONG -> 0.85f
        RelicResonanceStage.CONFIRMING -> breath
        RelicResonanceStage.ARRIVED -> 1f
    }
    Canvas(
        modifier.graphicsLayer { scaleX = scale; scaleY = scale }.semantics {
            contentDescription = "Relic resonance"
            stateDescription = resonanceStageLabel(stage)
            progressBarRangeInfo = ProgressBarRangeInfo(safeProgress, 0f..1f)
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
        drawCircle(RelicGold.copy(alpha = 0.65f), radius * 0.87f, centre, style = Stroke(0.7.dp.toPx()))
        drawArc(
            color = RelicGold.copy(alpha = emphasis), startAngle = -90f,
            sweepAngle = displayedProgress * 360f, useCenter = false,
            topLeft = centre - Offset(radius * 0.76f, radius * 0.76f),
            size = Size(radius * 1.52f, radius * 1.52f), style = Stroke(2.dp.toPx()),
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
            drawPath(petal, RelicGold.copy(alpha = emphasis * 0.3f))
            drawPath(petal, if (stage == RelicResonanceStage.ACQUIRING) Muted else Brand,
                style = Stroke(0.7.dp.toPx()))
        }
        drawCircle(RelicGold.copy(alpha = emphasis), radius * 0.09f, centre)
        if (stage == RelicResonanceStage.ARRIVED && arrivalSweep in 0f..1f) {
            drawArc(RelicGold, arrivalSweep * 360f - 90f, 25f, false,
                centre - Offset(radius * 0.87f, radius * 0.87f),
                Size(radius * 1.74f, radius * 1.74f), style = Stroke(1.6.dp.toPx()))
        }
    }
}

internal fun resonanceStageLabel(stage: RelicResonanceStage): String = when (stage) {
    RelicResonanceStage.ACQUIRING -> "Acquiring position"
    RelicResonanceStage.DORMANT -> "Outside resonance"
    RelicResonanceStage.FAINT -> "Faint resonance"
    RelicResonanceStage.DRAWN -> "Drawn closer"
    RelicResonanceStage.STRONG -> "Strong resonance"
    RelicResonanceStage.CONFIRMING -> "Confirming arrival"
    RelicResonanceStage.ARRIVED -> "Search area reached"
}
