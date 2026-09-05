package com.unimelb.losttreasures.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.unimelb.losttreasures.ui.components.RelicBadge
import com.unimelb.losttreasures.ui.components.SectionHeader
import com.unimelb.losttreasures.ui.data.discoveredRelics
import com.unimelb.losttreasures.ui.model.Relic
import com.unimelb.losttreasures.ui.model.RelicTone
import com.unimelb.losttreasures.ui.theme.Ink
import com.unimelb.losttreasures.ui.theme.toColor

@Composable
fun CollectionScreen(modifier: Modifier = Modifier) {
    var selectedRelicId by rememberSaveable { mutableStateOf<String?>(null) }
    val selectedRelic = discoveredRelics.firstOrNull { it.id == selectedRelicId }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        SectionHeader(
            title = "Treasure collection",
            detail = "2 x 2"
        )
        discoveredRelics.chunked(2).take(2).forEach { rowRelics ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                rowRelics.forEach { relic ->
                    CollectionTile(
                        relic = relic,
                        unlocked = relic.progress > 0f,
                        onClick = { selectedRelicId = relic.id },
                        modifier = Modifier.weight(1f)
                    )
                }
                if (rowRelics.size == 1) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }

    selectedRelic?.let { relic ->
        CollectionDetailDialog(
            relic = relic,
            unlocked = relic.progress > 0f,
            onDismiss = { selectedRelicId = null }
        )
    }
}

@Composable
private fun CollectionTile(
    relic: Relic,
    unlocked: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val typeColor = relic.tone.toCollectionColor()
    val contentColor = if (unlocked) typeColor else Color(0xFF7A817E)

    Card(
        modifier = modifier
            .aspectRatio(0.82f)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, typeColor.copy(alpha = if (unlocked) 0.36f else 0.18f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                contentColor.copy(alpha = if (unlocked) 0.95f else 0.32f),
                                contentColor.copy(alpha = if (unlocked) 0.32f else 0.14f)
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                RelicBadge(
                    color = if (unlocked) Color.White else Color(0xFFD9DDDB),
                    modifier = Modifier.size(50.dp)
                )
            }
            Text(
                text = relic.name,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = if (unlocked) "已找到" else "未知",
                style = MaterialTheme.typography.labelMedium,
                color = contentColor,
                maxLines = 1
            )
            TypeLabel(relic = relic)
        }
    }
}

@Composable
private fun CollectionDetailDialog(
    relic: Relic,
    unlocked: Boolean,
    onDismiss: () -> Unit
) {
    val typeColor = relic.tone.toCollectionColor()

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(8.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.dp, typeColor.copy(alpha = 0.28f))
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
                Column(
                    modifier = Modifier.padding(start = 18.dp, end = 18.dp, top = 22.dp, bottom = 18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(typeColor.copy(alpha = 0.1f))
                            .padding(vertical = 22.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        RelicBadge(
                            color = typeColor,
                            modifier = Modifier.size(68.dp)
                        )
                    }
                    Text(
                        text = relic.name,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Ink,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    DetailLine(label = "Location", value = relic.place)
                    DetailLine(label = "Status", value = if (unlocked) "已找到" else "未知")
                    DetailLine(label = "Collection type", value = relic.collectionTypeText())
                    Text(
                        text = relic.story,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun DetailLine(
    label: String,
    value: String
) {
    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.SemiBold,
            color = Ink
        )
    }
}

@Composable
private fun TypeLabel(relic: Relic) {
    val typeColor = relic.tone.toCollectionColor()

    Surface(
        shape = RoundedCornerShape(8.dp),
        color = typeColor.copy(alpha = 0.1f),
        border = BorderStroke(1.dp, typeColor.copy(alpha = 0.24f))
    ) {
        Text(
            text = relic.collectionTypeText(),
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall,
            color = typeColor,
            fontWeight = FontWeight.Bold,
            maxLines = 1
        )
    }
}

private fun Relic.collectionTypeText(): String {
    return if (tone == RelicTone.Gold) "Fragment" else "Complete"
}

private fun RelicTone.toCollectionColor(): Color {
    return when (this) {
        RelicTone.Gold -> toColor()
        RelicTone.Red -> toColor()
        else -> toColor()
    }
}
