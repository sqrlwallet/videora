package com.kevan.videorabykevan.ui.viewmodel

import android.app.Application
import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.kevan.videorabykevan.ai.ColorSuggestion
import com.kevan.videorabykevan.data.BuiltInAudioHelper
import com.kevan.videorabykevan.data.ProjectMediaStore
import com.kevan.videorabykevan.data.ProjectRepository
import com.kevan.videorabykevan.data.SampleMediaHelper
import com.kevan.videorabykevan.data.SoundEffectItem
import com.kevan.videorabykevan.data.StillVideoEncoder
import com.kevan.videorabykevan.data.TextToSpeechHelper
import com.kevan.videorabykevan.engine.ExportProgressState
import com.kevan.videorabykevan.engine.PlayerState
import com.kevan.videorabykevan.engine.VideoExportManager
import com.kevan.videorabykevan.engine.VideoPlayerManager
import com.kevan.videorabykevan.model.AspectRatioPreset
import com.kevan.videorabykevan.model.AudioTrack
import com.kevan.videorabykevan.model.ExportPreset
import com.kevan.videorabykevan.model.FREEZE_DEFAULT_MS
import com.kevan.videorabykevan.model.MIN_TRIM_MS
import com.kevan.videorabykevan.model.PHOTO_DEFAULT_MS
import com.kevan.videorabykevan.model.STILL_SOURCE_MS
import com.kevan.videorabykevan.model.Project
import com.kevan.videorabykevan.model.SafeZoneType
import com.kevan.videorabykevan.model.TextAnimation
import com.kevan.videorabykevan.model.TextOverlay
import com.kevan.videorabykevan.model.TextStylePreset
import com.kevan.videorabykevan.model.VideoClip
import com.kevan.videorabykevan.model.VideoFilter
import com.kevan.videorabykevan.model.VoiceEffect
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class EditorUiState(
    val project: Project = Project(),
    // -1 = no clip selected (the toolbar then shows project-level tools)
    val selectedClipIndex: Int = -1,
    val selectedOverlayId: String? = null,
    val selectedAudioTrackId: String? = null,
    val isLoading: Boolean = false,
    val canUndo: Boolean = false,
    val canRedo: Boolean = false,
    val showTrimSheet: Boolean = false,
    val showSpeedSheet: Boolean = false,
    val showFilterSheet: Boolean = false,
    val showTextEditorSheet: Boolean = false,
    val showAudioSheet: Boolean = false,
    val showSafeZoneSheet: Boolean = false,
    val showExportDialog: Boolean = false,
    val showVoiceoverSheet: Boolean = false,
    val showStickerSheet: Boolean = false,
    val showVolumeSheet: Boolean = false,
    val showAdjustSheet: Boolean = false,
    val showFadeSheet: Boolean = false,
    val showVoiceEffectSheet: Boolean = false,
    val showAiToolsSheet: Boolean = false,
    val isComparingOriginal: Boolean = false
)

private const val MIN_OVERLAY_MS = 300L

private val PART_SUFFIX = Regex("""(\s\(Part \d+\))+$""")

/** "Beach (Part 2)" split again gives "Beach (Part 1)"/"Beach (Part 2)", not "Beach (Part 2) (Part 1)". */
internal fun splitPartName(name: String, part: Int): String = "${name.replace(PART_SUFFIX, "")} (Part $part)"

/** One-off feedback shown as a snackbar; [offerUndo] adds an Undo action. */
data class EditorMessage(val text: String, val offerUndo: Boolean = false)

class EditorViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = ProjectRepository(application.applicationContext)
    val playerManager = VideoPlayerManager(application.applicationContext, viewModelScope)
    val exportManager = VideoExportManager.getInstance(application)

    val playerState: StateFlow<PlayerState> = playerManager.playerState
    val exportState: StateFlow<ExportProgressState> = exportManager.exportState

    private val _uiState = MutableStateFlow(EditorUiState())
    val uiState: StateFlow<EditorUiState> = _uiState.asStateFlow()

    private val _messages = MutableSharedFlow<EditorMessage>(extraBufferCapacity = 4)
    val messages: SharedFlow<EditorMessage> = _messages.asSharedFlow()

    private fun showMessage(text: String, offerUndo: Boolean = false) {
        _messages.tryEmit(EditorMessage(text, offerUndo))
    }

    init {
        // Keep the preview's music / voiceover / SFX players in step with the project
        viewModelScope.launch {
            _uiState.map { it.project.audioTracks }.distinctUntilChanged().collect {
                playerManager.setAudioTracks(it)
            }
        }
        viewModelScope.launch {
            _uiState.map { it.project.duckMusicUnderVoice }.distinctUntilChanged().collect {
                playerManager.setDucking(it)
            }
        }
    }

    private val undoStack = mutableListOf<Project>()
    private val redoStack = mutableListOf<Project>()

    fun loadProject(projectId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val project = repository.getProject(projectId) ?: Project(id = projectId)

            // Move media the project still reads from the cache or from expiring content:// grants
            // into the project's own folder, so it keeps working after the cache is cleared
            val context = getApplication<Application>().applicationContext
            val localized = ProjectMediaStore.localizeMedia(context, project)
            val updatedProject = localized.copy(audioTracks = ProjectMediaStore.measureAudioTracks(context, localized.audioTracks))
            if (updatedProject != project) {
                repository.saveProject(updatedProject)
            }

            undoStack.clear()
            redoStack.clear()
            _uiState.value = _uiState.value.copy(
                project = updatedProject,
                selectedClipIndex = -1,
                selectedOverlayId = null,
                canUndo = false,
                canRedo = false,
                isLoading = false
            )
            playerManager.setClips(updatedProject.clips, keepPosition = false)
        }
    }

    private fun pushUndoState(current: Project) {
        undoStack.add(current)
        if (undoStack.size > 25) {
            undoStack.removeAt(0)
        }
        redoStack.clear()
        updateUndoRedoAvailability()
    }

    fun undo() {
        if (undoStack.isNotEmpty()) {
            val prev = undoStack.removeAt(undoStack.lastIndex)
            redoStack.add(_uiState.value.project)
            _uiState.value = _uiState.value.copy(
                project = prev,
                selectedClipIndex = _uiState.value.selectedClipIndex.coerceIn(-1, prev.clips.size - 1)
            )
            updateUndoRedoAvailability()
            playerManager.setClips(prev.clips)
            persistProject()
        }
    }

    fun redo() {
        if (redoStack.isNotEmpty()) {
            val next = redoStack.removeAt(redoStack.lastIndex)
            undoStack.add(_uiState.value.project)
            _uiState.value = _uiState.value.copy(
                project = next,
                selectedClipIndex = _uiState.value.selectedClipIndex.coerceIn(-1, next.clips.size - 1)
            )
            updateUndoRedoAvailability()
            playerManager.setClips(next.clips)
            persistProject()
        }
    }

    private fun updateUndoRedoAvailability() {
        _uiState.value = _uiState.value.copy(
            canUndo = undoStack.isNotEmpty(),
            canRedo = redoStack.isNotEmpty()
        )
    }

    private fun persistProject() {
        val current = _uiState.value.project
        viewModelScope.launch {
            repository.saveProject(current)
        }
    }

    // Clip Management
    fun addVideoUris(uris: List<Uri>, context: Context) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val newClips = importVideos(uris, context)

            pushUndoState(_uiState.value.project)
            val updatedClips = _uiState.value.project.clips + newClips
            val updatedProject = _uiState.value.project.copy(clips = updatedClips)
            _uiState.value = _uiState.value.copy(
                project = updatedProject,
                selectedClipIndex = updatedClips.lastIndex.coerceAtLeast(0),
                isLoading = false
            )
            playerManager.setClips(updatedClips)
            persistProject()
        }
    }

    /**
     * Swaps the selected clip's video for [uri] but keeps its edits (speed, color, volume, mirror...)
     * and, where the new video is long enough, its length on the timeline.
     */
    fun replaceSelectedClip(uri: Uri, context: Context) {
        val index = _uiState.value.selectedClipIndex
        val old = _uiState.value.project.clips.getOrNull(index) ?: return
        viewModelScope.launch {
            val imported = importVideos(listOf(uri), context).firstOrNull()
            if (imported == null) {
                showMessage("Couldn't open that video")
                return@launch
            }
            val clips = _uiState.value.project.clips.toMutableList()
            if (index !in clips.indices || clips[index].id != old.id) return@launch
            pushUndoState(_uiState.value.project)
            clips[index] = old.copy(
                uri = imported.uri,
                name = imported.name,
                durationMs = imported.durationMs,
                isPhoto = imported.isPhoto,
                trimStartMs = 0L,
                trimEndMs = old.trimmedDurationMs.coerceIn(MIN_TRIM_MS.coerceAtMost(imported.durationMs), imported.durationMs)
            )
            _uiState.value = _uiState.value.copy(project = _uiState.value.project.copy(clips = clips))
            playerManager.setClips(clips)
            persistProject()
            showMessage("Clip replaced", offerUndo = true)
        }
    }

    /**
     * Copies picked videos into the project folder (fast seeks, and they survive the picker's grant
     * expiring). Photos are encoded into short still videos so they behave like any other clip.
     */
    private suspend fun importVideos(uris: List<Uri>, context: Context): List<VideoClip> {
        val newClips = mutableListOf<VideoClip>()
        val projectDir = File(ProjectMediaStore.projectDir(context, _uiState.value.project.id), "media")

        withContext(Dispatchers.IO) {
            projectDir.mkdirs()
            for ((index, uri) in uris.withIndex()) {
                if (context.contentResolver.getType(uri)?.startsWith("image/") == true) {
                    val bitmap = StillVideoEncoder.decodeImage(context, uri) ?: continue
                    val title = uri.lastPathSegment?.substringAfterLast('/') ?: "Photo ${newClips.size + 1}"
                    stillClip(bitmap, File(projectDir, "photo_${System.currentTimeMillis()}_$index.mp4"), title, PHOTO_DEFAULT_MS)
                        ?.let { newClips.add(it) }
                    bitmap.recycle()
                    continue
                }
                try {
                    // Copy content URI to local storage for instant zero-latency seeks and 100% persistent playback
                    val targetFile = File(projectDir, "clip_${System.currentTimeMillis()}_$index.mp4")
                    var copiedSuccessfully = false

                    try {
                        context.contentResolver.openInputStream(uri)?.use { input ->
                            targetFile.outputStream().use { output ->
                                input.copyTo(output)
                            }
                        }
                        copiedSuccessfully = targetFile.exists() && targetFile.length() > 0
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }

                    val finalUri = if (copiedSuccessfully) {
                        Uri.fromFile(targetFile).toString()
                    } else {
                        uri.toString()
                    }

                    val retriever = MediaMetadataRetriever()
                    try {
                        if (copiedSuccessfully) {
                            retriever.setDataSource(targetFile.absolutePath)
                        } else {
                            retriever.setDataSource(context, uri)
                        }
                        val durationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                        val durationMs = durationStr?.toLongOrNull() ?: 5000L
                        val title = uri.lastPathSegment?.substringAfterLast('/') ?: "Clip ${newClips.size + 1}"

                        newClips.add(
                            VideoClip(
                                uri = finalUri,
                                name = title,
                                durationMs = durationMs,
                                trimStartMs = 0L,
                                trimEndMs = durationMs
                            )
                        )
                    } catch (e: Exception) {
                        e.printStackTrace()
                    } finally {
                        try { retriever.release() } catch (_: Exception) {}
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
        return newClips
    }

    /** Encodes [bitmap] as a still clip showing for [showMs] (stretchable up to [STILL_SOURCE_MS]). */
    private suspend fun stillClip(bitmap: android.graphics.Bitmap, file: File, name: String, showMs: Long): VideoClip? {
        if (!StillVideoEncoder.encode(bitmap, STILL_SOURCE_MS, file)) return null
        val durationMs = withContext(Dispatchers.IO) {
            val retriever = MediaMetadataRetriever()
            try {
                retriever.setDataSource(file.absolutePath)
                retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull()
            } catch (e: Exception) {
                null
            } finally {
                try { retriever.release() } catch (_: Exception) {}
            }
        } ?: return null
        return VideoClip(
            uri = Uri.fromFile(file).toString(),
            name = name,
            durationMs = durationMs,
            trimStartMs = 0L,
            trimEndMs = showMs.coerceAtMost(durationMs),
            isPhoto = true
        )
    }

    /**
     * TikTok-style freeze frame: holds the frame under the playhead for a couple of seconds, cutting
     * the clip there (or adding it before / after the clip when the playhead is at an edge).
     */
    fun freezeFrameAtPlayhead() {
        val clips = _uiState.value.project.clips
        if (clips.isEmpty()) return
        val position = playerState.value.currentPositionMs
        var clipStart = 0L
        val index = clips.indices.firstOrNull { i ->
            val end = clipStart + clips[i].playbackDurationMs
            (position < end || i == clips.lastIndex).also { if (!it) clipStart = end }
        } ?: return
        val clip = clips[index]
        val sourceMs = (clip.trimStartMs + ((position - clipStart).coerceAtLeast(0L) * clip.speed).toLong())
            .coerceIn(clip.trimStartMs, clip.trimEndMs)

        viewModelScope.launch {
            showMessage("Freezing frame…")
            val context = getApplication<Application>().applicationContext
            val frame = withContext(Dispatchers.IO) {
                val retriever = MediaMetadataRetriever()
                try {
                    retriever.setDataSource(context, Uri.parse(clip.uri))
                    retriever.getFrameAtTime(sourceMs * 1000L, MediaMetadataRetriever.OPTION_CLOSEST)
                } catch (e: Exception) {
                    null
                } finally {
                    try { retriever.release() } catch (_: Exception) {}
                }
            }
            val projectDir = File(context.filesDir, "projects/${_uiState.value.project.id}/clips").apply { mkdirs() }
            val still = frame?.let { stillClip(it, File(projectDir, "freeze_${System.currentTimeMillis()}.mp4"), "Freeze · ${clip.name}", FREEZE_DEFAULT_MS) }
            frame?.recycle()
            if (still == null) {
                showMessage("Couldn't grab that frame")
                return@launch
            }
            // The frozen frame keeps the clip's look
            val freeze = clip.copyColorTo(still).copy(rotationDegrees = clip.rotationDegrees, isFlippedHorizontal = clip.isFlippedHorizontal)

            val current = _uiState.value.project.clips.toMutableList()
            if (current.getOrNull(index)?.id != clip.id) return@launch
            pushUndoState(_uiState.value.project)
            val freezeIndex = when {
                sourceMs <= clip.trimStartMs + MIN_TRIM_MS -> index
                sourceMs >= clip.trimEndMs - MIN_TRIM_MS -> index + 1
                else -> {
                    current[index] = clip.copy(name = splitPartName(clip.name, 1), trimEndMs = sourceMs)
                    current.add(index + 1, clip.copy(id = java.util.UUID.randomUUID().toString(), name = splitPartName(clip.name, 2), trimStartMs = sourceMs))
                    index + 1
                }
            }
            current.add(freezeIndex, freeze)
            _uiState.value = _uiState.value.copy(
                project = _uiState.value.project.copy(clips = current),
                selectedClipIndex = freezeIndex,
                selectedOverlayId = null,
                selectedAudioTrackId = null
            )
            playerManager.setClips(current)
            persistProject()
            showMessage("Freeze frame added", offerUndo = true)
        }
    }

    fun addDemoClip(context: Context) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val file = SampleMediaHelper.generateSampleVideoClip(
                context = context,
                clipTitle = "Scene ${_uiState.value.project.clips.size + 1}",
                durationSeconds = 4
            )

            val ownedFile = ProjectMediaStore.adoptFile(context, _uiState.value.project.id, file, move = true)
            val newClip = VideoClip(
                uri = Uri.fromFile(ownedFile).toString(),
                name = "Demo Scene ${_uiState.value.project.clips.size + 1}",
                durationMs = 4000L,
                trimStartMs = 0L,
                trimEndMs = 4000L
            )

            pushUndoState(_uiState.value.project)
            val updatedClips = _uiState.value.project.clips + newClip
            val updatedProject = _uiState.value.project.copy(clips = updatedClips)
            _uiState.value = _uiState.value.copy(
                project = updatedProject,
                selectedClipIndex = updatedClips.lastIndex,
                isLoading = false
            )
            playerManager.setClips(updatedClips)
            persistProject()
        }
    }

    /**
     * Tapping the selected clip again deselects it; tapping another selects it. The playhead only
     * jumps to the clip's start when it's outside the clip, so "select, then Split" cuts where the user aimed.
     */
    fun selectClip(index: Int) {
        val clips = _uiState.value.project.clips
        if (index !in clips.indices) return
        if (index == _uiState.value.selectedClipIndex) {
            clearSelection()
            return
        }
        _uiState.value = _uiState.value.copy(selectedClipIndex = index, selectedOverlayId = null, selectedAudioTrackId = null)
        val clipStart = clips.take(index).sumOf { it.playbackDurationMs }
        val position = playerState.value.currentPositionMs
        if (position !in clipStart until clipStart + clips[index].playbackDurationMs) {
            playerManager.seekToTimeline(clipStart)
        }
    }

    fun clearSelection() {
        _uiState.value = _uiState.value.copy(selectedClipIndex = -1, selectedOverlayId = null, selectedAudioTrackId = null)
    }

    // Color adjustments: live preview while dragging, one undo step per gesture
    private var adjustmentSnapshot: Project? = null

    /** Live update while a slider moves; [commitClipAdjustments] records it for undo. */
    fun updateSelectedClipColor(transform: (VideoClip) -> VideoClip) {
        val index = _uiState.value.selectedClipIndex
        val clips = _uiState.value.project.clips.toMutableList()
        if (index !in clips.indices) return
        if (adjustmentSnapshot == null) adjustmentSnapshot = _uiState.value.project
        clips[index] = transform(clips[index])
        _uiState.value = _uiState.value.copy(project = _uiState.value.project.copy(clips = clips))
    }

    fun resetSelectedClipColor() {
        updateSelectedClipColor { it.withoutColorAdjustments() }
        commitClipAdjustments()
    }

    /** Copies the selected clip's filter and color correction to every clip, so a whole video matches. */
    fun applySelectedClipColorToAll() {
        val source = _uiState.value.project.clips.getOrNull(_uiState.value.selectedClipIndex) ?: return
        pushUndoState(_uiState.value.project)
        val clips = _uiState.value.project.clips.map { source.copyColorTo(it) }
        _uiState.value = _uiState.value.copy(project = _uiState.value.project.copy(clips = clips))
        persistProject()
        showMessage("Color applied to all ${clips.size} clips", offerUndo = true)
    }

    /** While true the preview shows the clip without its filter and color correction. */
    fun setComparingOriginal(comparing: Boolean) = _uiState.update { it.copy(isComparingOriginal = comparing) }

    fun commitClipAdjustments() {
        val snapshot = adjustmentSnapshot ?: return
        adjustmentSnapshot = null
        if (snapshot == _uiState.value.project) return
        pushUndoState(snapshot)
        persistProject()
    }

    fun setSelectedClipVolume(volume: Float) {
        val index = _uiState.value.selectedClipIndex
        val clips = _uiState.value.project.clips.toMutableList()
        if (index in clips.indices) {
            pushUndoState(_uiState.value.project)
            clips[index] = clips[index].copy(volume = volume.coerceIn(0f, 1f), isMuted = volume <= 0f)
            val updatedProject = _uiState.value.project.copy(clips = clips)
            _uiState.value = _uiState.value.copy(project = updatedProject)
            playerManager.setClips(clips)
            persistProject()
        }
    }

    /** Long-press drag in the timeline: [to] is the clip's index after the move. */
    fun moveClip(from: Int, to: Int) {
        val clips = _uiState.value.project.clips.toMutableList()
        if (from !in clips.indices || to !in clips.indices || from == to) return
        pushUndoState(_uiState.value.project)
        clips.add(to, clips.removeAt(from))
        _uiState.value = _uiState.value.copy(project = _uiState.value.project.copy(clips = clips), selectedClipIndex = to)
        playerManager.setClips(clips)
        persistProject()
    }

    fun moveClipLeft(index: Int) {
        val clips = _uiState.value.project.clips.toMutableList()
        if (index > 0 && index < clips.size) {
            pushUndoState(_uiState.value.project)
            val item = clips.removeAt(index)
            clips.add(index - 1, item)
            val updated = _uiState.value.project.copy(clips = clips)
            _uiState.value = _uiState.value.copy(project = updated, selectedClipIndex = index - 1)
            playerManager.setClips(clips)
            persistProject()
        }
    }

    fun moveClipRight(index: Int) {
        val clips = _uiState.value.project.clips.toMutableList()
        if (index >= 0 && index < clips.lastIndex) {
            pushUndoState(_uiState.value.project)
            val item = clips.removeAt(index)
            clips.add(index + 1, item)
            val updated = _uiState.value.project.copy(clips = clips)
            _uiState.value = _uiState.value.copy(project = updated, selectedClipIndex = index + 1)
            playerManager.setClips(clips)
            persistProject()
        }
    }

    fun toggleClipFlip(index: Int) {
        val clips = _uiState.value.project.clips.toMutableList()
        if (index in clips.indices) {
            pushUndoState(_uiState.value.project)
            clips[index] = clips[index].copy(isFlippedHorizontal = !clips[index].isFlippedHorizontal)
            val updated = _uiState.value.project.copy(clips = clips)
            _uiState.value = _uiState.value.copy(project = updated)
            persistProject()
        }
    }

    fun rotateClip90(index: Int) {
        val clips = _uiState.value.project.clips.toMutableList()
        if (index in clips.indices) {
            pushUndoState(_uiState.value.project)
            val nextDeg = (clips[index].rotationDegrees + 90) % 360
            clips[index] = clips[index].copy(rotationDegrees = nextDeg)
            val updated = _uiState.value.project.copy(clips = clips)
            _uiState.value = _uiState.value.copy(project = updated)
            persistProject()
        }
    }

    fun trimSelectedClip(startMs: Long, endMs: Long) {
        val index = _uiState.value.selectedClipIndex
        val clips = _uiState.value.project.clips.toMutableList()
        if (index in clips.indices) {
            pushUndoState(_uiState.value.project)
            val clip = clips[index]
            clips[index] = clip.copy(trimStartMs = startMs, trimEndMs = endMs)
            val updatedProject = _uiState.value.project.copy(clips = clips)
            _uiState.value = _uiState.value.copy(project = updatedProject)
            playerManager.setClips(clips)
            persistProject()
        }
    }

    fun splitClipAtCurrentPlayhead() {
        val clips = _uiState.value.project.clips.toMutableList()
        if (clips.isEmpty()) return

        val currentTimelineMs = playerState.value.currentPositionMs

        // Find which clip contains the current playhead
        var accumulatedMs = 0L
        var targetIndex = -1
        var offsetWithinPlayback = 0L

        for (i in clips.indices) {
            val clipDuration = clips[i].playbackDurationMs
            if (currentTimelineMs >= accumulatedMs && currentTimelineMs <= accumulatedMs + clipDuration) {
                targetIndex = i
                offsetWithinPlayback = currentTimelineMs - accumulatedMs
                break
            }
            accumulatedMs += clipDuration
        }

        if (targetIndex != -1) {
            val targetClip = clips[targetIndex]
            val splitOffsetInSource = (offsetWithinPlayback * targetClip.speed).toLong()
            val absoluteSplitMs = targetClip.trimStartMs + splitOffsetInSource

            if (absoluteSplitMs > targetClip.trimStartMs + 300L && absoluteSplitMs < targetClip.trimEndMs - 300L) {
                pushUndoState(_uiState.value.project)
                showMessage("Clip split", offerUndo = true)

                val clipPart1 = targetClip.copy(
                    id = java.util.UUID.randomUUID().toString(),
                    name = splitPartName(targetClip.name, 1),
                    trimEndMs = absoluteSplitMs
                )
                val clipPart2 = targetClip.copy(
                    id = java.util.UUID.randomUUID().toString(),
                    name = splitPartName(targetClip.name, 2),
                    trimStartMs = absoluteSplitMs
                )

                clips.removeAt(targetIndex)
                clips.add(targetIndex, clipPart2)
                clips.add(targetIndex, clipPart1)

                val updatedProject = _uiState.value.project.copy(clips = clips)
                _uiState.value = _uiState.value.copy(
                    project = updatedProject,
                    selectedClipIndex = targetIndex
                )
                playerManager.setClips(clips, keepPosition = true)
                persistProject()
            } else {
                showMessage("Move the playhead at least 0.3s from the clip's edge to split")
            }
        }
    }

    fun setSelectedClipSpeed(speed: Float) {
        val index = _uiState.value.selectedClipIndex
        val clips = _uiState.value.project.clips.toMutableList()
        if (index in clips.indices) {
            pushUndoState(_uiState.value.project)
            clips[index] = clips[index].copy(speed = speed)
            val updatedProject = _uiState.value.project.copy(clips = clips)
            _uiState.value = _uiState.value.copy(project = updatedProject)
            playerManager.setClips(clips)
            persistProject()
        }
    }

    fun setSelectedClipFilter(filter: VideoFilter) {
        val index = _uiState.value.selectedClipIndex
        val clips = _uiState.value.project.clips.toMutableList()
        if (index in clips.indices) {
            pushUndoState(_uiState.value.project)
            clips[index] = clips[index].copy(filter = filter)
            val updatedProject = _uiState.value.project.copy(clips = clips)
            _uiState.value = _uiState.value.copy(project = updatedProject)
            persistProject()
        }
    }

    fun toggleSelectedClipMute() {
        val index = _uiState.value.selectedClipIndex
        val clips = _uiState.value.project.clips.toMutableList()
        if (index in clips.indices) {
            pushUndoState(_uiState.value.project)
            val clip = clips[index]
            clips[index] = clip.copy(isMuted = !clip.isMuted)
            val updatedProject = _uiState.value.project.copy(clips = clips)
            _uiState.value = _uiState.value.copy(project = updatedProject)
            playerManager.setClips(clips)
            persistProject()
        }
    }

    fun duplicateSelectedClip() {
        val index = _uiState.value.selectedClipIndex
        val clips = _uiState.value.project.clips.toMutableList()
        if (index in clips.indices) {
            pushUndoState(_uiState.value.project)
            val original = clips[index]
            val copy = original.copy(
                id = java.util.UUID.randomUUID().toString(),
                name = "${original.name} (Copy)"
            )
            clips.add(index + 1, copy)
            val updatedProject = _uiState.value.project.copy(clips = clips)
            _uiState.value = _uiState.value.copy(
                project = updatedProject,
                selectedClipIndex = index + 1
            )
            playerManager.setClips(clips)
            persistProject()
        }
    }

    fun deleteSelectedClip() {
        val index = _uiState.value.selectedClipIndex
        val clips = _uiState.value.project.clips.toMutableList()
        if (index in clips.indices) {
            pushUndoState(_uiState.value.project)
            clips.removeAt(index)
            showMessage("Clip deleted", offerUndo = true)
            val updatedProject = _uiState.value.project.copy(clips = clips)
            _uiState.value = _uiState.value.copy(
                project = updatedProject,
                selectedClipIndex = -1
            )
            playerManager.setClips(clips, keepPosition = false)
            persistProject()
        }
    }

    // Aspect Ratio & Safe Zone
    fun setAspectRatio(preset: AspectRatioPreset) {
        pushUndoState(_uiState.value.project)
        val updatedProject = _uiState.value.project.copy(aspectRatio = preset)
        _uiState.value = _uiState.value.copy(project = updatedProject)
        persistProject()
    }

    fun setSafeZone(safeZone: SafeZoneType) {
        val updatedProject = _uiState.value.project.copy(safeZone = safeZone)
        _uiState.value = _uiState.value.copy(project = updatedProject)
        persistProject()
    }

    // Text Overlays & Stickers
    fun addOrUpdateTextOverlay(overlay: TextOverlay) {
        pushUndoState(_uiState.value.project)
        val list = _uiState.value.project.textOverlays.toMutableList()
        val index = list.indexOfFirst { it.id == overlay.id }
        if (index >= 0) {
            list[index] = overlay
        } else {
            list.add(overlay)
        }
        val updatedProject = _uiState.value.project.copy(textOverlays = list)
        _uiState.value = _uiState.value.copy(
            project = updatedProject,
            selectedOverlayId = overlay.id
        )
        persistProject()
    }

    fun deleteTextOverlay(id: String) {
        pushUndoState(_uiState.value.project)
        showMessage("Deleted", offerUndo = true)
        val list = _uiState.value.project.textOverlays.toMutableList()
        list.removeAll { it.id == id }
        val updatedProject = _uiState.value.project.copy(textOverlays = list)
        _uiState.value = _uiState.value.copy(
            project = updatedProject,
            selectedOverlayId = null
        )
        persistProject()
    }

    /** Called once when a drag on the video ends, so each move is one undo step. */
    fun updateOverlayPosition(id: String, normX: Float, normY: Float) {
        val list = _uiState.value.project.textOverlays.toMutableList()
        val index = list.indexOfFirst { it.id == id }
        if (index >= 0 && (list[index].posX != normX || list[index].posY != normY)) {
            pushUndoState(_uiState.value.project)
            list[index] = list[index].copy(posX = normX, posY = normY)
            val updatedProject = _uiState.value.project.copy(textOverlays = list)
            _uiState.value = _uiState.value.copy(project = updatedProject)
            persistProject()
        }
    }

    /** Pinch to resize and rotate on the video; one undo step per gesture. */
    fun transformOverlay(id: String, fontSizeSp: Float, rotation: Float) {
        val list = _uiState.value.project.textOverlays
        val index = list.indexOfFirst { it.id == id }
        if (index < 0) return
        val current = list[index]
        if (current.fontSizeSp == fontSizeSp && current.rotation == rotation) return
        pushUndoState(_uiState.value.project)
        val updated = list.toMutableList().apply {
            this[index] = current.copy(fontSizeSp = fontSizeSp, rotation = rotation)
        }
        _uiState.value = _uiState.value.copy(project = _uiState.value.project.copy(textOverlays = updated))
        persistProject()
    }

    /** Pinch-to-resize on the video; one undo step per gesture. */
    fun resizeOverlay(id: String, fontSizeSp: Float) {
        val currentRot = _uiState.value.project.textOverlays.find { it.id == id }?.rotation ?: 0f
        transformOverlay(id, fontSizeSp, currentRot)
    }

    fun resetSelectedOverlayRotation() {
        val id = _uiState.value.selectedOverlayId ?: return
        val list = _uiState.value.project.textOverlays
        val index = list.indexOfFirst { it.id == id }
        if (index < 0 || list[index].rotation == 0f) return
        pushUndoState(_uiState.value.project)
        val updated = list.toMutableList().apply {
            this[index] = this[index].copy(rotation = 0f)
        }
        _uiState.value = _uiState.value.copy(project = _uiState.value.project.copy(textOverlays = updated))
        persistProject()
    }

    /** Dragging a text/sticker chip along the timeline keeps its length and moves its start. */
    fun moveOverlayInTime(id: String, startMs: Long) {
        val list = _uiState.value.project.textOverlays
        val index = list.indexOfFirst { it.id == id }
        if (index < 0) return
        val overlay = list[index]
        val start = startMs.coerceAtLeast(0L)
        if (start == overlay.startTimeMs) return
        pushUndoState(_uiState.value.project)
        val moved = overlay.copy(startTimeMs = start, endTimeMs = start + (overlay.endTimeMs - overlay.startTimeMs))
        val updated = list.toMutableList().apply { this[index] = moved }
        _uiState.value = _uiState.value.copy(project = _uiState.value.project.copy(textOverlays = updated))
        persistProject()
    }

    /** Dragging a text chip's edges on the timeline. */
    fun setOverlayTiming(id: String, startMs: Long, endMs: Long) {
        val list = _uiState.value.project.textOverlays
        val index = list.indexOfFirst { it.id == id }
        if (index < 0) return
        val start = startMs.coerceAtLeast(0L)
        val end = endMs.coerceAtLeast(start + MIN_OVERLAY_MS)
        if (start == list[index].startTimeMs && end == list[index].endTimeMs) return
        pushUndoState(_uiState.value.project)
        val updated = list.toMutableList().apply { this[index] = this[index].copy(startTimeMs = start, endTimeMs = end) }
        _uiState.value = _uiState.value.copy(project = _uiState.value.project.copy(textOverlays = updated))
        persistProject()
    }

    /** TikTok-style "Text-to-speech": the selected text is spoken into a voice track starting with the text. */
    fun readSelectedOverlayAloud() {
        val overlay = _uiState.value.project.textOverlays.find { it.id == _uiState.value.selectedOverlayId } ?: return
        if (overlay.text.isBlank()) return
        viewModelScope.launch {
            showMessage("Creating voice…")
            val context = getApplication<Application>().applicationContext
            val file = TextToSpeechHelper.synthesizeToFile(context, overlay.text)
            if (file == null) {
                showMessage("No text-to-speech voice found. Add one in Android Settings › Text-to-speech.")
                return@launch
            }
            val owned = ProjectMediaStore.adoptFile(context, _uiState.value.project.id, file, move = true)
            val track = ProjectMediaStore.measureAudioTracks(
                context,
                listOf(
                    AudioTrack(
                        uri = Uri.fromFile(owned).toString(),
                        title = "🗣 ${overlay.text.take(24)}",
                        timelineStartOffsetMs = overlay.startTimeMs,
                        volume = 1f,
                        isVoiceover = true
                    )
                )
            ).first()
            pushUndoState(_uiState.value.project)
            _uiState.value = _uiState.value.copy(
                project = _uiState.value.project.copy(audioTracks = _uiState.value.project.audioTracks + track)
            )
            persistProject()
            showMessage("Voice added where the text starts", offerUndo = true)
        }
    }

    fun selectOverlay(id: String?) {
        _uiState.value = if (id != null) {
            _uiState.value.copy(selectedOverlayId = id, selectedClipIndex = -1, selectedAudioTrackId = null)
        } else {
            _uiState.value.copy(selectedOverlayId = null)
        }
    }

    /** Selecting from the timeline also moves the playhead into the overlay so it's visible on the canvas. */
    fun selectOverlayFromTimeline(id: String) {
        val overlay = _uiState.value.project.textOverlays.find { it.id == id } ?: return
        if (_uiState.value.selectedOverlayId == id) {
            clearSelection()
            return
        }
        selectOverlay(id)
        val position = playerState.value.currentPositionMs
        if (position !in overlay.startTimeMs..overlay.endTimeMs) {
            playerManager.seekToTimeline(overlay.startTimeMs)
        }
    }

    private fun updateSelectedOverlay(transform: (TextOverlay) -> TextOverlay) {
        val id = _uiState.value.selectedOverlayId ?: return
        val list = _uiState.value.project.textOverlays
        val index = list.indexOfFirst { it.id == id }
        if (index < 0) return
        pushUndoState(_uiState.value.project)
        val updated = list.toMutableList().apply { this[index] = transform(this[index]) }
        _uiState.value = _uiState.value.copy(project = _uiState.value.project.copy(textOverlays = updated))
        persistProject()
    }

    /** Makes the selected text/sticker appear at the playhead (end is kept after it). */
    fun setSelectedOverlayStartToPlayhead() {
        val position = playerState.value.currentPositionMs
        updateSelectedOverlay { overlay ->
            val end = if (overlay.endTimeMs > position + MIN_OVERLAY_MS) overlay.endTimeMs else position + (overlay.endTimeMs - overlay.startTimeMs).coerceAtLeast(MIN_OVERLAY_MS)
            overlay.copy(startTimeMs = position, endTimeMs = end)
        }
    }

    /** Makes the selected text/sticker disappear at the playhead. */
    fun setSelectedOverlayEndToPlayhead() {
        val position = playerState.value.currentPositionMs
        updateSelectedOverlay { overlay ->
            val start = if (overlay.startTimeMs < position - MIN_OVERLAY_MS) overlay.startTimeMs else (position - MIN_OVERLAY_MS).coerceAtLeast(0L)
            overlay.copy(startTimeMs = start, endTimeMs = position.coerceAtLeast(start + MIN_OVERLAY_MS))
        }
    }

    fun duplicateSelectedOverlay() {
        val overlay = _uiState.value.project.textOverlays.find { it.id == _uiState.value.selectedOverlayId } ?: return
        val copy = overlay.copy(
            id = java.util.UUID.randomUUID().toString(),
            posX = (overlay.posX + 0.04f).coerceAtMost(0.9f),
            posY = (overlay.posY + 0.04f).coerceAtMost(0.9f)
        )
        addOrUpdateTextOverlay(copy)
    }

    // Individual audio tracks selected from the timeline
    fun selectAudioTrack(id: String) {
        _uiState.value = if (_uiState.value.selectedAudioTrackId == id) {
            _uiState.value.copy(selectedAudioTrackId = null)
        } else {
            _uiState.value.copy(selectedAudioTrackId = id, selectedClipIndex = -1, selectedOverlayId = null)
        }
    }

    private fun updateSelectedAudioTrack(recordUndo: Boolean = true, transform: (AudioTrack) -> AudioTrack?) {
        val id = _uiState.value.selectedAudioTrackId ?: return
        val tracks = _uiState.value.project.audioTracks
        val index = tracks.indexOfFirst { it.id == id }
        if (index < 0) return
        if (recordUndo) pushUndoState(_uiState.value.project)
        val updated = tracks.toMutableList()
        val result = transform(tracks[index])
        if (result == null) updated.removeAt(index) else updated[index] = result
        _uiState.value = _uiState.value.copy(
            project = _uiState.value.project.copy(audioTracks = updated),
            selectedAudioTrackId = if (result == null) null else id
        )
        persistProject()
    }

    fun setSelectedAudioVolume(volume: Float) = updateSelectedAudioTrack { it.copy(volume = volume) }

    fun moveSelectedAudioToPlayhead() {
        val position = playerState.value.currentPositionMs
        updateSelectedAudioTrack { it.copy(timelineStartOffsetMs = position) }
    }

    /** Dragging an audio chip along the timeline. */
    fun moveAudioTrackInTime(id: String, startMs: Long) {
        val track = _uiState.value.project.audioTracks.find { it.id == id } ?: return
        val start = startMs.coerceAtLeast(0L)
        if (start == track.timelineStartOffsetMs) return
        if (_uiState.value.selectedAudioTrackId != id) focusAudioTrack(id)
        updateSelectedAudioTrack { it.copy(timelineStartOffsetMs = start) }
    }

    /** Trimming an audio track by dragging its timeline edges. */
    fun trimAudioTrack(id: String, startOffsetMs: Long, trimStartMs: Long, trimEndMs: Long) {
        val tracks = _uiState.value.project.audioTracks
        val index = tracks.indexOfFirst { it.id == id }
        if (index < 0) return
        val original = tracks[index]
        if (original.timelineStartOffsetMs == startOffsetMs && original.trimStartMs == trimStartMs && original.trimEndMs == trimEndMs) return
        pushUndoState(_uiState.value.project)
        val updated = tracks.toMutableList().apply {
            this[index] = original.copy(
                timelineStartOffsetMs = startOffsetMs,
                trimStartMs = trimStartMs,
                trimEndMs = trimEndMs
            )
        }
        _uiState.value = _uiState.value.copy(project = _uiState.value.project.copy(audioTracks = updated))
        playerManager.setAudioTracks(updated)
        persistProject()
    }

    fun setSelectedAudioVoiceEffect(effect: VoiceEffect) = updateSelectedAudioTrack { it.copy(voiceEffect = effect) }

    fun setSelectedClipVoiceEffect(effect: VoiceEffect) {
        val index = _uiState.value.selectedClipIndex
        val clips = _uiState.value.project.clips.toMutableList()
        if (index !in clips.indices || clips[index].voiceEffect == effect) return
        pushUndoState(_uiState.value.project)
        clips[index] = clips[index].copy(voiceEffect = effect)
        _uiState.value = _uiState.value.copy(project = _uiState.value.project.copy(clips = clips))
        playerManager.setClips(clips)
        persistProject()
    }

    fun toggleDuckMusicUnderVoice() {
        pushUndoState(_uiState.value.project)
        _uiState.value = _uiState.value.copy(
            project = _uiState.value.project.let { it.copy(duckMusicUnderVoice = !it.duckMusicUnderVoice) }
        )
        persistProject()
    }

    fun toggleSelectedAudioLoop() = updateSelectedAudioTrack { it.copy(isLooping = !it.isLooping) }

    fun setSelectedAudioFades(fadeInMs: Long, fadeOutMs: Long) = updateSelectedAudioTrack {
        it.copy(fadeInMs = fadeInMs.coerceAtLeast(0L), fadeOutMs = fadeOutMs.coerceAtLeast(0L))
    }

    fun deleteSelectedAudioTrack() {
        updateSelectedAudioTrack { null }
        showMessage("Audio removed", offerUndo = true)
    }

    // Audio Track, SFX, Voiceovers
    fun setAudioTrack(uri: Uri, title: String) {
        viewModelScope.launch {
            val audioTrack = AudioTrack(
                uri = uri.toString(),
                title = title,
                volume = 0.8f
            )
            // Copy the picked file now, while the picker's read permission is still valid
            val context = getApplication<Application>().applicationContext
            val ownedTrack = ProjectMediaStore.localizeMedia(
                context,
                Project(id = _uiState.value.project.id, audioTracks = listOf(audioTrack))
            ).audioTracks.let { ProjectMediaStore.measureAudioTracks(context, it) }.first()
            pushUndoState(_uiState.value.project)
            // Added alongside existing sound effects and voiceovers instead of replacing them
            val updatedProject = _uiState.value.project.copy(audioTracks = _uiState.value.project.audioTracks + ownedTrack)
            _uiState.value = _uiState.value.copy(project = updatedProject)
            persistProject()
        }
    }

    fun addBuiltInSoundEffect(effect: SoundEffectItem, context: Context) {
        viewModelScope.launch {
            val sharedFile = BuiltInAudioHelper.getOrCreateSoundEffectFile(context, effect.id)
            // The generated SFX lives in the cache and is shared by all projects, so copy it
            val file = ProjectMediaStore.adoptFile(context, _uiState.value.project.id, sharedFile, move = false)
            val currentPos = playerState.value.currentPositionMs
            val audioTrack = ProjectMediaStore.measureAudioTracks(
                context,
                listOf(
                    AudioTrack(
                        uri = Uri.fromFile(file).toString(),
                        title = "${effect.iconEmoji} ${effect.name}",
                        timelineStartOffsetMs = currentPos,
                        volume = 0.9f
                    )
                )
            ).first()
            pushUndoState(_uiState.value.project)
            val list = _uiState.value.project.audioTracks + audioTrack
            val updated = _uiState.value.project.copy(audioTracks = list)
            _uiState.value = _uiState.value.copy(project = updated)
            persistProject()
        }
    }

    fun addVoiceoverTrack(track: AudioTrack) {
        viewModelScope.launch {
            // Recordings are written to the cache; move them into the project so they can't be evicted
            val context = getApplication<Application>().applicationContext
            val recording = Uri.parse(track.uri).path?.let { File(it) }
            val ownedTrack = if (recording != null && recording.exists()) {
                val owned = ProjectMediaStore.adoptFile(context, _uiState.value.project.id, recording, move = true)
                track.copy(uri = Uri.fromFile(owned).toString())
            } else {
                track
            }
            pushUndoState(_uiState.value.project)
            val list = _uiState.value.project.audioTracks + ownedTrack
            val updated = _uiState.value.project.copy(audioTracks = list)
            _uiState.value = _uiState.value.copy(project = updated)
            persistProject()
        }
    }

    /** Opens a track's own tools from the audio sheet (unlike the timeline tap, never toggles it off). */
    fun focusAudioTrack(id: String) {
        if (_uiState.value.selectedAudioTrackId != id) selectAudioTrack(id)
    }

    fun deleteAudioTrack(id: String) {
        val tracks = _uiState.value.project.audioTracks
        if (tracks.none { it.id == id }) return
        pushUndoState(_uiState.value.project)
        _uiState.value = _uiState.value.copy(
            project = _uiState.value.project.copy(audioTracks = tracks.filterNot { it.id == id }),
            selectedAudioTrackId = _uiState.value.selectedAudioTrackId.takeUnless { it == id }
        )
        persistProject()
        showMessage("Audio removed", offerUndo = true)
    }

    fun toggleMuteAllClips() {
        pushUndoState(_uiState.value.project)
        val currentAllMuted = _uiState.value.project.clips.all { it.isMuted }
        val clips = _uiState.value.project.clips.map { it.copy(isMuted = !currentAllMuted) }
        val updatedProject = _uiState.value.project.copy(clips = clips)
        _uiState.value = _uiState.value.copy(project = updatedProject)
        playerManager.setClips(clips)
        persistProject()
    }

    // AI results (AI edition): applied only when the user taps one, each as a single undo step

    fun addHookText(text: String) {
        val total = _uiState.value.project.totalDurationMs
        addOrUpdateTextOverlay(
            TextOverlay(
                text = text,
                style = TextStylePreset.VIRAL_HOOK,
                posY = 0.22f,
                startTimeMs = 0L,
                endTimeMs = minOf(3000L, total).coerceAtLeast(MIN_OVERLAY_MS),
                animation = TextAnimation.POP
            )
        )
        showMessage("Hook added", offerUndo = true)
    }

    /** Adds AI-made text (captions or a story); [replaceAutoCaptions] removes earlier auto-captions first. */
    fun addAiOverlays(overlays: List<TextOverlay>, replaceAutoCaptions: Boolean, message: String) {
        if (overlays.isEmpty()) return
        pushUndoState(_uiState.value.project)
        val kept = _uiState.value.project.textOverlays.filterNot { replaceAutoCaptions && it.isAutoCaption }
        _uiState.value = _uiState.value.copy(
            project = _uiState.value.project.copy(textOverlays = kept + overlays),
            selectedOverlayId = null
        )
        persistProject()
        showMessage(message, offerUndo = true)
    }

    /** Applies translations (overlay id to new text) in one step. */
    fun setOverlayTexts(texts: List<Pair<String, String>>) {
        val byId = texts.toMap()
        if (byId.isEmpty()) return
        pushUndoState(_uiState.value.project)
        val updated = _uiState.value.project.textOverlays.map { o -> byId[o.id]?.let { o.copy(text = it) } ?: o }
        _uiState.value = _uiState.value.copy(project = _uiState.value.project.copy(textOverlays = updated))
        persistProject()
        showMessage("Translated ${byId.size} text${if (byId.size == 1) "" else "s"}", offerUndo = true)
    }

    fun setSelectedOverlayText(text: String) = updateSelectedOverlay { it.copy(text = text) }

    fun applyColorSuggestion(suggestion: ColorSuggestion, toAllClips: Boolean) {
        val index = _uiState.value.selectedClipIndex
        val clips = _uiState.value.project.clips
        if (index !in clips.indices) return
        pushUndoState(_uiState.value.project)
        val updated = clips.mapIndexed { i, clip -> if (toAllClips || i == index) suggestion.applyTo(clip) else clip }
        _uiState.value = _uiState.value.copy(project = _uiState.value.project.copy(clips = updated))
        persistProject()
        showMessage(if (toAllClips) "AI color applied to all clips" else "AI color applied", offerUndo = true)
    }

    fun renameProject(title: String) {
        val trimmed = title.trim()
        if (trimmed.isEmpty() || trimmed == _uiState.value.project.title) return
        pushUndoState(_uiState.value.project)
        _uiState.value = _uiState.value.copy(project = _uiState.value.project.copy(title = trimmed))
        persistProject()
        showMessage("Renamed to \"$trimmed\"", offerUndo = true)
    }

    fun pause() {
        playerManager.pause()
    }

    fun toggleFitMode() {
        pushUndoState(_uiState.value.project)
        val updatedProject = _uiState.value.project.let { it.copy(isFitMode = !it.isFitMode) }
        _uiState.value = _uiState.value.copy(project = updatedProject)
        persistProject()
    }

    // Export
    fun startExport(preset: ExportPreset) {
        playerManager.pause()
        exportManager.exportProject(_uiState.value.project, preset)
    }

    fun cancelExport() {
        exportManager.cancelExport()
    }

    fun resetExportState() {
        exportManager.resetState()
    }

    // Dialog / Sheet visibility controls
    fun setShowTrimSheet(show: Boolean) = _uiState.update { it.copy(showTrimSheet = show) }
    fun setShowSpeedSheet(show: Boolean) = _uiState.update { it.copy(showSpeedSheet = show) }
    fun setShowFilterSheet(show: Boolean) = _uiState.update { it.copy(showFilterSheet = show) }
    fun setShowTextEditorSheet(show: Boolean) = _uiState.update { it.copy(showTextEditorSheet = show) }
    fun setShowAudioSheet(show: Boolean) = _uiState.update { it.copy(showAudioSheet = show) }
    fun setShowSafeZoneSheet(show: Boolean) = _uiState.update { it.copy(showSafeZoneSheet = show) }
    fun setShowExportDialog(show: Boolean) = _uiState.update { it.copy(showExportDialog = show) }
    fun setShowVoiceoverSheet(show: Boolean) = _uiState.update { it.copy(showVoiceoverSheet = show) }
    fun setShowStickerSheet(show: Boolean) = _uiState.update { it.copy(showStickerSheet = show) }
    fun setShowVolumeSheet(show: Boolean) = _uiState.update { it.copy(showVolumeSheet = show) }
    fun setShowAdjustSheet(show: Boolean) = _uiState.update { it.copy(showAdjustSheet = show) }
    fun setShowFadeSheet(show: Boolean) = _uiState.update { it.copy(showFadeSheet = show) }
    fun setShowVoiceEffectSheet(show: Boolean) = _uiState.update { it.copy(showVoiceEffectSheet = show) }
    fun setShowAiToolsSheet(show: Boolean) = _uiState.update { it.copy(showAiToolsSheet = show) }

    private inline fun MutableStateFlow<EditorUiState>.update(function: (EditorUiState) -> EditorUiState) {
        value = function(value)
    }

    override fun onCleared() {
        super.onCleared()
        playerManager.release()
    }
}
