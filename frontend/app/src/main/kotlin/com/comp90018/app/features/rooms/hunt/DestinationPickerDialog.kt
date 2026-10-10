package com.comp90018.app.features.rooms.hunt

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.comp90018.app.Brand
import com.comp90018.app.Ink
import com.comp90018.app.Muted
import com.comp90018.app.features.map.MapRelic

/** Lists catalogue treasures that can be selected as the room hunt destination. */
@Composable
internal fun DestinationPickerDialog(
    treasures: List<MapRelic>,
    loading: Boolean,
    error: String?,
    onSelected: (MapRelic) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Choose destination", color = Ink, fontWeight = FontWeight.Bold) },
        text = {
            when {
                loading -> Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = Brand) }
                error != null -> Text(error, color = MaterialTheme.colorScheme.error)
                treasures.isEmpty() -> Text("No enabled treasures are available yet. Add the six treasure records in Firestore, then try again.", color = Muted)
                else -> LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(treasures.take(6), key = { it.id }) { relic ->
                        OutlinedButton(onClick = { onSelected(relic) }, modifier = Modifier.fillMaxWidth()) {
                            Column(Modifier.fillMaxWidth()) {
                                Text(relic.name, color = Ink, fontWeight = FontWeight.Medium)
                                Text(relic.roomDestinationName(), color = Muted, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = Brand) } },
    )
}
