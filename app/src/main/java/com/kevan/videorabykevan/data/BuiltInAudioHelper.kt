package com.kevan.videorabykevan.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin
import kotlin.random.Random

data class SoundEffectItem(
    val id: String,
    val name: String,
    val category: String,
    val iconEmoji: String,
    val durationDesc: String
)

object BuiltInAudioHelper {

    val availableEffects = listOf(
        SoundEffectItem("whoosh", "Fast Whoosh", "Transitions", "💨", "0.4s"),
        SoundEffectItem("pop", "Bubble Pop", "UI & Elements", "🫧", "0.1s"),
        SoundEffectItem("bell", "Bell Ding", "Highlights", "🔔", "1.2s"),
        SoundEffectItem("bass_boom", "Dramatic Bass Boom", "Impacts", "💥", "1.5s"),
        SoundEffectItem("retro_coin", "Success Ding", "Gamification", "🪙", "0.4s"),
        SoundEffectItem("camera_click", "Camera Snap", "Media", "📸", "0.2s")
    )

    suspend fun getOrCreateSoundEffectFile(context: Context, effectId: String): File = withContext(Dispatchers.IO) {
        val sfxDir = File(context.cacheDir, "builtin_sfx").apply { mkdirs() }
        val targetFile = File(sfxDir, "$effectId.wav")
        if (targetFile.exists() && targetFile.length() > 100) {
            return@withContext targetFile
        }

        val sampleRate = 44100
        val pcmData = when (effectId) {
            "whoosh" -> generateWhooshPcm(sampleRate)
            "pop" -> generatePopPcm(sampleRate)
            "bell" -> generateBellPcm(sampleRate)
            "bass_boom" -> generateBassBoomPcm(sampleRate)
            "retro_coin" -> generateCoinPcm(sampleRate)
            "camera_click" -> generateCameraSnapPcm(sampleRate)
            else -> generatePopPcm(sampleRate)
        }

        writeWavFile(targetFile, pcmData, sampleRate)
        targetFile
    }

    private fun generateWhooshPcm(sampleRate: Int): ShortArray {
        val durationMs = 450
        val numSamples = (sampleRate * (durationMs / 1000f)).toInt()
        val samples = ShortArray(numSamples)
        var lastNoise = 0.0

        for (i in 0 until numSamples) {
            val progress = i.toDouble() / numSamples
            // Smooth bell envelope
            val envelope = sin(progress * PI) * sin(progress * PI)
            val whiteNoise = Random.nextDouble(-1.0, 1.0)
            // Low-pass filter smoothing
            val alpha = 0.15 + (0.5 * sin(progress * PI))
            val filtered = lastNoise + alpha * (whiteNoise - lastNoise)
            lastNoise = filtered

            val sampleVal = (filtered * envelope * 28000.0).toInt().coerceIn(-32768, 32767)
            samples[i] = sampleVal.toShort()
        }
        return samples
    }

    private fun generatePopPcm(sampleRate: Int): ShortArray {
        val durationMs = 90
        val numSamples = (sampleRate * (durationMs / 1000f)).toInt()
        val samples = ShortArray(numSamples)

        for (i in 0 until numSamples) {
            val progress = i.toDouble() / numSamples
            val envelope = (1.0 - progress) * (1.0 - progress)
            // Pitch slides down rapidly from 700Hz to 150Hz
            val freq = 700.0 - (550.0 * progress)
            val sampleVal = (sin(2.0 * PI * freq * i / sampleRate) * envelope * 28000.0).toInt()
            samples[i] = sampleVal.coerceIn(-32768, 32767).toShort()
        }
        return samples
    }

