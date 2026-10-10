package com.comp90018.app.diagnostics

/*
 * Names hardware/system components in user-visible sensor error messages.
 * SensorErrorHost uses these display names when formatting snackbar text.
 */

/** Physical/system components whose failure is worth surfacing directly to the user. */
enum class SensorComponent(val displayName: String) {
    GPS("GPS"),
    MICROPHONE("Microphone"),
    CAMERA("Camera"),
    NETWORK("Network"),
}
