package com.kevan.videorabykevan

import com.google.gson.Gson
import com.kevan.videorabykevan.data.BuiltInAudioHelper
import com.kevan.videorabykevan.data.ProjectJson
import com.kevan.videorabykevan.model.AspectRatioPreset
import com.kevan.videorabykevan.model.AudioTrack
import com.kevan.videorabykevan.model.ExportPreset
import com.kevan.videorabykevan.model.Project
import com.kevan.videorabykevan.model.SafeZoneType
import com.kevan.videorabykevan.model.TextOverlay
import com.kevan.videorabykevan.model.TextStylePreset
import com.kevan.videorabykevan.model.VideoClip
import com.kevan.videorabykevan.model.VideoFilter
import com.kevan.videorabykevan.ui.viewmodel.splitPartName
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class VideoraUnitTest {

    @Test
    fun testAspectRatioPresets() {
        val preset916 = AspectRatioPreset.TIKTOK_REELS_SHORTS_9_16
        assertEquals(1080, preset916.targetWidth)
        assertEquals(1920, preset916.targetHeight)
        assertEquals(9f / 16f, preset916.aspectRatio, 0.001f)

        val preset11 = AspectRatioPreset.SQUARE_1_1
        assertEquals(1.0f, preset11.aspectRatio, 0.001f)
    }

    @Test
    fun testVideoClipDurationAndSpeed() {
        val clip = VideoClip(
            uri = "file:///sample.mp4",
            name = "Test Clip",
            durationMs = 10_000L,
            trimStartMs = 2000L,
            trimEndMs = 8000L,
            speed = 2.0f
        )

        // Trimmed duration = 8000 - 2000 = 6000ms
        assertEquals(6000L, clip.trimmedDurationMs)
        // Playback duration at 2x = 6000 / 2 = 3000ms
        assertEquals(3000L, clip.playbackDurationMs)
    }

    @Test
    fun testVideoClipSlowMo() {
        val clip = VideoClip(
            uri = "file:///sample.mp4",
            name = "SlowMo Clip",
            durationMs = 4000L,
            trimStartMs = 0L,
            trimEndMs = 4000L,
            speed = 0.5f
        )

        assertEquals(4000L, clip.trimmedDurationMs)
        // Playback duration at 0.5x = 4000 / 0.5 = 8000ms
        assertEquals(8000L, clip.playbackDurationMs)
    }

    @Test
    fun testVideoClipFlipAndRotation() {
        val clip = VideoClip(
            uri = "file:///selfie.mp4",
            isFlippedHorizontal = true,
            rotationDegrees = 90
        )
        assertTrue(clip.isFlippedHorizontal)
        assertEquals(90, clip.rotationDegrees)
    }

    @Test
    fun testProjectDurationCalculation() {
        val clip1 = VideoClip(
            uri = "file:///clip1.mp4",
            durationMs = 5000L,
            trimStartMs = 0L,
            trimEndMs = 5000L,
            speed = 1.0f
        )
        val clip2 = VideoClip(
            uri = "file:///clip2.mp4",
            durationMs = 10000L,
            trimStartMs = 0L,
            trimEndMs = 10000L,
            speed = 2.0f // effective 5000L
        )

        val project = Project(
            title = "Test Short",
            clips = listOf(clip1, clip2)
        )

        assertEquals(10000L, project.totalDurationMs)
        assertEquals(2, project.clipCount)
        assertEquals("00:10.0", project.formattedDuration)
    }

    @Test
    fun testTextOverlayStylePresetsAndStickers() {
        val viralHook = TextStylePreset.VIRAL_HOOK
        assertEquals(0xFFFFDE03, viralHook.defaultTextColorHex)
        assertEquals(0xEE121212, viralHook.defaultBackgroundColorHex)

        val sticker = TextOverlay(
            text = "🔥",
            style = TextStylePreset.STICKER_EMOJI,
            isSticker = true
        )
        assertTrue(sticker.isSticker)
        assertEquals("🔥", sticker.text)

        assertTrue(TextOverlay.VIRAL_HOOK_TEMPLATES.isNotEmpty())
        assertTrue(TextOverlay.TRENDING_STICKERS.contains("🔥"))
    }

    @Test
    fun testBuiltInAudioEffectsList() {
        val effects = BuiltInAudioHelper.availableEffects
        assertTrue(effects.size >= 6)
        assertTrue(effects.any { it.id == "whoosh" })
        assertTrue(effects.any { it.id == "pop" })
        assertTrue(effects.any { it.id == "bell" })
        assertTrue(effects.any { it.id == "bass_boom" })
    }

    @Test
    fun testAudioTrackModel() {
        val audio = AudioTrack(
            uri = "file:///music.mp3",
            title = "Beat",
            durationMs = 30_000L,
            trimStartMs = 5000L,
            trimEndMs = 20_000L,
            volume = 0.75f
        )

        assertEquals(15_000L, audio.effectiveDurationMs)
        assertEquals(0.75f, audio.volume, 0.001f)
    }

    @Test
    fun testExportPresets() {
        val tiktok = ExportPreset.TIKTOK_OPTIMIZED
        assertEquals(1080, tiktok.targetWidth)
        assertEquals(1920, tiktok.targetHeight)
        assertEquals(30, tiktok.targetFps)

        val shorts = ExportPreset.SHORTS_MAX_QUALITY
        assertEquals(60, shorts.targetFps)
    }

    @Test
    fun testVideoFilters() {
        val vibrant = VideoFilter.VIBRANT
        assertTrue(vibrant.redScale > 1.0f)
        assertTrue(vibrant.contrast > 0.0f)

        val none = VideoFilter.NONE
        assertEquals(1.0f, none.redScale, 0.001f)
        assertEquals(0.0f, none.contrast, 0.001f)
    }

    @Test
    fun testProjectJsonSerialization() {
        val project = Project(
            title = "Serialization Test",
            aspectRatio = AspectRatioPreset.TIKTOK_REELS_SHORTS_9_16,
            safeZone = SafeZoneType.TIKTOK,
            clips = listOf(
                VideoClip(uri = "file:///1.mp4", durationMs = 3000L, trimEndMs = 3000L, isFlippedHorizontal = true)
            ),
            textOverlays = listOf(
                TextOverlay(text = "Hello World", startTimeMs = 500L, endTimeMs = 2500L)
            )
        )

        val gson = Gson()
        val json = gson.toJson(project)
        assertNotNull(json)

        val deserialized = gson.fromJson(json, Project::class.java)
        assertEquals(project.id, deserialized.id)
        assertEquals(project.title, deserialized.title)
        assertEquals(1, deserialized.clips.size)
        assertTrue(deserialized.clips.first().isFlippedHorizontal)
        assertEquals(1, deserialized.textOverlays.size)
        assertEquals("Hello World", deserialized.textOverlays.first().text)
    }

    @Test
    fun testExportOutputSizeFollowsProjectAspectRatio() {
        val tiktok = ExportPreset.TIKTOK_OPTIMIZED
        assertEquals(1080 to 1920, tiktok.outputSizeFor(AspectRatioPreset.TIKTOK_REELS_SHORTS_9_16))
        assertEquals(1080 to 1080, tiktok.outputSizeFor(AspectRatioPreset.SQUARE_1_1))
        assertEquals(1080 to 1350, tiktok.outputSizeFor(AspectRatioPreset.PORTRAIT_4_5))
        assertEquals(1920 to 1080, tiktok.outputSizeFor(AspectRatioPreset.LANDSCAPE_16_9))

        val draft = ExportPreset.FAST_DRAFT_720P
        assertEquals(720 to 1280, draft.outputSizeFor(AspectRatioPreset.TIKTOK_REELS_SHORTS_9_16))
        assertEquals(1280 to 720, draft.outputSizeFor(AspectRatioPreset.LANDSCAPE_16_9))
        assertEquals(720 to 900, draft.outputSizeFor(AspectRatioPreset.PORTRAIT_4_5))
    }

    @Test
    fun testFitModeDefaultsToFillForLegacyProjectJson() {
        val legacyJson = """{"id":"legacy","title":"Old Project","clips":[]}"""
        val project = Gson().fromJson(legacyJson, Project::class.java)
        assertFalse(project.isFitMode)

        val roundTripped = Gson().fromJson(Gson().toJson(project.copy(isFitMode = true)), Project::class.java)
        assertTrue(roundTripped.isFitMode)
    }

    @Test
    fun testLegacyClipJsonHasNoColorAdjustments() {
        // Shape of a clip saved before color adjustments existed
        val legacyJson = """{"id":"c1","uri":"file:///clip.mp4","name":"Clip","durationMs":4000,"trimStartMs":0,"trimEndMs":4000,"speed":1.0,"volume":1.0,"isMuted":false,"filter":"NONE","rotationDegrees":0,"isFlippedHorizontal":false}"""
        val clip = Gson().fromJson(legacyJson, VideoClip::class.java)
        assertEquals(0f, clip.brightness, 0f)
        assertEquals(0f, clip.saturation, 0f)
        assertFalse(clip.hasColorAdjustments)
        assertTrue(clip.copy(contrast = 0.2f).hasColorAdjustments)
    }

    @Test
    fun testProjectJsonFillsMissingAndUnknownFields() {
        val json = """[{"id":"p1","title":"Old","aspectRatio":"SOME_FUTURE_RATIO",
            "clips":[{"id":"c1","uri":"file:///a.mp4","durationMs":3000,"trimEndMs":3000,"filter":"UNKNOWN"}],
            "textOverlays":[{"id":"t1","text":"Hi","style":null}]}]"""
        val project = ProjectJson.decode(json).single()

        assertEquals("p1", project.id)
        assertEquals(AspectRatioPreset.TIKTOK_REELS_SHORTS_9_16, project.aspectRatio)
        assertEquals(SafeZoneType.TIKTOK, project.safeZone)
        assertTrue(project.audioTracks.isEmpty())

        val clip = project.clips.single()
        assertEquals("Clip", clip.name)
        assertEquals(VideoFilter.NONE, clip.filter)
        assertEquals(1.0f, clip.speed, 0f)
        // copy() used to throw NullPointerException on a clip loaded without a name
        assertEquals("Renamed", clip.copy(name = "Renamed").name)

        assertEquals(TextStylePreset.VIRAL_HOOK, project.textOverlays.single().style)
    }

    @Test
    fun testSplitPartNamesDoNotAccumulate() {
        assertEquals("Beach (Part 1)", splitPartName("Beach", 1))
        assertEquals("Beach (Part 2)", splitPartName("Beach (Part 1)", 2))
        assertEquals("Beach (Part 1)", splitPartName("Beach (Part 2) (Part 1)", 1))
        // Only a trailing suffix is stripped
        assertEquals("Part (Part 3) of trip (Part 1)", splitPartName("Part (Part 3) of trip", 1))
    }
}
