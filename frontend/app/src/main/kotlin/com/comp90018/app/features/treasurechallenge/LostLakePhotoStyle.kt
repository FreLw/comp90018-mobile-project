package com.comp90018.app.features.treasurechallenge

import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter

// The bundled postcard is already tilted; rotate the captured frame to match, not the artwork again.
internal const val LostLakePostcardTiltDegrees = -7.5f
internal const val LostLakePhotoMorphDurationMillis = 2200

/** The same sepia transform is used for the live texture, frozen frame and photograph in the book. */
internal val LostLakeSepiaMatrix = floatArrayOf(
    .393f, .769f, .189f, 0f, 0f,
    .349f, .686f, .168f, 0f, 0f,
    .272f, .534f, .131f, 0f, 0f,
    0f, 0f, 0f, 1f, 0f,
)

internal fun lostLakePhotoColorFilter() = ColorMatrixColorFilter(ColorMatrix(LostLakeSepiaMatrix))
