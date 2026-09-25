package com.comp90018.app.diagnostics

data class SensorErrorEvent(
    val component: SensorComponent,
    val message: String,
    val timestampNanos: Long,
)
