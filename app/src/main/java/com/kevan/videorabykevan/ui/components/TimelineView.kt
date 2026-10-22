package com.kevan.videorabykevan.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CallSplit
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kevan.videorabykevan.model.AudioTrack
import com.kevan.videorabykevan.model.TextOverlay
import com.kevan.videorabykevan.model.VideoClip
import com.kevan.videorabykevan.ui.theme.DarkBackground
import com.kevan.videorabykevan.ui.theme.DarkCard
import com.kevan.videorabykevan.ui.theme.DarkElevated
import com.kevan.videorabykevan.ui.theme.DarkStudioCardGradient
import com.kevan.videorabykevan.ui.theme.DarkSurface
import com.kevan.videorabykevan.ui.theme.DarkSurfaceVariant
import com.kevan.videorabykevan.ui.theme.GoldChampagne
import com.kevan.videorabykevan.ui.theme.GoldGlow
import com.kevan.videorabykevan.ui.theme.GoldMetallic
import com.kevan.videorabykevan.ui.theme.GoldRimGradient
import com.kevan.videorabykevan.ui.theme.HyperCyan
import com.kevan.videorabykevan.ui.theme.LuxuryGoldGradient
import com.kevan.videorabykevan.ui.theme.NeonCoral
import com.kevan.videorabykevan.ui.theme.RoyalAmethyst
import com.kevan.videorabykevan.ui.theme.TextPrimary
import com.kevan.videorabykevan.ui.theme.TextSecondary
import com.kevan.videorabykevan.ui.theme.TextTertiary
import kotlin.math.max

