package com.kevan.videorabykevan.engine

import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.BaseAudioProcessor
import com.kevan.videorabykevan.model.AudioTrack
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Applies a track's volume and fade in/out ([AudioTrack.fadeGainAt]) to one segment of that
 * track in the export. A looping track is split into several segments, so [segmentStartMs] says
 * where this one begins within the track's [trackLengthMs] on the timeline.
 */
class FadeAudioProcessor(
    private val track: AudioTrack,
    private val volume: Float,
    private val segmentStartMs: Long,
    private val trackLengthMs: Long
) : BaseAudioProcessor() {

    private var framesProcessed = 0L

    override fun onConfigure(inputAudioFormat: AudioProcessor.AudioFormat): AudioProcessor.AudioFormat {
        if (inputAudioFormat.encoding != C.ENCODING_PCM_16BIT && inputAudioFormat.encoding != C.ENCODING_PCM_FLOAT) {
            throw AudioProcessor.UnhandledAudioFormatException(inputAudioFormat)
        }
        return inputAudioFormat
    }

    override fun queueInput(inputBuffer: ByteBuffer) {
        val format = inputAudioFormat
        val remaining = inputBuffer.remaining()
        if (remaining == 0) return
        val output = replaceOutputBuffer(remaining)
        val input = inputBuffer.order(ByteOrder.nativeOrder())
        val isFloat = format.encoding == C.ENCODING_PCM_FLOAT
        val frames = remaining / format.bytesPerFrame

        for (frame in 0 until frames) {
            val positionMs = segmentStartMs + (framesProcessed + frame) * 1000L / format.sampleRate
            val gain = volume * track.fadeGainAt(positionMs, trackLengthMs)
            for (channel in 0 until format.channelCount) {
                if (isFloat) {
                    output.putFloat(input.getFloat() * gain)
                } else {
                    val sample = (input.getShort() * gain).toInt()
                    output.putShort(sample.coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort())
                }
            }
        }
        framesProcessed += frames
        output.flip()
    }

    override fun onFlush() {
        framesProcessed = 0L
    }

    override fun onReset() {
        framesProcessed = 0L
    }
}
