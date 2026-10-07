package com.comp90018.app.features.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.currentStateAsState
import com.comp90018.app.sensors.orientation.AndroidOrientationSensor
import com.comp90018.app.sensors.orientation.OrientationOutput
import com.comp90018.app.sensors.orientation.OrientationSensor

@Composable
internal fun rememberNavigationOrientationOutput(): OrientationOutput {
    val applicationContext = LocalContext.current.applicationContext
    val sensor: OrientationSensor = remember(applicationContext) { AndroidOrientationSensor(applicationContext) }
    val session = remember(sensor) { NavigationOrientationSession(sensor) }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val lifecycleState by lifecycle.currentStateAsState()
    // Keep collecting the reset output on stop, so resume cannot expose a cached heading.
    val output by session.output.collectAsState()

    DisposableEffect(lifecycle, session) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> session.start()
                Lifecycle.Event.ON_PAUSE, Lifecycle.Event.ON_STOP, Lifecycle.Event.ON_DESTROY -> session.stop()
                else -> Unit
            }
        }
        lifecycle.addObserver(observer)
        if (lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) session.start()
        onDispose {
            lifecycle.removeObserver(observer)
            session.stop()
        }
    }
    return if (lifecycleState.isAtLeast(Lifecycle.State.RESUMED)) output else OrientationOutput()
}
