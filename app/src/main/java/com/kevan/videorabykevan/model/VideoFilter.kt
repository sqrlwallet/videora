package com.kevan.videorabykevan.model

enum class VideoFilter(
    val displayName: String,
    val description: String,
    val redScale: Float = 1.0f,
    val greenScale: Float = 1.0f,
    val blueScale: Float = 1.0f,
    val contrast: Float = 0.0f // -1f to 1f in Media3 Contrast effect
) {
    NONE(
        displayName = "Original",
        description = "Natural colors"
    ),
    VIBRANT(
        displayName = "Vibrant Pop",
        description = "Punchy colors for TikTok feeds",
        redScale = 1.15f,
        greenScale = 1.15f,
        blueScale = 1.1f,
        contrast = 0.15f
    ),
    WARM_SUNSET(
        displayName = "Golden Sunset",
        description = "Warm golden lifestyle look",
        redScale = 1.25f,
        greenScale = 1.05f,
        blueScale = 0.85f,
        contrast = 0.05f
    ),
    COOL_CINEMATIC(
        displayName = "Cinematic Teal",
        description = "Moody cool cinematic tones",
        redScale = 0.85f,
        greenScale = 1.05f,
        blueScale = 1.2f,
        contrast = 0.10f
    ),
    VINTAGE_FILM(
        displayName = "Retro Film",
        description = "Nostalgic warm film feel",
        redScale = 1.1f,
        greenScale = 1.0f,
        blueScale = 0.8f,
        contrast = -0.05f
    ),
    CYBERPUNK(
        displayName = "Cyberpunk",
        description = "High-contrast neon styling",
        redScale = 1.2f,
        greenScale = 0.85f,
        blueScale = 1.25f,
        contrast = 0.25f
    ),
    MONOCHROME(
        displayName = "Noir B&W",
        description = "Classic dramatic black & white",
        redScale = 0.9f,
        greenScale = 0.9f,
        blueScale = 0.9f,
        contrast = 0.2f
    )
}
