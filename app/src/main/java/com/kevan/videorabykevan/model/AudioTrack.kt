package com.kevan.videorabykevan.model

import java.util.UUID

data class AudioTrack(
    val id: String = UUID.randomUUID().toString(),
    val uri: String,
    val title: String = "Background Music",
    val durationMs: Long = 0L,
    val timelineStartOffsetMs: Long = 0L,
    val trimStartMs: Long = 0L,
    val trimEndMs: Long = durationMs,
    val volume: Float = 0.8f,
    val isLooping: Boolean = false,
    val isVoiceover: Boolean = false
) {
    val effectiveDurationMs: Long
        get() = (trimEndMs - trimStartMs).coerceAtLeast(0L)
}
