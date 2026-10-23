package com.kevan.videorabykevan.model

import java.util.UUID

/** Shortest source span a clip can be trimmed to on the timeline. */
const val MIN_TRIM_MS = 300L

/** Photos and freeze frames are encoded this long, so they can be stretched up to it on the timeline. */
const val STILL_SOURCE_MS = 10_000L
const val PHOTO_DEFAULT_MS = 3_000L
const val FREEZE_DEFAULT_MS = 2_000L

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
    // A photo or freeze frame, stored as a silent still video (no speed / volume tools)
    val isPhoto: Boolean = false,
    val voiceEffect: VoiceEffect = VoiceEffect.NONE,
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
     * The clip after dragging one of its timeline edges by [timelineDeltaMs] (timeline time, so
     * speed is applied). The trim stays inside the source and keeps at least [MIN_TRIM_MS] of it.
     */
    fun trimmedByDrag(isStartEdge: Boolean, timelineDeltaMs: Long): VideoClip {
        val factor = if (speed <= 0.05f) 1.0f else speed
        val sourceDelta = (timelineDeltaMs * factor).toLong()
        val minLength = MIN_TRIM_MS.coerceAtMost(durationMs)
        return if (isStartEdge) {
            copy(trimStartMs = (trimStartMs + sourceDelta).coerceIn(0L, (trimEndMs - minLength).coerceAtLeast(0L)))
        } else {
            copy(trimEndMs = (trimEndMs + sourceDelta).coerceIn((trimStartMs + minLength).coerceAtMost(durationMs), durationMs))
        }
    }

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
