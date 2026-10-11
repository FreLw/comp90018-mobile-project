package com.comp90018.app.features.map.compass

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.comp90018.app.BrandSoft
import com.comp90018.app.GothicTreasureFontFamily
import com.comp90018.app.Ink
import com.comp90018.app.features.map.CompassGateConfig
import com.comp90018.app.features.map.components.DiscoveryHuntButton
import com.comp90018.app.features.map.components.formatDegrees
import com.comp90018.app.features.map.components.formatDistance
import com.comp90018.app.features.map.components.formatGateDistance
import kotlin.math.abs

/** Distance and bearing requirements, with the task-entry action. */
@Composable
internal fun CompassStatusCard(
    state: CompassGateUiState,
    config: CompassGateConfig,
    signalLocked: Boolean,
    instruction: String,
    leaving: Boolean,
    onContinue: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val distance = state.distanceMeters
    val turnDegrees = state.turnDegrees
    val nearTreasure = state.readiness.nearTreasure
    val facingTreasure = state.readiness.facingTreasure
    val allReady = state.readiness.allReady
    val proximityGlow = state.proximityGlow
    val headingGlow = state.headingGlow
    Card(
        modifier,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = if (signalLocked) BrandSoft else Color(0xFFFFF1C9)),
    ) {
        Column(
            Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 11.dp),
            verticalArrangement = Arrangement.spacedBy(7.dp),
        ) {
            Text(instruction, modifier = Modifier.fillMaxWidth(), color = Ink, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                OracleLamp("The Trail", "Draw nearer", "${distance.formatDistance()} / ${config.huntReadyRadiusMeters.formatGateDistance()}", nearTreasure, proximityGlow, Color(0xFFBD903D), Modifier.weight(1f))
                OracleLamp("The Bearing", "Follow the calling", turnDegrees?.let { "${abs(it).formatDegrees()} / ${config.compassAlignmentToleranceDegrees.formatDegrees()}" } ?: "Await signal", facingTreasure, headingGlow, Color(0xFFA44736), Modifier.weight(1f))
            }
            AnimatedVisibility(visible = allReady, enter = fadeIn(tween(350)) + slideInVertically { it / 2 }) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("All lanterns are alight. The relic awaits.", color = Ink, fontFamily = GothicTreasureFontFamily, fontSize = 18.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                    if (!leaving) DiscoveryHuntButton("Hunt", onContinue)
                }
            }
        }
    }
}
