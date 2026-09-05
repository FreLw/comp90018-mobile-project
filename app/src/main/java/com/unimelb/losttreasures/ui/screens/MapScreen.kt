package com.unimelb.losttreasures.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.unimelb.losttreasures.ui.components.RelicBadge
import com.unimelb.losttreasures.ui.data.sampleRelics
import com.unimelb.losttreasures.ui.model.Relic
import com.unimelb.losttreasures.ui.model.RelicTone
import com.unimelb.losttreasures.ui.theme.Ink
import com.unimelb.losttreasures.ui.theme.RelicGold
import com.unimelb.losttreasures.ui.theme.RelicRed
import com.unimelb.losttreasures.ui.theme.toColor

@Composable
fun MapScreen(
    selectedRelic: Relic?,
    onSelectRelic: (Relic) -> Unit,
    onDismissRelic: () -> Unit,
    modifier: Modifier = Modifier
) {
    var searchingRelicId by rememberSaveable { mutableStateOf<String?>(null) }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFEAF3EE))
    ) {
        // Front-end mock map. Later this can be replaced by Google Maps or Mapbox.
        CampusMapCanvas()

        sampleRelics.forEach { relic ->
            TreasurePin(
                relic = relic,
                selected = relic.id == selectedRelic?.id,
                x = maxWidth * relic.mapX,
                y = maxHeight * relic.mapY,
                onClick = {
                    searchingRelicId = null
                    onSelectRelic(relic)
                }
            )
        }

        AnimatedVisibility(
            visible = selectedRelic != null,
            enter = slideInVertically(initialOffsetY = { it / 2 }) + fadeIn() + scaleIn(initialScale = 0.94f),
            exit = slideOutVertically(targetOffsetY = { it / 2 }) + fadeOut() + scaleOut(targetScale = 0.94f),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(horizontal = 16.dp, vertical = 18.dp)
        ) {
            selectedRelic?.let { relic ->
                RelicFloatingCard(
                    relic = relic,
                    isSearching = searchingRelicId == relic.id,
                    onStartSearch = {
                        if (relic.id == "south-lawn") {
                            searchingRelicId = relic.id
                        }
                    },
                    onDismiss = {
                        searchingRelicId = null
                        onDismissRelic()
                    }
                )
            }
        }
    }
}

@Composable
private fun CampusMapCanvas() {
    Canvas(modifier = Modifier.fillMaxSize()) {
        drawRect(
            brush = Brush.linearGradient(
                colors = listOf(Color(0xFFDDEFE4), Color(0xFFCFE0D7))
            )
        )

        drawRoundRect(
            color = Color(0xFFC0D7CB),
            topLeft = Offset(size.width * 0.08f, size.height * 0.07f),
            size = Size(size.width * 0.28f, size.height * 0.16f),
            cornerRadius = CornerRadius(14f, 14f)
        )
        drawRoundRect(
            color = Color(0xFFC0D7CB),
            topLeft = Offset(size.width * 0.6f, size.height * 0.12f),
            size = Size(size.width * 0.28f, size.height * 0.18f),
            cornerRadius = CornerRadius(14f, 14f)
        )
        drawRoundRect(
            color = Color(0xFFC0D7CB),
            topLeft = Offset(size.width * 0.15f, size.height * 0.62f),
            size = Size(size.width * 0.22f, size.height * 0.15f),
            cornerRadius = CornerRadius(14f, 14f)
        )
        drawRoundRect(
            color = Color(0xFFC0D7CB),
            topLeft = Offset(size.width * 0.62f, size.height * 0.64f),
            size = Size(size.width * 0.26f, size.height * 0.16f),
            cornerRadius = CornerRadius(14f, 14f)
        )

        drawLine(
            color = Color.White.copy(alpha = 0.92f),
            start = Offset(size.width * 0.08f, size.height * 0.26f),
            end = Offset(size.width * 0.93f, size.height * 0.67f),
            strokeWidth = 24f,
            cap = StrokeCap.Round
        )
        drawLine(
            color = Color.White.copy(alpha = 0.88f),
            start = Offset(size.width * 0.22f, size.height * 0.88f),
            end = Offset(size.width * 0.78f, size.height * 0.08f),
            strokeWidth = 18f,
            cap = StrokeCap.Round
        )
        drawLine(
            color = Color.White.copy(alpha = 0.72f),
            start = Offset(size.width * 0.04f, size.height * 0.5f),
            end = Offset(size.width * 0.86f, size.height * 0.41f),
            strokeWidth = 12f,
            cap = StrokeCap.Round
        )

        drawCircle(
            color = Color(0xFFAED0BC),
            radius = size.minDimension * 0.2f,
            center = Offset(size.width * 0.48f, size.height * 0.54f)
        )
        drawCircle(
            color = Color(0xFFDDEFE4),
            radius = size.minDimension * 0.11f,
            center = Offset(size.width * 0.48f, size.height * 0.54f)
        )
    }
}

