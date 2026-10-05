package com.comp90018.app.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.runtime.Immutable
import com.comp90018.app.R

@Immutable
data class TreasureSticker(
    val treasureId: String,
    val name: String,
    @param:DrawableRes val drawableRes: Int,
)

/** The collectible sticker that belongs to each of the six campus treasures. */
val TreasureStickerCatalog = listOf(
    TreasureSticker("union_lawn_lost_lake", "Lost Lake Postcard", R.drawable.treasure_emoji_postcard),
    TreasureSticker("wilson_hall_rosette", "Wilson Hall Rosette", R.drawable.treasure_emoji_rosette),
    TreasureSticker("old_quad_fossil", "Old Quad Fossil", R.drawable.treasure_emoji_fern),
    TreasureSticker("south_lawn_atlas", "South Lawn Atlas", R.drawable.treasure_emoji_atlas),
    TreasureSticker("system_garden_glasshouse", "System Garden Glasshouse", R.drawable.treasure_emoji_glasshouse),
    TreasureSticker("grainger_tone_tool", "Grainger Tone Tool", R.drawable.treasure_emoji_press),
)

fun treasureStickerFor(treasureId: String): TreasureSticker? =
    TreasureStickerCatalog.firstOrNull { it.treasureId == treasureId }
