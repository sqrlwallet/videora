package com.kevan.videorabykevan.ui.components

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.CallSplit
import androidx.compose.material.icons.automirrored.filled.RotateRight
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.AddReaction
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FilterAlt
import androidx.compose.material.icons.filled.FindReplace
import androidx.compose.material.icons.filled.FirstPage
import androidx.compose.material.icons.filled.Flip
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.LastPage
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.ViewInAr
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kevan.videorabykevan.model.AudioTrack
import com.kevan.videorabykevan.model.TextOverlay
import com.kevan.videorabykevan.model.VideoClip
import com.kevan.videorabykevan.model.VoiceEffect
import com.kevan.videorabykevan.ui.theme.AccentCyan
import com.kevan.videorabykevan.ui.theme.AccentPink
import com.kevan.videorabykevan.ui.theme.DarkSurface
import com.kevan.videorabykevan.ui.theme.DarkSurfaceVariant
import com.kevan.videorabykevan.ui.theme.GoldChampagne
import com.kevan.videorabykevan.ui.theme.HyperCyan
import com.kevan.videorabykevan.ui.theme.NeonCoral

/**
 * Context-aware editing toolbar (CapCut / Reels style):
 * - nothing selected: project tools (add media, text, stickers, audio...)
 * - a clip selected: tools for that clip
 * - a text/sticker selected: tools for that overlay
 * - an audio track selected: tools for that track
 * The first button always says what is selected and closes it.
 */
