package com.kevan.videorabykevan.model

/** Music and sound effects drop to this level while a voiceover (or Read aloud) is playing. */
const val DUCK_LEVEL = 0.3f
/** Length of the ramp down before a voiceover and back up after it. */
const val DUCK_RAMP_MS = 300L

/** Where each voiceover is heard on a timeline of [projectDurationMs]. */
fun voiceoverSpans(tracks: List<AudioTrack>, projectDurationMs: Long): List<LongRange> =
    tracks.filter { it.isVoiceover && it.volume > 0f }.map { track ->
        val start = track.timelineStartOffsetMs.coerceAtLeast(0L)
        start until start + track.timelineLengthMs(projectDurationMs)
    }.filterNot { it.isEmpty() }

/**
 * Gain (DUCK_LEVEL..1) for music at [timelineMs] given the voiceover [spans]: fully ducked inside
 * a span, ramping linearly over [DUCK_RAMP_MS] on either side. Shared by the preview and the export.
 */
fun duckGainAt(timelineMs: Long, spans: List<LongRange>): Float {
    var depth = 0f
    for (span in spans) {
        val distance = when {
            timelineMs in span -> 0L
            timelineMs < span.first -> span.first - timelineMs
            else -> timelineMs - span.last
        }
        if (distance < DUCK_RAMP_MS) depth = maxOf(depth, 1f - distance.toFloat() / DUCK_RAMP_MS)
    }
    return 1f - (1f - DUCK_LEVEL) * depth
}
