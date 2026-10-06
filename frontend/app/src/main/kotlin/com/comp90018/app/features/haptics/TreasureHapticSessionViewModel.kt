package com.comp90018.app.features.haptics

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

/** Activity-retained, keyed by explorer in AppShell; survives map/detail and configuration changes. */
class TreasureHapticSessionViewModel(context: Context) : ViewModel() {
    private val driver = AndroidTreasureHapticDriver(context)
    val controller = TreasureHapticController(driver)

    fun setForeground(foreground: Boolean) {
        controller.foreground = foreground
        if (!foreground) driver.cancel()
    }

    fun setEnabled(enabled: Boolean) {
        controller.enabled = enabled
        if (!enabled) driver.cancel()
    }

    override fun onCleared() { setForeground(false) }

    companion object {
        fun factory(context: Context) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                TreasureHapticSessionViewModel(context.applicationContext) as T
        }
    }
}
