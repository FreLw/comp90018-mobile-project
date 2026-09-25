package com.comp90018.app.diagnostics

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import org.junit.Assert.assertEquals
import org.junit.Test

class SensorErrorReporterTest {
    @Test fun reportedEventIsDeliveredToActiveCollectors() {
        val scope = CoroutineScope(Dispatchers.Unconfined)
        val collected = mutableListOf<SensorErrorEvent>()
        scope.launch { SensorErrorReporter.events.collect { collected.add(it) } }

        SensorErrorReporter.report(SensorComponent.GPS, "Location hasn't updated in a while and could not be refreshed.")

        assertEquals(1, collected.size)
        val event = collected.single()
        assertEquals(SensorComponent.GPS, event.component)
        assertEquals("Location hasn't updated in a while and could not be refreshed.", event.message)
        scope.cancel()
    }
}
