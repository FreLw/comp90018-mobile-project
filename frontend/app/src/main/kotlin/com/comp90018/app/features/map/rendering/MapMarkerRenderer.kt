package com.comp90018.app.features.map.rendering

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.RadialGradient
import android.graphics.Shader
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import com.comp90018.app.R
import com.comp90018.app.features.map.MapRelic
import com.comp90018.app.features.map.TeamHuntFragment
import com.comp90018.app.features.treasure.treasureArtworkResource
import com.comp90018.app.sensors.location.GeoCoordinate
import com.comp90018.app.sensors.location.LocationOutput
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions

/** Creates treasure markers for the current scene; the caller owns clearing the previous scene. */
internal fun renderRelics(
    context: Context,
    map: GoogleMap,
    relics: List<MapRelic>,
    selectedRelic: MapRelic?,
    activeHuntTreasureId: String?,
    pulseScale: Float,
    discoveredTreasureIds: Set<String>,
): Map<String, Marker> {
    val markers = mutableMapOf<String, Marker>()
    relics.forEach { relic ->
        val marker = map.addMarker(
            MarkerOptions()
                .position(relic.coordinate.toLatLng())
                .title(relic.name)
                .snippet(relic.locationName)
                .icon(questMarkerIcon(
                    context, relic, relic.id in discoveredTreasureIds,
                    selected = relic.id == selectedRelic?.id,
                    active = relic.id == activeHuntTreasureId,
                    pulseScale = pulseScale,
                ))
                .anchor(0.5f, 0.5f),
        )
        marker?.tag = relic.id
        marker?.let { markers[relic.id] = it }
    }
    return markers
}

/** Creates shared Atlas fragment markers with artwork matching each fragment's found state. */
internal fun renderHuntFragments(
    context: Context,
    map: GoogleMap,
    fragments: List<TeamHuntFragment>,
    foundFragmentIds: Set<String>,
) {
    fragments.forEach { fragment ->
        val found = fragment.id in foundFragmentIds
        map.addMarker(
            MarkerOptions()
                .position(fragment.coordinate.toLatLng())
                .title(fragment.title)
                .snippet(if (found) "Found" else "Not found")
                .icon(fragmentMarkerIcon(context, fragment.id, found))
                .anchor(0.5f, 0.5f),
        )?.tag = "fragment:${fragment.id}"
    }
}

private val fragmentArtworkBounds = android.util.LruCache<Int, android.graphics.Rect>(4)

