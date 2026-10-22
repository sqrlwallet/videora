package com.kevan.videorabykevan.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CallSplit
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.decode.VideoFrameDecoder
import coil.request.ImageRequest
import coil.request.videoFrameMillis
import com.kevan.videorabykevan.model.AudioTrack
import com.kevan.videorabykevan.model.TextOverlay
import com.kevan.videorabykevan.model.VideoClip
import com.kevan.videorabykevan.ui.theme.DarkBackground
import com.kevan.videorabykevan.ui.theme.DarkCard
import com.kevan.videorabykevan.ui.theme.DarkElevated
import com.kevan.videorabykevan.ui.theme.DarkSurface
import com.kevan.videorabykevan.ui.theme.DarkSurfaceVariant
import com.kevan.videorabykevan.ui.theme.GoldChampagne
import com.kevan.videorabykevan.ui.theme.GoldGlow
import com.kevan.videorabykevan.ui.theme.GoldRimGradient
import com.kevan.videorabykevan.ui.theme.HyperCyan
import com.kevan.videorabykevan.ui.theme.LuxuryGoldGradient
import com.kevan.videorabykevan.ui.theme.NeonCoral
import com.kevan.videorabykevan.ui.theme.RoyalAmethyst
import com.kevan.videorabykevan.ui.theme.TextPrimary
import com.kevan.videorabykevan.ui.theme.TextSecondary
import com.kevan.videorabykevan.ui.theme.TextTertiary
import kotlinx.coroutines.launch
import kotlin.math.max

