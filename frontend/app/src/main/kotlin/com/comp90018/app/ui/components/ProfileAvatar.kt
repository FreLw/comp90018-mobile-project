package com.comp90018.app.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import com.comp90018.app.Brand
import com.comp90018.app.BrandSoft
import com.comp90018.app.ui.images.loadAvatarImage

/** Loads the avatar source with a fallback inside the shared circular avatar layout. */
@Composable
internal fun ProfileAvatar(source: String, fallback: String, size: Dp) {
    val context = LocalContext.current
    val bitmap by produceState<ImageBitmap?>(initialValue = null, source) {
        value = loadAvatarImage(context, source)
    }
    Box(Modifier.size(size).clip(CircleShape).background(BrandSoft), contentAlignment = Alignment.Center) {
        if (bitmap != null) Image(bitmap = bitmap!!, contentDescription = "Profile photo", modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        else Text(fallback.take(1).uppercase(), color = Brand, style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold)
    }
}
