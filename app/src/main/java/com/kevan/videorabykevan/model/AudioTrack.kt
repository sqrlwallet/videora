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
    val isVoiceover: Boolean = false,
    // Linear ramps at the track's start / end on the timeline; 0 = none
    val fadeInMs: Long = 0L,
    val fadeOutMs: Long = 0L
) {
    val effectiveDurationMs: Long
        get() = (trimEndMs - trimStartMs).coerceAtLeast(0L)

    val hasFades: Boolean
        get() = fadeInMs > 0L || fadeOutMs > 0L

    /**
     * How long the track is heard on a timeline of [projectDurationMs]: looping (or unknown
     * length) tracks run to the end of the video, others stop at their own end.
     */
    fun timelineLengthMs(projectDurationMs: Long): Long {
        val available = (projectDurationMs - timelineStartOffsetMs.coerceAtLeast(0L)).coerceAtLeast(0L)
        val knownLength = effectiveDurationMs.takeIf { trimEndMs > trimStartMs }
        return if (isLooping || knownLength == null) available else minOf(knownLength, available)
    }

    /**
     * Fade multiplier (0..1) at [positionMs] into the track, for a track heard for [lengthMs].
     * Shared by the preview and the export so both fade identically. Fades longer than half the
     * track are shortened so the in and out ramps never overlap past the middle.
     */
    fun fadeGainAt(positionMs: Long, lengthMs: Long): Float {
        if (!hasFades || lengthMs <= 0L) return 1f
        val half = lengthMs / 2
        val fadeIn = fadeInMs.coerceIn(0L, half)
        val fadeOut = fadeOutMs.coerceIn(0L, half)
        var gain = 1f
        if (fadeIn > 0L && positionMs < fadeIn) {
            gain = minOf(gain, positionMs.coerceAtLeast(0L).toFloat() / fadeIn)
        }
        val remaining = lengthMs - positionMs
        if (fadeOut > 0L && remaining < fadeOut) {
            gain = minOf(gain, remaining.coerceAtLeast(0L).toFloat() / fadeOut)
        }
        return gain
    }
}