private const val DP_PER_SECOND = 48f

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
    val density = LocalDensity.current
    val scrollState = rememberScrollState()
    val coroutineScope = rememberCoroutineScope()
    var isUserScrolling by remember { mutableStateOf(false) }

    val pxPerSecond = with(density) { DP_PER_SECOND.dp.toPx() }
    val totalDurationSafe = totalDurationMs.coerceAtLeast(1L)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(DarkSurface)
            .border(width = 1.dp, color = Color.White.copy(alpha = 0.06f))
            .padding(vertical = 6.dp)
    ) {
        // ==========================================
        // 1. Studio Header: Monospace Timecode & Transport
        // ==========================================
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val curSec = currentPositionMs / 1000
            val curMin = curSec / 60
            val curSecRem = curSec % 60
            val curMillis = (currentPositionMs % 1000) / 100

            val totSec = totalDurationMs / 1000
            val totMin = totSec / 60
            val totSecRem = totSec % 60

            Row(verticalAlignment = Alignment.CenterVertically) {
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
                    fontFamily = FontFamily.Monospace
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

            // Transport Buttons
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
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

                IconButton(
                    onClick = onTogglePlayPause,
                    modifier = Modifier
                        .size(40.dp)
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
                        modifier = Modifier.size(22.dp)
                    )
                }

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

        Spacer(modifier = Modifier.height(4.dp))

        // ==========================================
        // 2. Proportional Scrollable NLE Filmstrip (CapCut / Reels style)
        // ==========================================
        val filmstripTrackHeight = if (audioTracks.isNotEmpty()) 114.dp else 90.dp
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .height(filmstripTrackHeight)
                .padding(horizontal = 4.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(DarkBackground)
                .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(12.dp))
        ) {
            val viewWidthPx = constraints.maxWidth.toFloat().coerceAtLeast(1f)
            val halfViewWidthDp = with(density) { (viewWidthPx / 2f).toDp() }

            if (clips.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(86.dp)
                        .clickable(onClick = onAddClipClicked),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, tint = GoldChampagne, modifier = Modifier.size(18.dp))
                        Text(
                            text = "No clips in timeline. Tap here to add videos.",
                            color = TextSecondary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            } else {
                // Auto-sync timeline scroll with playback position when not actively user-dragging
                LaunchedEffect(currentPositionMs, isPlaying) {
                    if (isPlaying && !isUserScrolling) {
                        val currentSeconds = currentPositionMs / 1000f
                        val targetPx = (currentSeconds * pxPerSecond).toInt()
                        scrollState.scrollTo(targetPx)
                    }
                }

                Box(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Horizontally Scrollable Filmstrip Track
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(scrollState)
                            .pointerInput(Unit) {
                                detectDragGestures(
                                    onDragStart = { isUserScrolling = true },
                                    onDragEnd = { isUserScrolling = false },
                                    onDragCancel = { isUserScrolling = false },
                                    onDrag = { change, _ ->
                                        change.consume()
                                        val scrollOffset = scrollState.value
                                        val seekSeconds = scrollOffset / pxPerSecond
                                        val seekMs = (seekSeconds * 1000L).toLong().coerceIn(0L, totalDurationMs)
                                        onSeekTo(seekMs)
                                    }
                                )
                            }
                    ) {
                        // Leading spacer so time 00:00 sits precisely under the center playhead needle
                        Spacer(modifier = Modifier.width(halfViewWidthDp))

                        Column {
                            // Timeline Ruler Track (Ticks every 1s & 5s)
                            val totalSeconds = (totalDurationSafe / 1000f).toInt() + 1
                            Row(
                                modifier = Modifier
                                    .height(18.dp)
                                    .background(DarkSurfaceVariant)
                            ) {
                                for (sec in 0..totalSeconds) {
                                    Box(
                                        modifier = Modifier
                                            .width(DP_PER_SECOND.dp)
                                            .fillMaxHeight()
                                    ) {
                                        Canvas(modifier = Modifier.fillMaxSize()) {
                                            drawLine(
                                                color = Color.White.copy(alpha = 0.35f),
                                                start = Offset(0f, size.height),
                                                end = Offset(0f, 0f),
                                                strokeWidth = 1.2f
                                            )
                                        }
                                        Text(
                                            text = "${sec}s",
                                            color = TextTertiary,
                                            fontSize = 8.sp,
                                            fontFamily = FontFamily.Monospace,
                                            modifier = Modifier.padding(start = 3.dp, top = 2.dp)
                                        )
                                    }
                                }
                            }

                            // Primary Video Filmstrip Track
                            Row(
                                modifier = Modifier
                                    .height(68.dp)
                                    .background(DarkSurface)
                                    .padding(vertical = 3.dp),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                clips.forEachIndexed { index, clip ->
                                    val clipSeconds = clip.playbackDurationMs / 1000f
                                    val clipWidthDp = max(72f, clipSeconds * DP_PER_SECOND).dp
                                    val isSelected = index == selectedClipIndex

                                    VideoClipFilmstripBlock(
                                        clip = clip,
                                        widthDp = clipWidthDp,
                                        isSelected = isSelected,
                                        onClick = {
                                            onSelectClip(index)
                                            // Seek to clip start when selected
                                            val clipStartMs = clips.take(index).sumOf { it.playbackDurationMs }
                                            onSeekTo(clipStartMs)
                                        }
                                    )
                                }
                            }

                            // Secondary Audio & Voiceover Strip
                            if (audioTracks.isNotEmpty()) {
                                Row(
                                    modifier = Modifier
                                        .height(24.dp)
                                        .background(DarkSurfaceVariant)
                                        .padding(vertical = 2.dp),
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    audioTracks.forEach { track ->
                                        Row(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(if (track.isVoiceover) NeonCoral.copy(alpha = 0.25f) else HyperCyan.copy(alpha = 0.25f))
                                                .border(0.8.dp, if (track.isVoiceover) NeonCoral else HyperCyan, RoundedCornerShape(4.dp))
                                                .padding(horizontal = 8.dp, vertical = 2.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Icon(
                                                if (track.isVoiceover) Icons.Default.Mic else Icons.Default.MusicNote,
                                                contentDescription = null,
                                                tint = if (track.isVoiceover) NeonCoral else HyperCyan,
                                                modifier = Modifier.size(11.dp)
                                            )
                                            Text(
                                                text = track.title,
                                                color = TextPrimary,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Trailing spacer so the final timestamp can sit precisely under the center playhead needle
                        Spacer(modifier = Modifier.width(halfViewWidthDp))
                    }

                    // Center Magnetic Playhead Needle (The CapCut / Reels Standard)
                    // Needle stays fixed in center while the filmstrip smoothly glides beneath it!
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .align(Alignment.Center)
                            .width(2.dp)
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            // Needle Line
                            drawLine(
                                color = GoldChampagne,
                                start = Offset(size.width / 2f, 0f),
                                end = Offset(size.width / 2f, size.height),
                                strokeWidth = 2.dp.toPx()
                            )
                            // Top Diamond Head
                            val diamondRadius = 5.dp.toPx()
                            drawCircle(
                                color = GoldGlow,
                                radius = diamondRadius,
                                center = Offset(size.width / 2f, diamondRadius)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun VideoClipFilmstripBlock(
    clip: VideoClip,
    widthDp: androidx.compose.ui.unit.Dp,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val context = LocalContext.current

    Box(
        modifier = Modifier
            .width(widthDp)
            .fillMaxHeight()
            .clip(RoundedCornerShape(8.dp))
            .background(DarkCard)
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                brush = if (isSelected) GoldRimGradient else Brush.linearGradient(listOf(Color.White.copy(alpha = 0.15f), Color.White.copy(alpha = 0.05f))),
                shape = RoundedCornerShape(8.dp)
            )
            .clickable(onClick = onClick)
    ) {
        // Real Video Thumbnail Frame via Coil VideoFrameDecoder
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(clip.uri)
                .decoderFactory(VideoFrameDecoder.Factory())
                .videoFrameMillis(clip.trimStartMs)
                .crossfade(true)
                .build(),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(8.dp))
        )

        // Dark Gradient Scrim so text/badges remain 100% legible
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color.Black.copy(alpha = 0.65f),
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.75f)
                        )
                    )
                )
        )

        // Header and Footer Tags
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 6.dp, vertical = 4.dp),
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
                    fontSize = 10.sp,
                    fontWeight = if (isSelected) FontWeight.Black else FontWeight.Bold,
                    maxLines = 1
                )

                if (clip.speed != 1.0f) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(3.dp))
                            .background(HyperCyan.copy(alpha = 0.3f))
                            .padding(horizontal = 3.dp, vertical = 1.dp)
                    ) {
                        Text(
                            text = "${clip.speed}x",
                            color = HyperCyan,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val durSec = clip.playbackDurationMs / 1000f
                Text(
                    text = String.format(java.util.Locale.US, "%.1fs", durSec),
                    color = Color.White.copy(alpha = 0.85f),
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
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
                        Text("🪞", fontSize = 8.sp)
                    }
                    if (clip.filter != com.kevan.videorabykevan.model.VideoFilter.NONE) {
                        Text("🎨", fontSize = 8.sp)
                    }
                }
            }
        }

        // Active Clip Gold Trim Indicators on Left & Right
        if (isSelected) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(4.dp)
                    .align(Alignment.CenterStart)
                    .background(GoldChampagne)
            )
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(4.dp)
                    .align(Alignment.CenterEnd)
                    .background(GoldChampagne)
            )
        }
    }
}
