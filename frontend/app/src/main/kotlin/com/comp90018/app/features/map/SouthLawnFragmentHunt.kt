package com.comp90018.app.features.map

import com.comp90018.app.sensors.location.GeoCoordinate

/** Shared locations for the two-player South Lawn Atlas reconstruction hunt. */
data class TeamHuntFragment(
    val id: String,
    val title: String,
    val coordinate: GeoCoordinate,
)

internal const val SOUTH_LAWN_ATLAS_ID = "south_lawn_atlas"
// These IDs are also enforced by the room's Firestore security rules.
internal val SouthLawnFragmentIds = setOf(
    "south_lawn_north_west", "south_lawn_north_east",
    "south_lawn_south_west", "south_lawn_south_east",
)

data class FragmentHuntConfig(
    val fragments: List<TeamHuntFragment>,
    val collectionRadiusMeters: Double,
)
