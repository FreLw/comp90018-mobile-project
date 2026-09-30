package com.comp90018.app.features.map

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.comp90018.app.sensors.location.AndroidLocationSensor
import com.comp90018.app.sensors.location.LocationOutput
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

/**
 * Hoisted once in AppShell so it lives for the whole signed-in session: GPS starts updating as
 * soon as the app opens and keeps refreshing on [AndroidLocationSensor]'s own polling cadence,
 * independent of which bottom-nav tab is currently showing.
 */
class UserLocationViewModel(
    private val sensor: AndroidLocationSensor,
) : ViewModel() {
    val output: StateFlow<LocationOutput> = sensor.output.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = LocationOutput(),
    )

    init {
        sensor.start()
    }

    /** Sensor.start() is a no-op once permission was denied at launch; call this after the user grants it. */
    fun retryAfterPermissionGranted() {
        sensor.start()
    }

    override fun onCleared() {
        sensor.stop()
    }

    companion object {
        fun factory(context: Context, preciseLocationEnabled: Boolean) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T = UserLocationViewModel(
                AndroidLocationSensor(context.applicationContext, preciseLocationEnabled = preciseLocationEnabled),
            ) as T
        }
    }
}
