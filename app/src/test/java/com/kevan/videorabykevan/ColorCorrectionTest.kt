package com.kevan.videorabykevan

import com.kevan.videorabykevan.engine.ClipColorMatrix
import com.kevan.videorabykevan.engine.ClipColorMatrix.transform
import com.kevan.videorabykevan.model.VideoClip
import com.kevan.videorabykevan.model.VideoFilter
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ColorCorrectionTest {

    private val clip = VideoClip(uri = "file:///clip.mp4", durationMs = 1000L)

    private fun FloatArray.luma() = 0.213f * this[0] + 0.715f * this[1] + 0.072f * this[2]

    @Test
    fun noFilterAndNoCorrectionNeedsNoMatrix() {
        assertNull(ClipColorMatrix.matrixFor(clip))
        assertNull(ClipColorMatrix.matrixFor(clip.copy(exposure = 0.5f), includeColor = false))
    }

    @Test
    fun exposureIsMeasuredInStops() {
        // +0.5 on the slider = +1 stop = twice the light
        val m = ClipColorMatrix.matrixFor(clip.copy(exposure = 0.5f))!!
        assertArrayEquals(floatArrayOf(200f, 100f, 50f), m.transform(100f, 50f, 25f), 0.01f)
    }

    @Test
    fun temperatureWarmsAndCools() {
        val warm = ClipColorMatrix.matrixFor(clip.copy(temperature = 1f))!!.transform(100f, 100f, 100f)
        assertTrue(warm[0] > 100f && warm[2] < 100f)
        val cool = ClipColorMatrix.matrixFor(clip.copy(temperature = -1f))!!.transform(100f, 100f, 100f)
        assertTrue(cool[0] < 100f && cool[2] > 100f)
    }

    @Test
    fun tintMovesBetweenGreenAndMagenta() {
        val magenta = ClipColorMatrix.matrixFor(clip.copy(tint = 1f))!!.transform(100f, 100f, 100f)
        assertTrue(magenta[1] < 100f)
        val green = ClipColorMatrix.matrixFor(clip.copy(tint = -1f))!!.transform(100f, 100f, 100f)
        assertTrue(green[1] > 100f)
    }

    @Test
    fun fullDesaturationGivesGray() {
        val gray = ClipColorMatrix.matrixFor(clip.copy(saturation = -1f))!!.transform(200f, 50f, 10f)
        assertEquals(gray[0], gray[1], 0.01f)
        assertEquals(gray[1], gray[2], 0.01f)
    }

    @Test
    fun hueRotationKeepsBrightnessAndGrays() {
        val m = ClipColorMatrix.matrixFor(clip.copy(hue = 0.5f))!!
        val color = floatArrayOf(200f, 80f, 40f)
        val rotated = m.transform(color[0], color[1], color[2])
        assertEquals(color.luma(), rotated.luma(), 0.5f)
        assertFalse(rotated.contentEquals(color))
        // Neutral gray has no hue, so it must not change
        assertArrayEquals(floatArrayOf(128f, 128f, 128f), m.transform(128f, 128f, 128f), 0.5f)
    }

    @Test
    fun contrastPivotsAroundMidGray() {
        val m = ClipColorMatrix.matrixFor(clip.copy(contrast = 0.5f))!!
        assertArrayEquals(floatArrayOf(127.5f, 127.5f, 127.5f), m.transform(127.5f, 127.5f, 127.5f), 0.05f)
        assertTrue(m.transform(200f, 200f, 200f)[0] > 200f)
    }

    @Test
    fun filterAndCorrectionCombine() {
        val filterOnly = ClipColorMatrix.matrixFor(clip.copy(filter = VideoFilter.WARM_SUNSET))!!
        val both = ClipColorMatrix.matrixFor(clip.copy(filter = VideoFilter.WARM_SUNSET, brightness = 0.1f))!!
        val a = filterOnly.transform(100f, 100f, 100f)
        val b = both.transform(100f, 100f, 100f)
        for (i in 0..2) assertEquals(a[i] + 25.5f, b[i], 0.01f)
    }

    @Test
    fun media3MatrixIsColumnMajorWithNormalizedOffsets() {
        val m = ClipColorMatrix.matrixFor(clip.copy(brightness = 0.2f, exposure = 0.5f))!!
        val media3 = ClipColorMatrix.toMedia3Matrix(m)
        // Diagonal gain (column-major [col * 4 + row])
        assertEquals(2f, media3[0], 0.001f)
        assertEquals(2f, media3[5], 0.001f)
        assertEquals(2f, media3[10], 0.001f)
        // Brightness offset in the 4th column, in 0..1 units
        assertEquals(0.2f, media3[12], 0.001f)
        assertEquals(0.2f, media3[13], 0.001f)
        assertEquals(0.2f, media3[14], 0.001f)
        assertEquals(1f, media3[15], 0f)
        assertEquals(0f, media3[3], 0f)
    }

    @Test
    fun resetAndCopyColor() {
        val graded = clip.copy(filter = VideoFilter.CYBERPUNK, exposure = 0.3f, temperature = -0.2f, hue = 0.1f)
        assertTrue(graded.hasColorAdjustments)

        val reset = graded.withoutColorAdjustments()
        assertFalse(reset.hasColorAdjustments)
        assertEquals(VideoFilter.CYBERPUNK, reset.filter)

        val other = VideoClip(uri = "file:///other.mp4", durationMs = 2000L, speed = 2f)
        val copied = graded.copyColorTo(other)
        assertEquals(VideoFilter.CYBERPUNK, copied.filter)
        assertEquals(0.3f, copied.exposure, 0f)
        assertEquals(-0.2f, copied.temperature, 0f)
        assertEquals("file:///other.mp4", copied.uri)
        assertEquals(2f, copied.speed, 0f)
        assertNotNull(ClipColorMatrix.matrixFor(copied))
    }
}
