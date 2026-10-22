package com.kevan.videorabykevan.engine

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.os.Build
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import androidx.media3.effect.BitmapOverlay
import com.kevan.videorabykevan.model.TextOverlay
import com.kevan.videorabykevan.model.TextStylePreset
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min

/**
 * Shared sizing so text overlays look the same in the editor preview and in the exported video.
 * Sizes (font sp, padding dp, corner radius dp) are expressed relative to a frame whose shorter
 * side is [REFERENCE_SHORT_SIDE] units, then scaled to the actual canvas or output frame.
 */
object OverlayMetrics {
    const val REFERENCE_SHORT_SIDE = 240f
    const val PADDING_HORIZONTAL = 14f
    const val PADDING_VERTICAL = 6f

    fun pxPerUnit(frameWidthPx: Float, frameHeightPx: Float): Float =
        min(frameWidthPx, frameHeightPx) / REFERENCE_SHORT_SIDE

    fun cornerRadius(style: TextStylePreset): Float = when (style) {
        TextStylePreset.VIRAL_HOOK, TextStylePreset.BOLD_WHITE_PILL -> 12f
        TextStylePreset.CLEAN_SUBTITLE -> 6f
        TextStylePreset.MINIMAL_ACCENT -> 8f
        else -> 4f
    }

    fun fontWeight(style: TextStylePreset): Int = when (style) {
        TextStylePreset.VIRAL_HOOK, TextStylePreset.MEME_IMPACT -> 900
        TextStylePreset.BOLD_WHITE_PILL -> 700
        TextStylePreset.MINIMAL_ACCENT -> 500
        else -> 600
    }
}

/**
 * Draws every text overlay and sticker that is active at the frame's presentation time onto a
 * full-frame transparent bitmap. Bitmaps are cached per set of active overlays so the GPU texture
 * is only re-uploaded when an overlay appears or disappears.
 */
class TextOverlayBitmapOverlay(
    private val overlays: List<TextOverlay>,
    private val frameWidth: Int,
    private val frameHeight: Int
) : BitmapOverlay() {

    private val cache = mutableMapOf<List<String>, Bitmap>()

    override fun getBitmap(presentationTimeUs: Long): Bitmap {
        val timeMs = presentationTimeUs / 1000
        val active = overlays.filter { timeMs in it.startTimeMs..it.endTimeMs }
        return cache.getOrPut(active.map { it.id }) { render(active) }
    }

    override fun release() {
        super.release()
        cache.values.forEach { it.recycle() }
        cache.clear()
    }

    private fun render(active: List<TextOverlay>): Bitmap {
        val bitmap = Bitmap.createBitmap(frameWidth, frameHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        active.forEach { drawOverlay(canvas, it) }
        return bitmap
    }

    private fun drawOverlay(canvas: Canvas, overlay: TextOverlay) {
        val unit = OverlayMetrics.pxPerUnit(frameWidth.toFloat(), frameHeight.toFloat())
        val padH = OverlayMetrics.PADDING_HORIZONTAL * unit
        val padV = OverlayMetrics.PADDING_VERTICAL * unit
        val radius = OverlayMetrics.cornerRadius(overlay.style) * unit

        val textPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = overlay.textColorHex.toInt()
            textSize = overlay.fontSizeSp * unit
            typeface = typefaceFor(overlay.style)
        }

        // Wrap like the preview does (full frame width minus padding), then shrink the box to the text
        val maxTextWidth = max(1, (frameWidth - 2 * padH).toInt())
        var layout = buildLayout(overlay.text, textPaint, maxTextWidth)
        val textWidth = (0 until layout.lineCount).maxOfOrNull { layout.getLineWidth(it) } ?: 0f
        layout = buildLayout(overlay.text, textPaint, max(1, ceil(textWidth).toInt()))

        // posX / posY are the overlay's center (same as the editor canvas)
        val boxWidth = layout.width + 2 * padH
        val boxHeight = layout.height + 2 * padV
        val left = overlay.posX * frameWidth - boxWidth / 2f
        val top = overlay.posY * frameHeight - boxHeight / 2f
        val box = RectF(left, top, left + boxWidth, top + boxHeight)

        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = overlay.backgroundColorHex.toInt() }
        if (bgPaint.alpha > 0) {
            canvas.drawRoundRect(box, radius, radius, bgPaint)
        }

        canvas.save()
        canvas.translate(left + padH, top + padV)
        layout.draw(canvas)
        canvas.restore()
    }

    private fun buildLayout(text: String, paint: TextPaint, width: Int): StaticLayout =
        StaticLayout.Builder.obtain(text, 0, text.length, paint, width)
            .setAlignment(Layout.Alignment.ALIGN_CENTER)
            .setIncludePad(false)
            .build()

    private fun typefaceFor(style: TextStylePreset): Typeface {
        val family = if (style == TextStylePreset.MEME_IMPACT) Typeface.SANS_SERIF else Typeface.DEFAULT
        val weight = OverlayMetrics.fontWeight(style)
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            Typeface.create(family, weight, false)
        } else {
            Typeface.create(family, if (weight >= 600) Typeface.BOLD else Typeface.NORMAL)
        }
    }
}
