package com.kevan.videorabykevan.ui.viewmodel

import android.app.Application
import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.kevan.videorabykevan.data.BuiltInAudioHelper
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class EditorUiState(
    val project: Project = Project(),
    val selectedClipIndex: Int = 0,
    val selectedOverlayId: String? = null,
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
    val showStickerSheet: Boolean = false
)

class EditorViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = ProjectRepository(application.applicationContext)
    val playerManager = VideoPlayerManager(application.applicationContext, viewModelScope)
    val exportManager = VideoExportManager(application.applicationContext, viewModelScope)

    val playerState: StateFlow<PlayerState> = playerManager.playerState
    val exportState: StateFlow<ExportProgressState> = exportManager.exportState

    private val _uiState = MutableStateFlow(EditorUiState())
    val uiState: StateFlow<EditorUiState> = _uiState.asStateFlow()

    private val undoStack = mutableListOf<Project>()
    private val redoStack = mutableListOf<Project>()

    fun loadProject(projectId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val project = repository.getProject(projectId) ?: Project(id = projectId)
            undoStack.clear()
            redoStack.clear()
            _uiState.value = _uiState.value.copy(
                project = project,
                selectedClipIndex = 0,
                canUndo = false,
                canRedo = false,
                isLoading = false
            )
            playerManager.setClips(project.clips, keepPosition = false)
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
                selectedClipIndex = _uiState.value.selectedClipIndex.coerceIn(0, (prev.clips.size - 1).coerceAtLeast(0))
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
                selectedClipIndex = _uiState.value.selectedClipIndex.coerceIn(0, (next.clips.size - 1).coerceAtLeast(0))
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

            withContext(Dispatchers.IO) {
                for (uri in uris) {
                    val retriever = MediaMetadataRetriever()
                    try {
                        retriever.setDataSource(context, uri)
                        val durationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                        val durationMs = durationStr?.toLongOrNull() ?: 5000L
                        val title = uri.lastPathSegment?.substringAfterLast('/') ?: "Clip ${newClips.size + 1}"

                        newClips.add(
                            VideoClip(
                                uri = uri.toString(),
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

            val newClip = VideoClip(
                uri = Uri.fromFile(file).toString(),
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

    fun selectClip(index: Int) {
        val clips = _uiState.value.project.clips
        if (index in clips.indices) {
            _uiState.value = _uiState.value.copy(selectedClipIndex = index)
            // Seek player to start of this clip
            var precedingMs = 0L
            for (i in 0 until index) {
                precedingMs += clips[i].playbackDurationMs
            }
            playerManager.seekToTimeline(precedingMs)
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
            val newIndex = (index - 1).coerceAtLeast(0)
            val updatedProject = _uiState.value.project.copy(clips = clips)
            _uiState.value = _uiState.value.copy(
                project = updatedProject,
                selectedClipIndex = if (clips.isEmpty()) 0 else newIndex
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
        _uiState.value = _uiState.value.copy(selectedOverlayId = id)
    }

    // Audio Track, SFX, Voiceovers
    fun setAudioTrack(uri: Uri, title: String) {
        pushUndoState(_uiState.value.project)
        val audioTrack = AudioTrack(
            uri = uri.toString(),
            title = title,
            volume = 0.8f
        )
        val updatedProject = _uiState.value.project.copy(audioTracks = listOf(audioTrack))
        _uiState.value = _uiState.value.copy(project = updatedProject)
        persistProject()
    }

    fun addBuiltInSoundEffect(effect: SoundEffectItem, context: Context) {
        viewModelScope.launch {
            val file = BuiltInAudioHelper.getOrCreateSoundEffectFile(context, effect.id)
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
        pushUndoState(_uiState.value.project)
        val list = _uiState.value.project.audioTracks + track
        val updated = _uiState.value.project.copy(audioTracks = list)
        _uiState.value = _uiState.value.copy(project = updated)
        persistProject()
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

    private inline fun MutableStateFlow<EditorUiState>.update(function: (EditorUiState) -> EditorUiState) {
        value = function(value)
    }

    override fun onCleared() {
        super.onCleared()
        playerManager.release()
    }
}
