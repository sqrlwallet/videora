package com.kevan.videorabykevan.model

import java.util.UUID

data class Project(
    val id: String = UUID.randomUUID().toString(),
    val title: String = "Untitled Short",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val aspectRatio: AspectRatioPreset = AspectRatioPreset.TIKTOK_REELS_SHORTS_9_16,
    val safeZone: SafeZoneType = SafeZoneType.TIKTOK,
    val clips: List<VideoClip> = emptyList(),
    val audioTracks: List<AudioTrack> = emptyList(),
    val textOverlays: List<TextOverlay> = emptyList(),
    val thumbnailUri: String? = null
) {
    val totalDurationMs: Long
        get() = clips.sumOf { it.playbackDurationMs }

    val clipCount: Int
        get() = clips.size

    val formattedDuration: String
        get() {
            val totalSec = totalDurationMs / 1000
            val minutes = totalSec / 60
            val seconds = totalSec % 60
            val millis = (totalDurationMs % 1000) / 100
            return String.format("%02d:%02d.%d", minutes, seconds, millis)
        }
}
