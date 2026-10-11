package com.comp90018.app.features.map.compass

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Animation values and the guarded transition into the physical task. */
internal class CompassGateTransition(
    val compassEntrance: Animatable<Float, AnimationVector1D>,
    val lampEntrance: Animatable<Float, AnimationVector1D>,
    val leaving: Boolean,
    val onContinue: () -> Unit,
)

/** Keep the original relic-scoped animation state and callback timing. */
@Composable
internal fun rememberCompassGateTransition(
    relicId: String,
    allReady: Boolean,
    onContinue: () -> Unit,
): CompassGateTransition {
    val compassEntrance = remember(relicId) { Animatable(0f) }
    val lampEntrance = remember(relicId) { Animatable(0f) }
    val scope = rememberCoroutineScope()
    var leaving by remember(relicId) { mutableStateOf(false) }
    LaunchedEffect(relicId) {
        launch { compassEntrance.animateTo(1f, tween(1050, easing = FastOutSlowInEasing)) }
        delay(380)
        lampEntrance.animateTo(1f, tween(950, easing = FastOutSlowInEasing))
    }
    // Distance and heading gate entry only. The physical task remains responsible for completion and saving.
    fun continueToChallenge() {
        if (!allReady || leaving) return
        leaving = true
        scope.launch {
            launch { lampEntrance.animateTo(0f, tween(420)) }
            compassEntrance.animateTo(0f, tween(700, easing = FastOutSlowInEasing))
            onContinue()
        }
    }
    return CompassGateTransition(compassEntrance, lampEntrance, leaving, ::continueToChallenge)
}

@Composable
internal fun rememberCompassSignalLock(relicId: String, allReady: Boolean): Boolean {
    var signalLocked by remember(relicId) { mutableStateOf(false) }
    LaunchedEffect(allReady, signalLocked) {
        if (!allReady) signalLocked = false
        if (allReady && !signalLocked) {
            signalLocked = true
        }
    }
    return signalLocked
}

/** Owns tone allocation, newly lit star feedback, and resource cleanup. */
@Composable
internal fun CompassStarSoundEffects(relicId: String, stars: Int, soundEffectsEnabled: Boolean) {
    val tone = remember { runCatching { android.media.ToneGenerator(android.media.AudioManager.STREAM_MUSIC, 55) }.getOrNull() }
    DisposableEffect(tone) { onDispose { tone?.release() } }
    var previousStars by remember(relicId) { mutableIntStateOf(stars) }
    LaunchedEffect(stars, soundEffectsEnabled) {
        val newlyLit = stars - previousStars
        previousStars = stars
        if (soundEffectsEnabled && newlyLit > 0) {
            repeat(newlyLit) {
                tone?.startTone(android.media.ToneGenerator.TONE_PROP_BEEP, 85)
                delay(110)
            }
        }
    }
}