@Composable
fun TimelineView(
    clips: List<VideoClip>,
    currentPositionMs: Long,
    totalDurationMs: Long,
    selectedClipIndex: Int,
    isPlaying: Boolean,
    onTogglePlayPause: () -> Unit,
    onStepBackward: () -> Unit,
    onStepForward: () -> Unit,
    onSeekTo: (Long) -> Unit,
    onSelectClip: (Int) -> Unit,
    onSplitCurrentClip: () -> Unit,
    onAddClipClicked: () -> Unit,
    audioTracks: List<AudioTrack> = emptyList(),
    textOverlays: List<TextOverlay> = emptyList(),
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(DarkSurface)
            .border(width = 1.dp, color = Color.White.copy(alpha = 0.06f))
            .padding(vertical = 8.dp)
    ) {
        // ==========================================
        // 1. Studio Header: High-Contrast Monospace Timecode & Transport
        // ==========================================
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Timecode Readout
            val curSec = currentPositionMs / 1000
            val curMin = curSec / 60
            val curSecRem = curSec % 60
            val curMillis = (currentPositionMs % 1000) / 100

            val totSec = totalDurationMs / 1000
            val totMin = totSec / 60
            val totSecRem = totSec % 60

            Row(verticalAlignment = Alignment.CenterVertically) {
                // Current Time
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(DarkSurfaceVariant)
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = String.format(java.util.Locale.US, "%02d:%02d.%d", curMin, curSecRem, curMillis),
                        color = GoldChampagne,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Spacer(modifier = Modifier.width(6.dp))

                Text(
                    text = String.format(java.util.Locale.US, "/ %02d:%02d", totMin, totSecRem),
                    color = TextTertiary,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Medium
                )

                if (clips.isNotEmpty() && selectedClipIndex in clips.indices) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(DarkElevated)
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "CLIP ${selectedClipIndex + 1}/${clips.size}",
                            color = TextSecondary,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Transport Controls
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Step -1s
                IconButton(
                    onClick = onStepBackward,
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(DarkSurfaceVariant)
                ) {
                    Icon(
                        Icons.Default.FastRewind,
                        contentDescription = "-1s",
                        tint = TextSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                }

                // Play / Pause Showcase Button
                IconButton(
                    onClick = onTogglePlayPause,
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(
                            if (isPlaying) Brush.linearGradient(listOf(RoyalAmethyst, NeonCoral))
                            else LuxuryGoldGradient
                        )
                ) {
                    Icon(
                        if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = "Play/Pause",
                        tint = if (isPlaying) Color.White else Color.Black,
                        modifier = Modifier.size(24.dp)
                    )
                }

                // Step +1s
                IconButton(
                    onClick = onStepForward,
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(DarkSurfaceVariant)
                ) {
                    Icon(
                        Icons.Default.FastForward,
                        contentDescription = "+1s",
                        tint = TextSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                }

                // Split at playhead
                IconButton(
                    onClick = onSplitCurrentClip,
                    enabled = clips.isNotEmpty(),
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(DarkSurfaceVariant)
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.CallSplit,
                        contentDescription = "Split",
                        tint = if (clips.isNotEmpty()) NeonCoral else TextTertiary,
                        modifier = Modifier.size(16.dp)
                    )
                }

                // Add clip
                IconButton(
                    onClick = onAddClipClicked,
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(DarkSurfaceVariant)
                ) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = "Add Clip",
                        tint = GoldChampagne,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // ==========================================
        // 2. Timeline Strip: Ruler + Video Clips + Scrubber Needle
        // ==========================================
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(DarkBackground)
                .border(1.dp, Color.White.copy(alpha = 0.06f), RoundedCornerShape(12.dp))
        ) {
            val totalWidthPx = constraints.maxWidth.toFloat().coerceAtLeast(1f)
            val totalDurationSafe = totalDurationMs.coerceAtLeast(1L)
            val playheadFraction = (currentPositionMs.toFloat() / totalDurationSafe).coerceIn(0f, 1f)

            if (clips.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(84.dp)
                        .clickable(onClick = onAddClipClicked),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, tint = GoldChampagne, modifier = Modifier.size(18.dp))
                        Text(
                            text = "No clips in timeline. Tap here to add video clips.",
                            color = TextSecondary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .pointerInput(totalDurationSafe) {
                            detectTapGestures { offset ->
                                val fraction = (offset.x / totalWidthPx).coerceIn(0f, 1f)
                                onSeekTo((fraction * totalDurationSafe).toLong())
                            }
                        }
                        .pointerInput(totalDurationSafe) {
                            detectHorizontalDragGestures { change, _ ->
                                change.consume()
                                val fraction = (change.position.x / totalWidthPx).coerceIn(0f, 1f)
                                onSeekTo((fraction * totalDurationSafe).toLong())
                            }
                        }
                ) {
                    // Timeline Ruler Marks (Top 16dp)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(16.dp)
                            .background(DarkSurfaceVariant)
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val stepPx = size.width / 10f
                            for (i in 0..10) {
                                val x = i * stepPx
                                val isMajor = i % 2 == 0
                                drawLine(
                                    color = if (isMajor) Color.White.copy(alpha = 0.4f) else Color.White.copy(alpha = 0.15f),
                                    start = Offset(x, size.height),
                                    end = Offset(x, if (isMajor) size.height * 0.3f else size.height * 0.6f),
                                    strokeWidth = if (isMajor) 1.5f else 1.0f
                                )
                            }
                        }
                    }

                    // Main Video Filmstrip Track (64dp)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(64.dp)
                            .background(DarkSurface)
                            .padding(2.dp),
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        clips.forEachIndexed { index, clip ->
                            val clipWeight = max(0.06f, (clip.playbackDurationMs.toFloat() / totalDurationSafe))
                            val isSelected = index == selectedClipIndex

                            Box(
                                modifier = Modifier
                                    .weight(clipWeight)
                                    .fillMaxHeight()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) DarkCard else DarkSurfaceVariant)
                                    .border(
                                        width = if (isSelected) 2.dp else 1.dp,
                                        brush = if (isSelected) GoldRimGradient else Brush.linearGradient(listOf(Color.White.copy(alpha = 0.1f), Color.White.copy(alpha = 0.05f))),
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .clickable { onSelectClip(index) }
                                    .padding(horizontal = 6.dp, vertical = 4.dp)
                            ) {
                                Column(
                                    modifier = Modifier.fillMaxSize(),
                                    verticalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = clip.name,
                                            color = if (isSelected) GoldChampagne else TextPrimary,
                                            fontSize = 11.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                                            maxLines = 1
                                        )

                                        if (clip.speed != 1.0f) {
                                            Text(
                                                text = "${clip.speed}x",
                                                color = HyperCyan,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }

                                    // Bottom Badges: Duration, Filter, Flip
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        val durSec = clip.playbackDurationMs / 1000f
                                        Text(
                                            text = String.format(java.util.Locale.US, "%.1fs", durSec),
                                            color = TextSecondary,
                                            fontSize = 9.sp,
                                            fontFamily = FontFamily.Monospace
                                        )

                                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                            if (clip.isMuted) {
                                                Icon(
                                                    Icons.AutoMirrored.Filled.VolumeOff,
                                                    contentDescription = "Muted",
                                                    tint = Color(0xFFFF5252),
                                                    modifier = Modifier.size(11.dp)
                                                )
                                            }
                                            if (clip.isFlippedHorizontal) {
                                                Text("🪞", fontSize = 9.sp)
                                            }
                                            if (clip.filter != com.kevan.videorabykevan.model.VideoFilter.NONE) {
                                                Text("🎨", fontSize = 9.sp)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Optional Multi-Track Audio Ribbon (24dp)
                    if (audioTracks.isNotEmpty()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(22.dp)
                                .background(DarkSurfaceVariant)
                                .padding(horizontal = 4.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            audioTracks.forEach { track ->
                                Row(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(if (track.isVoiceover) NeonCoral.copy(alpha = 0.2f) else HyperCyan.copy(alpha = 0.2f))
                                        .border(
                                            0.6.dp,
                                            if (track.isVoiceover) NeonCoral else HyperCyan,
                                            RoundedCornerShape(4.dp)
                                        )
                                        .padding(horizontal = 6.dp, vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                                ) {
                                    Icon(
                                        if (track.isVoiceover) Icons.Default.Mic else Icons.Default.MusicNote,
                                        contentDescription = null,
                                        tint = if (track.isVoiceover) NeonCoral else HyperCyan,
                                        modifier = Modifier.size(10.dp)
                                    )
                                    Text(
                                        text = track.title,
                                        color = TextPrimary,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }
                }

                // ==========================================
                // 3. Scrubber Playhead (Golden Needle with Top Diamond)
                // ==========================================
                Canvas(
                    modifier = Modifier.fillMaxSize()
                ) {
                    val needleX = playheadFraction * size.width
                    // Needle line
                    drawLine(
                        color = GoldChampagne,
                        start = Offset(needleX, 0f),
                        end = Offset(needleX, size.height),
                        strokeWidth = 2.dp.toPx()
                    )
                    // Golden top diamond indicator
                    val capRadius = 4.dp.toPx()
                    drawCircle(
                        color = GoldGlow,
                        radius = capRadius,
                        center = Offset(needleX, capRadius)
                    )
                }
            }
        }
    }
}
