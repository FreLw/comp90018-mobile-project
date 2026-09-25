package com.comp90018.app.diagnostics

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * App-wide bus for sensor/component failures the UI should surface to the user (e.g. "GPS is
 * not responding"). Any sensor implementation calls [report]; a single UI surface observes
 * [events] so features don't each need their own error-toast plumbing.
 *
 * This singleton (and its buffer) survives sign-out/sign-in on the same device - it is not
 * scoped to a signed-in user, since a hardware failure isn't "whose" it is. That means a
 * collector must not assume every buffered event happened during *its* watch: filter by
 * [SensorErrorEvent.timestampNanos] against your own subscribe time (see
 * [SensorErrorHost][com.comp90018.app.ui.components.SensorErrorHost]) so a stale event from a
 * previous session never surfaces to whoever is signed in next.
 */
object SensorErrorReporter {
    private val _events = MutableSharedFlow<SensorErrorEvent>(extraBufferCapacity = 8)
    val events: SharedFlow<SensorErrorEvent> = _events.asSharedFlow()

    fun report(component: SensorComponent, message: String) {
        _events.tryEmit(SensorErrorEvent(component, message, System.nanoTime()))
    }
}
