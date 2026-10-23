package com.kevan.videorabykevan.model

/**
 * TikTok-style voice effects. Each is a pitch shift that keeps the timing, done with Sonic in both
 * the preview (ExoPlayer PlaybackParameters) and the export (SonicAudioProcessor).
 */
enum class VoiceEffect(val title: String, val emoji: String, val pitch: Float) {
    NONE("Original", "🎙️", 1.0f),
    CHIPMUNK("Chipmunk", "🐿️", 1.7f),
    HIGHER("Higher", "🎈", 1.3f),
    DEEP("Deep", "🎸", 0.78f),
    GIANT("Giant", "🗿", 0.6f)
}
