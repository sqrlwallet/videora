package com.kevan.videorabykevan.data

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import java.io.File

class VoiceRecorderHelper(private val context: Context) {

    private var recorder: MediaRecorder? = null
    private var currentOutputFile: File? = null
    private var startTimeMs: Long = 0L

    fun startRecording(): File? {
        val dir = File(context.cacheDir, "voiceovers").apply { mkdirs() }
        val file = File(dir, "voice_${System.currentTimeMillis()}.m4a")
        currentOutputFile = file

        return try {
            val mr = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }

            mr.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioEncodingBitRate(128000)
                setAudioSamplingRate(44100)
                setOutputFile(file.absolutePath)
                prepare()
                start()
            }
            recorder = mr
            startTimeMs = System.currentTimeMillis()
            file
        } catch (e: Exception) {
            e.printStackTrace()
            stopRecording()
            null
        }
    }

    fun stopRecording(): File? {
        val file = currentOutputFile
        try {
            recorder?.apply {
                stop()
                release()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            recorder = null
        }
        currentOutputFile = null
        return if (file != null && file.exists() && file.length() > 0) file else null
    }

    fun isRecording(): Boolean = recorder != null

    fun getElapsedDurationMs(): Long {
        return if (isRecording()) System.currentTimeMillis() - startTimeMs else 0L
    }
}
