package com.kevan.videorabykevan.data

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.media.MediaMuxer
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

object SampleMediaHelper {

    /**
     * Generates a 5-second 9:16 vertical sample video clip on-device
     * using Android MediaCodec + MediaMuxer.
     * Useful for instant offline testing and demonstration without needing gallery files.
     */
    suspend fun generateSampleVideoClip(
        context: Context,
        clipTitle: String = "Viral Scene",
        gradientStartColor: Int = Color.parseColor("#8A2387"),
        gradientMidColor: Int = Color.parseColor("#E94057"),
        gradientEndColor: Int = Color.parseColor("#F27121"),
        durationSeconds: Int = 4
    ): File = withContext(Dispatchers.IO) {
        val outputFile = File(context.cacheDir, "sample_clip_${System.currentTimeMillis()}.mp4")
        val width = 720
        val height = 1280
        val frameRate = 30
        val totalFrames = durationSeconds * frameRate
        val bitRate = 3_000_000

        val mimeType = MediaFormat.MIMETYPE_VIDEO_AVC
        val format = MediaFormat.createVideoFormat(mimeType, width, height).apply {
            setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface)
            setInteger(MediaFormat.KEY_BIT_RATE, bitRate)
            setInteger(MediaFormat.KEY_FRAME_RATE, frameRate)
            setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1)
        }

        var codec: MediaCodec? = null
        var muxer: MediaMuxer? = null

        try {
            codec = MediaCodec.createEncoderByType(mimeType)
            codec.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
            val inputSurface = codec.createInputSurface()
            codec.start()

            muxer = MediaMuxer(outputFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
            var videoTrackIndex = -1
            var muxerStarted = false
            val bufferInfo = MediaCodec.BufferInfo()

            val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.WHITE
                textSize = 48f
                textAlign = Paint.Align.CENTER
                setShadowLayer(8f, 0f, 4f, Color.BLACK)
            }
            val subTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#FFDE03")
                textSize = 32f
                textAlign = Paint.Align.CENTER
            }

            for (i in 0 until totalFrames) {
                val canvas: Canvas = inputSurface.lockHardwareCanvas()
                try {
                    // Draw vibrant dynamic vertical gradient
                    val shift = (i.toFloat() / totalFrames) * height
                    val gradient = LinearGradient(
                        0f, (0f + shift) % height,
                        width.toFloat(), ((height.toFloat() + shift) % height).coerceAtLeast(1f),
                        intArrayOf(gradientStartColor, gradientMidColor, gradientEndColor, gradientStartColor),
                        null,
                        Shader.TileMode.REPEAT
                    )
                    val bgPaint = Paint().apply { shader = gradient }
                    canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

                    // Draw decorative creator card
                    val cardPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        color = Color.parseColor("#80000000")
                    }
                    val cardRect = RectF(60f, height * 0.35f, width - 60f, height * 0.65f)
                    canvas.drawRoundRect(cardRect, 32f, 32f, cardPaint)

                    // Draw Title & Frame info
                    val secondsLeft = (totalFrames - i) / frameRate
                    canvas.drawText("⚡ $clipTitle", width / 2f, height * 0.46f, textPaint)
                    canvas.drawText("Frame ${i + 1}/$totalFrames (${secondsLeft}s)", width / 2f, height * 0.53f, subTextPaint)
                    canvas.drawText("9:16 Vertical Creator Mode", width / 2f, height * 0.58f, textPaint.apply { textSize = 28f })
                } finally {
                    inputSurface.unlockCanvasAndPost(canvas)
                }

                // Drain output from codec
                var outIndex = codec.dequeueOutputBuffer(bufferInfo, 10_000)
                while (outIndex >= 0 || outIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                    if (outIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                        val newFormat = codec.outputFormat
                        videoTrackIndex = muxer.addTrack(newFormat)
                        muxer.start()
                        muxerStarted = true
                    } else if (outIndex >= 0) {
                        val encodedData = codec.getOutputBuffer(outIndex)
                        if (encodedData != null && muxerStarted && bufferInfo.size > 0) {
                            bufferInfo.presentationTimeUs = (i * 1_000_000L / frameRate)
                            muxer.writeSampleData(videoTrackIndex, encodedData, bufferInfo)
                        }
                        codec.releaseOutputBuffer(outIndex, false)
                    }
                    outIndex = codec.dequeueOutputBuffer(bufferInfo, 0)
                }
            }

            codec.signalEndOfInputStream()

            // Drain remaining
            var outIndex = codec.dequeueOutputBuffer(bufferInfo, 100_000)
            while (outIndex >= 0) {
                val encodedData = codec.getOutputBuffer(outIndex)
                if (encodedData != null && muxerStarted && bufferInfo.size > 0) {
                    muxer.writeSampleData(videoTrackIndex, encodedData, bufferInfo)
                }
                codec.releaseOutputBuffer(outIndex, false)
                if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) break
                outIndex = codec.dequeueOutputBuffer(bufferInfo, 10_000)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            try { codec?.stop() } catch (_: Exception) {}
            try { codec?.release() } catch (_: Exception) {}
            try { muxer?.stop() } catch (_: Exception) {}
            try { muxer?.release() } catch (_: Exception) {}
        }

        outputFile
    }
}
