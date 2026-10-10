package com.comp90018.app.diagnostics

/*
 * Carries the failed component, message, and timestamp to the app-wide error UI.
 * The timestamp lets a newly mounted snackbar host ignore earlier-session events.
 */

data class SensorErrorEvent(
    val component: SensorComponent,
    val message: String,
    val timestampNanos: Long,
)
