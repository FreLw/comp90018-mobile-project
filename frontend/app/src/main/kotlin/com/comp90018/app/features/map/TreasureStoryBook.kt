package com.comp90018.app.features.map

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.comp90018.app.GothicTreasureFontFamily
import com.comp90018.app.features.treasure.HistoricalTreasureImage
import com.comp90018.app.features.treasure.TreasurePrototypeImage
import com.comp90018.app.features.treasure.historicalArtworkResource
import kotlinx.coroutines.launch
import kotlin.math.abs

private val BookInk = Color(0xFF35291F)
private val BookGold = Color(0xFF946C32)
private val BookPaper = Color(0xFFF3E5C6)
private data class Folio(val title: String, val body: String = "", val cover: Boolean = false, val archive: Boolean = false)

/** Keeps every paragraph; long leaves also scroll for large font settings and small screens. */
private fun bookLeaves(relic: MapRelic): List<Folio> = buildList {
    add(Folio("The discovery", cover = true))
    fun chapter(title: String, text: String) {
        if (text.isBlank()) return
        // Split on word boundaries, preserving the complete text and existing paragraph breaks.
        var remaining = text.trim()
        var part = 0
        while (remaining.isNotEmpty()) {
            val end = if (remaining.length <= 950) remaining.length else
                remaining.lastIndexOf(' ', 950).takeIf { it > 0 } ?: remaining.length
            add(Folio(if (part++ == 0) title else "$title · continued", remaining.substring(0, end)))
            remaining = remaining.substring(end).trimStart()
        }
    }
    chapter("The specimen", relic.description)
    chapter("The story", relic.story)
    chapter("The landmark", relic.buildingStory)
    if (relic.historicalImageUrl.isNotBlank() || historicalArtworkResource(relic) != null) {
        add(Folio("From the archives", archive = true))
    }
    val references = listOfNotNull(
        relic.historicalImageCredit.takeIf(String::isNotBlank)?.let { "Image credit\n$it" },
        relic.sourceTitle.takeIf(String::isNotBlank)?.let { "Historical source\n$it" },
        relic.sourceUrl.takeIf(String::isNotBlank),
    ).joinToString("\n\n")
    chapter("Notes & provenance", references)
}

