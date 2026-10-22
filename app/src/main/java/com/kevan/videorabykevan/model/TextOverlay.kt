package com.kevan.videorabykevan.model

import java.util.UUID

enum class TextStylePreset(
    val title: String,
    val defaultTextColorHex: Long,
    val defaultBackgroundColorHex: Long,
    val defaultFontSizeSp: Float
) {
    VIRAL_HOOK(
        title = "TikTok Yellow Hook",
        defaultTextColorHex = 0xFFFFDE03,
        defaultBackgroundColorHex = 0xEE121212,
        defaultFontSizeSp = 24f
    ),
    BOLD_WHITE_PILL(
        title = "Modern Pill",
        defaultTextColorHex = 0xFFFFFFFF,
        defaultBackgroundColorHex = 0xDD000000,
        defaultFontSizeSp = 22f
    ),
    NEON_CYAN(
        title = "Neon Glow",
        defaultTextColorHex = 0xFF00F2FE,
        defaultBackgroundColorHex = 0xAA0A0E27,
        defaultFontSizeSp = 24f
    ),
    MEME_IMPACT(
        title = "Meme Impact",
        defaultTextColorHex = 0xFFFFFFFF,
        defaultBackgroundColorHex = 0xFF000000,
        defaultFontSizeSp = 28f
    ),
    CLEAN_SUBTITLE(
        title = "Subtitle Clean",
        defaultTextColorHex = 0xFFFFFFFF,
        defaultBackgroundColorHex = 0x88000000,
        defaultFontSizeSp = 18f
    ),
    MINIMAL_ACCENT(
        title = "Minimal Pink",
        defaultTextColorHex = 0xFFFF2A85,
        defaultBackgroundColorHex = 0xEE18181B,
        defaultFontSizeSp = 22f
    )
}

data class TextOverlay(
    val id: String = UUID.randomUUID().toString(),
    val text: String = "Viral Hook Text",
    val style: TextStylePreset = TextStylePreset.VIRAL_HOOK,
    val fontSizeSp: Float = style.defaultFontSizeSp,
    val textColorHex: Long = style.defaultTextColorHex,
    val backgroundColorHex: Long = style.defaultBackgroundColorHex,
    val posX: Float = 0.5f, // Normalized 0..1 (center)
    val posY: Float = 0.3f, // Normalized 0..1 (upper-third ideal hook position)
    val startTimeMs: Long = 0L,
    val endTimeMs: Long = 5000L
)
