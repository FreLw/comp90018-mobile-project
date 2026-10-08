package com.comp90018.app.features.treasurechallenge

import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import androidx.annotation.DrawableRes
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import com.comp90018.app.R
import com.comp90018.app.contextengine.challenge.RelicChallengeType

internal const val QuestPhotoMorphDurationMillis = 2200
internal const val QuestPhotoRevealDurationMillis = 1600
internal val QuestPhotoRevealPaper = Color(0xFFF5E4B8)

/** The artwork rectangle, relative to the full page, survives the quest-to-discovery handoff. */
data class PhotoRevealArrival(val artworkBounds: Rect, @param:DrawableRes val artworkResId: Int)

internal data class QuestPhotoStyle(
    val tagPrefix: String,
    val triggerName: String,
    val sceneName: String,
    @param:DrawableRes val artworkResId: Int,
    val artworkDescription: String,
    val blendingDescription: String,
    val revealedDescription: String,
    val frameTiltDegrees: Float,
    val frameWidthFraction: Float,
    val frameHeightFraction: Float,
    val frameOffsetXFraction: Float = 0f,
    val frameOffsetYFraction: Float = 0f,
) {
    fun tag(suffix: String) = "${tagPrefix}_$suffix"
}

private val lakePhotoStyle = QuestPhotoStyle(
    tagPrefix = "union", triggerName = "camera", sceneName = "the lost lake",
    artworkResId = R.drawable.treasure_postcard, artworkDescription = "The lost lake postcard",
    blendingDescription = "Photograph blending into the lake postcard", revealedDescription = "Lake postcard revealed",
    // The bundled postcard is already tilted; rotate the captured frame, not the artwork again.
    frameTiltDegrees = -7.5f, frameWidthFraction = .69f, frameHeightFraction = .56f,
    frameOffsetXFraction = .008f, frameOffsetYFraction = .046f,
)
private val gardenPhotoStyle = QuestPhotoStyle(
    tagPrefix = "garden", triggerName = "glasshouse", sceneName = "the lost glasshouse",
    artworkResId = R.drawable.treasure_glasshouse, artworkDescription = "The lost glasshouse",
    blendingDescription = "Photograph blending into the glasshouse", revealedDescription = "Glasshouse revealed",
    frameTiltDegrees = 0f, frameWidthFraction = .84f, frameHeightFraction = .78f,
)

internal fun RelicChallengeType.photoRevealStyle(): QuestPhotoStyle? = when (this) {
    RelicChallengeType.UNION_LAWN_PHOTO -> lakePhotoStyle
    RelicChallengeType.SYSTEM_GARDEN_GLASSHOUSE -> gardenPhotoStyle
    else -> null
}

/** The same sepia transform is used for the live texture, frozen frame and photograph in the book. */
internal val QuestPhotoSepiaMatrix = floatArrayOf(
    .393f, .769f, .189f, 0f, 0f,
    .349f, .686f, .168f, 0f, 0f,
    .272f, .534f, .131f, 0f, 0f,
    0f, 0f, 0f, 1f, 0f,
)

internal fun questPhotoColorFilter() = ColorMatrixColorFilter(ColorMatrix(QuestPhotoSepiaMatrix))
