package com.comp90018.app.sensors.camera

import android.net.Uri
import androidx.camera.view.PreviewView
import androidx.lifecycle.LifecycleOwner

/**
 * Data boundary for reading photo captures and QR codes from the device camera. [onQrCodeDetected]
 * fires continuously while a QR code is in frame; [capturePhoto] takes a single still photo.
 */
interface CameraCapture {
    fun bindPreview(
        lifecycleOwner: LifecycleOwner,
        previewView: PreviewView,
        onQrCodeDetected: (String) -> Unit,
        onError: (String) -> Unit,
    )
    fun capturePhoto(onResult: (Uri?, String?) -> Unit)
    fun unbind()
}
