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
import androidx.annotation.DrawableRes
import com.comp90018.app.R
import com.comp90018.app.features.map.MapRelic
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URL

private val treasureImageCache = android.util.LruCache<String, ImageBitmap>(12)

/** Loads a remote treasure image and always falls back to the local unknown-relic artwork. */
@Composable
fun RemoteTreasureImage(
    imageUrl: String,
    contentDescription: String,
    modifier: Modifier = Modifier,
    silhouette: Boolean = false,
    @DrawableRes fallbackResId: Int = R.drawable.treasure_unknown,
) {
    val bitmap by produceState<ImageBitmap?>(initialValue = treasureImageCache.get(imageUrl), imageUrl) {
        value = treasureImageCache.get(imageUrl)
        value = value ?: withContext(Dispatchers.IO) {
            if (!imageUrl.startsWith("https://")) return@withContext null
            runCatching {
                URL(imageUrl).openConnection().apply {
                    connectTimeout = 10000
                    readTimeout = 10000
                }.getInputStream().use { BitmapFactory.decodeStream(it)?.asImageBitmap() }
                    ?.also { treasureImageCache.put(imageUrl, it) }
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
            painter = painterResource(fallbackResId),
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
        fallbackResId = treasureArtworkResource(relic.artworkKey),
    )
}

/** The same Firestore artwork key drives map, collection, and post-challenge artwork. */
@DrawableRes
fun treasureArtworkResource(artworkKey: String): Int = when (artworkKey) {
    "treasure_postcard" -> R.drawable.treasure_postcard
    "treasure_rosette" -> R.drawable.treasure_rosette
    "treasure_fern" -> R.drawable.treasure_fern
    "treasure_atlas" -> R.drawable.treasure_atlas
    "treasure_glasshouse" -> R.drawable.treasure_glasshouse
    "treasure_press" -> R.drawable.treasure_press
    else -> R.drawable.treasure_unknown
}

/** Bundled archive photographs remain available offline and when Firestore has no image URL. */
@DrawableRes
fun historicalArtworkResource(relic: MapRelic): Int? = relic.historicalImageResId ?: when (relic.id) {
    "union_lawn_lost_lake" -> R.drawable.historical_union_lake_1936
    "wilson_hall_rosette" -> R.drawable.historical_wilson_hall_fire_1952
    "old_quad_fossil" -> R.drawable.historical_old_quad_fossil_1875
    "south_lawn_atlas" -> R.drawable.historical_south_lawn_atlas_1900
    "system_garden_glasshouse" -> R.drawable.historical_system_garden
    "grainger_tone_tool" -> R.drawable.historical_grainger_tone_tool_1952
    else -> null
}

@Composable
fun HistoricalTreasureImage(relic: MapRelic, modifier: Modifier = Modifier) {
    val local = historicalArtworkResource(relic)
    if (relic.historicalImageUrl.isNotBlank()) {
        RemoteTreasureImage(relic.historicalImageUrl, "Historical reference for ${relic.name}", modifier,
            fallbackResId = local ?: R.drawable.treasure_unknown)
    } else if (local != null) {
        Image(painterResource(local), "Historical reference for ${relic.name}", modifier, contentScale = ContentScale.Fit)
    }
}
