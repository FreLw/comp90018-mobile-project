package com.comp90018.app.sensors.location

import com.comp90018.app.sensors.SensorValidity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test

class LocationReadingFilterTest {
    private val config = LocationConfig(
        staleTimeoutNanos = 10_000L,
        maxAcceptedAccuracyMeters = 50.0,
    )
    private val validReading = LocationReading(
        coordinate = GeoCoordinate(-37.798156, 144.960481),
        accuracyMeters = 12.0,
        timestampNanos = 1_000L,
    )

    @Test
    fun validRecentReadingIsAccepted() {
        val accepted = LocationReadingFilter.accepted(validReading, nowNanos = 5_000L, config = config)

        assertSame(validReading, accepted)
        assertEquals(SensorValidity.VALID, LocationReadingFilter.validity(validReading, 5_000L, config))
    }

    @Test
    fun staleReadingIsRejected() {
        val accepted = LocationReadingFilter.accepted(validReading, nowNanos = 20_000L, config = config)

        assertNull(accepted)
        assertEquals(SensorValidity.UNRELIABLE, LocationReadingFilter.validity(validReading, 20_000L, config))
    }

    @Test
    fun inaccurateReadingIsRejected() {
        val inaccurate = validReading.copy(accuracyMeters = 80.0)

        assertNull(LocationReadingFilter.accepted(inaccurate, nowNanos = 5_000L, config = config))
        assertEquals(SensorValidity.UNRELIABLE, LocationReadingFilter.validity(inaccurate, 5_000L, config))
    }

    @Test
    fun invalidCoordinateIsRejected() {
        val invalid = validReading.copy(coordinate = GeoCoordinate(latitude = -100.0, longitude = 144.960481))

        assertNull(LocationReadingFilter.accepted(invalid, nowNanos = 5_000L, config = config))
        assertEquals(SensorValidity.UNRELIABLE, LocationReadingFilter.validity(invalid, 5_000L, config))
    }
}
