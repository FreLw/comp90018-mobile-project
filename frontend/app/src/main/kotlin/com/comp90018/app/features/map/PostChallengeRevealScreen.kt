package com.comp90018.app.features.map

/*
 * Renders the post-challenge treasure, optional photo-history, and story views.
 * Uses a retained reveal session so save status and the captured-photo/artwork handoff stay consistent.
 */

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
import com.comp90018.app.features.treasurechallenge.DiscoverySaveStatus
import kotlinx.coroutines.delay

/** Routes the retained reveal session to treasure, photo-history, or story presentation. */
@Composable
fun PostChallengeRevealScreen(session: PostChallengeRevealSession, onReturnToMap: () -> Unit) {
    when (session.page) {
        PostChallengePage.PHOTO_HISTORY -> UnionPhotoHistoryReveal(session, onReturnToMap)
        PostChallengePage.TREASURE -> TreasureRevealPanel(session, onReturnToMap)
        PostChallengePage.STORY -> TreasureStoryPanel(
            relic = session.relic,
            collecting = session.discoverySave?.status == DiscoverySaveStatus.SAVING,
            collectionError = if (session.discoverySave?.status == DiscoverySaveStatus.FAILED)
                "Please retry saving this discovery to your collection." else null,
            onPutInBackpack = session.onRetrySave.takeIf { session.discoverySave?.status != DiscoverySaveStatus.SAVED },
            onBackToReveal = session::backToTreasure,
            onReturnToMap = onReturnToMap,
        )
    }
}

/** Displays the captured photograph alongside the archive scene before continuing to the treasure. */
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
                                colorFilter = com.comp90018.app.features.treasurechallenge.questPhotoColorFilter()
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

/** Binds discovery actions and collection retry state to the retained post-challenge session. */
@Composable
private fun TreasureRevealPanel(session: PostChallengeRevealSession, onReturnToMap: () -> Unit) {
    TreasureDiscoveryReveal(session.relic, session::viewStory, onReturnToMap,
        discoverySave = session.discoverySave, onRetrySave = session.onRetrySave, photoArrival = session.photoArrival)
}
