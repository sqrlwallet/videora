package com.kevan.videorabykevan.data

import android.content.Context
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import java.io.File
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Text-to-speech for text overlays ("Read aloud"), using whichever TTS engine the device has.
 * The app itself sends nothing anywhere: the engine runs as its own system service.
 */
object TextToSpeechHelper {

    private const val TIMEOUT_MS = 30_000L
    // A WAV header alone is 44 bytes, so anything that size or smaller has no audio
    private const val WAV_HEADER_BYTES = 44L

    /** Speaks [text] into a WAV file in the cache. Null when there's no engine or it fails. */
    suspend fun synthesizeToFile(context: Context, text: String): File? = withTimeoutOrNull(TIMEOUT_MS) {
        suspendCancellableCoroutine { cont ->
            val output = File(context.cacheDir, "tts_${System.currentTimeMillis()}.wav")
            var engine: TextToSpeech? = null

            fun finish(file: File?) {
                engine?.shutdown()
                if (cont.isActive) cont.resume(file)
            }

            engine = TextToSpeech(context.applicationContext) { status ->
                val tts = engine
                if (status != TextToSpeech.SUCCESS || tts == null) {
                    finish(null)
                    return@TextToSpeech
                }
                tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {}

                    override fun onDone(utteranceId: String?) {
                        finish(output.takeIf { it.length() > WAV_HEADER_BYTES })
                    }

                    @Deprecated("Deprecated in Java")
                    override fun onError(utteranceId: String?) {
                        finish(null)
                    }

                    override fun onError(utteranceId: String?, errorCode: Int) {
                        finish(null)
                    }
                })
                if (tts.synthesizeToFile(text, Bundle(), output, "videora_tts") != TextToSpeech.SUCCESS) {
                    finish(null)
                }
            }
            cont.invokeOnCancellation { engine?.shutdown() }
        }
    }
}
