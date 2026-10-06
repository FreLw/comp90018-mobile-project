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
    var teamClaims = ConfirmedTeamClaimHaptics(controller)
        private set

    fun beginSession() {
        if (controller.ended) {
            controller = TreasureHapticController(driver)
            teamClaims = ConfirmedTeamClaimHaptics(controller)
        }
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
        if (!foreground) {
            controller.cancelPlaybackProtection()
            cancelVibration()
        }
    }

    fun setPreference(preference: Boolean?) {
        controller.enabled = preference == true
        if (preference != true) {
            controller.cancelPlaybackProtection()
            cancelVibration()
        }
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
