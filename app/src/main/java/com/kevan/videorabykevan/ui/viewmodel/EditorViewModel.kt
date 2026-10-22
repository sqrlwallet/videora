package com.kevan.videorabykevan.ui.viewmodel

import android.app.Application
import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.kevan.videorabykevan.data.BuiltInAudioHelper
import com.kevan.videorabykevan.data.ProjectMediaStore
import com.kevan.videorabykevan.data.ProjectRepository
import com.kevan.videorabykevan.data.SampleMediaHelper
import com.kevan.videorabykevan.data.SoundEffectItem
import com.kevan.videorabykevan.engine.ExportProgressState
import com.kevan.videorabykevan.engine.PlayerState
import com.kevan.videorabykevan.engine.VideoExportManager
import com.kevan.videorabykevan.engine.VideoPlayerManager
import com.kevan.videorabykevan.model.AspectRatioPreset
import com.kevan.videorabykevan.model.AudioTrack
import com.kevan.videorabykevan.model.ExportPreset
import com.kevan.videorabykevan.model.Project
import com.kevan.videorabykevan.model.SafeZoneType
import com.kevan.videorabykevan.model.TextOverlay
import com.kevan.videorabykevan.model.VideoClip
import com.kevan.videorabykevan.model.VideoFilter
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
    val showOpenSourceSheet: Boolean = false,
    val showVoiceoverSheet: Boolean = false,
    val showStickerSheet: Boolean = false,
    val showVolumeSheet: Boolean = false,
    val showAdjustSheet: Boolean = false,
    val isComparingOriginal: Boolean = false
)

private const val MIN_OVERLAY_MS = 300L

/** One-off feedback shown as a snackbar; [offerUndo] adds an Undo action. */
data class EditorMessage(val text: String, val offerUndo: Boolean = false)

class EditorViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = ProjectRepository(application.applicationContext)
    val playerManager = VideoPlayerManager(application.applicationContext, viewModelScope)
    val exportManager = VideoExportManager(application.applicationContext, viewModelScope)

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
            val updatedProject = ProjectMediaStore.localizeMedia(context, project)
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
            val newClips = mutableListOf<VideoClip>()
            val projectDir = File(context.filesDir, "projects/${_uiState.value.project.id}/clips")

            withContext(Dispatchers.IO) {
                projectDir.mkdirs()
                for ((index, uri) in uris.withIndex()) {
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

    /** Tapping the selected clip again deselects it; tapping another selects it and jumps to its start. */
    fun selectClip(index: Int) {
        val clips = _uiState.value.project.clips
        if (index !in clips.indices) return
        if (index == _uiState.value.selectedClipIndex) {
            clearSelection()
            return
        }
        _uiState.value = _uiState.value.copy(selectedClipIndex = index, selectedOverlayId = null, selectedAudioTrackId = null)
        playerManager.seekToTimeline(clips.take(index).sumOf { it.playbackDurationMs })
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
                    name = "${targetClip.name} (Part 1)",
                    trimEndMs = absoluteSplitMs
                )
                val clipPart2 = targetClip.copy(
                    id = java.util.UUID.randomUUID().toString(),
                    name = "${targetClip.name} (Part 2)",
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

    fun updateOverlayPosition(id: String, normX: Float, normY: Float) {
        val list = _uiState.value.project.textOverlays.toMutableList()
        val index = list.indexOfFirst { it.id == id }
        if (index >= 0) {
            list[index] = list[index].copy(posX = normX, posY = normY)
            val updatedProject = _uiState.value.project.copy(textOverlays = list)
            _uiState.value = _uiState.value.copy(project = updatedProject)
            persistProject()
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

    fun toggleSelectedAudioLoop() = updateSelectedAudioTrack { it.copy(isLooping = !it.isLooping) }

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
            ).audioTracks.first()
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
            val audioTrack = AudioTrack(
                uri = Uri.fromFile(file).toString(),
                title = "${effect.iconEmoji} ${effect.name}",
                timelineStartOffsetMs = currentPos,
                volume = 0.9f
            )
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

    fun updateAudioVolume(volume: Float) {
        val tracks = _uiState.value.project.audioTracks.map { it.copy(volume = volume) }
        val updatedProject = _uiState.value.project.copy(audioTracks = tracks)
        _uiState.value = _uiState.value.copy(project = updatedProject)
        persistProject()
    }

    fun removeAudioTrack() {
        pushUndoState(_uiState.value.project)
        val updatedProject = _uiState.value.project.copy(audioTracks = emptyList())
        _uiState.value = _uiState.value.copy(project = updatedProject)
        persistProject()
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
    fun setShowOpenSourceSheet(show: Boolean) = _uiState.update { it.copy(showOpenSourceSheet = show) }
    fun setShowVoiceoverSheet(show: Boolean) = _uiState.update { it.copy(showVoiceoverSheet = show) }
    fun setShowStickerSheet(show: Boolean) = _uiState.update { it.copy(showStickerSheet = show) }
    fun setShowVolumeSheet(show: Boolean) = _uiState.update { it.copy(showVolumeSheet = show) }
    fun setShowAdjustSheet(show: Boolean) = _uiState.update { it.copy(showAdjustSheet = show) }

    private inline fun MutableStateFlow<EditorUiState>.update(function: (EditorUiState) -> EditorUiState) {
        value = function(value)
    }

    override fun onCleared() {
        super.onCleared()
        playerManager.release()
    }
}
