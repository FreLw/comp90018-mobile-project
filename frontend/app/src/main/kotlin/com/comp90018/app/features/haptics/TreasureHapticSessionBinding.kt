package com.comp90018.app.features.haptics

import androidx.activity.compose.LocalActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner

/** The production AppShell binding, also exercised by real Compose/Activity tests. */
@Composable
fun TreasureHapticSessionBinding(session: TreasureHapticSessionViewModel, preference: Boolean?) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val activity = LocalActivity.current
    SideEffect { session.setPreference(preference) }
    DisposableEffect(lifecycleOwner, session) {
        session.beginSession()
        session.setForeground(lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED))
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) session.setForeground(true)
            if (event == Lifecycle.Event.ON_PAUSE || event == Lifecycle.Event.ON_STOP) session.setForeground(false)
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            session.detach(activity?.isChangingConfigurations == true)
        }
    }
}
