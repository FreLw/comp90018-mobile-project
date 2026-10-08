package com.comp90018.app.features.treasurechallenge

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.*
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.comp90018.app.GothicTreasureFontFamily
import com.comp90018.app.contextengine.challenge.RelicChallengeType

private val ScrollInk = Color(0xFF51452F)
private val ScrollGold = Color(0xFF97703F)

@Composable
internal fun QuestFieldGuide(state: TreasureChallengeUiState) {
    var open by remember(state.challengeId) { mutableStateOf(false) }
    val unfurl = remember(state.challengeId) { Animatable(0f) }
    val scrollIntoView = remember(state.challengeId) { BringIntoViewRequester() }
    LaunchedEffect(open) {
        unfurl.animateTo(if (open) 1f else 0f, tween(if (open) 650 else 320))
        if (open) scrollIntoView.bringIntoView()
    }
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        TextButton(onClick = { open = !open }, modifier = Modifier.testTag("how_to_play"),
            colors = ButtonDefaults.textButtonColors(contentColor = QuestGold)) {
            Canvas(Modifier.size(27.dp)) {
                val margin = size.width * .15f
                val top = size.height * (.28f - unfurl.value * .12f)
                val bottom = size.height * (.7f + unfurl.value * .13f)
                drawRoundRect(QuestGold.copy(alpha = .13f), Offset(margin, top),
                    Size(size.width - margin * 2f, bottom - top), CornerRadius(1.dp.toPx()))
                drawRoundRect(QuestGold, Offset(margin, top), Size(size.width - margin * 2f, bottom - top),
                    CornerRadius(1.dp.toPx()), style = Stroke(1.dp.toPx()))
                for (y in listOf(top, bottom)) {
                    drawRoundRect(QuestForest, Offset(margin - 2.dp.toPx(), y - 2.dp.toPx()),
                        Size(size.width - margin * 2f + 4.dp.toPx(), 4.dp.toPx()), CornerRadius(2.dp.toPx()))
                    drawRoundRect(QuestGold, Offset(margin - 2.dp.toPx(), y - 2.dp.toPx()),
                        Size(size.width - margin * 2f + 4.dp.toPx(), 4.dp.toPx()),
                        CornerRadius(2.dp.toPx()), style = Stroke(.9.dp.toPx()))
                }
                repeat(3) { i ->
                    val y = top + (bottom - top) * (.3f + i * .2f)
                    drawLine(QuestGold.copy(alpha = .7f), Offset(size.width * .33f, y),
                        Offset(size.width * .67f, y), .7.dp.toPx())
                }
            }
            Spacer(Modifier.width(8.dp))
            Text(if (open) "Roll up the guide" else "How to play",
                fontFamily = GothicTreasureFontFamily, fontSize = 18.sp)
        }
        // An in-page scroll: no Dialog window or modal scrim.
        AnimatedVisibility(open,
            enter = slideInVertically(tween(650, easing = FastOutSlowInEasing)) { it / 2 } +
                fadeIn(tween(650)) + expandVertically(tween(650), expandFrom = Alignment.Bottom),
            exit = slideOutVertically(tween(320)) { it / 3 } + fadeOut(tween(280)) +
                shrinkVertically(tween(320), shrinkTowards = Alignment.Bottom)) {
            Box(Modifier.fillMaxWidth().padding(vertical = 8.dp)
                .bringIntoViewRequester(scrollIntoView).testTag("field_guide_scroll")) {
                Canvas(Modifier.matchParentSize()) { drawQuestScroll() }
                Column(Modifier.fillMaxWidth().padding(horizontal = 38.dp, vertical = 40.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(13.dp)) {
                    Text(state.challengeType.scrollTitle(), color = ScrollInk, fontFamily = GothicTreasureFontFamily,
                        fontSize = 27.sp, lineHeight = 30.sp, textAlign = TextAlign.Center)
                    Canvas(Modifier.width(100.dp).height(14.dp)) {
                        drawCrossStar(center, 5.dp.toPx(), ScrollGold)
                        for (side in listOf(-1f, 1f)) {
                            val curve = Path().apply {
                                moveTo(center.x + side * 12.dp.toPx(), center.y)
                                cubicTo(center.x + side * 25.dp.toPx(), center.y - 6.dp.toPx(),
                                    center.x + side * 30.dp.toPx(), center.y + 6.dp.toPx(),
                                    center.x + side * 45.dp.toPx(), center.y)
                            }
                            drawPath(curve, ScrollGold, style = Stroke(.8.dp.toPx()))
                        }
                    }
                    Text(state.challengeType.taskInstructions(), color = ScrollInk,
                        fontFamily = FontFamily.Serif, fontSize = 15.sp, lineHeight = 23.sp,
                        textAlign = TextAlign.Center)
                    Text(if (state.challengeType == RelicChallengeType.SOUTH_LAWN_VIEWING_ANGLE ||
                        state.challengeType == RelicChallengeType.WILSON_HALL_OBSERVATION)
                        "Light all four stars to reveal the relic." else "Light every star to reveal the relic.", color = ScrollGold,
                        fontFamily = FontFamily.Serif, fontSize = 12.sp, textAlign = TextAlign.Center)
                    TextButton(onClick = { open = false }, modifier = Modifier.semantics {
                        contentDescription = "Close field guide"
                    }, colors = ButtonDefaults.textButtonColors(contentColor = ScrollInk)) {
                        Text("Roll up", fontFamily = GothicTreasureFontFamily, fontSize = 17.sp)
                    }
                }
            }
        }
    }
}

