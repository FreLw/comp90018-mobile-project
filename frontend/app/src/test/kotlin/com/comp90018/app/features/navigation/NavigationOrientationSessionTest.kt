package com.comp90018.app.features.navigation

import com.comp90018.app.sensors.DirectionOutput
import com.comp90018.app.sensors.DirectionProcessor
import com.comp90018.app.sensors.SensorValidity
import com.comp90018.app.sensors.orientation.FakeOrientationSensor
import com.comp90018.app.sensors.orientation.OrientationOutput
import com.comp90018.app.sensors.orientation.OrientationSensor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class NavigationOrientationSessionTest {
    @Test
    fun validFormalHeadingIsExposedIncludingNorth() {
        val sensor = FakeOrientationSensor()
        val session = NavigationOrientationSession(sensor)
        session.start()
        listOf(90.0, 0.0).forEach { heading ->
            sensor.emitDirection(DirectionProcessor.evaluate(heading, null))
            assertEquals(heading, requireNotNull(navigationDeviceHeading(session.output.value)), 0.0)
        }
    }

    @Test
    fun unknownWarmingUpAndUnreliableHeadingAreUnavailable() {
        listOf(SensorValidity.UNKNOWN, SensorValidity.WARMING_UP, SensorValidity.UNRELIABLE)
            .forEach { validity ->
                assertNull(navigationDeviceHeading(orientation(90.0, validity)))
            }
    }

    @Test
    fun missingOrNonFiniteHeadingIsUnavailableEvenWithValidStatus() {
        listOf(null, Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY)
            .forEach { heading ->
                assertNull(navigationDeviceHeading(orientation(heading)))
            }
    }

    @Test
    fun debugHeadingOverridesSensorUpdatesAndDisablingItReturnsToRealHeading() {
        val sensor = FakeOrientationSensor()
        sensor.emitDirection(DirectionProcessor.evaluate(45.0, null))
        assertEquals(270.0, requireNotNull(navigationDeviceHeading(sensor.output.value, 270.0)), 0.0)
        sensor.emitDirection(DirectionProcessor.evaluate(90.0, null))
        assertEquals(270.0, requireNotNull(navigationDeviceHeading(sensor.output.value, 270.0)), 0.0)
        assertEquals(90.0, requireNotNull(navigationDeviceHeading(sensor.output.value, null)), 0.0)
        assertEquals(0.0, requireNotNull(navigationDeviceHeading(OrientationOutput(), 0.0)), 0.0)
        assertNull(navigationDeviceHeading(OrientationOutput(), null))
    }

    @Test
    fun invalidSimulationCannotBecomeSemanticHeading() {
        listOf(Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY).forEach { heading ->
            assertNull(navigationDeviceHeading(orientation(90.0), heading))
        }
    }

    @Test
    fun sessionStartsAndStopsOnceAndRestartsWithFreshOutputAndNoTargetComparison() {
        val fake = FakeOrientationSensor()
        val sensor = RecordingOrientationSensor(fake)
        val session = NavigationOrientationSession(sensor)
        session.start()
        session.start()
        assertEquals(1, sensor.starts)
        assertEquals(listOf<Double?>(null), sensor.targets)
        fake.emitDirection(DirectionProcessor.evaluate(90.0, null))
        session.stop()
        session.stop()
        assertEquals(1, sensor.stops)
        assertNull(navigationDeviceHeading(session.output.value))
        session.start()
        assertEquals(2, sensor.starts)
        assertNull(navigationDeviceHeading(session.output.value))
        fake.emitDirection(DirectionProcessor.evaluate(180.0, null))
        assertEquals(180.0, requireNotNull(navigationDeviceHeading(session.output.value)), 0.0)
        assertNull(session.output.value.direction.targetBearingDegrees)
        session.stop()
        assertEquals(2, sensor.stops)
    }

    @Test
    fun uiStateRetainsNullableHeadingWithoutIntroducingDirectionGuidance() {
        val sample = RelicArrivalSample(true, 50.0, 5.0, 100L)
        val confirmation = updateRelicArrivalConfirmation(RelicArrivalConfirmationState(), sample)
        listOf(null, 0.0, 90.0).forEach { heading ->
            val state = deriveRelicNavigationUiState(sample, confirmation, deviceHeadingDegrees = heading)
            assertEquals(heading, state.deviceHeadingDegrees)
            assertEquals(NavigationDirectionHint.UNAVAILABLE, state.directionHint)
            assertNull(state.headingErrorDegrees)
        }
        assertNull(RelicNavigationUiState().deviceHeadingDegrees)
        assertNull(deriveRelicNavigationUiState(sample, confirmation, deviceHeadingDegrees = Double.NaN).deviceHeadingDegrees)
    }

    private fun orientation(heading: Double?, validity: SensorValidity = SensorValidity.VALID) =
        OrientationOutput(direction = DirectionOutput(headingDegrees = heading, headingValidity = validity))

    private class RecordingOrientationSensor(private val fake: FakeOrientationSensor) : OrientationSensor by fake {
        var starts = 0
        var stops = 0
        val targets = mutableListOf<Double?>()

        override fun start() {
            starts += 1
            fake.start()
        }

        override fun stop() {
            stops += 1
            fake.stop()
        }

        override fun setTargetBearing(targetBearingDegrees: Double?) {
            targets += targetBearingDegrees
        }
    }
}