@Composable
private fun TreasurePin(
    relic: Relic,
    selected: Boolean,
    x: Dp,
    y: Dp,
    onClick: () -> Unit
) {
    val color = relic.tone.toColor()
    val pinSize = if (selected) 52.dp else 42.dp
    val labelWidth = 124.dp

    Column(
        modifier = Modifier
            .offset(x = x - labelWidth / 2, y = y - pinSize - 32.dp)
            .width(labelWidth)
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = Color.White.copy(alpha = 0.92f),
            border = BorderStroke(1.dp, color.copy(alpha = 0.22f))
        ) {
            Text(
                text = relic.name,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = Ink,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Box(
            modifier = Modifier.size(pinSize),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Rounded.LocationOn,
                contentDescription = relic.name,
                modifier = Modifier.fillMaxSize(),
                tint = color
            )
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = if (selected) 10.dp else 8.dp)
                    .size(if (selected) 15.dp else 12.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.96f))
            )
        }
    }
}

@Composable
private fun RelicFloatingCard(
    relic: Relic,
    isSearching: Boolean,
    onStartSearch: () -> Unit,
    onDismiss: () -> Unit
) {
    val color = relic.tone.toColor()

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, color.copy(alpha = 0.3f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 10.dp)
    ) {
        Box {
            IconButton(
                onClick = onDismiss,
                modifier = Modifier.align(Alignment.TopEnd)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Close,
                    contentDescription = "Close",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (isSearching) {
                SearchingContent(
                    relic = relic,
                    color = color,
                    modifier = Modifier.padding(start = 18.dp, end = 18.dp, top = 18.dp, bottom = 20.dp)
                )
            } else {
                RelicInfoContent(
                    relic = relic,
                    color = color,
                    onStartSearch = onStartSearch,
                    modifier = Modifier.padding(start = 14.dp, end = 50.dp, top = 14.dp, bottom = 16.dp)
                )
            }
        }
    }
}

@Composable
private fun RelicInfoContent(
    relic: Relic,
    color: Color,
    onStartSearch: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (relic.id == "south-lawn") {
                IconButton(onClick = onStartSearch) {
                    Icon(
                        imageVector = Icons.Rounded.Search,
                        contentDescription = "Start search",
                        modifier = Modifier.size(30.dp),
                        tint = color
                    )
                }
            }
            Surface(
                modifier = Modifier.size(58.dp),
                shape = RoundedCornerShape(8.dp),
                color = color.copy(alpha = 0.12f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    RelicBadge(
                        color = color,
                        modifier = Modifier.size(38.dp)
                    )
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = relic.name,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Ink,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = relic.place,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TreasureTypeLabel(tone = relic.tone)
            Text(
                text = relic.distance,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Text(
            text = relic.story,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun SearchingContent(
    relic: Relic,
    color: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        AnimatedSearchingIcon(color = color)
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = "Searching",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = color
            )
            Text(
                text = "Please align a little more",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Text(
                text = "Please face the treasure",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
        Surface(
            modifier = Modifier.size(92.dp),
            shape = RoundedCornerShape(8.dp),
            color = color.copy(alpha = 0.12f)
        ) {
            Box(contentAlignment = Alignment.Center) {
                RelicBadge(
                    color = color,
                    modifier = Modifier.size(58.dp)
                )
            }
        }
        Text(
            text = relic.name,
            modifier = Modifier.fillMaxWidth(),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = Ink,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun AnimatedSearchingIcon(color: Color) {
    val transition = rememberInfiniteTransition(label = "searching")
    val scale by transition.animateFloat(
        initialValue = 0.9f,
        targetValue = 1.12f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 820),
            repeatMode = RepeatMode.Reverse
        ),
        label = "search-scale"
    )
    val rotation by transition.animateFloat(
        initialValue = -8f,
        targetValue = 8f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 640),
            repeatMode = RepeatMode.Reverse
        ),
        label = "search-rotation"
    )

    Surface(
        modifier = Modifier
            .size(86.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                rotationZ = rotation
            },
        shape = CircleShape,
        color = color.copy(alpha = 0.12f),
        border = BorderStroke(1.dp, color.copy(alpha = 0.24f))
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = Icons.Rounded.Search,
                contentDescription = null,
                modifier = Modifier.size(46.dp),
                tint = color
            )
        }
    }
}

@Composable
private fun TreasureTypeLabel(tone: RelicTone) {
    val text = if (tone == RelicTone.Gold) "Fragment" else "Complete"
    val color = if (tone == RelicTone.Gold) RelicGold else RelicRed

    Surface(
        shape = RoundedCornerShape(8.dp),
        color = color.copy(alpha = 0.1f),
        border = BorderStroke(1.dp, color.copy(alpha = 0.28f))
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            style = MaterialTheme.typography.labelMedium,
            color = color,
            fontWeight = FontWeight.Bold,
            maxLines = 1
        )
    }
}
