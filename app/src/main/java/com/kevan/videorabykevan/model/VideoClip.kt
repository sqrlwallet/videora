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
    val rotationDegrees: Int = 0
) {
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
