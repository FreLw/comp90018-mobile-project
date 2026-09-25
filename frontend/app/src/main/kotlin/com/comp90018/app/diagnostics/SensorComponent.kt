package com.comp90018.app.diagnostics

/** Physical/system components whose failure is worth surfacing directly to the user. */
enum class SensorComponent(val displayName: String) {
    GPS("GPS"),
    MICROPHONE("Microphone"),
    CAMERA("Camera"),
    NETWORK("Network"),
}
