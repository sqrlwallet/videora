package com.kevan.videorabykevan.model

import java.util.UUID

data class VideoClip(
    val id: String = UUID.randomUUID().toString(),
    val uri: String,
    val name: String = "Clip",
    val durationMs: Long = 0L,
    val trimStartMs: Long = 0L,
    val trimEndMs: Long = durationMs,
    val speed: Float = 1.0f,
    val volume: Float = 1.0f,
    val isMuted: Boolean = false,
    val filter: VideoFilter = VideoFilter.NONE,
    val rotationDegrees: Int = 0,
    val isFlippedHorizontal: Boolean = false,
    // Color correction, each -1..1 with 0 = unchanged (applied after the filter)
    val exposure: Float = 0f,
    val brightness: Float = 0f,
    val contrast: Float = 0f,
    val temperature: Float = 0f,
    val tint: Float = 0f,
    val saturation: Float = 0f,
    val hue: Float = 0f
) {
    val hasColorAdjustments: Boolean
        get() = exposure != 0f || brightness != 0f || contrast != 0f ||
            temperature != 0f || tint != 0f || saturation != 0f || hue != 0f

    /** Same clip with every color correction value reset (the filter is kept). */
    fun withoutColorAdjustments(): VideoClip =
        copy(exposure = 0f, brightness = 0f, contrast = 0f, temperature = 0f, tint = 0f, saturation = 0f, hue = 0f)

    /** Copies this clip's filter and color correction onto [other]. */
    fun copyColorTo(other: VideoClip): VideoClip = other.copy(
        filter = filter, exposure = exposure, brightness = brightness, contrast = contrast,
        temperature = temperature, tint = tint, saturation = saturation, hue = hue
    )

    /**
     * The trimmed source length in milliseconds before speed adjustment.
     */
    val trimmedDurationMs: Long
        get() = (trimEndMs - trimStartMs).coerceAtLeast(0L)

    /**
     * Duration of this clip as it plays in the timeline after speed factor is applied.
     */
    val playbackDurationMs: Long
        get() {
            val trimmed = trimmedDurationMs
            val factor = if (speed <= 0.05f) 1.0f else speed
            return (trimmed / factor).toLong().coerceAtLeast(100L)
        }
}
