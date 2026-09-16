package com.comp90018.app.features.map

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.MyLocation
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.comp90018.app.Brand
import com.comp90018.app.features.treasure.RelicItem

/** A deliberately offline campus map placeholder that can later be replaced by Google Maps. */
@Composable
fun CampusMap(
    relics: List<RelicItem>,
    selectedId: String?,
    onRelicSelected: (RelicItem) -> Unit,
    modifier: Modifier = Modifier,
    focusRelic: RelicItem? = null,
) {
    BoxWithConstraints(modifier.background(Color(0xFFEAE8DC))) {
        Canvas(Modifier.matchParentSize()) {
            drawRect(Color(0xFFECEBDD))

            // Parkland and campus blocks.
            drawRoundRect(
                color = Color(0xFFCFE1BE),
                topLeft = Offset(size.width * .56f, size.height * .05f),
                size = Size(size.width * .38f, size.height * .28f),
                cornerRadius = CornerRadius(34f, 34f),
            )
            drawRoundRect(
                color = Color(0xFFD7E5C8),
                topLeft = Offset(size.width * .06f, size.height * .58f),
                size = Size(size.width * .38f, size.height * .31f),
                cornerRadius = CornerRadius(38f, 38f),
            )

            val road = Color(0xFFF9F8F2)
            val roadEdge = Color(0xFFD3D0C4)
            listOf(.18f, .48f, .82f).forEach { x ->
                drawLine(roadEdge, Offset(size.width * x, 0f), Offset(size.width * (x - .08f), size.height), 38f)
                drawLine(road, Offset(size.width * x, 0f), Offset(size.width * (x - .08f), size.height), 30f)
            }
            listOf(.25f, .55f, .84f).forEach { y ->
                drawLine(roadEdge, Offset(0f, size.height * y), Offset(size.width, size.height * (y + .04f)), 38f)
                drawLine(road, Offset(0f, size.height * y), Offset(size.width, size.height * (y + .04f)), 30f)
            }

            // Simplified buildings.
            val building = Color(0xFFD7BDA3)
            val roof = Color(0xFFBC9C7D)
            val blocks = listOf(
                floatArrayOf(.07f, .08f, .24f, .12f),
                floatArrayOf(.33f, .10f, .18f, .12f),
                floatArrayOf(.28f, .32f, .25f, .14f),
                floatArrayOf(.61f, .38f, .27f, .12f),
                floatArrayOf(.50f, .60f, .19f, .14f),
                floatArrayOf(.67f, .82f, .25f, .10f),
                floatArrayOf(.08f, .39f, .13f, .12f),
            )
            blocks.forEach { b ->
                val topLeft = Offset(size.width * b[0], size.height * b[1])
                val blockSize = Size(size.width * b[2], size.height * b[3])
                drawRoundRect(building, topLeft, blockSize, CornerRadius(12f, 12f))
                drawRoundRect(roof, topLeft, blockSize, CornerRadius(12f, 12f), style = Stroke(4f))
            }

            val walk = Path().apply {
                moveTo(size.width * .04f, size.height * .93f)
                cubicTo(size.width * .26f, size.height * .76f, size.width * .34f, size.height * .21f, size.width * .93f, size.height * .08f)
            }
            drawPath(walk, Color(0xFFFDFBF3), style = Stroke(18f))
            drawPath(walk, Color(0xFFC9C4B8), style = Stroke(2f))

            // Pond and current position.
            drawOval(
                Color(0xFFAED8D7),
                topLeft = Offset(size.width * .06f, size.height * .68f),
                size = Size(size.width * .19f, size.height * .10f),
            )
            drawCircle(Color.White, 18f, Offset(size.width * .44f, size.height * .80f))
            drawCircle(Brand, 11f, Offset(size.width * .44f, size.height * .80f))
        }

        val displayedRelics = focusRelic?.let(::listOf) ?: relics
        displayedRelics.forEach { relic ->
            val x = maxWidth * relic.mapX - 27.dp
            val y = maxHeight * relic.mapY - 48.dp
            val selected = relic.id == selectedId || focusRelic?.id == relic.id
            IconButton(
                onClick = { onRelicSelected(relic) },
                modifier = Modifier
                    .offset(x = x, y = y)
                    .size(if (selected) 58.dp else 50.dp)
                    .shadow(if (selected) 9.dp else 4.dp, CircleShape)
                    .background(if (selected) relic.accent else Color.White, CircleShape)
                    .border(3.dp, Color.White, CircleShape),
            ) {
                Icon(
                    Icons.Rounded.LocationOn,
                    contentDescription = "Open ${relic.shortName}",
                    tint = if (selected) Color.White else relic.accent,
                    modifier = Modifier.size(if (selected) 32.dp else 28.dp),
                )
            }
        }

        Box(
            Modifier
                .align(Alignment.BottomEnd)
                .offset(x = (-16).dp, y = (-16).dp)
                .size(44.dp)
                .shadow(4.dp, CircleShape)
                .background(Color.White, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Rounded.MyLocation, contentDescription = "Current location", tint = Brand)
        }
    }
}
