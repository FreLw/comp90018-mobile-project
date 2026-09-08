package com.comp90018.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.comp90018.app.Brand
import com.comp90018.app.BrandSoft
import com.comp90018.app.Ink
import com.comp90018.app.Muted

@Composable
fun EmptyState(icon: ImageVector, title: String, subtitle: String) = Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp)) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 28.dp, vertical = 42.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.size(72.dp).background(BrandSoft, CircleShape), contentAlignment = Alignment.Center) { Icon(icon, null, tint = Brand, modifier = Modifier.size(34.dp)) }
        Spacer(Modifier.height(18.dp)); Text(title, fontWeight = FontWeight.Bold, color = Ink, textAlign = TextAlign.Center)
        Spacer(Modifier.height(6.dp)); Text(subtitle, color = Muted, textAlign = TextAlign.Center)
    }
}
