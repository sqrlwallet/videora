package com.kevan.videorabykevan.ai

import android.content.Context
import android.graphics.Bitmap
import android.media.AudioFormat
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMetadataRetriever
import android.net.Uri
import com.kevan.videorabykevan.model.Project
import com.kevan.videorabykevan.model.VideoClip
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.Base64
import kotlin.math.max
import kotlin.math.roundToInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Prepares what the AI tools send: small JPEG frames and 16 kHz mono WAV audio. Runs on the
 * device; nothing here touches the network (the AI edition's client does that).
 */
object AiMedia {

    const val SPEECH_SAMPLE_RATE = 16_000
    private const val FRAME_MAX_SIDE = 512
    private const val JPEG_QUALITY = 80
    private const val CODEC_TIMEOUT_US = 10_000L

    /** The frame [atSourceMs] into [clip]'s source, scaled down and base64-encoded as JPEG. */
    suspend fun frameJpeg(context: Context, clip: VideoClip, atSourceMs: Long): String? = withContext(Dispatchers.IO) {
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(context, Uri.parse(clip.uri))
            val frame = retriever.getFrameAtTime(atSourceMs * 1000L, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                ?: return@withContext null
            val scale = FRAME_MAX_SIDE.toFloat() / max(frame.width, frame.height)
            val scaled = if (scale < 1f) {
                Bitmap.createScaledBitmap(frame, (frame.width * scale).roundToInt(), (frame.height * scale).roundToInt(), true)
            } else {
                frame
            }
            val bytes = ByteArrayOutputStream().use {
                scaled.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, it)
                it.toByteArray()
            }
            if (scaled !== frame) scaled.recycle()
            frame.recycle()
            Base64.getEncoder().encodeToString(bytes)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        } finally {
            try { retriever.release() } catch (_: Exception) {}
        }
    }

    /** The middle frame of up to [count] clips spread across the project, in timeline order. */
    suspend fun projectFrames(context: Context, project: Project, count: Int = 4): List<String> {
        val clips = project.clips
        if (clips.isEmpty()) return emptyList()
        val picks = if (clips.size <= count) clips.indices.toList()
        else (0 until count).map { it * (clips.size - 1) / (count - 1) }.distinct()
        return picks.mapNotNull { i ->
            val clip = clips[i]
            frameJpeg(context, clip, (clip.trimStartMs + clip.trimEndMs) / 2)
        }
    }

    /**
     * Decodes [startMs]..[endMs] of the audio in [uri] to 16 kHz mono PCM. Null when the media has
     * no audio track or it can't be decoded.
     */
    suspend fun decodeSpeechPcm(context: Context, uri: Uri, startMs: Long, endMs: Long): ShortArray? = withContext(Dispatchers.IO) {
        val extractor = MediaExtractor()
        var codec: MediaCodec? = null
        try {
            extractor.setDataSource(context, uri, null)
            val trackIndex = (0 until extractor.trackCount).firstOrNull {
                extractor.getTrackFormat(it).getString(MediaFormat.KEY_MIME)?.startsWith("audio/") == true
            } ?: return@withContext null
            val format = extractor.getTrackFormat(trackIndex)
            extractor.selectTrack(trackIndex)
            extractor.seekTo(startMs * 1000L, MediaExtractor.SEEK_TO_PREVIOUS_SYNC)

            val decoder = MediaCodec.createDecoderByType(format.getString(MediaFormat.KEY_MIME)!!).also { codec = it }
            decoder.configure(format, null, null, 0)
            decoder.start()

            val startUs = startMs * 1000L
            val endUs = endMs * 1000L
            var sampleRate = format.getInteger(MediaFormat.KEY_SAMPLE_RATE)
            var channels = format.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
            var isFloat = false
            val mono = FloatList()
            val info = MediaCodec.BufferInfo()
            var inputDone = false
            var emptyPolls = 0

            while (emptyPolls < 500) {
                if (!inputDone) {
                    val inIndex = decoder.dequeueInputBuffer(CODEC_TIMEOUT_US)
                    if (inIndex >= 0) {
                        val buffer = decoder.getInputBuffer(inIndex)!!
                        val size = extractor.readSampleData(buffer, 0)
                        if (size < 0 || extractor.sampleTime > endUs) {
                            decoder.queueInputBuffer(inIndex, 0, 0, 0L, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                            inputDone = true
                        } else {
                            decoder.queueInputBuffer(inIndex, 0, size, extractor.sampleTime, 0)
                            extractor.advance()
                        }
                    }
                }
                val outIndex = decoder.dequeueOutputBuffer(info, CODEC_TIMEOUT_US)
                when {
                    outIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                        val out = decoder.outputFormat
                        sampleRate = out.getInteger(MediaFormat.KEY_SAMPLE_RATE)
                        channels = out.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
                        isFloat = out.containsKey(MediaFormat.KEY_PCM_ENCODING) &&
                            out.getInteger(MediaFormat.KEY_PCM_ENCODING) == AudioFormat.ENCODING_PCM_FLOAT
                    }
                    outIndex >= 0 -> {
                        emptyPolls = 0
                        val buffer = decoder.getOutputBuffer(outIndex)
                        if (buffer != null && info.size > 0) {
                            buffer.position(info.offset)
                            buffer.limit(info.offset + info.size)
                            appendMono(buffer.order(ByteOrder.nativeOrder()), isFloat, channels, sampleRate, info.presentationTimeUs, startUs, endUs, mono)
                        }
                        decoder.releaseOutputBuffer(outIndex, false)
                        if (info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) break
                    }
                    else -> if (inputDone) emptyPolls++
                }
            }
            if (mono.size == 0) null else resample(mono.toArray(), sampleRate, SPEECH_SAMPLE_RATE)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        } finally {
            try { codec?.stop() } catch (_: Exception) {}
            try { codec?.release() } catch (_: Exception) {}
            extractor.release()
        }
    }

    /** Averages each frame's channels, keeping only samples between [startUs] and [endUs]. */
    private fun appendMono(
        buffer: ByteBuffer,
        isFloat: Boolean,
        channels: Int,
        sampleRate: Int,
        bufferStartUs: Long,
        startUs: Long,
        endUs: Long,
        out: FloatList
    ) {
        val bytesPerSample = if (isFloat) 4 else 2
        val frames = buffer.remaining() / (bytesPerSample * channels)
        for (frame in 0 until frames) {
            var sum = 0f
            for (c in 0 until channels) {
                sum += if (isFloat) buffer.getFloat() else buffer.getShort() / 32768f
            }
            val timeUs = bufferStartUs + frame * 1_000_000L / sampleRate
            if (timeUs in startUs until endUs) out.add(sum / channels)
        }
    }

    /** Linear-interpolation resample to [toRate], as 16-bit samples. */
    internal fun resample(samples: FloatArray, fromRate: Int, toRate: Int): ShortArray {
        if (samples.isEmpty()) return ShortArray(0)
        val outLength = max(1, (samples.size.toLong() * toRate / fromRate).toInt())
        val step = fromRate.toDouble() / toRate
        return ShortArray(outLength) { i ->
            val position = i * step
            val index = position.toInt().coerceAtMost(samples.lastIndex)
            val next = (index + 1).coerceAtMost(samples.lastIndex)
            val fraction = (position - index).toFloat()
            val value = samples[index] + (samples[next] - samples[index]) * fraction
            (value.coerceIn(-1f, 1f) * 32767f).roundToInt().toShort()
        }
    }

    /** A complete 16-bit mono WAV file for [pcm]. */
    fun wav(pcm: ShortArray, sampleRate: Int = SPEECH_SAMPLE_RATE): ByteArray {
        val dataSize = pcm.size * 2
        val buffer = ByteBuffer.allocate(44 + dataSize).order(ByteOrder.LITTLE_ENDIAN)
        buffer.put("RIFF".toByteArray(Charsets.US_ASCII))
        buffer.putInt(36 + dataSize)
        buffer.put("WAVE".toByteArray(Charsets.US_ASCII))
        buffer.put("fmt ".toByteArray(Charsets.US_ASCII))
        buffer.putInt(16)                 // fmt chunk size
        buffer.putShort(1)                // PCM
        buffer.putShort(1)                // mono
        buffer.putInt(sampleRate)
        buffer.putInt(sampleRate * 2)     // byte rate
        buffer.putShort(2)                // block align
        buffer.putShort(16)               // bits per sample
        buffer.put("data".toByteArray(Charsets.US_ASCII))
        buffer.putInt(dataSize)
        pcm.forEach { buffer.putShort(it) }
        return buffer.array()
    }

    fun base64(bytes: ByteArray): String = Base64.getEncoder().encodeToString(bytes)

    /** Growable float buffer, so minutes of audio don't become millions of boxed Floats. */
    private class FloatList {
        private var data = FloatArray(1 shl 16)
        var size = 0
            private set

        fun add(value: Float) {
            if (size == data.size) data = data.copyOf(data.size * 2)
            data[size++] = value
        }

        fun toArray(): FloatArray = data.copyOf(size)
    }
}
