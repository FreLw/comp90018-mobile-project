package com.unimelb.losttreasures.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun RelicBadge(
    color: Color,
    modifier: Modifier = Modifier
) {
    // Reusable placeholder relic mark until final illustrated treasure assets are available.
    Canvas(modifier = modifier) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val outer = size.minDimension * 0.48f
        val inner = size.minDimension * 0.24f
        val points = 8
        val path = Path()

        repeat(points * 2) { index ->
            val radius = if (index % 2 == 0) outer else inner
            val angle = Math.toRadians((index * 180f / points - 90f).toDouble())
            val point = Offset(
                x = center.x + cos(angle).toFloat() * radius,
                y = center.y + sin(angle).toFloat() * radius
            )

            if (index == 0) {
                path.moveTo(point.x, point.y)
            } else {
                path.lineTo(point.x, point.y)
            }
        }

        path.close()
        drawPath(path = path, color = color)
    }
}
