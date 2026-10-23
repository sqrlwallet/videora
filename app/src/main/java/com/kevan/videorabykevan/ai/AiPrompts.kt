package com.kevan.videorabykevan.ai

import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.kevan.videorabykevan.model.Project
import com.kevan.videorabykevan.model.TextOverlay
import com.kevan.videorabykevan.model.TextStylePreset
import com.kevan.videorabykevan.model.VideoClip

// Prompts for every AI tool and the parsers for their replies. Pure Kotlin so both editions unit
// test it. Replies are asked for as plain JSON (not response_format, which fails outright on models
// without structured-output support) and parsed leniently: code fences and chatter are tolerated.

/** What the prompts know about the project. */
data class AiProjectContext(
    val title: String,
    val durationMs: Long,
    val clipCount: Int,
    val onScreenText: List<String>
)

/** Ways to rewrite a text overlay. */
enum class RewriteStyle(val title: String, val instruction: String) {
    PUNCHIER("Punchier", "Make it more attention-grabbing and punchy, like a viral hook."),
    SHORTER("Shorter", "Make it noticeably shorter while keeping the meaning."),
    FIX("Fix spelling", "Fix spelling, grammar and punctuation only. Keep the wording otherwise."),
    EMOJIS("Add emojis", "Keep the wording and add one or two fitting emojis."),
    TRANSLATE("Translate", "Translate it into the requested language, keeping the tone.")
}

data class PostCaption(val caption: String, val hashtags: List<String>) {
    /** Caption and hashtags as one block, ready to paste into TikTok / Instagram. */
    val fullText: String get() = if (hashtags.isEmpty()) caption else "$caption\n\n${hashtags.joinToString(" ")}"
}

/** Suggested color correction; each value -1..1 like [VideoClip]'s controls. */
data class ColorSuggestion(
    val exposure: Float,
    val brightness: Float,
    val contrast: Float,
    val temperature: Float,
    val tint: Float,
    val saturation: Float,
    val hue: Float,
    val reason: String
) {
    fun applyTo(clip: VideoClip): VideoClip = clip.copy(
        exposure = exposure, brightness = brightness, contrast = contrast,
        temperature = temperature, tint = tint, saturation = saturation, hue = hue
    )
}

/** A transcribed stretch of speech, in ms from the start of whatever audio it describes. */
data class TranscriptSegment(val startMs: Long, val endMs: Long, val text: String)

/** One request's worth of audio: [sourceStartMs] into [uri], placed on the timeline by [piece]. */
data class SpeechPiece(val uri: String, val sourceStartMs: Long, val piece: AudioPiece)

/**
 * A piece of audio sent for transcription: it starts [sourceOffsetMs] into a clip / track whose
 * trimmed start sits at [timelineStartMs], played at [speed]. Lets segments be mapped to the timeline.
 */
data class AudioPiece(
    val timelineStartMs: Long,
    val sourceOffsetMs: Long,
    val lengthMs: Long,
    val speed: Float
)

object AiPrompts {

    const val SYSTEM = "You are the creative assistant inside Videora, a phone video editor for short " +
        "vertical videos (TikTok, Instagram Reels, YouTube Shorts). Reply with a single JSON object only: " +
        "no markdown, no code fences, no commentary."

