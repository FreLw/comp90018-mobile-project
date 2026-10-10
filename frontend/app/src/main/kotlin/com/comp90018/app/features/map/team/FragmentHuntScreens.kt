package com.comp90018.app.features.map.team

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.comp90018.app.Brand
import com.comp90018.app.BuildConfig
import com.comp90018.app.Ink
import com.comp90018.app.Muted
import com.comp90018.app.data.rooms.TeamHuntLocation
import com.comp90018.app.features.map.LocationActionPolicy
import com.comp90018.app.features.map.TeamHuntFragment
import com.comp90018.app.features.map.components.formatDistance
import com.comp90018.app.features.map.rendering.GoogleMapView
import com.comp90018.app.sensors.location.LocationCalculator
import com.comp90018.app.sensors.location.LocationOutput
import kotlinx.coroutines.flow.collect

/** Renders shared fragment markers and the find-fragment interaction for the Atlas hunt. */
@Composable
internal fun SouthLawnFragmentHuntScreen(
    teammateLocations: List<TeamHuntLocation>,
    huntMemberNames: Map<String, String>,
    huntOwnerId: String?,
    fragments: List<TeamHuntFragment>,
    collectionRadiusMeters: Double?,
    foundFragmentIds: List<String>,
    userLocation: LocationOutput,
    deviceHeading: Float,
    onFindFragment: (String) -> Unit,
    onDebugUnlockFragments: (List<String>, (String?) -> Unit) -> Unit,
) {
    var selectedFragmentId by remember { mutableStateOf<String?>(null) }
    var debugUnlocking by remember { mutableStateOf(false) }
    var debugUnlockError by remember { mutableStateOf<String?>(null) }
    val selectedFragment = fragments.firstOrNull { it.id == selectedFragmentId }
    val currentLocation = LocationActionPolicy.actionableCoordinate(userLocation)
    val selectedDistance = selectedFragment?.let { fragment ->
        currentLocation?.let { LocationCalculator.distanceMeters(it, fragment.coordinate) }
    }
    val canCollect = collectionRadiusMeters != null && selectedFragment?.let { fragment ->
        LocationActionPolicy.isWithinRadius(
            location = userLocation,
            target = fragment.coordinate,
            radiusMeters = collectionRadiusMeters,
        )
    } == true

    Box(Modifier.fillMaxSize()) {
        GoogleMapView(
            relics = emptyList(),
            selectedRelic = null,
            locationOutput = userLocation,
            deviceHeading = deviceHeading,
            huntFragments = fragments,
            teammateLocations = teammateLocations,
            huntMemberNames = huntMemberNames,
            huntOwnerId = huntOwnerId,
            foundFragmentIds = foundFragmentIds.toSet(),
            focusSelectedRelic = false,
            onRelicSelected = {},
            onFragmentSelected = { selectedFragmentId = it.id },
            modifier = Modifier.fillMaxSize(),
        )
        Card(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(14.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.96f)),
            shape = RoundedCornerShape(20.dp),
        ) {
            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("South Lawn Atlas fragments", color = Ink, fontWeight = FontWeight.Bold)
                if (collectionRadiusMeters == null) {
                    Text("Fragment hunt unavailable. Check the treasure configuration in Firebase.", color = Muted)
                } else {
                    Text("${fragments.count { it.id in foundFragmentIds }}/${fragments.size} fragments found", color = Brand)
                }
                fragments.forEach { fragment ->
                    val found = fragment.id in foundFragmentIds
                    Text(
                        "${if (found) "✓" else "○"} ${fragment.title}: ${if (found) "is found" else "not found"}",
                        color = if (found) Brand else Muted,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                if (BuildConfig.DEBUG) {
                    Button(
                        onClick = {
                            debugUnlocking = true
                            debugUnlockError = null
                            onDebugUnlockFragments(fragments.map { it.id }) { error ->
                                debugUnlocking = false
                                debugUnlockError = error
                            }
                        },
                        enabled = collectionRadiusMeters != null && !debugUnlocking,
                    ) {
                        Text(if (debugUnlocking) "Unlocking fragments…" else "Debug: Unlock all 4 fragments")
                    }
                    debugUnlockError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                }
            }
        }
        selectedFragment?.takeIf { it.id !in foundFragmentIds }?.let { fragment ->
            Card(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.97f)),
                shape = RoundedCornerShape(20.dp),
            ) {
                Column(Modifier.padding(14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(fragment.title, color = Ink, fontWeight = FontWeight.Bold)
                    Text(selectedDistance.formatDistance() + " away", color = Muted)
                    Button(
                        onClick = { onFindFragment(fragment.id); selectedFragmentId = null },
                        enabled = canCollect && !debugUnlocking,
                        colors = ButtonDefaults.buttonColors(containerColor = Brand),
                    ) { Text("Collect fragment") }
                    if (!canCollect) Text("Move within $collectionRadiusMeters m to collect this fragment.", color = Muted, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

/** Shown after this explorer has claimed the reconstructed Atlas, until their teammate does too. */
@Composable
internal fun SouthLawnClaimWaitingScreen(
    teammateLocations: List<TeamHuntLocation>,
    huntMemberNames: Map<String, String>,
    huntOwnerId: String?,
    userLocation: LocationOutput,
    deviceHeading: Float,
) {
    Box(Modifier.fillMaxSize()) {
        GoogleMapView(
            relics = emptyList(),
            selectedRelic = null,
            locationOutput = userLocation,
            deviceHeading = deviceHeading,
            focusSelectedRelic = false,
            teammateLocations = teammateLocations,
            huntMemberNames = huntMemberNames,
            huntOwnerId = huntOwnerId,
            onRelicSelected = {},
            modifier = Modifier.fillMaxSize(),
        )
        Card(
            modifier = Modifier.align(Alignment.TopCenter).padding(14.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.96f)),
            shape = RoundedCornerShape(20.dp),
        ) {
            Column(Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Atlas claimed", color = Ink, fontWeight = FontWeight.Bold)
                Text("Waiting for your teammate to claim their Atlas.", color = Muted, textAlign = TextAlign.Center)
            }
        }
    }
}