private fun DrawScope.drawQuestScroll() {
    val left = 12.dp.toPx()
    val right = size.width - left
    val top = 13.dp.toPx()
    val bottom = size.height - top
    val paper = Path().apply {
        moveTo(left, top); lineTo(right, top); lineTo(right, bottom)
        quadraticTo(size.width * .75f, bottom - 3.dp.toPx(), center.x, bottom)
        quadraticTo(size.width * .25f, bottom + 3.dp.toPx(), left, bottom); close()
    }
    drawPath(paper, Color(0xFFF1E1B9))
    drawPath(paper, ScrollGold.copy(alpha = .65f), style = Stroke(.8.dp.toPx()))
    drawRoundRect(ScrollGold.copy(alpha = .55f), Offset(left + 9.dp.toPx(), top + 14.dp.toPx()),
        Size(right - left - 18.dp.toPx(), bottom - top - 28.dp.toPx()),
        CornerRadius(12.dp.toPx()), style = Stroke(.65.dp.toPx()))
    // Flat rolled ends and mirrored, engraved scrollwork echo the living vines.
    for (y in listOf(top, bottom)) {
        drawRoundRect(Color(0xFFE5CD98), Offset(5.dp.toPx(), y - 6.dp.toPx()),
            Size(size.width - 10.dp.toPx(), 12.dp.toPx()), CornerRadius(6.dp.toPx()))
        drawRoundRect(ScrollGold, Offset(5.dp.toPx(), y - 6.dp.toPx()),
            Size(size.width - 10.dp.toPx(), 12.dp.toPx()), CornerRadius(6.dp.toPx()),
            style = Stroke(.8.dp.toPx()))
        drawLine(ScrollGold.copy(alpha = .35f), Offset(18.dp.toPx(), y + 2.dp.toPx()),
            Offset(size.width - 18.dp.toPx(), y + 2.dp.toPx()), .6.dp.toPx())
        for (x in listOf(11.dp.toPx(), size.width - 11.dp.toPx())) {
            drawOval(ScrollGold.copy(alpha = .65f), Offset(x - 3.dp.toPx(), y - 5.dp.toPx()),
                Size(6.dp.toPx(), 10.dp.toPx()), style = Stroke(.75.dp.toPx()))
        }
    }
    for (side in listOf(-1f, 1f)) {
        val x = if (side < 0) left + 15.dp.toPx() else right - 15.dp.toPx()
        val curl = Path().apply {
            moveTo(x, top + 28.dp.toPx())
            cubicTo(x - side * 15.dp.toPx(), top + 23.dp.toPx(),
                x - side * 15.dp.toPx(), top + 43.dp.toPx(), x, top + 48.dp.toPx())
            cubicTo(x + side * 5.dp.toPx(), top + 50.dp.toPx(),
                x + side * 6.dp.toPx(), top + 40.dp.toPx(), x, top + 40.dp.toPx())
            moveTo(x, bottom - 28.dp.toPx())
            cubicTo(x - side * 15.dp.toPx(), bottom - 23.dp.toPx(),
                x - side * 15.dp.toPx(), bottom - 43.dp.toPx(), x, bottom - 48.dp.toPx())
            cubicTo(x + side * 5.dp.toPx(), bottom - 50.dp.toPx(),
                x + side * 6.dp.toPx(), bottom - 40.dp.toPx(), x, bottom - 40.dp.toPx())
        }
        drawPath(curl, ScrollGold.copy(alpha = .65f), style = Stroke(.85.dp.toPx()))
    }
}

private fun RelicChallengeType.scrollTitle(): String = when (this) {
    RelicChallengeType.UNION_LAWN_PHOTO -> "The Lake Scroll"
    RelicChallengeType.WILSON_HALL_OBSERVATION -> "The Rosette Scroll"
    RelicChallengeType.OLD_QUAD_EXCAVATION -> "The Fern Scroll"
    RelicChallengeType.SOUTH_LAWN_VIEWING_ANGLE -> "The Atlas Scroll"
    RelicChallengeType.SYSTEM_GARDEN_GLASSHOUSE -> "The Glasshouse Scroll"
    RelicChallengeType.GRAINGER_MUSEUM_TONE_TOOL -> "The Melody Scroll"
}
