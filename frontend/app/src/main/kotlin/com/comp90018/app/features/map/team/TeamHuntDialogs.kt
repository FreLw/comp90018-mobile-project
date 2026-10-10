package com.comp90018.app.features.map.team

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.WbIncandescent
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.comp90018.app.Background
import com.comp90018.app.Ink
import com.comp90018.app.Muted

/** Offers the location-triggered team-task entry while preserving acknowledgement across visits. */
@Composable
internal fun TeamHuntArrivalDialog(
    isOwner: Boolean,
    onDismiss: () -> Unit,
    onContinue: () -> Unit,
) {
    val glow = rememberInfiniteTransition(label = "team_hunt_lightbulb")
    val glowScale by glow.animateFloat(
        initialValue = 0.94f,
        targetValue = 1.10f,
        animationSpec = infiniteRepeatable(tween(850), RepeatMode.Reverse),
        label = "team_hunt_lightbulb_glow",
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Rounded.WbIncandescent,
                contentDescription = null,
                tint = Color(0xFFFFB300),
                modifier = Modifier.size(54.dp).graphicsLayer {
                    scaleX = glowScale
                    scaleY = glowScale
                    shadowElevation = 18f
                },
            )
        },
        title = {
            Text(
                "Location found!",
                color = Ink,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        },
        text = {
            Text(
                if (isOwner) {
                    "Congratulations, you have found the location. Next, complete the following task to unlock the treasure!"
                } else {
                    "Congratulations, you have found the location. Next, answer the following questions to unlock the treasure!"
                },
                color = Muted,
                textAlign = TextAlign.Center,
            )
        },
        confirmButton = {
            Button(onClick = onContinue) {
                Text(if (isOwner) "Start task" else "Answer questions")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) { Text("Later") }
        },
    )
}

/** Explains a team hunt state with optional task entry and a return action. */
@Composable
internal fun TeamHuntStatusScreen(message: String, onStartTask: (() -> Unit)?, onBack: () -> Unit) {
    Column(Modifier.fillMaxSize().background(Background).padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("Team hunt", style = MaterialTheme.typography.headlineSmall, color = Ink)
        Text(message, color = Muted)
        onStartTask?.let { start ->
            Button(onClick = start, modifier = Modifier.fillMaxWidth()) { Text("Start task") }
        }
        OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) { Text("Back to map") }
    }
}
