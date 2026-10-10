package com.comp90018.app.features.map

/*
 * Animates four Atlas fragments into the complete treasure before the team claim.
 * Uses the original untrimmed quadrants so the assembled image matches the full Atlas artwork.
 */

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.comp90018.app.Background
import com.comp90018.app.Brand
import com.comp90018.app.Ink
import com.comp90018.app.Muted
import com.comp90018.app.R
import kotlinx.coroutines.delay

/** Uses the untrimmed quadrants so their final arrangement exactly recreates the Atlas. */
@Composable
internal fun SouthLawnAssemblyScreen(
    onClaim: ((String?) -> Unit) -> Unit,
    onContinue: () -> Unit,
    onBack: () -> Unit,
) {
    val claim by rememberUpdatedState(onClaim)
    val progress = remember { Animatable(0f) }
    var attempt by remember { mutableIntStateOf(0) }
    var saving by remember { mutableStateOf(true) }
    var saved by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var assembled by remember { mutableStateOf(false) }
    val unlocked = saved && assembled
    BackHandler { if (!saving) { if (unlocked) onContinue() else onBack() } }
    LaunchedEffect(attempt) {
        saving = true
        saved = false
        error = null
        assembled = false
        progress.snapTo(0f)
        claim { failure ->
            saving = false
            saved = failure == null
            error = failure
        }
        delay(250)
        progress.animateTo(1f, tween(1600, easing = FastOutSlowInEasing))
        assembled = true
    }

    Column(
        Modifier.fillMaxSize().background(Background).safeDrawingPadding().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            if (unlocked) "Congratulations!" else "Reconstructing the Atlas",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold, color = Ink, textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(40.dp))
        BoxWithConstraints(Modifier.fillMaxWidth().widthIn(max = 320.dp), contentAlignment = Alignment.Center) {
            val side = minOf(maxWidth, 280.dp)
            val pieceSide = side / 2
            val travel = with(LocalDensity.current) { (side * 0.18f).toPx() }
            val artwork = listOf(
                R.drawable.treasure_atlas_fragment_north_west,
                R.drawable.treasure_atlas_fragment_north_east,
                R.drawable.treasure_atlas_fragment_south_west,
                R.drawable.treasure_atlas_fragment_south_east,
            )
            val alignments = listOf(Alignment.TopStart, Alignment.TopEnd, Alignment.BottomStart, Alignment.BottomEnd)
            Box(Modifier.size(side)) {
                artwork.forEachIndexed { index, resource ->
                    val xDirection = if (index % 2 == 0) -1f else 1f
                    val yDirection = if (index < 2) -1f else 1f
                    Image(
                        painterResource(resource), contentDescription = null,
                        contentScale = ContentScale.FillBounds,
                        modifier = Modifier.size(pieceSide).align(alignments[index]).graphicsLayer {
                            val remaining = 1f - progress.value
                            translationX = xDirection * travel * remaining
                            translationY = yDirection * travel * remaining
                            rotationZ = xDirection * yDirection * 12f * remaining
                            scaleX = 0.88f + progress.value * 0.12f
                            scaleY = scaleX
                            alpha = 0.6f + progress.value * 0.4f
                        },
                    )
                }
            }
        }
        Spacer(Modifier.height(40.dp))
        if (saving) {
            CircularProgressIndicator(Modifier.size(24.dp), color = Brand)
            Spacer(Modifier.height(12.dp))
            Text("Saving your treasure…", color = Muted)
        }
        AnimatedVisibility(unlocked) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("You've unlocked the South Lawn Atlas!", color = Brand,
                    style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
                Text("Your treasure is now in your backpack.", color = Muted, textAlign = TextAlign.Center)
                Spacer(Modifier.height(24.dp))
                Button(onClick = onContinue) { Text("View treasure") }
            }
        }
        error?.let { message ->
            Text(message, color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Center)
            Spacer(Modifier.height(16.dp))
            Button(onClick = { attempt += 1 }, enabled = assembled) { Text("Retry claim") }
            TextButton(onClick = onBack) { Text("Back to map") }
        }
    }
}
