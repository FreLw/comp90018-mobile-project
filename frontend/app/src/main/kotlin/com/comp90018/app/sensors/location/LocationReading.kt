package com.comp90018.app.sensors.location

data class LocationReading(
    val coordinate: GeoCoordinate,
    val accuracyMeters: Double?,
    val timestampNanos: Long,
)
