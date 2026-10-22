package com.kevan.videorabykevan.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.ViewInAr
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.kevan.videorabykevan.model.AspectRatioPreset
import com.kevan.videorabykevan.ui.components.AdjustSheet
import com.kevan.videorabykevan.ui.components.AudioSheet
import com.kevan.videorabykevan.ui.components.ClipActionRow
import com.kevan.videorabykevan.ui.components.ExportDialog
import com.kevan.videorabykevan.ui.components.FadeSheet
import com.kevan.videorabykevan.ui.components.FilterSheet
import com.kevan.videorabykevan.ui.components.OpenSourceSheet
import com.kevan.videorabykevan.ui.components.SafeZoneSheet
import com.kevan.videorabykevan.ui.components.SpeedSheet
import com.kevan.videorabykevan.ui.components.StickerSheet
import com.kevan.videorabykevan.ui.components.TextEditorSheet
import com.kevan.videorabykevan.ui.components.TimelineView
import com.kevan.videorabykevan.ui.components.TrimSheet
import com.kevan.videorabykevan.ui.components.VideoPlayerView
import com.kevan.videorabykevan.ui.components.VoiceoverSheet
import com.kevan.videorabykevan.ui.components.VolumeSheet
import com.kevan.videorabykevan.ui.theme.AccentCyan
import com.kevan.videorabykevan.ui.theme.DarkBackground
import com.kevan.videorabykevan.ui.theme.DarkCard
import com.kevan.videorabykevan.ui.theme.DarkSurfaceVariant
import com.kevan.videorabykevan.ui.theme.GoldChampagne
import com.kevan.videorabykevan.ui.theme.GoldMetallic
import com.kevan.videorabykevan.ui.viewmodel.EditorViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(
    projectId: String,
    viewModel: EditorViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val playerState by viewModel.playerState.collectAsState()
    val exportState by viewModel.exportState.collectAsState()

    var showAspectMenu by remember { mutableStateOf(false) }

    // Media Picker for videos
    val videoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia()
    ) { uris ->
        if (uris.isNotEmpty()) {
            viewModel.addVideoUris(uris, context)
        }
    }

    // Audio Picker for background music
    val audioPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            val title = uri.lastPathSegment?.substringAfterLast('/') ?: "Music Track"
            viewModel.setAudioTrack(uri, title)
        }
    }

    // Only for the export progress notification; the export runs whether or not it's granted
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { }

    LaunchedEffect(projectId) {
        viewModel.loadProject(projectId)
    }

    // Action feedback (split, delete...) with a one-tap Undo
    val snackbarHostState = remember { SnackbarHostState() }
    val haptics = LocalHapticFeedback.current
    LaunchedEffect(viewModel) {
        viewModel.messages.collect { message ->
            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
            snackbarHostState.currentSnackbarData?.dismiss()
            val result = snackbarHostState.showSnackbar(
                message = message.text,
                actionLabel = if (message.offerUndo) "Undo" else null,
                duration = SnackbarDuration.Short
            )
            if (result == SnackbarResult.ActionPerformed) {
                viewModel.undo()
            }
        }
    }

    // Stop playback automatically when leaving the screen or backgrounding the app
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE || event == Lifecycle.Event.ON_STOP) {
                viewModel.pause()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            viewModel.pause()
        }
    }

    val selectedClip = uiState.project.clips.getOrNull(uiState.selectedClipIndex)
    val selectedTextOverlay = uiState.project.textOverlays.find { it.id == uiState.selectedOverlayId }
    val selectedAudioTrack = uiState.project.audioTracks.find { it.id == uiState.selectedAudioTrackId }

    // System back first closes the clip / text / audio tools, then leaves the editor
    BackHandler(enabled = selectedClip != null || selectedTextOverlay != null || selectedAudioTrack != null) {
        viewModel.clearSelection()
    }

    Scaffold(
        containerColor = DarkBackground,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Aspect Ratio Selector Dropdown Pill
                        Box {
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(DarkSurfaceVariant)
                                    .border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(10.dp))
                                    .clickable { showAspectMenu = true }
                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = uiState.project.aspectRatio.label,
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Icon(
                                    Icons.Default.ArrowDropDown,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                            }

                            DropdownMenu(
                                expanded = showAspectMenu,
                                onDismissRequest = { showAspectMenu = false },
                                modifier = Modifier.background(DarkCard)
                            ) {
                                AspectRatioPreset.entries.forEach { preset ->
                                    DropdownMenuItem(
                                        text = {
                                            Column {
                                                Text(preset.label, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                                Text(preset.description, color = Color.White.copy(alpha = 0.6f), fontSize = 11.sp)
                                            }
                                        },
                                        onClick = {
                                            viewModel.setAspectRatio(preset)
                                            showAspectMenu = false
                                        }
                                    )
                                }
                            }
                        }

                        // Safe Zone Indicator Pill
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(DarkSurfaceVariant)
                                .border(1.dp, AccentCyan.copy(alpha = 0.3f), RoundedCornerShape(10.dp))
                                .clickable { viewModel.setShowSafeZoneSheet(true) }
                                .padding(horizontal = 8.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.ViewInAr, contentDescription = null, tint = AccentCyan, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = uiState.project.safeZone.badge,
                                color = AccentCyan,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = {
                        viewModel.pause()
                        onNavigateBack()
                    }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                },
                actions = {
                    // Undo
                    IconButton(
                        onClick = { viewModel.undo() },
                        enabled = uiState.canUndo,
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.Undo,
                            contentDescription = "Undo",
                            tint = if (uiState.canUndo) GoldChampagne else Color.White.copy(alpha = 0.25f),
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    // Redo
                    IconButton(
                        onClick = { viewModel.redo() },
                        enabled = uiState.canRedo,
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.Redo,
                            contentDescription = "Redo",
                            tint = if (uiState.canRedo) GoldChampagne else Color.White.copy(alpha = 0.25f),
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(4.dp))

                    // Export Button
                    Button(
                        onClick = { viewModel.setShowExportDialog(true) },
                        // Nothing to export until there's at least one clip
                        enabled = uiState.project.clips.isNotEmpty(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = GoldChampagne,
                            disabledContainerColor = GoldChampagne.copy(alpha = 0.3f)
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Icon(Icons.Default.FileUpload, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Export", color = Color.Black, fontWeight = FontWeight.Black, fontSize = 13.sp)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkBackground)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Main Center Player Viewport (9:16 target)
            VideoPlayerView(
                exoPlayer = viewModel.playerManager.exoPlayer,
                aspectRatioPreset = uiState.project.aspectRatio,
                safeZoneType = uiState.project.safeZone,
                textOverlays = uiState.project.textOverlays,
                activeClip = uiState.project.clips.getOrNull(playerState.activeClipIndex),
                showOriginalColors = uiState.isComparingOriginal,
                currentPositionMs = playerState.currentPositionMs,
                isPlaying = playerState.isPlaying,
                isBuffering = playerState.isBuffering,
                hasClips = uiState.project.clips.isNotEmpty(),
                selectedOverlayId = uiState.selectedOverlayId,
                isFitMode = uiState.project.isFitMode,
                onToggleFitMode = { viewModel.toggleFitMode() },
                onTogglePlayPause = { viewModel.playerManager.togglePlayPause() },
                onSelectOverlay = { viewModel.selectOverlay(it) },
                onEditOverlay = { viewModel.setShowTextEditorSheet(true) },
                onUpdateOverlayPosition = { id, x, y -> viewModel.updateOverlayPosition(id, x, y) },
                onAddClipClicked = {
                    videoPickerLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)
                    )
                },
                onGenerateDemoClipClicked = {
                    viewModel.addDemoClip(context)
                },
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 16.dp, vertical = 6.dp)
            )

            // Multi-clip Timeline & Transport Scrubbing Bar
            TimelineView(
                clips = uiState.project.clips,
                currentPositionMs = playerState.currentPositionMs,
                totalDurationMs = playerState.totalDurationMs,
                selectedClipIndex = uiState.selectedClipIndex,
                isPlaying = playerState.isPlaying,
                onTogglePlayPause = { viewModel.playerManager.togglePlayPause() },
                onStepBackward = { viewModel.playerManager.stepBy(-1000L) },
                onStepForward = { viewModel.playerManager.stepBy(1000L) },
                onSeekTo = { viewModel.playerManager.seekToTimeline(it) },
                onSelectClip = { viewModel.selectClip(it) },
                onSplitCurrentClip = { viewModel.splitClipAtCurrentPlayhead() },
                onAddClipClicked = {
                    videoPickerLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)
                    )
                },
                audioTracks = uiState.project.audioTracks,
                textOverlays = uiState.project.textOverlays,
                selectedOverlayId = uiState.selectedOverlayId,
                onSelectOverlay = { viewModel.selectOverlayFromTimeline(it) },
                selectedAudioTrackId = uiState.selectedAudioTrackId,
                onSelectAudioTrack = { viewModel.selectAudioTrack(it) },
                onScrubStarted = { viewModel.pause() }
            )

            // Bottom Editing Toolbar (switches between project, clip and text/sticker tools)
            ClipActionRow(
                selectedClip = selectedClip,
                selectedClipIndex = uiState.selectedClipIndex,
                clipCount = uiState.project.clips.size,
                selectedOverlay = selectedTextOverlay,
                selectedAudioTrack = selectedAudioTrack,
                onClearSelection = { viewModel.clearSelection() },
                onAddClip = {
                    videoPickerLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)
                    )
                },
                onOpenText = {
                    // Main toolbar "Text" always adds a new overlay
                    viewModel.selectOverlay(null)
                    viewModel.setShowTextEditorSheet(true)
                },
                onOpenStickers = { viewModel.setShowStickerSheet(true) },
                onOpenAudio = { viewModel.setShowAudioSheet(true) },
                onOpenVoiceover = { viewModel.setShowVoiceoverSheet(true) },
                onOpenSafeZones = { viewModel.setShowSafeZoneSheet(true) },
                onSplitClip = { viewModel.splitClipAtCurrentPlayhead() },
                onOpenTrim = { viewModel.setShowTrimSheet(true) },
                onOpenSpeed = { viewModel.setShowSpeedSheet(true) },
                onOpenVolume = { viewModel.setShowVolumeSheet(true) },
                onOpenAdjust = { viewModel.setShowAdjustSheet(true) },
                onOpenFilters = { viewModel.setShowFilterSheet(true) },
                onMoveClipLeft = { viewModel.moveClipLeft(uiState.selectedClipIndex) },
                onMoveClipRight = { viewModel.moveClipRight(uiState.selectedClipIndex) },
                onToggleFlip = { viewModel.toggleClipFlip(uiState.selectedClipIndex) },
                onRotate90 = { viewModel.rotateClip90(uiState.selectedClipIndex) },
                onToggleMute = { viewModel.toggleSelectedClipMute() },
                onDuplicateClip = { viewModel.duplicateSelectedClip() },
                onDeleteClip = { viewModel.deleteSelectedClip() },
                onEditOverlay = { viewModel.setShowTextEditorSheet(true) },
                onOverlayStartHere = { viewModel.setSelectedOverlayStartToPlayhead() },
                onOverlayEndHere = { viewModel.setSelectedOverlayEndToPlayhead() },
                onDuplicateOverlay = { viewModel.duplicateSelectedOverlay() },
                onDeleteOverlay = { selectedTextOverlay?.let { viewModel.deleteTextOverlay(it.id) } },
                onOpenAudioVolume = { viewModel.setShowVolumeSheet(true) },
                onMoveAudioToPlayhead = { viewModel.moveSelectedAudioToPlayhead() },
                onToggleAudioLoop = { viewModel.toggleSelectedAudioLoop() },
                onOpenAudioFades = { viewModel.setShowFadeSheet(true) },
                onDeleteAudio = { viewModel.deleteSelectedAudioTrack() }
            )
        }

        // Bottom Sheets & Dialogs
        if (uiState.showTrimSheet && selectedClip != null) {
            TrimSheet(
                clip = selectedClip,
                onDismiss = { viewModel.setShowTrimSheet(false) },
                onSaveTrim = { start, end -> viewModel.trimSelectedClip(start, end) }
            )
        }

        if (uiState.showSpeedSheet && selectedClip != null) {
            SpeedSheet(
                clip = selectedClip,
                onDismiss = { viewModel.setShowSpeedSheet(false) },
                onSelectSpeed = { viewModel.setSelectedClipSpeed(it) }
            )
        }

        if (uiState.showVolumeSheet && selectedAudioTrack != null) {
            VolumeSheet(
                title = "🔊 ${selectedAudioTrack.title}",
                description = "Volume of this track in the mix.",
                initialVolume = selectedAudioTrack.volume,
                onDismiss = { viewModel.setShowVolumeSheet(false) },
                onSetVolume = { viewModel.setSelectedAudioVolume(it) }
            )
        } else if (uiState.showVolumeSheet && selectedClip != null) {
            VolumeSheet(
                title = "🔊 Clip Volume",
                description = "Lower the original sound so music or a voiceover stands out. 0% mutes the clip.",
                initialVolume = if (selectedClip.isMuted) 0f else selectedClip.volume,
                onDismiss = { viewModel.setShowVolumeSheet(false) },
                onSetVolume = { viewModel.setSelectedClipVolume(it) }
            )
        }

        if (uiState.showFadeSheet && selectedAudioTrack != null) {
            FadeSheet(
                track = selectedAudioTrack,
                onDismiss = { viewModel.setShowFadeSheet(false) },
                onSetFades = { fadeIn, fadeOut -> viewModel.setSelectedAudioFades(fadeIn, fadeOut) }
            )
        }

        if (uiState.showAdjustSheet && selectedClip != null) {
            AdjustSheet(
                clip = selectedClip,
                clipCount = uiState.project.clips.size,
                onDismiss = {
                    viewModel.commitClipAdjustments()
                    viewModel.setShowAdjustSheet(false)
                },
                onAdjust = { transform -> viewModel.updateSelectedClipColor(transform) },
                onCommit = { viewModel.commitClipAdjustments() },
                onReset = { viewModel.resetSelectedClipColor() },
                onApplyToAll = { viewModel.applySelectedClipColorToAll() },
                onCompare = { viewModel.setComparingOriginal(it) }
            )
        }

        if (uiState.showFilterSheet && selectedClip != null) {
            FilterSheet(
                currentFilter = selectedClip.filter,
                onDismiss = { viewModel.setShowFilterSheet(false) },
                onSelectFilter = { viewModel.setSelectedClipFilter(it) }
            )
        }

        if (uiState.showTextEditorSheet) {
            TextEditorSheet(
                existingOverlay = selectedTextOverlay,
                totalDurationMs = playerState.totalDurationMs,
                currentPositionMs = playerState.currentPositionMs,
                onDismiss = { viewModel.setShowTextEditorSheet(false) },
                onSaveOverlay = { viewModel.addOrUpdateTextOverlay(it) },
                onDeleteOverlay = { viewModel.deleteTextOverlay(it) }
            )
        }

        if (uiState.showStickerSheet) {
            StickerSheet(
                totalDurationMs = playerState.totalDurationMs,
                currentPositionMs = playerState.currentPositionMs,
                onDismiss = { viewModel.setShowStickerSheet(false) },
                onAddSticker = { viewModel.addOrUpdateTextOverlay(it) }
            )
        }

        if (uiState.showAudioSheet) {
            AudioSheet(
                audioTracks = uiState.project.audioTracks,
                allClipsMuted = uiState.project.clips.isNotEmpty() && uiState.project.clips.all { it.isMuted },
                onDismiss = { viewModel.setShowAudioSheet(false) },
                onPickAudioFile = { audioPickerLauncher.launch("audio/*") },
                onOpenVoiceover = { viewModel.setShowVoiceoverSheet(true) },
                onAddSoundEffect = { viewModel.addBuiltInSoundEffect(it, context) },
                onToggleMuteOriginalAudio = { viewModel.toggleMuteAllClips() },
                onSelectAudioTrack = { viewModel.focusAudioTrack(it) },
                onRemoveAudioTrack = { viewModel.deleteAudioTrack(it) }
            )
        }

        if (uiState.showVoiceoverSheet) {
            VoiceoverSheet(
                currentTimelinePositionMs = playerState.currentPositionMs,
                onDismiss = { viewModel.setShowVoiceoverSheet(false) },
                onAddVoiceoverTrack = { viewModel.addVoiceoverTrack(it) }
            )
        }

        if (uiState.showSafeZoneSheet) {
            SafeZoneSheet(
                currentSafeZone = uiState.project.safeZone,
                onDismiss = { viewModel.setShowSafeZoneSheet(false) },
                onSelectSafeZone = { viewModel.setSafeZone(it) }
            )
        }

        if (uiState.showExportDialog) {
            ExportDialog(
                exportState = exportState,
                onStartExport = { preset ->
                    viewModel.startExport(preset)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
                    ) {
                        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                },
                onCancelExport = { viewModel.cancelExport() },
                onDismiss = {
                    viewModel.setShowExportDialog(false)
                    viewModel.resetExportState()
                }
            )
        }

        if (uiState.showOpenSourceSheet) {
            OpenSourceSheet(onDismiss = { viewModel.setShowOpenSourceSheet(false) })
        }
    }
}
