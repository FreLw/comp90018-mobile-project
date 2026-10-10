package com.comp90018.app.features.map.detail

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.drawscope.rotate
import com.comp90018.app.features.map.sensors.rememberDeviceMotion

/** Presents the staged search illustration used by the legacy hunt flow. */
@Composable
internal fun TreasureSearchPanel(
    searchStep: Int,
    readyToDig: Boolean,
    deviceHeading: Float,
    onDig: () -> Unit,
    onBack: () -> Unit,
) {
    val phase = searchStep.coerceIn(0, 2)
    val motion = rememberDeviceMotion()
    val command = when {
        readyToDig -> "Signal captured — the relic is beneath your feet."
        phase == 0 -> "Freeze the trail — stop and steady your stance."
        phase == 1 -> "Balance the relic lens — hold your phone level."
        else -> "Sweep the horizon — rotate slowly until the signal blooms."
    }
    val hint = if (readyToDig) "The hidden lock has opened. Dig when your team is ready."
    else "Calm motion, centre the level spark, then turn with the compass glow."
    HuntScanPanel(
        headline = command,
        hintText = hint,
        ready = readyToDig,
        acceleration = motion.accelerationMagnitude,
        levelTilt = motion.tiltDegrees,
        heading = deviceHeading,
        primaryActionLabel = "Dig for treasure",
        onPrimaryAction = onDig,
        onBack = onBack,
    )
}
