package com.comp90018.app.sensors.orientation

import com.comp90018.app.sensors.DirectionOutput
import com.comp90018.app.sensors.RotationOutput

data class OrientationOutput(
    val direction: DirectionOutput = DirectionOutput(),
    val rotation: RotationOutput = RotationOutput(),
)
