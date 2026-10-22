package com.kevan.videorabykevan.model

enum class AspectRatioPreset(
    val label: String,
    val description: String,
    val widthRatio: Float,
    val heightRatio: Float,
    val targetWidth: Int,
    val targetHeight: Int
) {
    TIKTOK_REELS_SHORTS_9_16(
        label = "9:16",
        description = "TikTok, Shorts & Reels",
        widthRatio = 9f,
        heightRatio = 16f,
        targetWidth = 1080,
        targetHeight = 1920
    ),
    SQUARE_1_1(
        label = "1:1",
        description = "Instagram Post",
        widthRatio = 1f,
        heightRatio = 1f,
        targetWidth = 1080,
        targetHeight = 1080
    ),
    PORTRAIT_4_5(
        label = "4:5",
        description = "Instagram Portrait",
        widthRatio = 4f,
        heightRatio = 5f,
        targetWidth = 1080,
        targetHeight = 1350
    ),
    LANDSCAPE_16_9(
        label = "16:9",
        description = "YouTube Landscape",
        widthRatio = 16f,
        heightRatio = 9f,
        targetWidth = 1920,
        targetHeight = 1080
    );

    val aspectRatio: Float
        get() = widthRatio / heightRatio
}