@Composable
internal fun TreasureStoryPanel(
    relic: MapRelic,
    collecting: Boolean,
    collectionError: String?,
    onPutInBackpack: (() -> Unit)?,
    onBackToReveal: (() -> Unit)? = null,
    onReturnToMap: (() -> Unit)? = null,
    headerArtworkVisible: Boolean = true,
    onHeaderArtworkBounds: (Rect) -> Unit = {},
) {
    val context = LocalContext.current
    val leaves = remember(relic) { bookLeaves(relic) }
    val pager = rememberPagerState(pageCount = { leaves.size })
    val scope = rememberCoroutineScope()
    Column(Modifier.fillMaxSize().background(Brush.verticalGradient(
        listOf(Color(0xFF30271F), Color(0xFF55412E), Color(0xFF30271F)))),
        horizontalAlignment = Alignment.CenterHorizontally) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            if (onBackToReveal != null) TextButton(onClick = onBackToReveal) {
                Text("‹ Discovery", color = BookPaper, fontFamily = FontFamily.Serif)
            }
            Text("FIELD CHRONICLE", color = Color(0xFFE0BF80), fontFamily = FontFamily.Serif,
                fontSize = 12.sp, letterSpacing = 2.sp, textAlign = TextAlign.Center, modifier = Modifier.weight(1f))
            if (onReturnToMap != null) TextButton(onClick = onReturnToMap) {
                Text("Map ›", color = BookPaper, fontFamily = FontFamily.Serif)
            }
        }
        Box(Modifier.weight(1f).fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp)) {
            // The edges of the next leaves remain visible beneath the moving page.
            Box(Modifier.fillMaxSize().padding(start = 5.dp, top = 5.dp).background(Color(0xFFAD9774)))
            Box(Modifier.fillMaxSize().padding(start = 2.dp, top = 2.dp, end = 3.dp, bottom = 3.dp).background(Color(0xFFDDCBA7)))
            HorizontalPager(state = pager, modifier = Modifier.fillMaxSize().padding(end = 6.dp, bottom = 6.dp),
                beyondViewportPageCount = 1, key = { "${relic.id}_$it" }) { index ->
                val offset = (pager.currentPage - index) + pager.currentPageOffsetFraction
                Box(Modifier.fillMaxSize().graphicsLayer {
                    transformOrigin = TransformOrigin(if (offset >= 0) 0f else 1f, .5f)
                    rotationY = -offset.coerceIn(-1f, 1f) * 72f
                    cameraDistance = 18 * density
                    shadowElevation = abs(offset) * 16.dp.toPx()
                }) {
                    Parchment(Modifier.fillMaxSize())
                    val leaf = leaves[index]
                    Column(Modifier.fillMaxSize().padding(start = 28.dp, end = 22.dp, top = 20.dp, bottom = 12.dp)) {
                        Text("✦  ${relic.locationName}  ✦", color = BookGold, fontFamily = FontFamily.Serif,
                            fontSize = 12.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                        HorizontalDivider(Modifier.padding(vertical = 12.dp), color = BookGold.copy(alpha = .4f))
                        Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()),
                            horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            Text(if (leaf.cover) relic.name else leaf.title, fontFamily = GothicTreasureFontFamily,
                                color = BookInk, fontSize = 29.sp, lineHeight = 35.sp, textAlign = TextAlign.Center)
                            if (leaf.cover) {
                                Box(Modifier.fillMaxWidth().height(205.dp), contentAlignment = Alignment.Center) {
                                    TreasurePrototypeImage(relic, true, Modifier.fillMaxSize()
                                        .onGloballyPositioned { if (index == 0) onHeaderArtworkBounds(it.boundsInRoot()) }
                                        .graphicsLayer { alpha = if (headerArtworkVisible) 1f else 0f })
                                }
                                BookBody("Found at ${relic.locationName}", centered = true)
                                if (relic.treasureTypeLabel.isNotBlank()) BookBody(relic.treasureTypeLabel, centered = true)
                                BookBody("A recovered wonder, preserved between these leaves. Turn the page to read its history.", centered = true)
                            }
                            if (leaf.body.isNotBlank()) BookBody(leaf.body)
                            if (leaf.archive) {
                                HistoricalTreasureImage(relic, Modifier.fillMaxWidth().height(260.dp))
                                if (relic.historicalImageCredit.isNotBlank()) BookBody(relic.historicalImageCredit)
                            }
                            if (index == leaves.lastIndex && relic.sourceUrl.startsWith("https://")) {
                                TextButton(onClick = { runCatching {
                                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(relic.sourceUrl)))
                                } }) { Text("Consult the historical source ↗", color = BookGold, fontFamily = FontFamily.Serif) }
                            }
                        }
                        HorizontalDivider(Modifier.padding(top = 12.dp, bottom = 8.dp), color = BookGold.copy(alpha = .4f))
                        Text("—  ${index + 1}  —", fontFamily = FontFamily.Serif, color = BookGold,
                            fontSize = 13.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                    }
                    // A moving shadow at the spine makes the turning leaf read as paper.
                    if (abs(offset) > .01f) Box(Modifier.fillMaxSize().background(Brush.horizontalGradient(
                        listOf(Color.Black.copy(alpha = abs(offset).coerceAtMost(.35f)), Color.Transparent))))
                }
            }
        }
        Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            TextButton(enabled = pager.currentPage > 0 && !pager.isScrollInProgress,
                onClick = { scope.launch { pager.animateScrollToPage(pager.currentPage - 1) } }) {
                Text("‹ Previous", fontFamily = FontFamily.Serif, color = if (pager.currentPage > 0) BookPaper else BookPaper.copy(alpha = .35f))
            }
            Text("${pager.currentPage + 1} / ${leaves.size}", fontFamily = FontFamily.Serif, color = BookPaper,
                textAlign = TextAlign.Center, modifier = Modifier.weight(1f))
            TextButton(enabled = pager.currentPage < leaves.lastIndex && !pager.isScrollInProgress,
                onClick = { scope.launch { pager.animateScrollToPage(pager.currentPage + 1) } }) {
                Text("Next ›", fontFamily = FontFamily.Serif, color = if (pager.currentPage < leaves.lastIndex) BookPaper else BookPaper.copy(alpha = .35f))
            }
        }
        if (onPutInBackpack != null) {
            Button(onClick = onPutInBackpack, enabled = !collecting,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 22.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF795637), contentColor = BookPaper)) {
                if (collecting) CircularProgressIndicator(Modifier.size(18.dp), color = BookPaper, strokeWidth = 2.dp)
                Text(if (collecting) "  Saving…" else "Preserve in backpack", fontFamily = FontFamily.Serif)
            }
        } else Text("Inscribed in your collection", color = Color(0xFFE0BF80), fontFamily = FontFamily.Serif, fontSize = 12.sp)
        collectionError?.let { Text(it, color = Color(0xFFFFC8B7), modifier = Modifier.padding(12.dp), fontFamily = FontFamily.Serif) }
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun BookBody(text: String, centered: Boolean = false) {
    Text(text, color = BookInk, fontFamily = FontFamily.Serif, fontWeight = FontWeight.Normal,
        fontSize = 18.sp, lineHeight = 29.sp, textAlign = if (centered) TextAlign.Center else TextAlign.Start,
        modifier = Modifier.fillMaxWidth())
}

@Composable
internal fun Parchment(modifier: Modifier = Modifier) {
    Canvas(modifier) {
        drawRect(Brush.horizontalGradient(listOf(Color(0xFFC9AF81), BookPaper, Color(0xFFF9EED6), Color(0xFFE6D2AA))))
        repeat(100) { i ->
            val y = size.height * i / 100
            drawLine(Color(0xFF987647).copy(alpha = .035f), Offset(0f, y), Offset(size.width, y + (i % 3 - 1)), 1f)
        }
        drawLine(BookGold.copy(alpha = .25f), Offset(12.dp.toPx(), 0f), Offset(12.dp.toPx(), size.height), 1.dp.toPx())
    }
}

/** A full-size leaf folds across the specimen as it settles into the chronicle. */
@Composable
internal fun BookInsertionLeaf(progress: Float) {
    val fold = ((progress - .55f) / .45f).coerceIn(0f, 1f)
    if (fold <= 0f || fold >= 1f) return
    Box(Modifier.fillMaxSize().padding(horizontal = 18.dp, vertical = 52.dp).graphicsLayer {
        transformOrigin = TransformOrigin(0f, .5f)
        rotationY = -180f * fold
        cameraDistance = 18 * density
        alpha = kotlin.math.sin(fold * Math.PI).toFloat().coerceIn(0f, 1f)
        shadowElevation = 18.dp.toPx()
    }) {
        Parchment(Modifier.fillMaxSize())
        Column(Modifier.align(Alignment.Center).padding(30.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("✦", color = BookGold, fontSize = 42.sp)
            Text("A wonder preserved", fontFamily = GothicTreasureFontFamily, fontSize = 32.sp,
                color = BookInk, textAlign = TextAlign.Center)
            Text("In the leaves of the field chronicle", fontFamily = FontFamily.Serif,
                color = BookInk, fontSize = 18.sp, textAlign = TextAlign.Center)
        }
    }
}
