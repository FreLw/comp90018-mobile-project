package com.comp90018.app.features.haptics

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModelProvider

/** Activity-retained, keyed by explorer in AppShell; survives map/detail and configuration changes. */
class TreasureHapticSessionViewModel(
    private val driver: TreasureHapticDriver,
    private val cancelVibration: () -> Unit = {},
) : ViewModel() {
    var controller by mutableStateOf(TreasureHapticController(driver))
        private set

    fun beginSession() {
        if (controller.ended) controller = TreasureHapticController(driver)
    }

    fun endSession() {
        controller.endSession()
        cancelVibration()
    }

    fun detach(changingConfigurations: Boolean) {
        if (changingConfigurations) setForeground(false) else endSession()
    }

    fun setForeground(foreground: Boolean) {
        controller.foreground = foreground
        if (!foreground) cancelVibration()
    }

    fun setPreference(preference: Boolean?) {
        controller.enabled = preference == true
        if (preference != true) cancelVibration()
    }

    override fun onCleared() { endSession() }

    companion object {
        fun factory(context: Context) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                val driver = AndroidTreasureHapticDriver(context.applicationContext)
                return TreasureHapticSessionViewModel(driver, driver::cancel) as T
            }
        }
    }
}
