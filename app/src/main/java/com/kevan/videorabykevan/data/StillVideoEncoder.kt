package com.kevan.videorabykevan.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.ImageDecoder
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Rect
import android.media.ExifInterface
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.media.MediaMuxer
import android.net.Uri
import android.os.Build
import java.io.File
import kotlin.math.max
import kotlin.math.roundToInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Turns a still image (a photo, or a freeze frame) into a short silent MP4.
 *
 * Photos become ordinary clips this way, so playback, trimming, color, thumbnails and the export
 * all work on them unchanged, and the preview matches the export by construction.
 */
object StillVideoEncoder {

    // A still barely needs frames; a low rate keeps encoding fast
    private const val FRAME_RATE = 10
    private const val BIT_RATE = 4_000_000
    // Long side of the encoded video; dimensions are multiples of 16 for encoder compatibility
    private const val MAX_LONG_SIDE = 1920
    private const val DIMENSION_ALIGN = 16
    private const val DRAIN_TIMEOUT_US = 10_000L
    // Give up waiting for the encoder's last frames after this many empty polls (~5 s)
    private const val MAX_EMPTY_POLLS = 500

    /** Decodes a picked image the right way up (EXIF orientation applied). */
    suspend fun decodeImage(context: Context, uri: Uri): Bitmap? = withContext(Dispatchers.IO) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                // ImageDecoder applies EXIF rotation itself; software memory so the canvas can draw it
                ImageDecoder.decodeBitmap(ImageDecoder.createSource(context.contentResolver, uri)) { decoder, info, _ ->
                    decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                    val longSide = max(info.size.width, info.size.height)
                    if (longSide > MAX_LONG_SIDE * 2) {
                        val scale = MAX_LONG_SIDE * 2f / longSide
                        decoder.setTargetSize((info.size.width * scale).roundToInt(), (info.size.height * scale).roundToInt())
                    }
                }
            } else {
                val bitmap = context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it) } ?: return@withContext null
                val orientation = context.contentResolver.openInputStream(uri)?.use {
                    ExifInterface(it).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
                } ?: ExifInterface.ORIENTATION_NORMAL
                val degrees = when (orientation) {
                    ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                    ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                    ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                    else -> 0f
                }
                if (degrees == 0f) bitmap else Bitmap.createBitmap(
                    bitmap, 0, 0, bitmap.width, bitmap.height, Matrix().apply { postRotate(degrees) }, true
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Encodes [bitmap] as [durationMs] of H.264 video into [outputFile].
     * Returns false if the device's encoder failed.
     */
    suspend fun encode(bitmap: Bitmap, durationMs: Long, outputFile: File): Boolean = withContext(Dispatchers.IO) {
        val (width, height) = encodedSize(bitmap.width, bitmap.height)
        val totalFrames = max(1, (durationMs * FRAME_RATE / 1000).toInt())
        val frameDurationUs = 1_000_000L / FRAME_RATE

        val format = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, width, height).apply {
            setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface)
            setInteger(MediaFormat.KEY_BIT_RATE, BIT_RATE)
            setInteger(MediaFormat.KEY_FRAME_RATE, FRAME_RATE)
            setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1)
        }

        var codec: MediaCodec? = null
        var muxer: MediaMuxer? = null
        var muxerStarted = false
        var writtenFrames = 0
        try {
            val encoder = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_VIDEO_AVC).also { codec = it }
            encoder.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
            val inputSurface = encoder.createInputSurface()
            encoder.start()
            val output = MediaMuxer(outputFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4).also { muxer = it }
            var trackIndex = -1
            val info = MediaCodec.BufferInfo()
            val paint = Paint(Paint.FILTER_BITMAP_FLAG)
            val target = Rect(0, 0, width, height)

            // Output timestamps are assigned here, one frame apart, because a surface encoder stamps
            // frames with the wall clock and they're drawn much faster than real time
            fun drain(timeoutUs: Long, untilEndOfStream: Boolean) {
                var emptyPolls = 0
                while (true) {
                    val index = encoder.dequeueOutputBuffer(info, timeoutUs)
                    when {
                        index == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                            trackIndex = output.addTrack(encoder.outputFormat)
                            output.start()
                            muxerStarted = true
                        }
                        index >= 0 -> {
                            val data = encoder.getOutputBuffer(index)
                            val isConfig = info.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG != 0
                            if (data != null && muxerStarted && info.size > 0 && !isConfig) {
                                info.presentationTimeUs = writtenFrames * frameDurationUs
                                output.writeSampleData(trackIndex, data, info)
                                writtenFrames++
                            }
                            encoder.releaseOutputBuffer(index, false)
                            if (info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) return
                        }
                        // No output yet: keep waiting at the end, otherwise go draw the next frame
                        !untilEndOfStream || ++emptyPolls > MAX_EMPTY_POLLS -> return
                    }
                }
            }

            repeat(totalFrames) {
                val canvas = inputSurface.lockHardwareCanvas()
                try {
                    canvas.drawColor(Color.BLACK)
                    canvas.drawBitmap(bitmap, null, target, paint)
                } finally {
                    inputSurface.unlockCanvasAndPost(canvas)
                }
                drain(DRAIN_TIMEOUT_US, untilEndOfStream = false)
            }
            encoder.signalEndOfInputStream()
            drain(DRAIN_TIMEOUT_US, untilEndOfStream = true)
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            try { codec?.stop() } catch (_: Exception) {}
            try { codec?.release() } catch (_: Exception) {}
            try { if (muxerStarted) muxer?.stop() } catch (_: Exception) {}
            try { muxer?.release() } catch (_: Exception) {}
        }
        writtenFrames > 0 && outputFile.length() > 0
    }

    /** The bitmap's aspect, long side capped at [MAX_LONG_SIDE], both sides multiples of 16. */
    internal fun encodedSize(sourceWidth: Int, sourceHeight: Int): Pair<Int, Int> {
        val longSide = max(sourceWidth, sourceHeight).coerceAtLeast(1)
        val scale = (MAX_LONG_SIDE.toFloat() / longSide).coerceAtMost(1f)
        fun align(value: Float) = max(DIMENSION_ALIGN, (value / DIMENSION_ALIGN).roundToInt() * DIMENSION_ALIGN)
        return align(sourceWidth * scale) to align(sourceHeight * scale)
    }
}
