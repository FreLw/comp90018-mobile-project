package com.comp90018.app.features.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.comp90018.app.Background
import com.comp90018.app.Brand
import com.comp90018.app.BrandSoft
import com.comp90018.app.Ink
import com.comp90018.app.Muted
import com.comp90018.app.features.map.MapRelic
import com.comp90018.app.features.treasure.TreasurePrototypeImage

/** Standalone navigation surface. Map tracking and relic energy are added in later stages. */
@Composable
fun RelicNavigationScreen(
    relic: MapRelic,
    onStopNavigation: () -> Unit,
) {
    Box(Modifier.fillMaxSize().background(Background)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onStopNavigation) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Stop navigation", tint = Ink)
            }
            Text(
                "Navigation",
                color = Ink,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleLarge,
            )
        }

        Card(
            modifier = Modifier.align(Alignment.Center).padding(horizontal = 24.dp),
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Box(
                    modifier = Modifier.size(132.dp).background(BrandSoft, RoundedCornerShape(20.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    TreasurePrototypeImage(relic, discovered = false, modifier = Modifier.size(116.dp))
                }
                Text(
                    relic.name,
                    color = Ink,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.headlineSmall,
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.LocationOn, contentDescription = null, tint = Brand)
                    Spacer(Modifier.width(5.dp))
                    Text(relic.locationName, color = Muted)
                }
                OutlinedButton(onClick = onStopNavigation, modifier = Modifier.fillMaxWidth()) {
                    Text("Stop Navigation")
                }
            }
        }
    }
}
