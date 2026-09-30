package com.comp90018.app.features.treasure

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.comp90018.app.R
import com.comp90018.app.features.map.MapRelic
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URL

/** Loads a remote treasure image and always falls back to the local unknown-relic artwork. */
@Composable
fun RemoteTreasureImage(
    imageUrl: String,
    contentDescription: String,
    modifier: Modifier = Modifier,
    silhouette: Boolean = false,
) {
    val bitmap by produceState<ImageBitmap?>(initialValue = null, imageUrl) {
        value = withContext(Dispatchers.IO) {
            if (!imageUrl.startsWith("https://")) return@withContext null
            runCatching {
                URL(imageUrl).openStream().use { BitmapFactory.decodeStream(it)?.asImageBitmap() }
            }.getOrNull()
        }
    }
    val tint = if (silhouette) ColorFilter.tint(Color(0xFF563B2D)) else null
    if (bitmap != null) {
        Image(
            bitmap = requireNotNull(bitmap),
            contentDescription = contentDescription,
            modifier = modifier,
            contentScale = ContentScale.Fit,
            colorFilter = tint,
        )
    } else {
        Image(
            painter = painterResource(R.drawable.treasure_unknown),
            contentDescription = contentDescription,
            modifier = modifier,
            contentScale = ContentScale.Fit,
            colorFilter = tint,
        )
    }
}

@Composable
fun TreasurePrototypeImage(
    relic: MapRelic,
    discovered: Boolean,
    modifier: Modifier = Modifier,
) {
    RemoteTreasureImage(
        imageUrl = relic.prototypeImageUrl,
        contentDescription = if (discovered) relic.name else "Locked ${relic.name} silhouette",
        modifier = modifier,
        silhouette = !discovered,
    )
}
