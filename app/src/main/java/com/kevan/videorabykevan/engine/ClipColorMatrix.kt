package com.kevan.videorabykevan.engine

import android.graphics.ColorMatrix
import androidx.media3.effect.RgbMatrix
import com.kevan.videorabykevan.model.VideoClip
import com.kevan.videorabykevan.model.VideoFilter
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin

/**
 * The single source of truth for a clip's color: filter look + color correction, as one 4x5
 * color matrix (Android [ColorMatrix] layout: row-major, offsets in 0..255).
 *
 * The preview applies it to the TextureView and the export converts it into a Media3
 * [RgbMatrix], so what you see while editing is exactly what gets exported.
 */
object ClipColorMatrix {

    // Rec. 709 luma weights, as used by Android's ColorMatrix.setSaturation
    private const val LUMA_R = 0.213f
    private const val LUMA_G = 0.715f
    private const val LUMA_B = 0.072f

    /** Max exposure change at the ends of the slider, in stops. */
    const val MAX_EXPOSURE_STOPS = 2f
    /** Max hue rotation at the ends of the slider, in degrees. */
    const val MAX_HUE_DEGREES = 180f

    /** Returns null when the clip has no filter and no correction (nothing to apply). */
    fun matrixFor(clip: VideoClip, includeColor: Boolean = true): FloatArray? {
        if (!includeColor || (clip.filter == VideoFilter.NONE && !clip.hasColorAdjustments)) return null

        var m = identity()
        // 1. Filter look
        when (clip.filter) {
            VideoFilter.NONE -> {}
            VideoFilter.MONOCHROME -> m = m.then(saturation(0f)).then(contrast(clip.filter.contrast))
            else -> m = m
                .then(scale(clip.filter.redScale, clip.filter.greenScale, clip.filter.blueScale))
                .then(contrast(clip.filter.contrast))
        }
        // 2. Color correction, in the order a colorist would: light, then white balance, then color
        if (clip.exposure != 0f) {
            val gain = 2f.pow(clip.exposure * MAX_EXPOSURE_STOPS)
            m = m.then(scale(gain, gain, gain))
        }
        if (clip.temperature != 0f) {
            // Warmer = more red / less blue
            m = m.then(scale(1f + 0.25f * clip.temperature, 1f, 1f - 0.25f * clip.temperature))
        }
        if (clip.tint != 0f) {
            // Positive = magenta (less green), negative = green
            m = m.then(scale(1f + 0.08f * clip.tint, 1f - 0.2f * clip.tint, 1f + 0.08f * clip.tint))
        }
        if (clip.brightness != 0f) m = m.then(offset(clip.brightness * 255f))
        if (clip.contrast != 0f) m = m.then(contrast(clip.contrast))
        if (clip.saturation != 0f) m = m.then(saturation(1f + clip.saturation))
        if (clip.hue != 0f) m = m.then(hueRotation(clip.hue * MAX_HUE_DEGREES))
        return m
    }

    fun colorMatrixFor(clip: VideoClip, includeColor: Boolean = true): ColorMatrix? =
        matrixFor(clip, includeColor)?.let { ColorMatrix(it) }

    /** Media3 effect applying the same matrix during export; null when there's nothing to apply. */
    fun exportEffectFor(clip: VideoClip): RgbMatrix? {
        val matrix = matrixFor(clip) ?: return null
        val media3Matrix = toMedia3Matrix(matrix)
        return RgbMatrix { _, _ -> media3Matrix }
    }

    /**
     * Converts a 4x5 row-major matrix with 0..255 offsets into Media3's 4x4 column-major RGB
     * matrix with 0..1 offsets (offsets live in the 4th column, applied via alpha = 1).
     */
    fun toMedia3Matrix(m: FloatArray): FloatArray {
        val out = FloatArray(16)
        for (row in 0..2) {
            for (col in 0..2) {
                out[col * 4 + row] = m[row * 5 + col]
            }
            out[12 + row] = m[row * 5 + 4] / 255f
        }
        out[15] = 1f
        return out
    }