/** Builds the bitmap artwork used by an individual Atlas-fragment map marker. */
private fun fragmentMarkerIcon(context: Context, fragmentId: String, found: Boolean): BitmapDescriptor {
    val artworkRes = when (fragmentId) {
        "south_lawn_north_west" -> R.drawable.treasure_atlas_fragment_north_west
        "south_lawn_north_east" -> R.drawable.treasure_atlas_fragment_north_east
        "south_lawn_south_west" -> R.drawable.treasure_atlas_fragment_south_west
        "south_lawn_south_east" -> R.drawable.treasure_atlas_fragment_south_east
        else -> R.drawable.treasure_unknown
    }
    val artwork = treasureMarkerArtwork.get(artworkRes) ?: BitmapFactory.decodeResource(context.resources, artworkRes).also {
        treasureMarkerArtwork.put(artworkRes, it)
    }
    val artworkBounds = fragmentArtworkBounds.get(artworkRes) ?: run {
        val pixels = IntArray(artwork.width * artwork.height)
        artwork.getPixels(pixels, 0, artwork.width, 0, 0, artwork.width, artwork.height)
        var left = artwork.width
        var top = artwork.height
        var right = -1
        var bottom = -1
        for (y in 0 until artwork.height) {
            for (x in 0 until artwork.width) {
                // Near-transparent stray pixels in the south-west crop sit far outside
                // the visible artwork and must not shift its apparent centre.
                if (android.graphics.Color.alpha(pixels[y * artwork.width + x]) > 8) {
                    left = minOf(left, x)
                    top = minOf(top, y)
                    right = maxOf(right, x)
                    bottom = maxOf(bottom, y)
                }
            }
        }
        val bounds = if (right >= left && bottom >= top) {
            android.graphics.Rect(left, top, right + 1, bottom + 1)
        } else {
            android.graphics.Rect(0, 0, artwork.width, artwork.height)
        }
        bounds.also { fragmentArtworkBounds.put(artworkRes, it) }
    }
    val density = context.resources.displayMetrics.density
    val size = (56f * density).toInt().coerceAtLeast(1)
    val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = android.graphics.Canvas(bitmap)
    val center = size / 2f
    val radius = size * 0.43f
    // A softly shaded sphere keeps every fragment readable against the map.
    canvas.drawCircle(center, center + size * 0.035f, radius + density, Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.argb(55, 45, 31, 19)
    })
    canvas.drawCircle(center, center, radius, Paint(Paint.ANTI_ALIAS_FLAG).apply {
        shader = RadialGradient(
            center - radius * 0.35f, center - radius * 0.4f, radius * 1.6f,
            intArrayOf(
                android.graphics.Color.rgb(255, 253, 245),
                android.graphics.Color.rgb(244, 229, 202),
                android.graphics.Color.rgb(193, 158, 105),
            ),
            floatArrayOf(0f, 0.65f, 1f), Shader.TileMode.CLAMP,
        )
    })
    canvas.drawCircle(center, center, radius, Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = if (found) android.graphics.Color.rgb(168, 112, 39) else android.graphics.Color.rgb(105, 87, 65)
        style = Paint.Style.STROKE
        strokeWidth = 1.5f * density
    })
    val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply {
        if (!found) colorFilter = PorterDuffColorFilter(android.graphics.Color.BLACK, PorterDuff.Mode.SRC_IN)
    }
    val halfExtent = radius * 0.66f
    val fit = halfExtent * 2f / maxOf(artworkBounds.width(), artworkBounds.height())
    val halfWidth = artworkBounds.width() * fit / 2f
    val halfHeight = artworkBounds.height() * fit / 2f
    canvas.drawBitmap(artwork, artworkBounds, android.graphics.RectF(
        center - halfWidth, center - halfHeight, center + halfWidth, center + halfHeight,
    ), paint)
    canvas.drawOval(android.graphics.RectF(
        center - radius * 0.63f, center - radius * 0.78f,
        center - radius * 0.08f, center - radius * 0.58f,
    ), Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.argb(175, 255, 255, 255)
    })
    return BitmapDescriptorFactory.fromBitmap(bitmap)
}

/**
 * [stale] means [currentLocation] is a last-known fallback rather than a live fix (see
 * [com.comp90018.app.sensors.location.LocationOutput.lastKnownLocation]) - the dot is dimmed
 * instead of being removed so the explorer still has a position to go by.
 */
internal fun renderCurrentLocation(
    context: Context,
    map: GoogleMap,
    currentLocationMarker: Marker?,
    currentLocation: GeoCoordinate?,
    headingDegrees: Float,
    stale: Boolean,
): Marker? {
    if (currentLocation == null) {
        currentLocationMarker?.remove()
        return null
    }
    val position = currentLocation.toLatLng()
    val alpha = if (stale) STALE_LOCATION_ALPHA else 1f
    if (currentLocationMarker != null) {
        currentLocationMarker.position = position
        currentLocationMarker.rotation = headingDegrees
        currentLocationMarker.alpha = alpha
        currentLocationMarker.title = if (stale) "Last known location" else "You"
        currentLocationMarker.zIndex = CURRENT_LOCATION_Z_INDEX
        return currentLocationMarker
    }
    return map.addMarker(
        MarkerOptions()
            .position(position)
            .title(if (stale) "Last known location" else "You")
            .icon(currentLocationIcon(context))
            .anchor(0.5f, 0.72f)
            .flat(true)
            .rotation(headingDegrees)
            .alpha(alpha)
            // Relics sit at zIndex 0, so when a relic shares the player's exact spot (e.g. an
            // emulator's mock location pinned on top of a relic for testing) the "you are here"
            // dot still renders above it instead of being hidden underneath.
            .zIndex(CURRENT_LOCATION_Z_INDEX),
    )
}

internal const val CURRENT_LOCATION_Z_INDEX = 10f

private const val STALE_LOCATION_ALPHA = 0.5f

private val treasureMarkerIcons = android.util.LruCache<String, BitmapDescriptor>(96)

