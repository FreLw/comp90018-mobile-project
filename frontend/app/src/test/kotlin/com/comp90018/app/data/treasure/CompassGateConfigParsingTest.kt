package com.comp90018.app.data.treasure

import com.comp90018.app.features.map.CompassGateConfig
import org.junit.Assert.assertEquals
import org.junit.Test

class CompassGateConfigParsingTest {
    @Test fun firestoreNumbersOverrideAllThreeThresholds() {
        assertEquals(CompassGateConfig(25.0, 30.5, 20.0), parseCompassGateConfig(mapOf(
            "huntReadyRadiusMeters" to 25L,
            "compassAlignmentToleranceDegrees" to 30.5,
            "horizontalToleranceDegrees" to 20L,
        )))
    }

    @Test fun missingOrInvalidFieldsFallBackIndependently() {
        assertEquals(CompassGateConfig(), parseCompassGateConfig(emptyMap()))
        assertEquals(CompassGateConfig(25.0, 15.0, 12.0), parseCompassGateConfig(mapOf(
            "huntReadyRadiusMeters" to 25.0,
            "compassAlignmentToleranceDegrees" to Double.NaN,
            "horizontalToleranceDegrees" to 91.0,
        )))
        assertEquals(CompassGateConfig(), parseCompassGateConfig(mapOf(
            "huntReadyRadiusMeters" to -1,
            "compassAlignmentToleranceDegrees" to 181,
            "horizontalToleranceDegrees" to "12",
        )))
    }
}
