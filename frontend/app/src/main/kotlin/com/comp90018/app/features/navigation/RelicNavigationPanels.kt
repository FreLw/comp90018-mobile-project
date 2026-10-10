package com.comp90018.app.features.navigation

/*
 * Renders the destination card, resonance description, and arrival/start-hunt action.
 * Edit panel hierarchy, captions, and button styling here.
 */

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.comp90018.app.Background
import com.comp90018.app.Brand
import com.comp90018.app.Ink
import com.comp90018.app.Muted
import com.comp90018.app.RelicGold
import kotlin.math.abs
import kotlin.math.roundToInt

/** Displays destination identity, current distance, and the Stop Navigation action. */
@Composable
internal fun RelicNavigationTargetPanel(
    name: String,
    locationName: String,
    distanceMeters: Double?,
    onStopNavigation: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.semantics { paneTitle = "Navigation target" },
        shape = RoundedCornerShape(18.dp),
        color = Background.copy(alpha = 0.97f),
        border = BorderStroke(0.8.dp, Brand.copy(alpha = 0.25f)),
        shadowElevation = 2.dp,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState())
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(name, color = Ink, fontWeight = FontWeight.SemiBold,
                    style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(locationName, color = Muted, style = MaterialTheme.typography.bodyMedium,
                    maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(navigationDistanceLabel(distanceMeters), color = Brand,
                    style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
            }
            Spacer(Modifier.width(12.dp))
            OutlinedButton(
                onClick = onStopNavigation,
                modifier = Modifier.heightIn(min = 48.dp),
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(0.8.dp, Brand.copy(alpha = 0.45f)),
            ) {
                Text("Stop", color = Brand)
            }
        }
    }
}

/** Presents compass strength and location readiness, enabling Start Hunt only after confirmed arrival. */
@Composable
internal fun RelicNavigationResonancePanel(
    state: RelicNavigationUiState,
    presentation: RelicArrivalPresentation,
    arrivalSweep: Float,
    arrivalScale: Float,
    onBeginHunt: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.semantics { paneTitle = "Relic resonance guidance" },
        shape = RoundedCornerShape(18.dp),
        color = Background.copy(alpha = 0.97f),
        border = BorderStroke(0.8.dp, Brand.copy(alpha = 0.25f)),
        shadowElevation = 2.dp,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                RosetteResonanceGauge(
                    progress = state.resonanceProgress,
                    stage = state.resonanceStage,
                    stateDescription = presentation.stateDescription,
                    arrivalSweep = arrivalSweep,
                    scale = arrivalScale,
                    modifier = Modifier.size(64.dp),
                )
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text("Relic Resonance", color = Ink, fontWeight = FontWeight.SemiBold,
                        style = MaterialTheme.typography.titleMedium)
                    Text(presentation.stateDescription, color = Muted,
                        style = MaterialTheme.typography.bodySmall)
                    Text(
                        when (state.directionHint) {
                            NavigationDirectionHint.UNAVAILABLE -> "Finding direction…"
                            NavigationDirectionHint.ALIGNED -> "Trail aligned"
                            NavigationDirectionHint.TURN_LEFT -> "Turn left · ${abs(requireNotNull(state.headingErrorDegrees)).roundToInt()}°"
                            NavigationDirectionHint.TURN_RIGHT -> "Turn right · ${abs(requireNotNull(state.headingErrorDegrees)).roundToInt()}°"
                        },
                        color = Ink,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
            if (presentation.showBeginHunt) {
                HorizontalDivider(color = RelicGold.copy(alpha = 0.3f), thickness = 0.5.dp)
                Button(onClick = onBeginHunt, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                    Text("Begin Hunt")
                }
            }
        }
    }
}