@Composable
fun ClipActionRow(
    selectedClip: VideoClip?,
    selectedClipIndex: Int,
    clipCount: Int,
    selectedOverlay: TextOverlay?,
    selectedAudioTrack: AudioTrack?,
    onClearSelection: () -> Unit,
    onAddClip: () -> Unit,
    onOpenText: () -> Unit,
    onOpenStickers: () -> Unit,
    onOpenAudio: () -> Unit,
    onOpenVoiceover: () -> Unit,
    onOpenSafeZones: () -> Unit,
    onSplitClip: () -> Unit,
    onFreezeFrame: () -> Unit,
    onOpenTrim: () -> Unit,
    onOpenSpeed: () -> Unit,
    onOpenVolume: () -> Unit,
    onOpenAdjust: () -> Unit,
    onOpenFilters: () -> Unit,
    onMoveClipLeft: () -> Unit,
    onMoveClipRight: () -> Unit,
    onToggleFlip: () -> Unit,
    onRotate90: () -> Unit,
    onToggleMute: () -> Unit,
    onDuplicateClip: () -> Unit,
    onReplaceClip: () -> Unit,
    onDeleteClip: () -> Unit,
    onEditOverlay: () -> Unit,
    onOverlayStartHere: () -> Unit,
    onOverlayEndHere: () -> Unit,
    onDuplicateOverlay: () -> Unit,
    onReadAloud: () -> Unit,
    onDeleteOverlay: () -> Unit,
    onOpenAudioVolume: () -> Unit,
    onMoveAudioToPlayhead: () -> Unit,
    onToggleAudioLoop: () -> Unit,
    onOpenAudioFades: () -> Unit,
    onOpenVoiceEffect: () -> Unit,
    onDeleteAudio: () -> Unit,
    modifier: Modifier = Modifier
) {
    val mode = when {
        selectedAudioTrack != null -> ToolbarMode.AUDIO
        selectedOverlay != null -> ToolbarMode.OVERLAY
        selectedClip != null -> ToolbarMode.CLIP
        else -> ToolbarMode.PROJECT
    }

    Crossfade(targetState = mode, label = "toolbarMode", modifier = modifier) { currentMode ->
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(DarkSurface)
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            when (currentMode) {
                ToolbarMode.PROJECT -> {
                    ActionButton(Icons.Default.AddCircle, "Add Media", GoldChampagne, onClick = onAddClip)
                    ActionButton(Icons.Default.TextFields, "Text/Hook", AccentCyan, onClick = onOpenText)
                    ActionButton(Icons.Default.AddReaction, "Stickers", Color(0xFFFFDE03), onClick = onOpenStickers)
                    ActionButton(Icons.Default.MusicNote, "Audio/SFX", onClick = onOpenAudio)
                    ActionButton(Icons.Default.Mic, "Voiceover", AccentPink, onClick = onOpenVoiceover)
                    ActionButton(Icons.Default.ViewInAr, "Safe Zone", onClick = onOpenSafeZones)
                    if (clipCount > 0) {
                        HintLabel("Tap a clip in the timeline to edit it")
                    }
                }

                ToolbarMode.CLIP -> {
                    val clip = selectedClip ?: return@Row
                    SelectionChip("${if (clip.isPhoto) "Photo" else "Clip"} ${selectedClipIndex + 1}/$clipCount", onClearSelection)
                    ActionButton(Icons.AutoMirrored.Filled.CallSplit, "Split", NeonCoral, onClick = onSplitClip)
                    ActionButton(Icons.Default.ContentCut, if (clip.isPhoto) "Duration" else "Trim", onClick = onOpenTrim)
                    // Photos and freeze frames are silent stills: no speed, sound or freezing them again
                    if (!clip.isPhoto) {
                        ActionButton(Icons.Default.AcUnit, "Freeze", HyperCyan, onClick = onFreezeFrame)
                        ActionButton(Icons.Default.Speed, "Speed", onClick = onOpenSpeed)
                        ActionButton(Icons.AutoMirrored.Filled.VolumeUp, "Volume", onClick = onOpenVolume)
                        ActionButton(
                            Icons.Default.GraphicEq,
                            "Voice",
                            if (clip.voiceEffect != VoiceEffect.NONE) AccentPink else Color.White,
                            onClick = onOpenVoiceEffect
                        )
                        ActionButton(
                            if (clip.isMuted) Icons.AutoMirrored.Filled.VolumeOff else Icons.AutoMirrored.Filled.VolumeUp,
                            if (clip.isMuted) "Unmute" else "Mute",
                            onClick = onToggleMute
                        )
                    }
                    ActionButton(Icons.Default.FilterAlt, "Filters", AccentPink, onClick = onOpenFilters)
                    ActionButton(Icons.Default.Tune, "Color", AccentCyan, onClick = onOpenAdjust)
                    ActionButton(Icons.Default.Flip, "Mirror", onClick = onToggleFlip)
                    ActionButton(Icons.AutoMirrored.Filled.RotateRight, "Rotate", onClick = onRotate90)
                    ActionButton(
                        Icons.AutoMirrored.Filled.ArrowBack, "Move ◀",
                        enabled = selectedClipIndex > 0, onClick = onMoveClipLeft
                    )
                    ActionButton(
                        Icons.AutoMirrored.Filled.ArrowForward, "Move ▶",
                        enabled = selectedClipIndex < clipCount - 1, onClick = onMoveClipRight
                    )
                    ActionButton(Icons.Default.ContentCopy, "Duplicate", onClick = onDuplicateClip)
                    ActionButton(Icons.Default.FindReplace, "Replace", onClick = onReplaceClip)
                    ActionButton(Icons.Default.Delete, "Delete", Color(0xFFFF453A), onClick = onDeleteClip)
                    HintLabel("Drag the gold edges to trim · long-press a clip to reorder it")
                }

                ToolbarMode.OVERLAY -> {
                    val overlay = selectedOverlay ?: return@Row
                    SelectionChip(if (overlay.isSticker) "Sticker ${overlay.text}" else "Text", onClearSelection)
                    ActionButton(Icons.Default.Edit, "Edit", AccentCyan, onClick = onEditOverlay)
                    if (!overlay.isSticker) {
                        ActionButton(Icons.Default.RecordVoiceOver, "Read aloud", AccentPink, onClick = onReadAloud)
                    }
                    ActionButton(Icons.Default.FirstPage, "Start here", onClick = onOverlayStartHere)
                    ActionButton(Icons.Default.LastPage, "End here", onClick = onOverlayEndHere)
                    ActionButton(Icons.Default.ContentCopy, "Duplicate", onClick = onDuplicateOverlay)
                    ActionButton(Icons.Default.Delete, "Delete", Color(0xFFFF453A), onClick = onDeleteOverlay)
                    HintLabel("Drag to move, pinch to resize · drag its timeline bar or its ends to retime")
                }

                ToolbarMode.AUDIO -> {
                    val track = selectedAudioTrack ?: return@Row
                    SelectionChip(if (track.isVoiceover) "Voiceover" else "Audio", onClearSelection)
                    ActionButton(Icons.AutoMirrored.Filled.VolumeUp, "Volume", onClick = onOpenAudioVolume)
                    ActionButton(Icons.Default.FirstPage, "Start here", onClick = onMoveAudioToPlayhead)
                    ActionButton(
                        Icons.Default.Repeat,
                        if (track.isLooping) "Loop on" else "Loop off",
                        if (track.isLooping) GoldChampagne else Color.White,
                        onClick = onToggleAudioLoop
                    )
                    ActionButton(
                        Icons.AutoMirrored.Filled.TrendingUp,
                        "Fade",
                        if (track.hasFades) GoldChampagne else Color.White,
                        onClick = onOpenAudioFades
                    )
                    ActionButton(
                        Icons.Default.GraphicEq,
                        "Voice",
                        if (track.voiceEffect != VoiceEffect.NONE) AccentPink else Color.White,
                        onClick = onOpenVoiceEffect
                    )
                    ActionButton(Icons.Default.Delete, "Delete", Color(0xFFFF453A), onClick = onDeleteAudio)
                    HintLabel("${track.title} · drag its timeline bar to move it")
                }
            }
        }
    }
}

private enum class ToolbarMode { PROJECT, CLIP, OVERLAY, AUDIO }

/** Leading "what's selected" pill; tapping it returns to the project tools. */
@Composable
private fun SelectionChip(label: String, onClose: () -> Unit) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClose)
            .padding(horizontal = 6.dp, vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(GoldChampagne),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Close, contentDescription = "Done editing", tint = Color.Black, modifier = Modifier.size(20.dp))
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            color = GoldChampagne,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1
        )
    }
}

@Composable
private fun HintLabel(text: String) {
    Text(
        text = text,
        color = Color.White.copy(alpha = 0.45f),
        fontSize = 11.sp,
        modifier = Modifier.padding(start = 4.dp, end = 8.dp)
    )
}

@Composable
private fun ActionButton(
    icon: ImageVector,
    label: String,
    accentColor: Color = Color.White,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    val alpha = if (enabled) 1f else 0.35f
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 6.dp, vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(DarkSurfaceVariant.copy(alpha = alpha))
                .border(
                    1.dp,
                    if (accentColor != Color.White) accentColor.copy(alpha = 0.3f * alpha) else Color.White.copy(alpha = 0.1f * alpha),
                    RoundedCornerShape(12.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = accentColor.copy(alpha = alpha),
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            color = Color.White.copy(alpha = alpha),
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium
        )
    }
}
