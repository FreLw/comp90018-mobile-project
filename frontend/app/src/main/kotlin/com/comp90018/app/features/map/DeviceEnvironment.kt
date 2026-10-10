package com.comp90018.app.features.map

/*
 * Centralizes emulator detection used by map and signed-in location presentation.
 * Keeps emulator-specific display policy consistent between UI entry points.
 */

import android.os.Build
import java.util.Locale

/**
 * Emulator detection is kept outside map UI so both AppShell and MapScreen use
 * one definition of the test-location policy.
 */
internal fun isProbablyEmulator(): Boolean =
    Build.FINGERPRINT.startsWith("generic") ||
        Build.FINGERPRINT.lowercase(Locale.US).contains("emulator") ||
        Build.MODEL.lowercase(Locale.US).let {
            "google_sdk" in it || "sdk_gphone" in it || "emulator" in it ||
                "android sdk built for" in it
        } ||
        Build.MANUFACTURER.lowercase(Locale.US).contains("genymotion") ||
        Build.PRODUCT.lowercase(Locale.US).let { it.startsWith("sdk") || "emulator" in it } ||
        Build.HARDWARE.lowercase(Locale.US).let { "goldfish" in it || "ranchu" in it } ||
        (Build.BRAND.startsWith("generic") && Build.DEVICE.startsWith("generic"))
