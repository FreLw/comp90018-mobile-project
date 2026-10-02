package com.comp90018.app.features.map

import android.net.Uri
import android.widget.ImageView
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.comp90018.app.features.treasure.RemoteTreasureImage
import com.comp90018.app.features.treasure.TreasurePrototypeImage
import kotlinx.coroutines.delay

@Composable
fun PostChallengeRevealScreen(session: PostChallengeRevealSession, onReturnToMap: () -> Unit) {
    when (session.page) {
        PostChallengePage.PHOTO_HISTORY -> UnionPhotoHistoryReveal(session, onReturnToMap)
        PostChallengePage.TREASURE -> TreasureRevealPanel(session.relic, session::viewStory, onReturnToMap)
        PostChallengePage.STORY -> TreasureStoryPanel(
            relic = session.relic,
            collecting = false,
            collectionError = null,
            onPutInBackpack = null,
            onBackToReveal = session::backToTreasure,
            onReturnToMap = onReturnToMap,
        )
    }
}

@Composable
private fun UnionPhotoHistoryReveal(session: PostChallengeRevealSession, onReturnToMap: () -> Unit) {
    val photoUri = session.capturedPhotoUri ?: return
    val relic = session.relic
    LaunchedEffect(session) {
        if (session.hasHistoricalImage && !session.historicalShown) {
            delay(1_200L)
            session.showHistorical()
        }
    }
    Column(
        Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState()).padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("Relic discovered", style = MaterialTheme.typography.headlineMedium)
        Text(relic.name, style = MaterialTheme.typography.titleLarge)
        Card(Modifier.fillMaxWidth()) {
            Crossfade(targetState = session.historicalShown, label = "union_lawn_history_reveal") { historical ->
                Box(Modifier.fillMaxWidth().aspectRatio(4f / 3f), contentAlignment = Alignment.Center) {
                    when {
                        historical && relic.historicalImageUrl.startsWith("https://") -> RemoteTreasureImage(
                            imageUrl = relic.historicalImageUrl,
                            contentDescription = "Historical reference for ${relic.name}",
                            modifier = Modifier.fillMaxSize(),
                        )
                        historical && relic.historicalImageResId != null -> Image(
                            painter = painterResource(relic.historicalImageResId),
                            contentDescription = "Historical reference for ${relic.name}",
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.fillMaxSize(),
                        )
                        else -> AndroidView(
                            factory = { context -> ImageView(context).apply {
                                scaleType = ImageView.ScaleType.CENTER_CROP
                                setImageURI(Uri.parse(photoUri))
                            } },
                            update = { it.setImageURI(Uri.parse(photoUri)) },
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                }
            }
        }
        Text(if (session.hasHistoricalImage) {
            if (session.historicalShown) "Historical reference revealed" else "Your photograph is becoming a window into the past…"
        } else "Historical image pending; your photograph is saved on this device.")
        Button(onClick = session::continueToTreasure, modifier = Modifier.fillMaxWidth(),
            enabled = !session.hasHistoricalImage || session.historicalShown) {
            Text("Continue to treasure")
        }
        OutlinedButton(onClick = onReturnToMap, modifier = Modifier.fillMaxWidth()) {
            Text("Return to map")
        }
    }
}

@Composable
private fun TreasureRevealPanel(relic: MapRelic, onViewStory: () -> Unit, onReturnToMap: () -> Unit) {
    Column(
        Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState()).padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text("Relic discovered", style = MaterialTheme.typography.headlineMedium)
        TreasurePrototypeImage(relic = relic, discovered = true, modifier = Modifier.size(240.dp))
        Text(relic.name, style = MaterialTheme.typography.headlineSmall)
        Text(relic.locationName, style = MaterialTheme.typography.titleMedium)
        if (relic.treasureType.isNotBlank()) Text(relic.treasureTypeLabel)
        Text(relic.description, style = MaterialTheme.typography.bodyLarge)
        Text("Saved to your collection", color = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(4.dp))
        Button(onClick = onViewStory, modifier = Modifier.fillMaxWidth()) {
            Text("View story / historical reference")
        }
        OutlinedButton(onClick = onReturnToMap, modifier = Modifier.fillMaxWidth()) {
            Text("Return to map")
        }
    }
}
