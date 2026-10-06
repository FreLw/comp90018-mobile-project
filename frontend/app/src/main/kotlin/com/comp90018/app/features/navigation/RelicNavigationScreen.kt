package com.comp90018.app.features.navigation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.MyLocation
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.comp90018.app.Brand
import com.comp90018.app.Ink
import com.comp90018.app.Muted
import com.comp90018.app.features.map.GoogleMapView
import com.comp90018.app.features.map.MapPerspective
import com.comp90018.app.features.map.MapRelic
import com.comp90018.app.features.map.rememberDeviceHeading
import com.comp90018.app.sensors.location.LocationOutput

/** Standalone target-focused map. It does not own or mutate the existing hunt state machine. */
@Composable
fun RelicNavigationScreen(
    relic: MapRelic,
    userLocation: LocationOutput,
    onStopNavigation: () -> Unit,
) {
    val deviceHeading = rememberDeviceHeading()
    var recenterRequestKey by remember { mutableIntStateOf(0) }
    Box(Modifier.fillMaxSize()) {
        GoogleMapView(
            relics = listOf(relic),
            selectedRelic = relic,
            locationOutput = userLocation,
            deviceHeading = deviceHeading,
            activeHuntTreasureId = relic.id,
            focusSelectedRelic = false,
            perspective = MapPerspective.HUNT,
            allowCameraGestures = true,
            allowZoomGestures = false,
            allowRotateGestures = false,
            allowTiltGestures = false,
            recenterRequestKey = recenterRequestKey,
            onRelicSelected = {},
            modifier = Modifier.fillMaxSize(),
        )

        Surface(
            modifier = Modifier.align(Alignment.CenterEnd).padding(end = 12.dp),
            shape = RoundedCornerShape(18.dp),
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
            shadowElevation = 5.dp,
        ) {
            IconButton(onClick = { recenterRequestKey += 1 }) {
                Icon(Icons.Rounded.MyLocation, "Recenter map", tint = Brand)
            }
        }

        Surface(
            modifier = Modifier.align(Alignment.TopCenter).fillMaxWidth(),
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
            shadowElevation = 4.dp,
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onStopNavigation) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Stop navigation", tint = Ink)
                }
                Text(
                    "Navigation",
                    color = Ink,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleLarge,
                )
            }
        }

        Surface(
            modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(12.dp),
            shape = RoundedCornerShape(22.dp),
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.97f),
            shadowElevation = 10.dp,
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Column(Modifier.weight(1f)) {
                    Text(relic.name, color = Ink, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                    Text(relic.locationName, color = Muted, style = MaterialTheme.typography.bodyMedium)
                }
                OutlinedButton(onClick = onStopNavigation) {
                    Text("Stop")
                }
            }
        }
    }
}
