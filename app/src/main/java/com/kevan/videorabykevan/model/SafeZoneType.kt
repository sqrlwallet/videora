package com.kevan.videorabykevan.model

enum class SafeZoneType(
    val title: String,
    val badge: String,
    val description: String
) {
    NONE(
        title = "Clean View",
        badge = "Off",
        description = "No overlay guides"
    ),
    TIKTOK(
        title = "TikTok Guide",
        badge = "TikTok",
        description = "Simulates TikTok right buttons, sound disc & bottom caption"
    ),
    INSTAGRAM_REELS(
        title = "Instagram Reels",
        badge = "Reels",
        description = "Simulates Reels right actions & bottom account profile"
    ),
    YOUTUBE_SHORTS(
        title = "YouTube Shorts",
        badge = "Shorts",
        description = "Simulates Shorts right action icons & bottom title bar"
    )
}
