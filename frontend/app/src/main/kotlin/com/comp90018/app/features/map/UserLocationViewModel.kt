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

    /** Re-read Android's permission state before (re)starting location updates. */
    fun retryAfterPermissionGranted() {
        sensor.refreshPermissionState()
        sensor.start()
    }

    /** Covers permissions changed in Settings while the app was in the background. */
    fun refreshWhenForegrounded() {
        sensor.refreshPermissionState()
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
