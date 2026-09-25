package com.comp90018.app.ui.components

import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.comp90018.app.diagnostics.SensorErrorReporter
import kotlinx.coroutines.flow.filter

/**
 * Mount once near the root of the signed-in UI (e.g. as a Scaffold's snackbarHost) so sensor
 * failures reported via [SensorErrorReporter] are visible no matter which tab is active.
 *
 * [SensorErrorReporter] is not scoped to a signed-in user, so a previous session's buffered
 * event could otherwise be delivered right after a different user signs in on the same device.
 * Events older than this composable's own subscribe time are filtered out to prevent that.
 */
@Composable
fun SensorErrorHost(modifier: Modifier = Modifier) {
    val hostState = remember { SnackbarHostState() }
    LaunchedEffect(Unit) {
        val subscribedAtNanos = System.nanoTime()
        SensorErrorReporter.events
            .filter { it.timestampNanos >= subscribedAtNanos }
            .collect { event ->
                hostState.showSnackbar("${event.component.displayName} issue: ${event.message}")
            }
    }
    SnackbarHost(hostState = hostState, modifier = modifier)
}
