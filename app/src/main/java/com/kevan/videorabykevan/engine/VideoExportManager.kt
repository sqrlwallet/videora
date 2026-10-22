package com.kevan.videorabykevan.engine

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import androidx.media3.common.Effect
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.ChannelMixingAudioProcessor
import androidx.media3.common.audio.ChannelMixingMatrix
import androidx.media3.common.audio.SonicAudioProcessor
import androidx.media3.effect.FrameDropEffect
import androidx.media3.effect.OverlayEffect
import androidx.media3.effect.Presentation
import androidx.media3.effect.ScaleAndRotateTransformation
import androidx.media3.effect.SpeedChangeEffect
import androidx.media3.effect.TextureOverlay
import androidx.media3.transformer.AudioEncoderSettings
import androidx.media3.transformer.Composition
import androidx.media3.transformer.DefaultEncoderFactory
import androidx.media3.transformer.EditedMediaItem
import androidx.media3.transformer.EditedMediaItemSequence
import androidx.media3.transformer.Effects
import androidx.media3.transformer.ExportException
import androidx.media3.transformer.ExportResult
import androidx.media3.transformer.ProgressHolder
import androidx.media3.transformer.Transformer
import androidx.media3.transformer.VideoEncoderSettings
import com.google.common.collect.ImmutableList
import com.kevan.videorabykevan.model.AudioTrack
import com.kevan.videorabykevan.model.ExportPreset
import com.kevan.videorabykevan.model.Project
import com.kevan.videorabykevan.model.VideoClip
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream

data class ExportProgressState(
    val isExporting: Boolean = false,
    val progress: Float = 0f, // 0.0 to 1.0
    val statusText: String = "",
    val error: String? = null,
    val exportedFile: File? = null,
    val mediaStoreUri: Uri? = null,
    val isSuccess: Boolean = false
)

/**
 * One per process (see [getInstance]) and outlives any editor screen, so an export keeps going
 * after the user leaves the editor; [ExportService] keeps the process alive meanwhile.
 */
class VideoExportManager private constructor(private val context: Context) {
    private val coroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val _exportState = MutableStateFlow(ExportProgressState())
    val exportState: StateFlow<ExportProgressState> = _exportState.asStateFlow()

    private var activeTransformer: Transformer? = null
    private var progressPollingJob: Job? = null

    fun exportProject(project: Project, preset: ExportPreset) {
        // Transformer runs one export at a time; a second one would also clobber the shared state
        if (_exportState.value.isExporting) return
        if (project.clips.isEmpty()) {
            _exportState.value = ExportProgressState(
                error = "Cannot export empty project. Add at least one video clip."
            )
            return
        }

        val outputDir = File(context.cacheDir, "exports").apply { mkdirs() }
        // Earlier exports were already copied to the gallery (or failed); don't let them pile up
        outputDir.listFiles()?.forEach { it.delete() }
        val fileName = "videora_${System.currentTimeMillis()}_${preset.badge.lowercase()}.mp4"
        val tempOutputFile = File(outputDir, fileName)

        _exportState.value = ExportProgressState(
            isExporting = true,
            progress = 0f,
            statusText = "Configuring ${preset.title}..."
        )

        val progressHolder = ProgressHolder()

        val listener = object : Transformer.Listener {
            override fun onCompleted(composition: Composition, exportResult: ExportResult) {
                progressPollingJob?.cancel()
                coroutineScope.launch(Dispatchers.IO) {
                    val publicUri = saveToGallery(tempOutputFile, fileName)
                    // Once the gallery has its copy, the cache file is redundant; keep it only as the share fallback
                    if (publicUri != null) {
                        tempOutputFile.delete()
                    }
                    _exportState.value = ExportProgressState(
                        isExporting = false,
                        progress = 1.0f,
                        statusText = "Export Complete!",
                        exportedFile = tempOutputFile.takeIf { publicUri == null },
                        mediaStoreUri = publicUri,
                        isSuccess = true
                    )
                }
            }

            override fun onError(
                composition: Composition,
                exportResult: ExportResult,
                exportException: ExportException
            ) {
                progressPollingJob?.cancel()
                _exportState.value = ExportProgressState(
                    isExporting = false,
                    error = "Export failed: ${exportException.localizedMessage ?: "Unknown error"}"
                )
            }
        }

        // Bitrates from the preset; fallback lets devices that can't hit them pick the closest supported value
        val encoderFactory = DefaultEncoderFactory.Builder(context)
            .setRequestedVideoEncoderSettings(
                VideoEncoderSettings.Builder().setBitrate(preset.videoBitrateBps).build()
            )
            .setRequestedAudioEncoderSettings(
                AudioEncoderSettings.Builder().setBitrate(preset.audioBitrateBps).build()
            )
            .setEnableFallback(true)
            .build()

        val transformer = Transformer.Builder(context)
            .setEncoderFactory(encoderFactory)
            .setVideoMimeType(MimeTypes.VIDEO_H264)
            .setAudioMimeType(MimeTypes.AUDIO_AAC)
            .addListener(listener)
            .build()

        activeTransformer = transformer

        try {
            val composition = buildComposition(project, preset)

            transformer.start(composition, tempOutputFile.absolutePath)
            ExportService.start(context)

            // Start polling progress
            progressPollingJob?.cancel()
            progressPollingJob = coroutineScope.launch(Dispatchers.Main) {
                while (isActive) {
                    val progressState = transformer.getProgress(progressHolder)
                    if (progressState == Transformer.PROGRESS_STATE_AVAILABLE) {
                        val percentage = progressHolder.progress / 100f
                        _exportState.value = _exportState.value.copy(
                            progress = percentage,
                            statusText = "Encoding ${preset.badge} (${(percentage * 100).toInt()}%)"
                        )
                    }
                    delay(100)
                }
            }
        } catch (e: Exception) {
            progressPollingJob?.cancel()
            _exportState.value = ExportProgressState(
                isExporting = false,
                error = "Failed to start export: ${e.message}"
            )
        }
    }

