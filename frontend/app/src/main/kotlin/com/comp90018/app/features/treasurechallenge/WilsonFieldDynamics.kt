package com.comp90018.app.features.treasurechallenge

import kotlin.math.floor

internal data class RosetteDrift(val x: Float, val y: Float, val spin: Float, val pulse: Float)

/** Each flower follows its own seeded, smooth random path. The two-minute loop has no jump. */
internal fun rosetteDrift(seed: Int, seconds: Float): RosetteDrift {
    fun noise(channel: Int): Float {
        val channelSeed = seed xor (channel * 0x45d9f3b)
        val steps = 81 + Math.floorMod(channelSeed, 73)
        val cycle = if (seconds.isFinite()) ((seconds % 120f + 120f) % 120f) / 120f else 0f
        val position = cycle * steps
        val sample = floor(position).toInt()
        val fraction = position - sample
        val mix = fraction * fraction * (3f - 2f * fraction)
        fun value(index: Int): Float {
            var hash = channelSeed * 374761393 + Math.floorMod(index, steps) * 668265263
            hash = (hash xor (hash ushr 13)) * 1274126177
            hash = hash xor (hash ushr 16)
            return (hash ushr 1).toFloat() / Int.MAX_VALUE.toFloat() * 2f - 1f
        }
        return value(sample) * (1f - mix) + value(sample + 1) * mix
    }
    return RosetteDrift(noise(1), noise(2), noise(3), noise(4))
}

internal data class RosetteWaterStar(val x: Float, val y: Float, val radius: Float) {
    // These normalized positions gild from the bottom of the full-screen water upwards.
    val illuminationThreshold: Float get() = ((86f - y) / 172f).coerceIn(0f, 1f)
}

internal val RosetteWaterStars = listOf(
    RosetteWaterStar(-38f, -78f, 3.5f), RosetteWaterStar(35f, -76f, 3f),
    RosetteWaterStar(-77f, -40f, 4f), RosetteWaterStar(77f, -36f, 3.5f),
    RosetteWaterStar(-85f, 4f, 3.5f), RosetteWaterStar(83f, 8f, 4f),
    RosetteWaterStar(-73f, 44f, 3f), RosetteWaterStar(72f, 48f, 3.5f),
    RosetteWaterStar(-33f, 79f, 3.5f), RosetteWaterStar(32f, 77f, 3f),
)

internal fun waterStarIlluminated(fill: Float, star: RosetteWaterStar): Boolean =
    fill.isFinite() && fill > 0f && fill >= star.illuminationThreshold
