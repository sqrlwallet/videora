package com.kevan.videorabykevan.model

enum class ExportPreset(
    val title: String,
    val badge: String,
    val description: String,
    val targetWidth: Int,
    val targetHeight: Int,
    val targetFps: Int,
    val videoBitrateBps: Int,
    val audioBitrateBps: Int = 192_000
) {
    TIKTOK_OPTIMIZED(
        title = "TikTok Recommended",
        badge = "TikTok",
        description = "1080x1920 @ 30fps • High bitrate H.264",
        targetWidth = 1080,
        targetHeight = 1920,
        targetFps = 30,
        videoBitrateBps = 14_000_000
    ),
    SHORTS_MAX_QUALITY(
        title = "YouTube Shorts HQ",
        badge = "Shorts",
        description = "1080x1920 @ 60fps • Ultra smooth playback",
        targetWidth = 1080,
        targetHeight = 1920,
        targetFps = 60,
        videoBitrateBps = 18_000_000
    ),
    REELS_BALANCED(
        title = "Instagram Reels",
        badge = "Reels",
        description = "1080x1920 @ 30fps • Optimal IG compression",
        targetWidth = 1080,
        targetHeight = 1920,
        targetFps = 30,
        videoBitrateBps = 12_000_000
    ),
    FAST_DRAFT_720P(
        title = "Quick Draft (720p)",
        badge = "Draft",
        description = "720x1280 @ 30fps • Fastest local export",
        targetWidth = 720,
        targetHeight = 1280,
        targetFps = 30,
        videoBitrateBps = 6_000_000
    )
}