    private fun buildComposition(project: Project, preset: ExportPreset): Composition {
        val (frameWidth, frameHeight) = preset.outputSizeFor(project.aspectRatio)

        val videoSequence = EditedMediaItemSequence.Builder(
            project.clips.map { buildEditedMediaItem(it, frameWidth, frameHeight, project.isFitMode) }
        ).build()

        // Each music / voiceover / SFX track becomes its own audio-only sequence that the mixer
        // blends with the clips' original audio.
        val audioSequences = project.audioTracks.mapNotNull {
            buildAudioSequence(it, project.totalDurationMs)
        }

        val compositionVideoEffects = mutableListOf<Effect>()
        // Overlays use the output timeline (after speed changes), matching the preview playhead
        if (project.textOverlays.isNotEmpty()) {
            compositionVideoEffects.add(
                OverlayEffect(
                    ImmutableList.of<TextureOverlay>(TextOverlayBitmapOverlay(project.textOverlays, frameWidth, frameHeight))
                )
            )
        }
        // Caps the output at the preset's frame rate (sped-up clips would otherwise exceed it)
        compositionVideoEffects.add(FrameDropEffect.createDefaultFrameDropEffect(preset.targetFps.toFloat()))

        return Composition.Builder(listOf(videoSequence) + audioSequences)
            .setEffects(Effects(emptyList(), compositionVideoEffects))
            // Generates silence for clips without audio (muted clips, demo clips) so the
            // video sequence always has an audio track to mix into
            .experimentalSetForceAudioTrack(true)
            .build()
    }

    private fun buildAudioSequence(track: AudioTrack, projectDurationMs: Long): EditedMediaItemSequence? {
        val startOffsetMs = track.timelineStartOffsetMs.coerceAtLeast(0L)
        val availableMs = projectDurationMs - startOffsetMs
        if (availableMs <= 0L || track.volume <= 0f) return null

        val trimStartMs = track.trimStartMs.coerceAtLeast(0L)
        val knownLengthMs = track.effectiveDurationMs.takeIf { track.trimEndMs > trimStartMs }

        val items = mutableListOf<EditedMediaItem>()
        var remainingMs = availableMs
        do {
            // Unknown source length: play once from the trim start, cut off at the end of the video
            val segmentMs = knownLengthMs?.let { minOf(it, remainingMs) } ?: remainingMs
            items += buildAudioItem(track, trimStartMs, trimStartMs + segmentMs)
            remainingMs -= segmentMs
        } while (track.isLooping && knownLengthMs != null && remainingMs > 0L)

        val builder = EditedMediaItemSequence.Builder()
        if (startOffsetMs > 0L) {
            builder.addGap(startOffsetMs * 1000)
        }
        return builder.addItems(items).build()
    }

    private fun buildAudioItem(track: AudioTrack, startMs: Long, endMs: Long): EditedMediaItem {
        val mediaItem = MediaItem.Builder()
            .setUri(Uri.parse(track.uri))
            .setClippingConfiguration(
                MediaItem.ClippingConfiguration.Builder()
                    .setStartPositionMs(startMs)
                    .setEndPositionMs(endMs)
                    .build()
            )
            .build()

        return EditedMediaItem.Builder(mediaItem)
            .setRemoveVideo(true)
            .setEffects(Effects(listOfNotNull(volumeProcessor(track.volume)), emptyList()))
            .build()
    }

