package com.comp90018.app.features.map

import com.comp90018.app.sensors.location.GeoCoordinate

/** Shared locations for the two-player South Lawn Atlas reconstruction hunt. */
data class TeamHuntFragment(
    val id: String,
    val title: String,
    val coordinate: GeoCoordinate,
)

internal const val SOUTH_LAWN_ATLAS_ID = "south_lawn_atlas"
internal const val SOUTH_LAWN_FRAGMENT_RADIUS_METERS = 8.0

internal val SouthLawnFragments = listOf(
    TeamHuntFragment("south_lawn_north_west", "North-west fragment", GeoCoordinate(-37.798490, 144.959940)),
    TeamHuntFragment("south_lawn_north_east", "North-east fragment", GeoCoordinate(-37.798500, 144.960570)),
    TeamHuntFragment("south_lawn_south_west", "South-west fragment", GeoCoordinate(-37.798850, 144.959940)),
    TeamHuntFragment("south_lawn_south_east", "South-east fragment", GeoCoordinate(-37.798910, 144.960530)),
)
