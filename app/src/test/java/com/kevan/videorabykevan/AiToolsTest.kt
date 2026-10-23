package com.kevan.videorabykevan

import com.google.gson.JsonParser
import com.kevan.videorabykevan.ai.AiContent
import com.kevan.videorabykevan.ai.AiError
import com.kevan.videorabykevan.ai.AiMedia
import com.kevan.videorabykevan.ai.AiMessage
import com.kevan.videorabykevan.ai.AiPrompts
import com.kevan.videorabykevan.ai.AudioPiece
import com.kevan.videorabykevan.ai.OpenRouterJson
import com.kevan.videorabykevan.ai.TranscriptSegment
import com.kevan.videorabykevan.model.AudioTrack
import com.kevan.videorabykevan.model.Project
import com.kevan.videorabykevan.model.VideoClip
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class AiToolsTest {

    @Test
    fun chatRequestUsesContentParts() {
        val body = OpenRouterJson.chatRequest(
            "~google/gemini-flash-latest",
            listOf(
                AiMessage.system("sys"),
                AiMessage.user(AiContent.Text("hi"), AiContent.ImageJpeg("AAA"), AiContent.AudioWav("BBB"))
            ),
            maxTokens = 500
        )
        val json = JsonParser.parseString(body).asJsonObject
        assertEquals("~google/gemini-flash-latest", json.get("model").asString)
        assertEquals(500, json.get("max_tokens").asInt)
        val parts = json.getAsJsonArray("messages")[1].asJsonObject.getAsJsonArray("content")
        assertEquals("text", parts[0].asJsonObject.get("type").asString)
        assertEquals("data:image/jpeg;base64,AAA", parts[1].asJsonObject.getAsJsonObject("image_url").get("url").asString)
        val audio = parts[2].asJsonObject.getAsJsonObject("input_audio")
        assertEquals("BBB", audio.get("data").asString)
        assertEquals("wav", audio.get("format").asString)
    }

    @Test
    fun chatReplyReadsStringOrParts() {
        assertEquals("hello", OpenRouterJson.parseChatReply("""{"choices":[{"message":{"content":"hello"}}]}"""))
        assertEquals(
            "ab",
            OpenRouterJson.parseChatReply("""{"choices":[{"message":{"content":[{"type":"text","text":"a"},{"type":"text","text":"b"}]}}]}""")
        )
        expect<AiError.BadResponse> { OpenRouterJson.parseChatReply("""{"choices":[]}""") }
        expect<AiError.Server> { OpenRouterJson.parseChatReply("""{"error":{"message":"model overloaded"}}""") }
    }

    @Test
    fun jsonIsFoundInsideFencesAndChatter() {
        val reply = "Sure! Here you go:\n```json\n{\"hooks\": [\"One\", \" \", \"Two\"]}\n```"
        assertEquals(listOf("One", "Two"), AiPrompts.parseStringList(reply, "hooks"))
        expect<AiError.BadResponse> { AiPrompts.parseStringList("no json here", "hooks") }
        expect<AiError.BadResponse> { AiPrompts.parseStringList("""{"hooks": []}""", "hooks") }
    }

    @Test
    fun postCaptionNormalisesHashtags() {
        val caption = AiPrompts.parsePostCaption("""{"caption":"Morning run 🏃","hashtags":["running","#5k", "early bird"]}""")
        assertEquals(listOf("#running", "#5k", "#earlybird"), caption.hashtags)
        assertEquals("Morning run 🏃\n\n#running #5k #earlybird", caption.fullText)
    }

    @Test
    fun translationsMustMatchCount() {
        assertEquals(listOf("Hola", "Adiós"), AiPrompts.parseTranslations("""{"translations":["Hola","Adiós"]}""", 2))
        expect<AiError.BadResponse> { AiPrompts.parseTranslations("""{"translations":["Hola"]}""", 2) }
    }

    @Test
    fun colorValuesAreClampedAndDefaulted() {
        val color = AiPrompts.parseColor("""{"exposure": 2.5, "contrast": "0.2", "saturation": -0.3, "reason": "Too dark"}""")
        assertEquals(1f, color.exposure, 0f)
        assertEquals(0.2f, color.contrast, 0.001f)
        assertEquals(-0.3f, color.saturation, 0.001f)
        assertEquals(0f, color.hue, 0f)
        assertEquals("Too dark", color.reason)
        val clip = color.applyTo(VideoClip(uri = "file:///c.mp4"))
        assertEquals(1f, clip.exposure, 0f)
    }

    @Test
    fun transcriptAcceptsNumbersAndTimestamps() {
        val segments = AiPrompts.parseTranscript(
            """{"segments":[{"start":"00:02.5","end":"00:03.0","text":"second"},{"start":0.5,"end":1.25,"text":"first"},{"start":4,"text":"no end"},{"text":"no start"}]}"""
        )
        assertEquals(listOf("first", "second", "no end"), segments.map { it.text })
        assertEquals(500L, segments[0].startMs)
        assertEquals(2500L, segments[1].startMs)
        assertEquals(5500L, segments[2].endMs)
    }

    @Test
    fun segmentsMapThroughTrimOffsetAndSpeed() {
        // Second minute of a clip that starts 10 s into the timeline and plays at 2x
        val piece = AudioPiece(timelineStartMs = 10_000L, sourceOffsetMs = 60_000L, lengthMs = 20_000L, speed = 2f)
        val mapped = AiPrompts.mapToTimeline(
            listOf(TranscriptSegment(2_000L, 4_000L, "a"), TranscriptSegment(19_000L, 30_000L, "cut"), TranscriptSegment(25_000L, 26_000L, "beyond")),
            piece
        )
        assertEquals(listOf("a", "cut"), mapped.map { it.text })
        assertEquals(10_000L + 31_000L, mapped[0].startMs)
        assertEquals(10_000L + 32_000L, mapped[0].endMs)
        // Clamped to the end of the piece on the timeline
        assertEquals(10_000L + 40_000L, mapped[1].endMs)
    }

    @Test
    fun captionsNeverOverlapAndStayInsideTheVideo() {
        val overlays = AiPrompts.captionOverlays(
            listOf(TranscriptSegment(0L, 200L, "quick"), TranscriptSegment(500L, 2_000L, "next"), TranscriptSegment(9_500L, 12_000L, "late")),
            totalDurationMs = 10_000L
        )
        assertEquals(500L, overlays[0].endTimeMs)
        assertEquals(10_000L, overlays[2].endTimeMs)
        assertTrue(overlays.all { it.isAutoCaption && it.endTimeMs > it.startTimeMs })
    }

    @Test
    fun storyFillsTheVideoEvenly() {
        val overlays = AiPrompts.storyOverlays(listOf("a", "b", "c"), 10_000L)
        assertEquals(listOf(0L, 3_333L, 6_666L), overlays.map { it.startTimeMs })
        assertEquals(10_000L, overlays.last().endTimeMs)
    }

    @Test
    fun speechPiecesSkipSilentClipsAndChunkLongOnes() {
        val project = Project(
            clips = listOf(
                VideoClip(uri = "file:///a.mp4", durationMs = 150_000L, trimStartMs = 10_000L, trimEndMs = 150_000L),
                VideoClip(uri = "file:///muted.mp4", durationMs = 5_000L, isMuted = true),
                VideoClip(uri = "file:///photo.mp4", durationMs = 10_000L, trimEndMs = 3_000L, isPhoto = true)
            ),
            audioTracks = listOf(
                AudioTrack(uri = "file:///music.mp3", durationMs = 30_000L),
                AudioTrack(uri = "file:///vo.m4a", durationMs = 4_000L, timelineStartOffsetMs = 2_000L, isVoiceover = true)
            )
        )
        val pieces = AiPrompts.speechPieces(project, maxPieceMs = 60_000L)
        assertEquals(listOf("file:///a.mp4", "file:///a.mp4", "file:///a.mp4", "file:///vo.m4a"), pieces.map { it.uri })
        assertEquals(listOf(10_000L, 70_000L, 130_000L), pieces.take(3).map { it.sourceStartMs })
        assertEquals(20_000L, pieces[2].piece.lengthMs)
        assertEquals(2_000L, pieces[3].piece.timelineStartMs)
        // The total cap trims the plan
        assertEquals(30_000L, AiPrompts.speechPieces(project, maxTotalMs = 30_000L).sumOf { it.piece.lengthMs })
    }

    @Test
    fun wavHeaderAndResampling() {
        val wav = AiMedia.wav(ShortArray(16_000), 16_000)
        assertEquals(44 + 32_000, wav.size)
        assertEquals("RIFF", String(wav, 0, 4, Charsets.US_ASCII))
        assertEquals("WAVE", String(wav, 8, 4, Charsets.US_ASCII))
        assertEquals(16_000, AiMedia.resample(FloatArray(48_000), 48_000, 16_000).size)
        val ramp = AiMedia.resample(floatArrayOf(0f, 1f), 2, 4)
        assertEquals(4, ramp.size)
        assertEquals(16_384.0, ramp[1].toDouble(), 1.0)
    }

    private inline fun <reified T : Throwable> expect(block: () -> Unit) {
        try {
            block()
            fail("Expected ${T::class.simpleName}")
        } catch (e: Throwable) {
            if (e !is T) throw e
        }
    }
}
