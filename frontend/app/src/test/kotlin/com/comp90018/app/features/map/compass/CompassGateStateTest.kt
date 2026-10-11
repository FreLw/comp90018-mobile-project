package com.comp90018.app.features.map.compass

import com.comp90018.app.features.map.CompassGateConfig
import com.comp90018.app.sensors.SensorValidity
import com.comp90018.app.sensors.location.LocationOutput
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Guards live/preview input selection and the existing compass entry conditions. */
class CompassGateStateTest {
    private val config = CompassGateConfig()
    private val liveLocation = LocationOutput(
        distanceToTargetMeters = 5.0,
        targetBearingDegrees = 45.0,
        validity = SensorValidity.VALID,
    )

    @Test fun liveReadingsIgnorePreviewValuesWhenSimulationIsOff() {
        val state = compassGateUiState(config, liveLocation, 30f, false, 100f, 180f)
        assertEquals(5.0, state.distanceMeters!!, 0.0)
        assertEquals(15.0, state.turnDegrees!!, 0.0)
        assertTrue(state.readiness.allReady)
        assertEquals(10, state.stars)
    }

    @Test fun unreliableGpsCannotUnlockTheTaskEvenWhenDistanceAndHeadingMatch() {
        val state = compassGateUiState(
            config, liveLocation.copy(validity = SensorValidity.UNKNOWN), 45f, false, 5f, 45f,
        )
        assertFalse(state.locationReady)
        assertFalse(state.readiness.allReady)
        assertEquals("Waiting for a reliable location signal…", compassGateInstruction(state, false, config))
    }

    @Test fun missingDistanceOrBearingKeepsTheWaitingInstruction() {
        for (location in listOf(
            liveLocation.copy(distanceToTargetMeters = null),
            liveLocation.copy(targetBearingDegrees = null),
        )) {
            val state = compassGateUiState(config, location, 45f, false, 5f, 45f)
            assertFalse(state.readiness.allReady)
            assertEquals("Waiting for a reliable location signal…", compassGateInstruction(state, false, config))
        }
    }

    @Test fun previewCanUseANorthBearingWhenGpsHasNoBearing() {
        val state = compassGateUiState(config, LocationOutput(), 180f, true, 5f, 0f)
        assertEquals(0.0, state.targetBearingDegrees!!, 0.0)
        assertEquals(0.0, state.turnDegrees!!, 0.0)
        assertTrue(state.locationReady)
        assertTrue(state.readiness.allReady)
    }

    @Test fun previewRetainsTheRealTargetBearingWhenOneIsAvailable() {
        val state = compassGateUiState(config, liveLocation, 180f, true, 5f, 40f)
        assertEquals(45.0, state.targetBearingDegrees!!, 0.0)
        assertEquals(5.0, state.turnDegrees!!, 0.0)
        assertTrue(state.readiness.allReady)
    }

    @Test fun configuredDistanceAndHeadingBoundariesRemainInclusive() {
        val custom = CompassGateConfig(huntReadyRadiusMeters = 25.0, compassAlignmentToleranceDegrees = 30.0)
        val location = liveLocation.copy(distanceToTargetMeters = 25.0, targetBearingDegrees = 0.0)
        assertTrue(compassGateUiState(custom, location, 30f, false, 0f, 0f).readiness.allReady)
        assertFalse(compassGateUiState(custom, location.copy(distanceToTargetMeters = 25.01), 30f, false, 0f, 0f).readiness.allReady)
        assertFalse(compassGateUiState(custom, location, 30.01f, false, 0f, 0f).readiness.allReady)
    }

    @Test fun headingInstructionsKeepTheirDirectionAndLockedMessagePriority() {
        val location = liveLocation.copy(targetBearingDegrees = 0.0)
        val left = compassGateUiState(config, location, 30f, false, 0f, 0f)
        val right = compassGateUiState(config, location, 330f, false, 0f, 0f)
        assertTrue(compassGateInstruction(left, false, config).startsWith("Turn left "))
        assertTrue(compassGateInstruction(right, false, config).startsWith("Turn right "))
        assertEquals("Treasure signal locked — the hidden trial has awakened.", compassGateInstruction(left, true, config))
    }
}
