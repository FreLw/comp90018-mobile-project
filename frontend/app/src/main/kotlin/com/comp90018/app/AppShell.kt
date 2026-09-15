package com.comp90018.app

import android.hardware.display.DisplayManager
import android.os.Handler
import android.os.Looper
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.comp90018.app.data.profile.FirebaseProfileRepository
import com.comp90018.app.features.friends.FriendsScreen
import com.comp90018.app.features.map.MapScreen
import com.comp90018.app.features.profile.ProfileScreen
import com.comp90018.app.features.rooms.RoomsScreen
import com.comp90018.app.features.sensors.SensorDiagnosticsScreen
import com.comp90018.app.features.sensors.SensorDiagnosticsViewModel
import com.comp90018.app.sensors.AndroidSensorController
import com.comp90018.app.navigation.AppBottomNavigation
import com.comp90018.app.navigation.AppDestination
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import java.util.concurrent.atomic.AtomicInteger

/** Owns signed-in navigation; profile state belongs to [AppShellViewModel]. */
@Composable
fun AppShell(user: FirebaseUser, firestore: FirebaseFirestore, onLogout: () -> Unit) {
    var destination by remember { mutableStateOf(AppDestination.Friends) }
    val repository = remember(firestore) { FirebaseProfileRepository(firestore) }
    val viewModel: AppShellViewModel = viewModel(
        key = "app_shell_${user.uid}",
        factory = AppShellViewModel.factory(repository, user.uid, user.email.orEmpty()),
    )
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(containerColor = Background, bottomBar = { AppBottomNavigation(destination) { destination = it } }) { padding ->
        Box(Modifier.fillMaxSize().padding(padding).padding(horizontal = 20.dp)) {
            when (destination) {
                // Temporary sensor diagnostics entry; replace with the real Treasure/Scanner UI later.
                AppDestination.Treasure -> TemporarySensorDiagnosticsEntry()
                AppDestination.Rooms -> RoomsScreen(user, firestore, state.profile)
                AppDestination.Map -> MapScreen()
                AppDestination.Friends -> FriendsScreen(user, firestore, state.profile)
                AppDestination.Profile -> ProfileScreen(user.uid, user.email.orEmpty(), repository, state.profile, state.profileError, onRetry = viewModel::retry, onLogout = onLogout)
            }
        }
    }
}

@Composable
private fun TemporarySensorDiagnosticsEntry() {
    val applicationContext = LocalContext.current.applicationContext
    val view = LocalView.current
    // Only the atomic value crosses onto the sensor thread; the controller never retains the View.
    val rotation = remember { AtomicInteger(view.display?.rotation ?: -1) }
    val owner = remember {
        object : ViewModelStoreOwner {
            override val viewModelStore = ViewModelStore()
        }
    }
    DisposableEffect(owner) {
        onDispose { owner.viewModelStore.clear() }
    }
    DisposableEffect(applicationContext, view, rotation) {
        val displays = applicationContext.getSystemService(DisplayManager::class.java)
        val listener = object : DisplayManager.DisplayListener {
            private fun update() { rotation.set(view.display?.rotation ?: -1) }
            override fun onDisplayAdded(displayId: Int) = update()
            override fun onDisplayRemoved(displayId: Int) = update()
            override fun onDisplayChanged(displayId: Int) = update()
        }
        rotation.set(view.display?.rotation ?: -1)
        displays?.registerDisplayListener(listener, Handler(Looper.getMainLooper()))
        onDispose { displays?.unregisterDisplayListener(listener) }
    }
    val factory = remember(applicationContext, rotation) {
        SensorDiagnosticsViewModel.factory {
            AndroidSensorController(applicationContext, displayRotation = rotation::get)
        }
    }
    val diagnostics: SensorDiagnosticsViewModel = viewModel(viewModelStoreOwner = owner, factory = factory)
    SensorDiagnosticsScreen(diagnostics)
}