    /** Media3's contrast curve: (1 + c) / (1 - c) around mid-gray. */
    fun contrastFactor(contrast: Float): Float = (1f + contrast) / (1.0001f - contrast)

    fun identity(): FloatArray = floatArrayOf(
        1f, 0f, 0f, 0f, 0f,
        0f, 1f, 0f, 0f, 0f,
        0f, 0f, 1f, 0f, 0f,
        0f, 0f, 0f, 1f, 0f
    )

    /** Applies this matrix, then [next] (same as ColorMatrix.postConcat). */
    fun FloatArray.then(next: FloatArray): FloatArray {
        val out = FloatArray(20)
        for (row in 0..3) {
            for (col in 0..4) {
                var sum = if (col == 4) next[row * 5 + 4] else 0f
                for (k in 0..3) {
                    sum += next[row * 5 + k] * this[k * 5 + col]
                }
                out[row * 5 + col] = sum
            }
        }
        return out
    }

    /** Applies the matrix to an RGB color in 0..255 (no clamping), for tests. */
    fun FloatArray.transform(r: Float, g: Float, b: Float): FloatArray = FloatArray(3) { row ->
        this[row * 5] * r + this[row * 5 + 1] * g + this[row * 5 + 2] * b + this[row * 5 + 3] * 255f + this[row * 5 + 4]
    }

    private fun scale(r: Float, g: Float, b: Float) = floatArrayOf(
        r, 0f, 0f, 0f, 0f,
        0f, g, 0f, 0f, 0f,
        0f, 0f, b, 0f, 0f,
        0f, 0f, 0f, 1f, 0f
    )

    private fun offset(value: Float) = floatArrayOf(
        1f, 0f, 0f, 0f, value,
        0f, 1f, 0f, 0f, value,
        0f, 0f, 1f, 0f, value,
        0f, 0f, 0f, 1f, 0f
    )

    private fun contrast(contrast: Float): FloatArray {
        val f = contrastFactor(contrast)
        val o = 127.5f * (1f - f)
        return floatArrayOf(
            f, 0f, 0f, 0f, o,
            0f, f, 0f, 0f, o,
            0f, 0f, f, 0f, o,
            0f, 0f, 0f, 1f, 0f
        )
    }

    private fun saturation(s: Float): FloatArray {
        val inv = 1f - s
        val r = LUMA_R * inv
        val g = LUMA_G * inv
        val b = LUMA_B * inv
        return floatArrayOf(
            r + s, g, b, 0f, 0f,
            r, g + s, b, 0f, 0f,
            r, g, b + s, 0f, 0f,
            0f, 0f, 0f, 1f, 0f
        )
    }

    /** Luminance-preserving hue rotation (same formula as the CSS hue-rotate filter). */
    private fun hueRotation(degrees: Float): FloatArray {
        val rad = Math.toRadians(degrees.toDouble())
        val c = cos(rad).toFloat()
        val s = sin(rad).toFloat()
        return floatArrayOf(
            LUMA_R + c * (1 - LUMA_R) - s * LUMA_R, LUMA_G - c * LUMA_G - s * LUMA_G, LUMA_B - c * LUMA_B + s * (1 - LUMA_B), 0f, 0f,
            LUMA_R - c * LUMA_R + s * 0.143f, LUMA_G + c * (1 - LUMA_G) + s * 0.140f, LUMA_B - c * LUMA_B - s * 0.283f, 0f, 0f,
            LUMA_R - c * LUMA_R - s * (1 - LUMA_R), LUMA_G - c * LUMA_G + s * LUMA_G, LUMA_B + c * (1 - LUMA_B) + s * LUMA_B, 0f, 0f,
            0f, 0f, 0f, 1f, 0f
        )
    }
}
