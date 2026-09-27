package com.comp90018.app.features.treasure

import android.graphics.BitmapFactory
import androidx.annotation.DrawableRes
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
    @DrawableRes fallbackDrawableRes: Int = R.drawable.treasure_unknown,
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
            painter = painterResource(fallbackDrawableRes),
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
    Image(
        painter = painterResource(relic.localTreasureArtworkResId()),
        contentDescription = if (discovered) relic.name else "Locked ${relic.name} silhouette",
        modifier = modifier,
        contentScale = ContentScale.Fit,
        colorFilter = if (discovered) null else ColorFilter.tint(Color(0xFF563B2D)),
    )
}

/**
 * Artwork is bundled with the APK so treasure cards work offline and never depend on a Firebase
 * image URL. Firestore still owns the content and coordinates; a stable id/name selects the asset.
 */
@DrawableRes
fun MapRelic.localTreasureArtworkResId(): Int {
    val key = "$id $name $locationName $treasureType".lowercase().filter(Char::isLetterOrDigit)
    return when {
        "southlawn" in key || "atlas" in key -> R.drawable.treasure_atlas
        "systemgarden" in key || "glasshouse" in key -> R.drawable.treasure_glasshouse
        "grainger" in key || "tonetool" in key || "printingpress" in key -> R.drawable.treasure_press
        "unionlawn" in key || "lostlake" in key || "postcard" in key -> R.drawable.treasure_postcard
        "oldquad" in key || "oldquadrangle" in key || "rosette" in key -> R.drawable.treasure_rosette
        "wilson" in key || "fern" in key || "fossil" in key -> R.drawable.treasure_fern
        else -> R.drawable.nav_treasure_game
    }
}
