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
import androidx.media3.effect.Contrast
import androidx.media3.effect.Presentation
import androidx.media3.effect.RgbAdjustment
import androidx.media3.effect.RgbFilter
import androidx.media3.transformer.Composition
import androidx.media3.transformer.EditedMediaItem
import androidx.media3.transformer.EditedMediaItemSequence
import androidx.media3.transformer.Effects
import androidx.media3.transformer.ExportException
import androidx.media3.transformer.ExportResult
import androidx.media3.transformer.ProgressHolder
import androidx.media3.transformer.Transformer
import com.kevan.videorabykevan.model.ExportPreset
import com.kevan.videorabykevan.model.Project
import com.kevan.videorabykevan.model.VideoClip
import com.kevan.videorabykevan.model.VideoFilter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
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

class VideoExportManager(
    private val context: Context,
    private val coroutineScope: CoroutineScope
) {
    private val _exportState = MutableStateFlow(ExportProgressState())
    val exportState: StateFlow<ExportProgressState> = _exportState.asStateFlow()

    private var activeTransformer: Transformer? = null
    private var progressPollingJob: Job? = null

    fun exportProject(project: Project, preset: ExportPreset) {
        if (project.clips.isEmpty()) {
            _exportState.value = ExportProgressState(
                error = "Cannot export empty project. Add at least one video clip."
            )
            return
        }

        val outputDir = File(context.cacheDir, "exports").apply { mkdirs() }
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
                    _exportState.value = ExportProgressState(
                        isExporting = false,
                        progress = 1.0f,
                        statusText = "Export Complete!",
                        exportedFile = tempOutputFile,
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

        val transformer = Transformer.Builder(context)
            .setVideoMimeType(MimeTypes.VIDEO_H264)
            .setAudioMimeType(MimeTypes.AUDIO_AAC)
            .addListener(listener)
            .build()

        activeTransformer = transformer

        try {
            val editedClips = project.clips.map { clip ->
                buildEditedMediaItem(clip, preset)
            }

            val sequence = EditedMediaItemSequence.Builder(editedClips).build()
            val composition = Composition.Builder(listOf(sequence)).build()

            transformer.start(composition, tempOutputFile.absolutePath)

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

    private fun buildEditedMediaItem(clip: VideoClip, preset: ExportPreset): EditedMediaItem {
        val clippingConfig = MediaItem.ClippingConfiguration.Builder()
            .setStartPositionMs(clip.trimStartMs)
            .setEndPositionMs(if (clip.trimEndMs > 0) clip.trimEndMs else clip.durationMs)
            .build()

        val mediaItem = MediaItem.Builder()
            .setUri(Uri.parse(clip.uri))
            .setClippingConfiguration(clippingConfig)
            .build()

        val effectsList = mutableListOf<Effect>()

        // Force output dimensions and portrait scale
        effectsList.add(
            Presentation.createForWidthAndHeight(
                preset.targetWidth,
                preset.targetHeight,
                Presentation.LAYOUT_SCALE_TO_FIT_WITH_CROP
            )
        )

        // Apply video filter LUT adjustments
        when (clip.filter) {
            VideoFilter.VIBRANT -> {
                effectsList.add(
                    RgbAdjustment.Builder()
                        .setRedScale(1.15f)
                        .setGreenScale(1.15f)
                        .setBlueScale(1.10f)
                        .build()
                )
                effectsList.add(Contrast(0.15f))
            }
            VideoFilter.WARM_SUNSET -> {
                effectsList.add(
                    RgbAdjustment.Builder()
                        .setRedScale(1.25f)
                        .setGreenScale(1.05f)
                        .setBlueScale(0.85f)
                        .build()
                )
                effectsList.add(Contrast(0.05f))
            }
            VideoFilter.COOL_CINEMATIC -> {
                effectsList.add(
                    RgbAdjustment.Builder()
                        .setRedScale(0.85f)
                        .setGreenScale(1.05f)
                        .setBlueScale(1.20f)
                        .build()
                )
                effectsList.add(Contrast(0.10f))
            }
            VideoFilter.CYBERPUNK -> {
                effectsList.add(
                    RgbAdjustment.Builder()
                        .setRedScale(1.20f)
                        .setGreenScale(0.85f)
                        .setBlueScale(1.25f)
                        .build()
                )
                effectsList.add(Contrast(0.25f))
            }
            VideoFilter.VINTAGE_FILM -> {
                effectsList.add(
                    RgbAdjustment.Builder()
                        .setRedScale(1.10f)
                        .setGreenScale(1.00f)
                        .setBlueScale(0.80f)
                        .build()
                )
                effectsList.add(Contrast(-0.05f))
            }
            VideoFilter.MONOCHROME -> {
                effectsList.add(RgbFilter.createGrayscaleFilter())
                effectsList.add(Contrast(0.15f))
            }
            VideoFilter.NONE -> {}
        }

        return EditedMediaItem.Builder(mediaItem)
            .setEffects(Effects(emptyList(), effectsList))
            .setRemoveAudio(clip.isMuted)
            .build()
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