    private fun describe(context: AiProjectContext): String = buildString {
        append("Project title: \"${context.title}\". ")
        append("Length: ${context.durationMs / 1000} seconds over ${context.clipCount} clip(s). ")
        if (context.onScreenText.isNotEmpty()) {
            append("Text already on screen: ${context.onScreenText.joinToString("; ") { "\"$it\"" }}. ")
        }
    }

    fun hooks(context: AiProjectContext, topic: String, withFrames: Boolean): String = buildString {
        append(describe(context))
        if (topic.isNotBlank()) append("The video is about: $topic. ")
        if (withFrames) append("The attached images are frames from the video, in order. ")
        append("Write 6 different on-screen hooks for the first seconds of the video, each at most 9 words, ")
        append("varied in style (question, bold claim, POV, curiosity gap, number list, challenge). ")
        append("""Return {"hooks": ["...", "..."]}.""")
    }

    fun postCaption(context: AiProjectContext, topic: String, withFrames: Boolean): String = buildString {
        append(describe(context))
        if (topic.isNotBlank()) append("The video is about: $topic. ")
        if (withFrames) append("The attached images are frames from the video, in order. ")
        append("Write a post caption for TikTok / Reels (1 to 3 short sentences, natural, may use an emoji) ")
        append("and 5 to 8 relevant hashtags mixing broad and niche ones. ")
        append("""Return {"caption": "...", "hashtags": ["#one", "#two"]}.""")
    }

    fun rewrite(text: String, style: RewriteStyle, language: String): String = buildString {
        append("Rewrite this on-screen text from a short video: \"$text\". ")
        append(style.instruction)
        if (style == RewriteStyle.TRANSLATE) append(" Target language: ${language.ifBlank { "English" }}.")
        append(" It must stay short enough to read on a phone screen. Give 3 alternatives. ")
        append("""Return {"options": ["...", "...", "..."]}.""")
    }

    fun translateAll(texts: List<String>, language: String): String = buildString {
        append("Translate each on-screen text from a short video into ${language.ifBlank { "English" }}, ")
        append("keeping emojis, tone and roughly the same length. Keep the order and the number of items. ")
        append("Texts: ")
        append(JsonArray().apply { texts.forEach { add(it) } }.toString())
        append(""". Return {"translations": ["...", "..."]}.""")
    }

    fun titles(context: AiProjectContext, topic: String): String = buildString {
        append(describe(context))
        if (topic.isNotBlank()) append("The video is about: $topic. ")
        append("Suggest 5 short project names (at most 5 words each). ")
        append("""Return {"titles": ["...", "..."]}.""")
    }

    fun script(context: AiProjectContext, topic: String, lineCount: Int): String = buildString {
        append(describe(context))
        append("Write a text story for this video about: ${topic.ifBlank { context.title }}. ")
        append("Exactly $lineCount short lines (at most 10 words each) that will appear on screen one after ")
        append("another, building to a payoff in the last line. ")
        append("""Return {"lines": ["...", "..."]}.""")
    }

    fun color(): String =
        "The image is a frame from one clip of a short vertical video. Suggest a subtle, flattering color " +
            "correction for it. Each value is between -1 and 1 with 0 meaning unchanged: exposure, brightness, " +
            "contrast, temperature (positive = warmer), tint (positive = magenta), saturation, hue. Keep most " +
            "values within -0.4..0.4 and leave hue at 0 unless the colors are clearly off. " +
            """Return {"exposure": 0, "brightness": 0, "contrast": 0, "temperature": 0, "tint": 0, """ +
            """"saturation": 0, "hue": 0, "reason": "one short sentence"}."""

    fun transcribe(): String =
        "Transcribe the speech in this audio for on-screen captions. Split it into segments of at most 6 words " +
            "that follow natural pauses. Times are seconds from the start of this audio. Use the spoken " +
            "language; don't translate. If there's no speech, return an empty list. " +
            """Return {"segments": [{"start": 0.0, "end": 1.2, "text": "..."}]}."""

    // ---------------------------------------------------------------
    // Reply parsing
    // ---------------------------------------------------------------

    /** The first JSON object in [reply], ignoring code fences or text around it. */
    fun extractJson(reply: String): JsonObject {
        val start = reply.indexOf('{')
        val end = reply.lastIndexOf('}')
        if (start < 0 || end <= start) throw AiError.BadResponse()
        val json = try {
            JsonParser.parseString(reply.substring(start, end + 1))
        } catch (e: Exception) {
            throw AiError.BadResponse()
        }
        return json.takeIf { it.isJsonObject }?.asJsonObject ?: throw AiError.BadResponse()
    }

    /** The non-blank strings in the [key] array of [reply]. */
    fun parseStringList(reply: String, key: String): List<String> =
        extractJson(reply).array(key)
            ?.mapNotNull { it.takeIf { e -> e.isJsonPrimitive }?.asString?.trim() }
            ?.filter { it.isNotEmpty() }
            ?.takeIf { it.isNotEmpty() }
            ?: throw AiError.BadResponse()

    fun parsePostCaption(reply: String): PostCaption {
        val json = extractJson(reply)
        val caption = json.get("caption")?.takeIf { it.isJsonPrimitive }?.asString?.trim()
            ?.takeIf { it.isNotEmpty() } ?: throw AiError.BadResponse()
        val hashtags = json.array("hashtags")
            ?.mapNotNull { it.takeIf { e -> e.isJsonPrimitive }?.asString?.trim() }
            ?.filter { it.isNotEmpty() }
            ?.map { tag -> "#" + tag.trimStart('#').replace(" ", "") }
            .orEmpty()
        return PostCaption(caption, hashtags)
    }

    /** Translations must line up one-to-one with the texts sent, or they can't be applied safely. */
    fun parseTranslations(reply: String, expectedCount: Int): List<String> =
        parseStringList(reply, "translations").takeIf { it.size == expectedCount } ?: throw AiError.BadResponse()

    fun parseColor(reply: String): ColorSuggestion {
        val json = extractJson(reply)
        fun value(key: String): Float =
            json.get(key)?.takeIf { it.isJsonPrimitive }?.let { runCatching { it.asFloat }.getOrNull() }
                ?.takeIf { it.isFinite() }?.coerceIn(-1f, 1f) ?: 0f
        return ColorSuggestion(
            exposure = value("exposure"),
            brightness = value("brightness"),
            contrast = value("contrast"),
            temperature = value("temperature"),
            tint = value("tint"),
            saturation = value("saturation"),
            hue = value("hue"),
            reason = json.get("reason")?.takeIf { it.isJsonPrimitive }?.asString?.trim().orEmpty()
        )
    }

    fun parseTranscript(reply: String): List<TranscriptSegment> {
        val segments = extractJson(reply).array("segments") ?: throw AiError.BadResponse()
        return segments.mapNotNull { element ->
            val segment = element.takeIf { it.isJsonObject }?.asJsonObject ?: return@mapNotNull null
            val text = segment.get("text")?.takeIf { it.isJsonPrimitive }?.asString?.trim()
                ?.takeIf { it.isNotEmpty() } ?: return@mapNotNull null
            val start = segment.seconds("start") ?: return@mapNotNull null
            val end = segment.seconds("end")?.takeIf { it > start } ?: (start + 1.5)
            TranscriptSegment((start * 1000).toLong(), (end * 1000).toLong(), text)
        }.sortedBy { it.startMs }
    }

    // ---------------------------------------------------------------
    // Turning results into overlays
    // ---------------------------------------------------------------

    /**
     * Everything with speech worth captioning, cut into pieces of at most [maxPieceMs] of source:
     * clips with sound (not muted, not photos) and voiceovers. Music is left out (lyrics aren't captions).
     * Stops at [maxTotalMs] of audio so a long project can't run up a surprise bill.
     */
    fun speechPieces(project: Project, maxPieceMs: Long = 60_000L, maxTotalMs: Long = 10 * 60_000L): List<SpeechPiece> {
        val pieces = mutableListOf<SpeechPiece>()
        fun addRange(uri: String, timelineStartMs: Long, trimStartMs: Long, trimEndMs: Long, speed: Float) {
            var offset = 0L
            while (trimStartMs + offset < trimEndMs) {
                val total = pieces.sumOf { it.piece.lengthMs }
                if (total >= maxTotalMs) return
                val length = minOf(maxPieceMs, trimEndMs - trimStartMs - offset, maxTotalMs - total)
                pieces += SpeechPiece(uri, trimStartMs + offset, AudioPiece(timelineStartMs, offset, length, speed))
                offset += length
            }
        }
        var clipStart = 0L
        for (clip in project.clips) {
            if (!clip.isMuted && !clip.isPhoto && clip.volume > 0f) {
                addRange(clip.uri, clipStart, clip.trimStartMs, clip.trimEndMs, clip.speed)
            }
            clipStart += clip.playbackDurationMs
        }
        for (track in project.audioTracks) {
            if (track.isVoiceover && track.volume > 0f && track.trimEndMs > track.trimStartMs) {
                addRange(track.uri, track.timelineStartOffsetMs, track.trimStartMs, track.trimEndMs, 1f)
            }
        }
        return pieces
    }

    /** Moves [segments] (times within [piece]) onto the timeline, dropping anything outside the piece. */
    fun mapToTimeline(segments: List<TranscriptSegment>, piece: AudioPiece): List<TranscriptSegment> {
        val speed = if (piece.speed <= 0.05f) 1f else piece.speed
        fun toTimeline(pieceMs: Long) = piece.timelineStartMs + ((piece.sourceOffsetMs + pieceMs) / speed).toLong()
        val pieceEnd = toTimeline(piece.lengthMs)
        return segments
            .filter { it.startMs < piece.lengthMs }
            .map { it.copy(startMs = toTimeline(it.startMs), endMs = toTimeline(it.endMs).coerceAtMost(pieceEnd)) }
            .filter { it.endMs > it.startMs }
    }

    /**
     * Subtitle overlays for transcribed speech: one per segment, never overlapping the next one,
     * each on screen at least [MIN_CAPTION_MS] where there's room.
     */
    fun captionOverlays(segments: List<TranscriptSegment>, totalDurationMs: Long): List<TextOverlay> {
        val sorted = segments.sortedBy { it.startMs }.filter { it.startMs < totalDurationMs }
        return sorted.mapIndexed { i, segment ->
            val nextStart = sorted.getOrNull(i + 1)?.startMs ?: totalDurationMs
            val end = maxOf(segment.endMs, segment.startMs + MIN_CAPTION_MS).coerceAtMost(nextStart).coerceAtMost(totalDurationMs)
            caption(segment.text, segment.startMs, end.coerceAtLeast(segment.startMs + 1))
        }
    }

    /** A text story: [lines] shown one after another, evenly across the video. */
    fun storyOverlays(lines: List<String>, totalDurationMs: Long): List<TextOverlay> {
        if (lines.isEmpty() || totalDurationMs <= 0L) return emptyList()
        val each = totalDurationMs / lines.size
        return lines.mapIndexed { i, line ->
            val start = i * each
            val end = if (i == lines.lastIndex) totalDurationMs else start + each
            TextOverlay(
                text = line,
                style = TextStylePreset.BOLD_WHITE_PILL,
                posY = 0.3f,
                startTimeMs = start,
                endTimeMs = end,
                animation = com.kevan.videorabykevan.model.TextAnimation.POP
            )
        }
    }

    private fun caption(text: String, startMs: Long, endMs: Long) = TextOverlay(
        text = text,
        style = TextStylePreset.CLEAN_SUBTITLE,
        posY = 0.72f,
        startTimeMs = startMs,
        endTimeMs = endMs,
        isAutoCaption = true
    )

    private const val MIN_CAPTION_MS = 700L

    private fun JsonObject.array(key: String): JsonArray? = get(key)?.takeIf { it.isJsonArray }?.asJsonArray

    /** Seconds as a number, or as a string like "1.5" or "00:01.5". */
    private fun JsonObject.seconds(key: String): Double? {
        val value: JsonElement = get(key)?.takeIf { it.isJsonPrimitive } ?: return null
        runCatching { return value.asDouble }
        val text = value.asString.trim()
        val parts = text.split(":")
        return runCatching { parts.fold(0.0) { acc, part -> acc * 60 + part.toDouble() } }.getOrNull()
    }
}