    private fun buildEditedMediaItem(
        clip: VideoClip,
        frameWidth: Int,
        frameHeight: Int,
        isFitMode: Boolean
    ): EditedMediaItem {
        val clippingConfig = MediaItem.ClippingConfiguration.Builder()
            .setStartPositionMs(clip.trimStartMs)
            .setEndPositionMs(if (clip.trimEndMs > 0) clip.trimEndMs else clip.durationMs)
            .build()

        val mediaItem = MediaItem.Builder()
            .setUri(Uri.parse(clip.uri))
            .setClippingConfiguration(clippingConfig)
            .build()

        val effectsList = mutableListOf<Effect>()
        val audioProcessors = mutableListOf<AudioProcessor>()

        // Speed: retime video frames and time-stretch audio without changing pitch (same as preview)
        if (clip.speed > 0.05f && clip.speed != 1.0f) {
            effectsList.add(SpeedChangeEffect(clip.speed))
            audioProcessors.add(SonicAudioProcessor().apply {
                setSpeed(clip.speed)
                setPitch(1.0f)
            })
        }

        volumeProcessor(clip.volume)?.let { audioProcessors.add(it) }

        // Handle rotation and horizontal flip
        if (clip.rotationDegrees != 0 || clip.isFlippedHorizontal) {
            effectsList.add(
                ScaleAndRotateTransformation.Builder()
                    // Rotate is clockwise in the editor; Media3 rotates counter-clockwise
                    .setRotationDegrees(-clip.rotationDegrees.toFloat())
                    .setScale(if (clip.isFlippedHorizontal) -1f else 1f, 1f)
                    .build()
            )
        }

        // Scale every clip into the project's frame: Fill crops, Fit letterboxes
        effectsList.add(
            Presentation.createForWidthAndHeight(
                frameWidth,
                frameHeight,
                if (isFitMode) Presentation.LAYOUT_SCALE_TO_FIT else Presentation.LAYOUT_SCALE_TO_FIT_WITH_CROP
            )
        )

        // Filter + color correction as one matrix: the exact one the preview shows
        ClipColorMatrix.exportEffectFor(clip)?.let { effectsList.add(it) }

        return EditedMediaItem.Builder(mediaItem)
            .setEffects(Effects(audioProcessors, effectsList))
            .setRemoveAudio(clip.isMuted)
            .build()
    }

    /**
     * Scales PCM samples by [requested]; null when no change is needed. Capped at 100% because the
     * preview's ExoPlayer can't play louder, and older projects may still hold boosts up to 150%.
     */
    private fun volumeProcessor(requested: Float): AudioProcessor? {
        val volume = requested.coerceIn(0f, 1f)
        if (volume == 1.0f) return null
        return ChannelMixingAudioProcessor().apply {
            for (channels in 1..MAX_CHANNELS) {
                val coefficients = FloatArray(channels * channels) { i ->
                    if (i % (channels + 1) == 0) volume else 0f
                }
                putChannelMixingMatrix(ChannelMixingMatrix(channels, channels, coefficients))
            }
        }
    }

    private suspend fun saveToGallery(sourceFile: File, displayName: String): Uri? = withContext(Dispatchers.IO) {
        val resolver = context.contentResolver
        val contentValues = ContentValues().apply {
            put(MediaStore.Video.Media.DISPLAY_NAME, displayName)
            put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
            put(MediaStore.Video.Media.TITLE, displayName)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Video.Media.RELATIVE_PATH, "${Environment.DIRECTORY_MOVIES}/Videora")
                put(MediaStore.Video.Media.IS_PENDING, 1)
            }
        }

        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        } else {
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        }

        val uri = resolver.insert(collection, contentValues) ?: return@withContext null

        try {
            resolver.openOutputStream(uri)?.use { out ->
                FileInputStream(sourceFile).use { input ->
                    input.copyTo(out)
                }
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                contentValues.clear()
                contentValues.put(MediaStore.Video.Media.IS_PENDING, 0)
                resolver.update(uri, contentValues, null, null)
            }
            uri
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun cancelExport() {
        progressPollingJob?.cancel()
        activeTransformer?.cancel()
        activeTransformer = null
        _exportState.value = ExportProgressState()
    }

    fun resetState() {
        _exportState.value = ExportProgressState()
    }

    companion object {
        private const val MAX_CHANNELS = 8

        @Volatile
        private var instance: VideoExportManager? = null

        fun getInstance(context: Context): VideoExportManager =
            instance ?: synchronized(this) {
                instance ?: VideoExportManager(context.applicationContext).also { instance = it }
            }

        fun createShareIntent(context: Context, file: File?, mediaUri: Uri?): Intent {
            val shareUri = mediaUri ?: file?.let {
                FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    it
                )
            }

            return Intent(Intent.ACTION_SEND).apply {
                type = "video/mp4"
                putExtra(Intent.EXTRA_STREAM, shareUri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
        }
    }
}
