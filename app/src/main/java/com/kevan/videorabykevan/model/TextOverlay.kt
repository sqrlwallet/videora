package com.kevan.videorabykevan.model

import java.util.UUID

/** Entrance (and matching exit) animation for a text overlay or sticker. */
enum class TextAnimation(val title: String) {
    NONE("None"),
    FADE("Fade"),
    POP("Pop"),
    SLIDE_UP("Slide up"),
    TYPEWRITER("Typewriter")
}

/**
 * One moment of a [TextAnimation]. [offsetY] is a fraction of the frame height (down is positive);
 * [visibleChars] = null shows all the text.
 */
data class TextAnimationFrame(
    val alpha: Float = 1f,
    val scale: Float = 1f,
    val offsetY: Float = 0f,
    val visibleChars: Int? = null
) {
    val isStatic: Boolean get() = this == STATIC

    companion object {
        val STATIC = TextAnimationFrame()
    }
}

private const val ANIMATION_IN_MS = 400L
private const val ANIMATION_OUT_MS = 250L
private const val SLIDE_DISTANCE = 0.05f
private const val TYPEWRITER_MS_PER_CHAR = 45L

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
    ),
    STICKER_EMOJI(
        title = "Reaction Sticker",
        defaultTextColorHex = 0xFFFFFFFF,
        defaultBackgroundColorHex = 0x00000000,
        defaultFontSizeSp = 48f
    )
}

data class TextOverlay(
    val id: String = UUID.randomUUID().toString(),
    val text: String = "Viral Hook Text",
    val style: TextStylePreset = TextStylePreset.VIRAL_HOOK,
    val fontSizeSp: Float = style.defaultFontSizeSp,
    val textColorHex: Long = style.defaultTextColorHex,
    val backgroundColorHex: Long = style.defaultBackgroundColorHex,
    val posX: Float = 0.5f, // Normalized 0..1, horizontal center of the overlay
    val posY: Float = 0.3f, // Normalized 0..1, vertical center (upper third = ideal hook position)
    val startTimeMs: Long = 0L,
    val endTimeMs: Long = 5000L,
    val isSticker: Boolean = false,
    val animation: TextAnimation = TextAnimation.NONE,
    // Made by AI auto-captions; re-running them replaces these
    val isAutoCaption: Boolean = false
) {
    /** How the overlay looks [timeMs] into the timeline; same values drive the preview and the export. */
    fun animationFrameAt(timeMs: Long): TextAnimationFrame {
        if (animation == TextAnimation.NONE || timeMs !in startTimeMs..endTimeMs) return TextAnimationFrame.STATIC
        val length = (endTimeMs - startTimeMs).coerceAtLeast(1L)
        val sinceStart = timeMs - startTimeMs
        val untilEnd = endTimeMs - timeMs
        // Short overlays get shorter animations so they're never all transition
        val inMs = ANIMATION_IN_MS.coerceAtMost(length / 3).coerceAtLeast(1L)
        val outMs = ANIMATION_OUT_MS.coerceAtMost(length / 4).coerceAtLeast(1L)
        val inProgress = (sinceStart.toFloat() / inMs).coerceIn(0f, 1f)
        val outProgress = (untilEnd.toFloat() / outMs).coerceIn(0f, 1f)
        return when (animation) {
            TextAnimation.NONE -> TextAnimationFrame.STATIC
            TextAnimation.FADE -> TextAnimationFrame(alpha = minOf(inProgress, outProgress))
            TextAnimation.POP -> {
                // Overshoots to 110% then settles, shrinks slightly as it leaves
                val popIn = if (inProgress < 0.7f) 0.5f + 0.6f * (inProgress / 0.7f) else 1.1f - 0.1f * ((inProgress - 0.7f) / 0.3f)
                val scale = if (outProgress < 1f) 0.8f + 0.2f * outProgress else popIn
                TextAnimationFrame(alpha = minOf((inProgress * 2f).coerceAtMost(1f), outProgress), scale = scale)
            }
            TextAnimation.SLIDE_UP -> {
                val eased = 1f - (1f - inProgress) * (1f - inProgress)
                TextAnimationFrame(alpha = minOf(inProgress, outProgress), offsetY = SLIDE_DISTANCE * (1f - eased))
            }
            TextAnimation.TYPEWRITER -> {
                val typingMs = (text.length * TYPEWRITER_MS_PER_CHAR).coerceAtMost(length / 2).coerceAtLeast(1L)
                val chars = (text.length * (sinceStart.toFloat() / typingMs)).toInt().coerceIn(0, text.length)
                TextAnimationFrame(visibleChars = chars)
            }
        }
    }


    companion object {
        val VIRAL_HOOK_TEMPLATES = listOf(
            "POV: Nobody is talking about this 🤫",
            "Wait until the end... 😱",
            "Stop scrolling right now! 🛑",
            "3 secrets you didn't know 💡",
            "How I did this in 24 hours ⚡",
            "Save this for later! 📌",
            "Part 1: The Untold Story 🔥"
        )

        val TRENDING_STICKERS = listOf(
            "🔥", "💀", "😱", "🚀", "💯", "⚡", "🎯", "👍",
            "🛑", "🤫", "🚨", "✨", "🧠", "🎬", "👀", "📈",
            "❤️", "🙌", "😂", "🤯", "💥", "👑", "🍕", "🎵"
        )
    }
}
