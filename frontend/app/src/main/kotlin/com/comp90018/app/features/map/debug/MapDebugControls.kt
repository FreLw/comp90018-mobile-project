package com.comp90018.app.features.map.debug

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.comp90018.app.Brand
import com.comp90018.app.BuildConfig
import com.comp90018.app.Ink
import com.comp90018.app.RelicRed
import com.comp90018.app.features.map.MapRelic
import java.util.Locale

/** Lists local task previews available from the debug map interface. */
@Composable
internal fun DebugChallengeLauncher(
    relics: List<MapRelic>,
    onLaunch: (MapRelic) -> Unit,
    teamTaskLabel: String,
    canCompleteTeamTask: Boolean,
    huntSessionKey: String?,
    onCompleteTeamTask: ((String?) -> Unit) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (!BuildConfig.DEBUG) return
    var expanded by remember(huntSessionKey) { mutableStateOf(false) }
    var completing by remember(huntSessionKey) { mutableStateOf(false) }
    var completionError by remember(huntSessionKey) { mutableStateOf<String?>(null) }
    Box(modifier) {
        Surface(shape = RoundedCornerShape(16.dp), color = Color.White.copy(alpha = 0.96f), shadowElevation = 4.dp) {
            TextButton(onClick = { expanded = true }) {
                Icon(Icons.Rounded.Star, "Open debug tasks", tint = Brand, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("task", color = Brand, fontWeight = FontWeight.SemiBold)
            }
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { if (!completing) expanded = false }) {
            relics.forEach { relic ->
                DropdownMenuItem(
                    text = { Text(relic.locationName) },
                    enabled = !completing,
                    onClick = { expanded = false; completionError = null; onLaunch(relic) },
                )
            }
            DropdownMenuItem(
                text = { Text(if (completing) "Completing team task..." else teamTaskLabel) },
                enabled = canCompleteTeamTask && !completing,
                onClick = {
                    completing = true
                    completionError = null
                    onCompleteTeamTask { error ->
                        completing = false
                        completionError = error
                        if (error == null) expanded = false
                    }
                },
            )
            completionError?.let { message ->
                Text(message, color = RelicRed, modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
            }
        }
    }
}

/** Provides the map debug distance slider without changing the live location source. */
@Composable
internal fun DistanceSimulationControl(
    distance: Double?,
    onDistance: (Double?) -> Unit,
    modifier: Modifier = Modifier,
    maximumDistance: Float = 120f,
    initialDistance: Double = 100.0,
) {
    Surface(modifier.width(240.dp), shape = RoundedCornerShape(18.dp), color = Color(0xFFFFF4DE), shadowElevation = 4.dp) {
        Column(Modifier.padding(horizontal = 14.dp, vertical = 6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Test distance: ${distance?.let { String.format(Locale.US, "%.1f m", it) } ?: "GPS"}", Modifier.weight(1f), color = Ink, fontSize = 12.sp)
                Switch(checked = distance != null, onCheckedChange = { onDistance(if (it) initialDistance else null) })
            }
            Slider(
                value = (distance ?: initialDistance).toFloat(),
                onValueChange = { onDistance(it.toDouble()) },
                valueRange = 0f..maximumDistance,
            )
        }
    }
}

/** Provides the map debug heading slider used to preview directional presentation. */
@Composable
internal fun HeadingSimulationControl(
    headingDegrees: Double?,
    onHeading: (Double?) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(modifier.width(240.dp), shape = RoundedCornerShape(18.dp), color = Color(0xFFFFF4DE), shadowElevation = 4.dp) {
        Column(Modifier.padding(horizontal = 14.dp, vertical = 6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Test heading: ${headingDegrees?.let { String.format(Locale.US, "%.0f°", it) } ?: "sensor"}",
                    Modifier.weight(1f),
                    color = Ink,
                    fontSize = 12.sp,
                )
                Switch(checked = headingDegrees != null, onCheckedChange = { onHeading(if (it) 0.0 else null) })
            }
            Slider(
                value = (headingDegrees ?: 0.0).toFloat(),
                onValueChange = { onHeading(it.toDouble()) },
                valueRange = 0f..360f,
            )
        }
    }
}

/** Styles one continuous sensor-preview slider and its current numeric reading. */
@Composable
internal fun SensorTestSlider(label: String, value: Float, range: ClosedFloatingPointRange<Float>, unit: String, color: Color, onChange: (Float) -> Unit) {
    Text("$label  ${String.format(Locale.US, "%.1f", value)}$unit", color = Ink, style = MaterialTheme.typography.labelMedium)
    Slider(value = value, onValueChange = onChange, valueRange = range, colors = androidx.compose.material3.SliderDefaults.colors(thumbColor = color, activeTrackColor = color))
}
