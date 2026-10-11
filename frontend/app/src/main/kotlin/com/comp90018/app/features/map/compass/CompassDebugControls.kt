package com.comp90018.app.features.map.compass

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.comp90018.app.Ink
import com.comp90018.app.features.map.debug.SensorTestSlider

/** Manual distance and heading preview; the entry point restricts this panel to debug builds. */
@Composable
internal fun CompassDebugControls(
    simulateSensors: Boolean,
    testDistance: Float,
    testHeading: Float,
    nearTreasure: Boolean,
    facingTreasure: Boolean,
    onSimulateSensors: (Boolean) -> Unit,
    onTestDistance: (Float) -> Unit,
    onTestHeading: (Float) -> Unit,
) {
    Surface(color = Color(0xFFE8DDC6), shape = RoundedCornerShape(16.dp)) {
        Column(Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Sensor test controls", Modifier.weight(1f), color = Ink, fontWeight = FontWeight.Bold)
                Switch(simulateSensors, onSimulateSensors)
            }
            if (simulateSensors) {
                SensorTestSlider("Distance · GPS", testDistance, 0f..30f, "m", if (nearTreasure) Color(0xFFB58A42) else Color(0xFF8D8984)) { onTestDistance(it) }
                SensorTestSlider("Heading · rotation", testHeading, 0f..360f, "°", if (facingTreasure) Color(0xFFA44736) else Color(0xFF8D8984)) { onTestHeading(it) }
            }
        }
    }
}