    private fun generateBellPcm(sampleRate: Int): ShortArray {
        val durationMs = 1200
        val numSamples = (sampleRate * (durationMs / 1000f)).toInt()
        val samples = ShortArray(numSamples)

        for (i in 0 until numSamples) {
            val t = i.toDouble() / sampleRate
            val decay = exp(-3.2 * t)
            // Harmonics
            val wave = 0.6 * sin(2.0 * PI * 1320.0 * t) +
                    0.3 * sin(2.0 * PI * 2640.0 * t) +
                    0.1 * sin(2.0 * PI * 3960.0 * t)

            val sampleVal = (wave * decay * 26000.0).toInt()
            samples[i] = sampleVal.coerceIn(-32768, 32767).toShort()
        }
        return samples
    }

    private fun generateBassBoomPcm(sampleRate: Int): ShortArray {
        val durationMs = 1500
        val numSamples = (sampleRate * (durationMs / 1000f)).toInt()
        val samples = ShortArray(numSamples)

        for (i in 0 until numSamples) {
            val t = i.toDouble() / sampleRate
            val progress = i.toDouble() / numSamples
            val decay = exp(-2.5 * t)
            // Frequency drops from 120Hz to 40Hz
            val freq = 120.0 - (80.0 * progress)
            val wave = sin(2.0 * PI * freq * t)

            val sampleVal = (wave * decay * 31000.0).toInt()
            samples[i] = sampleVal.coerceIn(-32768, 32767).toShort()
        }
        return samples
    }

    private fun generateCoinPcm(sampleRate: Int): ShortArray {
        val durationMs = 380
        val numSamples = (sampleRate * (durationMs / 1000f)).toInt()
        val samples = ShortArray(numSamples)
        val splitSample = (sampleRate * 0.08).toInt()

        for (i in 0 until numSamples) {
            val t = i.toDouble() / sampleRate
            val (freq, decay) = if (i < splitSample) {
                987.77 to 1.0
            } else {
                val t2 = (i - splitSample).toDouble() / sampleRate
                1318.51 to exp(-7.0 * t2)
            }
            val sampleVal = (sin(2.0 * PI * freq * t) * decay * 24000.0).toInt()
            samples[i] = sampleVal.coerceIn(-32768, 32767).toShort()
        }
        return samples
    }

    private fun generateCameraSnapPcm(sampleRate: Int): ShortArray {
        val durationMs = 180
        val numSamples = (sampleRate * (durationMs / 1000f)).toInt()
        val samples = ShortArray(numSamples)

        for (i in 0 until numSamples) {
            val t = i.toDouble() / sampleRate
            val envelope = exp(-25.0 * t) + if (t > 0.06) exp(-30.0 * (t - 0.06)) * 0.8 else 0.0
            val noise = Random.nextDouble(-1.0, 1.0)
            val sampleVal = (noise * envelope * 24000.0).toInt()
            samples[i] = sampleVal.coerceIn(-32768, 32767).toShort()
        }
        return samples
    }

    private fun writeWavFile(file: File, pcmData: ShortArray, sampleRate: Int) {
        val totalAudioLen = pcmData.size * 2L
        val totalDataLen = totalAudioLen + 36
        val channels = 1
        val byteRate = sampleRate * channels * 2

        val header = ByteArray(44)
        val buffer = ByteBuffer.wrap(header).order(ByteOrder.LITTLE_ENDIAN)

        buffer.put("RIFF".toByteArray())
        buffer.putInt(totalDataLen.toInt())
        buffer.put("WAVE".toByteArray())
        buffer.put("fmt ".toByteArray())
        buffer.putInt(16) // Subchunk1Size
        buffer.putShort(1) // AudioFormat (1 = PCM)
        buffer.putShort(channels.toShort())
        buffer.putInt(sampleRate)
        buffer.putInt(byteRate)
        buffer.putShort((channels * 2).toShort()) // BlockAlign
        buffer.putShort(16) // BitsPerSample
        buffer.put("data".toByteArray())
        buffer.putInt(totalAudioLen.toInt())

        FileOutputStream(file).use { fos ->
            fos.write(header)
            val pcmBytes = ByteArray(pcmData.size * 2)
            ByteBuffer.wrap(pcmBytes).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().put(pcmData)
            fos.write(pcmBytes)
        }
    }
}
