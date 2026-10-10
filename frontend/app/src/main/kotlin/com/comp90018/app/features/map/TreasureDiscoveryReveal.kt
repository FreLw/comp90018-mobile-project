package com.comp90018.app.features.map

/*
 * Renders the discovery artwork, completion heading, collection status, and story action.
 * Photo-task arrival reuses the measured logo rectangle; story opening animates the artwork into the book.
 */

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.IntOffset
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.comp90018.app.GothicTreasureFontFamily
import com.comp90018.app.features.treasure.TreasurePrototypeImage
import com.comp90018.app.features.treasurechallenge.ChallengeDiscoverySave
import com.comp90018.app.features.treasurechallenge.DiscoverySaveStatus
import com.comp90018.app.features.treasurechallenge.PhotoRevealArrival
import com.comp90018.app.features.treasurechallenge.QuestPhotoRevealPaper
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.roundToInt

/** Reveals a completed task's discovery and transfers its artwork into the story header. */
@Composable
internal fun TreasureDiscoveryReveal(
    relic: MapRelic, onViewTreasure: () -> Unit, onReturnToMap: () -> Unit,
    discoverySave: ChallengeDiscoverySave? = null, onRetrySave: (() -> Unit)? = null,
    photoArrival: PhotoRevealArrival? = null,
    arrivalProgress: Float = 1f,
    drawBackground: Boolean = true,
    saveStatus: DiscoverySaveStatus? = discoverySave?.status,
) {
    var movingToStory by remember(relic.id) { mutableStateOf(false) }
    var sourceBounds by remember(relic.id) { mutableStateOf<Rect?>(null) }
    var targetBounds by remember(relic.id) { mutableStateOf<Rect?>(null) }
    var rootBounds by remember { mutableStateOf(Rect.Zero) }
    val transfer = remember(relic.id) { Animatable(0f) }
    val density = LocalDensity.current
    LaunchedEffect(movingToStory, targetBounds) {
        if (movingToStory && targetBounds != null) {
            transfer.animateTo(1f, tween(1600, easing = FastOutSlowInEasing))
            onViewTreasure()
        }
    }
    val entrance = remember(relic.id, photoArrival != null) { Animatable(if (photoArrival != null) 1f else 0f) }
    LaunchedEffect(relic.id, photoArrival) {
        if (photoArrival != null) entrance.snapTo(1f) else entrance.animateTo(1f, tween(1000))
    }
    val copyAlpha = if (photoArrival == null) 1f else ((arrivalProgress - .48f) / .52f).coerceIn(0f, 1f)
    val copyModifier = Modifier.graphicsLayer { alpha = copyAlpha }
    val transition = rememberInfiniteTransition(label = "relic_radiance")
    val glow by transition.animateFloat(0.4f, 0.85f, infiniteRepeatable(tween(1700), RepeatMode.Reverse), label = "relic_glow")
    val orbit by transition.animateFloat(0f, 360f, infiniteRepeatable(tween(28000, easing = LinearEasing)), label = "relic_sparks")
    val radianceGlow = if (photoArrival == null) glow else .6f
    val radianceOrbit = if (photoArrival == null) orbit else 0f
    val gold = Color(0xFFE8BC65)
    val ink = Color(0xFF382B40)
    BoxWithConstraints(Modifier.fillMaxSize().testTag("treasure_discovery_page").semantics {
        stateDescription = if (arrivalProgress < 1f) "Transitioning to discovery" else "Discovery revealed"
    }.onGloballyPositioned { rootBounds = it.boundsInRoot() }) {
        val pageWidth = constraints.maxWidth.toFloat()
        val verticalOffset = photoArrival?.let {
            minOf(0f, it.artworkBounds.top - with(density) { 104.dp.toPx() })
        } ?: 0f
        if (movingToStory) {
            TreasureStoryPanel(relic, false, null, null,
                headerArtworkVisible = transfer.value >= .7f,
                onHeaderArtworkBounds = { if (targetBounds == null) targetBounds = it })
        } else Column(
            Modifier.fillMaxSize().background(if (!drawBackground) Color.Transparent
                else if (photoArrival != null) QuestPhotoRevealPaper else Color(0xFFF5ECD9))
                .graphicsLayer { translationY = verticalOffset }
                .verticalScroll(rememberScrollState(), enabled = arrivalProgress >= 1f).padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            val photoBounds = photoArrival?.artworkBounds
            val photoWidth = with(density) { (photoBounds?.width ?: 0f).toDp() }
            val photoHeight = with(density) { (photoBounds?.height ?: 0f).toDp() }
            // Heading height plus the column's two gaps and padding leaves the logo exactly where it was.
            val topSpace = if (photoBounds == null) 20.dp else
                (with(density) { photoBounds.top.toDp() } - 104.dp).coerceAtLeast(0.dp)
            Spacer(Modifier.height(topSpace))
            Text("✦ A HIDDEN WONDER AWAKENS ✦", color = ink, fontFamily = GothicTreasureFontFamily,
                fontSize = 20.sp, textAlign = TextAlign.Center,
                modifier = if (photoArrival == null) copyModifier else
                    copyModifier.fillMaxWidth().height(48.dp).testTag("photo_reveal_heading"))
            Box(Modifier.fillMaxWidth().height(if (photoBounds == null) 340.dp else photoHeight), contentAlignment = Alignment.Center) {
                Canvas(Modifier.fillMaxSize().graphicsLayer { alpha = copyAlpha }) {
                    val c = Offset(size.width / 2f, size.height / 2f)
                    val r = size.minDimension * 0.43f
                    drawCircle(Brush.radialGradient(listOf(gold.copy(alpha = radianceGlow), gold.copy(alpha = radianceGlow * 0.25f), Color.Transparent), c, r), r, c)
                    drawCircle(gold.copy(alpha = (1f - entrance.value) * 0.8f), r * (0.3f + entrance.value * 0.8f), c, style = Stroke(3.dp.toPx()))
                    repeat(18) { i ->
                        val angle = Math.toRadians(i * 20.0 + radianceOrbit)
                        val radius = r * (0.72f + (i % 3) * 0.1f)
                        val p = c + Offset(cos(angle).toFloat(), sin(angle).toFloat()) * radius
                        val extent = (if (i % 3 == 0) 5f else 3f).dp.toPx() * entrance.value
                        val color = gold.copy(alpha = radianceGlow)
                        drawLine(color, p - Offset(extent, 0f), p + Offset(extent, 0f), 2.dp.toPx())
                        drawLine(color, p - Offset(0f, extent), p + Offset(0f, extent), 2.dp.toPx())
                    }
                }
                if (photoArrival != null) {
                    // Reuse the same bitmap without a new entrance, scale, rotation, or remote-image replacement.
                    Image(painterResource(photoArrival.artworkResId), relic.name,
                        Modifier.offset(x = with(density) {
                            (photoArrival.artworkBounds.left - ((pageWidth - photoArrival.artworkBounds.width) / 2f).roundToInt()).toDp()
                        })
                            .size(photoWidth, photoHeight).testTag("photo_reveal_logo")
                            .onGloballyPositioned { sourceBounds = it.boundsInRoot() }, contentScale = ContentScale.Fit)
                } else TreasurePrototypeImage(relic, discovered = true,
                    modifier = Modifier.fillMaxWidth(0.82f).height(290.dp).onGloballyPositioned { sourceBounds = it.boundsInRoot() }.graphicsLayer {
                        alpha = entrance.value
                        scaleX = 0.65f + entrance.value * 0.35f
                        scaleY = scaleX
                        translationY = (1f - entrance.value) * 36.dp.toPx()
                    })
            }
            Column(copyModifier.fillMaxWidth().testTag("photo_reveal_copy"),
                horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(relic.name, color = ink, fontFamily = GothicTreasureFontFamily, fontSize = 34.sp, lineHeight = 39.sp, textAlign = TextAlign.Center)
                Text("The trail has revealed its secret. Discover the story within.", color = Color(0xFF786B60), textAlign = TextAlign.Center, fontSize = 14.sp)
                when (saveStatus) {
                    DiscoverySaveStatus.SAVING -> Text("Saving to your collection…", color = ink, fontSize = 12.sp)
                    DiscoverySaveStatus.FAILED -> {
                        Text("Your discovery is ready. Please retry saving it to your collection.",
                            color = ink, fontSize = 12.sp, textAlign = TextAlign.Center)
                        if (onRetrySave != null) OutlinedButton(onClick = onRetrySave, enabled = arrivalProgress >= 1f) { Text("Retry save") }
                    }
                    else -> Unit
                }
                Button(onClick = { if (sourceBounds != null) movingToStory = true }, enabled = arrivalProgress >= 1f,
                    modifier = Modifier.fillMaxWidth().height(56.dp), shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = ink, disabledContainerColor = ink)) {
                    Text("View treasure", fontFamily = GothicTreasureFontFamily, color = Color(0xFFFFE1A0), fontSize = 22.sp)
                }
                TextButton(onClick = onReturnToMap, enabled = arrivalProgress >= 1f) { Text("Return to map", color = ink) }
            }
        }
        if (movingToStory) {
            val source = sourceBounds
            val target = targetBounds ?: source
            if (source != null && target != null) {
                val fraction = (transfer.value / .7f).coerceIn(0f, 1f)
                val left = source.left + (target.left - source.left) * fraction - rootBounds.left
                val top = source.top + (target.top - source.top) * fraction - rootBounds.top
                val width = source.width + (target.width - source.width) * fraction
                val height = source.height + (target.height - source.height) * fraction
                TreasurePrototypeImage(relic, true, Modifier
                    .offset { IntOffset(left.toInt(), top.toInt()) }
                    .size(with(density) { width.toDp() }, with(density) { height.toDp() })
                    .graphicsLayer {
                        rotationZ = sin(fraction * Math.PI).toFloat() * -9f
                        alpha = if (transfer.value >= .7f) 0f else 1f
                    })
            }
            BookInsertionLeaf(transfer.value)
        }
    }

}
