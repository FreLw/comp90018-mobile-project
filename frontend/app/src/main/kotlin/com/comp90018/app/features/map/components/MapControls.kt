package com.comp90018.app.features.map.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.comp90018.app.Brand
import com.comp90018.app.GothicTreasureFontFamily
import com.comp90018.app.Ink
import com.comp90018.app.features.map.rendering.MapPerspective

@Composable
internal fun FindTreasurePrompt(
    message: String,
    loading: Boolean,
    onRetry: (() -> Unit)?,
    actionLabel: String? = null,
    onAction: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    ElevatedCard(modifier = modifier, shape = RoundedCornerShape(20.dp), colors = CardDefaults.elevatedCardColors(containerColor = Color.White.copy(alpha = 0.96f))) {
        Column(
            Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (loading) CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 3.dp, color = Brand)
                Text(message, modifier = Modifier.weight(1f), color = if (onRetry == null) Ink else MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                onRetry?.let { retry -> Button(onClick = retry) { Text("Retry") } }
            }
            actionLabel?.let { label ->
                DiscoveryHuntButton(label, onAction)
            }
        }
    }
}

@Composable
internal fun PerspectiveSwitch(
    selected: MapPerspective,
    onSelected: (MapPerspective) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(modifier = modifier, shape = RoundedCornerShape(18.dp), shadowElevation = 6.dp, color = Color.White.copy(alpha = 0.96f)) {
        Row(Modifier.padding(4.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            PerspectiveOption("God view", selected == MapPerspective.GOD) { onSelected(MapPerspective.GOD) }
            PerspectiveOption("Hunt view", selected == MapPerspective.HUNT) { onSelected(MapPerspective.HUNT) }
        }
    }
}

@Composable
private fun PerspectiveOption(label: String, selected: Boolean, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        color = if (selected) Brand else Color.Transparent,
    ) {
        Text(
            label,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
            color = if (selected) Color.White else Ink,
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.labelMedium,
        )
    }
}

@Composable
internal fun DiscoveryHuntButton(label: String, onClick: () -> Unit) {
    val pulse = rememberInfiniteTransition(label = "discovery")
    val halo by pulse.animateFloat(0.2f, 1f, infiniteRepeatable(tween(1100), RepeatMode.Reverse), label = "discovery_halo")
    val reveal = remember { androidx.compose.animation.core.MutableTransitionState(false).apply { targetState = true } }
    AnimatedVisibility(visibleState = reveal, enter = fadeIn(tween(450)) + slideInVertically { it / 2 }) {
        Box(Modifier.fillMaxWidth().height(66.dp), contentAlignment = Alignment.Center) {
            Canvas(Modifier.fillMaxSize()) {
                repeat(8) { i ->
                    val x = size.width * (i + 0.5f) / 8f
                    val y = if (i % 2 == 0) 5f else size.height - 5f
                    drawLine(Color(0xFFB58A42).copy(alpha = halo), Offset(x - 4f, y), Offset(x + 4f, y), 2f)
                    drawLine(Color(0xFFB58A42).copy(alpha = halo), Offset(x, y - 4f), Offset(x, y + 4f), 2f)
                }
            }
            Button(onClick = onClick, modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp).height(48.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF382B40)), shape = RoundedCornerShape(12.dp)) {
                Text("✦  $label  ✦", fontFamily = GothicTreasureFontFamily, fontSize = 20.sp, color = Color(0xFFFFE1A0))
            }
        }
    }
}

/** Shown when GPS has gone stale/inaccurate and the dot on the map is a last-known fallback, not a live fix. */
@Composable
internal fun StaleLocationBadge(modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        color = Color(0xFFFFF4D6),
        shadowElevation = 2.dp,
    ) {
        Text(
            "Weak signal — showing last known location",
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            color = Ink,
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp,
        )
    }
}