private val treasureMarkerArtwork = android.util.LruCache<Int, Bitmap>(8)

/** Builds the selected/available treasure marker bitmap at the requested presentation size. */
internal fun questMarkerIcon(
    context: Context,
    relic: MapRelic,
    discovered: Boolean,
    selected: Boolean,
    active: Boolean = false,
    pulseScale: Float = 1f,
    flipScale: Float = 1f,
): BitmapDescriptor {
    val density = context.resources.displayMetrics.density
    val artworkRes = treasureArtworkResource(relic.artworkKey)
    val pulseBucket = (pulseScale * 20).toInt()
    val key = "$artworkRes:$density:$discovered:$selected:$active:$pulseBucket:${(flipScale * 40).toInt()}"
    treasureMarkerIcons.get(key)?.let { return it }
    val scale = pulseBucket / 20f
    val size = (56f * density).toInt().coerceAtLeast(1)
    val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = android.graphics.Canvas(bitmap)
    val center = size / 2f
    canvas.scale(flipScale, 1f, center, center)
    val radius = size * (if (selected || active) 0.40f else 0.33f) * scale
    val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    paint.color = if (active) android.graphics.Color.argb(85, 242, 181, 67) else android.graphics.Color.argb(45, 126, 76, 42)
    canvas.drawCircle(center, center, radius * 1.15f, paint)
    paint.color = android.graphics.Color.rgb(255, 247, 232)
    canvas.drawCircle(center, center, radius, paint)
    paint.style = Paint.Style.STROKE
    paint.strokeWidth = (if (selected || active) 2.5f else 1.5f) * density
    paint.color = if (active) android.graphics.Color.rgb(242, 181, 67) else android.graphics.Color.rgb(126, 76, 42)
    canvas.drawCircle(center, center, radius, paint)
    val artwork = treasureMarkerArtwork.get(artworkRes) ?: BitmapFactory.decodeResource(context.resources, artworkRes).also {
        treasureMarkerArtwork.put(artworkRes, it)
    }
    val imagePaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply {
        if (!discovered) colorFilter = PorterDuffColorFilter(android.graphics.Color.rgb(90, 59, 40), PorterDuff.Mode.SRC_IN)
    }
    val extent = radius * 1.42f
    val fit = extent / maxOf(artwork.width, artwork.height)
    val width = artwork.width * fit
    val height = artwork.height * fit
    canvas.drawBitmap(artwork, null, android.graphics.RectF(center - width / 2, center - height / 2, center + width / 2, center + height / 2), imagePaint)
    return BitmapDescriptorFactory.fromBitmap(bitmap).also { treasureMarkerIcons.put(key, it) }
}

/** Builds the explorer location-marker bitmap used by the map. */
internal fun currentLocationIcon(
    context: Context,
    dotColor: Int = android.graphics.Color.rgb(217, 84, 53),
    showHeading: Boolean = true,
): BitmapDescriptor {
    val density = context.resources.displayMetrics.density
    val size = (68 * density).toInt().coerceAtLeast(1)
    val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = android.graphics.Canvas(bitmap)
    val center = size / 2f
    val dotY = size * 0.72f
    val viewPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        shader = RadialGradient(
            center,
            dotY,
            size * 0.72f,
            intArrayOf(
                android.graphics.Color.argb(165, 242, 181, 67),
                android.graphics.Color.argb(70, 242, 181, 67),
                android.graphics.Color.TRANSPARENT,
            ),
            floatArrayOf(0f, 0.46f, 1f),
            Shader.TileMode.CLAMP,
        )
    }
    val haloPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.WHITE
        style = Paint.Style.FILL
    }
    val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = dotColor
        style = Paint.Style.FILL
    }
    val viewCone = Path().apply {
        moveTo(center, dotY)
        lineTo(size * 0.08f, size * 0.04f)
        lineTo(size * 0.92f, size * 0.04f)
        close()
    }
    if (showHeading) canvas.drawPath(viewCone, viewPaint)
    canvas.drawCircle(center, dotY, size * 0.14f, haloPaint)
    canvas.drawCircle(center, dotY, size * 0.095f, dotPaint)
    return BitmapDescriptorFactory.fromBitmap(bitmap)
}
