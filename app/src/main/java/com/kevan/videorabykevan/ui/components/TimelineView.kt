package com.kevan.videorabykevan.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.collectIsDraggedAsState
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
import androidx.compose.material.icons.filled.Image
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
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.decode.VideoFrameDecoder
import coil.request.ImageRequest
import coil.request.videoFrameMillis
import com.kevan.videorabykevan.model.AudioTrack
import com.kevan.videorabykevan.model.MIN_AUDIO_TRIM_MS
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.max

private const val DEFAULT_DP_PER_SECOND = 48f
private const val MIN_DP_PER_SECOND = 12f
private const val MAX_DP_PER_SECOND = 240f
private val RULER_LABEL_STEPS = listOf(1, 2, 5, 10, 15, 30, 60)
private const val TRACK_HEIGHT_DP = 24
private const val MIN_CHIP_WIDTH_DP = 28
// A dragged chip snaps to the playhead, a cut or 0:00 when it gets this close
private const val SNAP_DISTANCE_DP = 10f

// Shortest a text chip can be resized to (same as the editor's minimum overlay length)
private const val MIN_TEXT_CHIP_MS = 300L

/** Which end of a text chip is being dragged; none means the whole chip moves. */
private enum class ChipEdge { START, END }

/** A text/sticker or audio chip being moved (or, with [edge], resized) along the timeline. */
private data class ChipDrag(
    val id: String,
    val edge: ChipEdge?,
    val originalStartMs: Long,
    val originalEndMs: Long,
    val deltaPx: Float,
    val snappedTo: Long?
)

/** A clip lifted by a long press and dragged to a new place; [grabX] is where the finger took it, from the clip's left edge. */
private data class ClipReorder(val fromIndex: Int, val grabX: Float, val deltaPx: Float)

// While reordering, holding the finger this close to either edge scrolls the timeline
private const val REORDER_EDGE_DP = 48
private const val REORDER_SCROLL_STEP_DP = 6

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
    selectedOverlayId: String? = null,
    onSelectOverlay: (String) -> Unit = {},
    selectedAudioTrackId: String? = null,
    onSelectAudioTrack: (String) -> Unit = {},
    onScrubStarted: () -> Unit = {},
    onTrimSelectedClip: (startMs: Long, endMs: Long) -> Unit = { _, _ -> },
    onMoveOverlay: (id: String, startMs: Long) -> Unit = { _, _ -> },
    onMoveAudioTrack: (id: String, startMs: Long) -> Unit = { _, _ -> },
    onSetOverlayTiming: (id: String, startMs: Long, endMs: Long) -> Unit = { _, _, _ -> },
    onTrimAudioTrack: (id: String, startOffsetMs: Long, trimStartMs: Long, trimEndMs: Long) -> Unit = { _, _, _, _ -> },
    onMoveClip: (from: Int, to: Int) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val haptics = LocalHapticFeedback.current
    val scrollState = rememberScrollState()

    // True while the user is dragging the filmstrip or it is still flinging from their drag.
    // Programmatic scrolls (following playback) never set it, so they don't seek the player.
    val isDragged by scrollState.interactionSource.collectIsDraggedAsState()
    var isUserScrubbing by remember { mutableStateOf(false) }
    LaunchedEffect(isDragged, scrollState.isScrollInProgress) {
        if (isDragged) {
            if (!isUserScrubbing) onScrubStarted()
            isUserScrubbing = true
        } else if (!scrollState.isScrollInProgress) {
            isUserScrubbing = false
        }
    }

    // Pinch the filmstrip to zoom: fewer dp per second shows more of a long video at once
    var dpPerSecond by remember { mutableFloatStateOf(DEFAULT_DP_PER_SECOND) }
    val pxPerSecond = with(density) { dpPerSecond.dp.toPx() }

    // Dragging the selected clip's gold edges trims it live; the edit is saved when the finger lifts
    var trimPreview by remember { mutableStateOf<VideoClip?>(null) }
    val displayClips = trimPreview
        ?.takeIf { selectedClipIndex in clips.indices && clips[selectedClipIndex].id == it.id }
        ?.let { preview -> clips.toMutableList().also { it[selectedClipIndex] = preview } }
        ?: clips
    // Laid out from the clips (not the player) so a live trim resizes the strip immediately
    val totalDurationSafe = displayClips.sumOf { it.playbackDurationMs }.coerceAtLeast(1L)

    // Dragging the selected audio track's edges trims it live; saved on release
    var audioTrimPreview by remember { mutableStateOf<AudioTrack?>(null) }
    val displayAudioTracks = audioTrimPreview
        ?.takeIf { it.id == selectedAudioTrackId }
        ?.let { preview -> audioTracks.map { if (it.id == preview.id) preview else it } }
        ?: audioTracks

    var chipDrag by remember { mutableStateOf<ChipDrag?>(null) }
    val clipBoundariesMs = remember(clips) { clips.runningFold(0L) { acc, clip -> acc + clip.playbackDurationMs } }

    /** Where the dragged edge (or the start, when moving) would be, before snapping. */
    fun rawDragMs(drag: ChipDrag): Long {
        val fromMs = if (drag.edge == ChipEdge.END) drag.originalEndMs else drag.originalStartMs
        val maxMs = if (drag.edge == ChipEdge.END) totalDurationSafe else (totalDurationSafe - 100L).coerceAtLeast(0L)
        return (fromMs + (drag.deltaPx / pxPerSecond * 1000f).toLong()).coerceIn(0L, maxMs)
    }

    /** A chip's start/end, following the finger while it's being dragged. */
    fun chipSpan(id: String, startMs: Long, endMs: Long): Pair<Long, Long> {
        val drag = chipDrag?.takeIf { it.id == id } ?: return startMs to endMs
        val atMs = drag.snappedTo ?: rawDragMs(drag)
        return when (drag.edge) {
            null -> atMs to atMs + (drag.originalEndMs - drag.originalStartMs)
            ChipEdge.START -> atMs.coerceAtMost(drag.originalEndMs - MIN_TEXT_CHIP_MS).coerceAtLeast(0L) to drag.originalEndMs
            ChipEdge.END -> drag.originalStartMs to atMs.coerceAtLeast(drag.originalStartMs + MIN_TEXT_CHIP_MS)
        }
    }

    fun dragChip(id: String, edge: ChipEdge?, startMs: Long, endMs: Long, deltaPx: Float) {
        val current = chipDrag?.takeIf { it.id == id && it.edge == edge } ?: ChipDrag(id, edge, startMs, endMs, 0f, null)
        val moved = current.copy(deltaPx = current.deltaPx + deltaPx)
        val rawMs = rawDragMs(moved)
        val snapMs = (SNAP_DISTANCE_DP / dpPerSecond * 1000f).toLong()
        val snapped = (clipBoundariesMs + currentPositionMs).minByOrNull { kotlin.math.abs(it - rawMs) }
            ?.takeIf { kotlin.math.abs(it - rawMs) <= snapMs }
        if (snapped != null && snapped != current.snappedTo) {
            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        }
        chipDrag = moved.copy(snappedTo = snapped)
    }

    fun endChipDrag(onMove: (String, Long) -> Unit) {
        val drag = chipDrag ?: return
        val (startMs, endMs) = chipSpan(drag.id, drag.originalStartMs, drag.originalEndMs)
        chipDrag = null
        if (startMs == drag.originalStartMs && endMs == drag.originalEndMs) return
        if (drag.edge == null) onMove(drag.id, startMs) else onSetOverlayTiming(drag.id, startMs, endMs)
    }

    // Long-press a clip and drag it sideways to reorder; the gold marker shows where it will land
    var reorder by remember { mutableStateOf<ClipReorder?>(null) }

    /** The index the lifted clip would take if dropped now (its index after removal from the list). */
    fun reorderTarget(drag: ClipReorder): Int {
        val starts = clipBoundariesMs
        val draggedCenterPx = (starts[drag.fromIndex] + starts[drag.fromIndex + 1]) / 2f / 1000f * pxPerSecond + drag.deltaPx
        return clips.indices
            .filter { it != drag.fromIndex }
            .count { (starts[it] + starts[it + 1]) / 2f / 1000f * pxPerSecond < draggedCenterPx }
    }

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
        val extraTracks = (if (textOverlays.isNotEmpty()) 1 else 0) + (if (audioTracks.isNotEmpty()) 1 else 0)
        val filmstripTrackHeight = (90 + extraTracks * TRACK_HEIGHT_DP).dp
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
                            text = "No clips yet. Tap here to add videos or photos.",
                            color = TextSecondary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            } else {
                val latestOnSeekTo by rememberUpdatedState(onSeekTo)
                val latestTotalMs by rememberUpdatedState(totalDurationMs)

                // Scrubbing: the playhead is fixed in the center, so the scroll offset IS the time
                LaunchedEffect(scrollState, pxPerSecond) {
                    snapshotFlow { scrollState.value }.collect { offsetPx ->
                        if (isUserScrubbing) {
                            val seekMs = (offsetPx / pxPerSecond * 1000f).toLong().coerceIn(0L, latestTotalMs)
                            latestOnSeekTo(seekMs)
                        }
                    }
                }

                // Follow the playhead during playback and after seeks, steps, splits and clip taps
                LaunchedEffect(currentPositionMs, pxPerSecond) {
                    if (!isUserScrubbing) {
                        val targetPx = (currentPositionMs / 1000f * pxPerSecond).toInt()
                        if (targetPx != scrollState.value) {
                            scrollState.scrollTo(targetPx)
                        }
                    }
                }

                fun msToDp(ms: Long) = (ms / 1000f * dpPerSecond).dp

                // Reordering past the visible part: scroll while the finger is held near an edge,
                // moving the lifted clip by the same amount so it stays under the finger
                val isReordering = reorder != null
                LaunchedEffect(isReordering) {
                    val edgePx = with(density) { REORDER_EDGE_DP.dp.toPx() }
                    val stepPx = with(density) { REORDER_SCROLL_STEP_DP.dp.toPx() }
                    while (isReordering) {
                        val drag = reorder ?: break
                        val fingerContentPx = clipBoundariesMs[drag.fromIndex] / 1000f * pxPerSecond + drag.grabX + drag.deltaPx
                        val fingerViewPx = fingerContentPx + viewWidthPx / 2f - scrollState.value
                        val direction = when {
                            fingerViewPx < edgePx -> -1f
                            fingerViewPx > viewWidthPx - edgePx -> 1f
                            else -> 0f
                        }
                        if (direction != 0f) {
                            val scrolled = scrollState.scrollBy(direction * stepPx)
                            reorder = reorder?.let { it.copy(deltaPx = it.deltaPx + scrolled) }
                        }
                        delay(16)
                    }
                }
                val contentWidthDp = msToDp(totalDurationSafe)

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .pointerInput(Unit) {
                            // Two-finger pinch zooms; one-finger drags still reach the scroller.
                            // Initial pass + consume stops the filmstrip scrolling mid-pinch.
                            awaitEachGesture {
                                awaitFirstDown(requireUnconsumed = false)
                                do {
                                    val event = awaitPointerEvent(PointerEventPass.Initial)
                                    if (event.changes.count { it.pressed } >= 2) {
                                        val zoom = event.calculateZoom()
                                        if (zoom != 1f) {
                                            dpPerSecond = (dpPerSecond * zoom).coerceIn(MIN_DP_PER_SECOND, MAX_DP_PER_SECOND)
                                        }
                                        event.changes.forEach { it.consume() }
                                    }
                                } while (event.changes.any { it.pressed })
                            }
                        }
                ) {
                    // Horizontally Scrollable Filmstrip Track
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(scrollState)
                    ) {
                        // Leading spacer so time 00:00 sits precisely under the center playhead needle
                        Spacer(modifier = Modifier.width(halfViewWidthDp))

                        Column(modifier = Modifier.width(contentWidthDp)) {
                            // Timeline Ruler: a tick every second, labels spaced to stay readable at any zoom
                            val totalSeconds = (totalDurationSafe / 1000L).toInt()
                            val labelEverySeconds = RULER_LABEL_STEPS.firstOrNull { it * dpPerSecond >= 36f } ?: RULER_LABEL_STEPS.last()
                            val textMeasurer = rememberTextMeasurer()
                            val labelStyle = TextStyle(color = TextTertiary, fontSize = 8.sp, fontFamily = FontFamily.Monospace)
                            Canvas(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(18.dp)
                                    .background(DarkSurfaceVariant)
                            ) {
                                val labelPadding = 3.dp.toPx()
                                for (sec in 0..totalSeconds) {
                                    val x = sec * pxPerSecond
                                    val isLabelled = sec % labelEverySeconds == 0
                                    drawLine(
                                        color = Color.White.copy(alpha = if (isLabelled) 0.6f else 0.3f),
                                        start = Offset(x, size.height),
                                        end = Offset(x, if (isLabelled) 0f else size.height / 2f),
                                        strokeWidth = 1.2f
                                    )
                                    if (isLabelled) {
                                        drawText(
                                            textMeasurer = textMeasurer,
                                            text = "${sec}s",
                                            style = labelStyle,
                                            topLeft = Offset(x + labelPadding, 2.dp.toPx())
                                        )
                                    }
                                }
                            }

                            // Primary Video Filmstrip Track: widths are exactly proportional to
                            // playback duration so every pixel maps to the same moment as the playhead
                            Box(modifier = Modifier.fillMaxWidth().height(68.dp)) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .fillMaxHeight()
                                        .background(DarkSurface)
                                        .padding(vertical = 3.dp)
                                ) {
                                    displayClips.forEachIndexed { index, clip ->
                                        val isSelected = index == selectedClipIndex
                                        val lifted = reorder?.takeIf { it.fromIndex == index }
                                        VideoClipFilmstripBlock(
                                            modifier = if (lifted != null) {
                                                Modifier
                                                    .zIndex(1f)
                                                    .offset { IntOffset(lifted.deltaPx.toInt(), 0) }
                                                    .graphicsLayer {
                                                        scaleY = 0.9f
                                                        alpha = 0.85f
                                                    }
                                            } else Modifier,
                                            onReorderStart = { grabX ->
                                                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                                reorder = ClipReorder(index, grabX, 0f)
                                            },
                                            onReorderDrag = { deltaPx ->
                                                reorder = reorder?.let { it.copy(deltaPx = it.deltaPx + deltaPx) }
                                            },
                                            onReorderEnd = {
                                                reorder?.let { drag ->
                                                    val to = reorderTarget(drag)
                                                    if (to != drag.fromIndex) onMoveClip(drag.fromIndex, to)
                                                }
                                                reorder = null
                                            },
                                            // The thumbnail stays on the original frame while trimming so it doesn't reload every move
                                            clip = clip,
                                            thumbnailFrameMs = clips.getOrNull(index)?.trimStartMs ?: clip.trimStartMs,
                                            widthDp = msToDp(clip.playbackDurationMs),
                                            isSelected = isSelected,
                                            onClick = { onSelectClip(index) },
                                            onTrimDrag = if (isSelected) { isStartEdge, deltaPx ->
                                                val base = trimPreview ?: clips[index]
                                                trimPreview = base.trimmedByDrag(isStartEdge, (deltaPx / pxPerSecond * 1000f).toLong())
                                            } else null,
                                            onTrimEnd = {
                                                trimPreview?.let { preview ->
                                                    val original = clips.getOrNull(index)
                                                    if (original != null && (preview.trimStartMs != original.trimStartMs || preview.trimEndMs != original.trimEndMs)) {
                                                        onTrimSelectedClip(preview.trimStartMs, preview.trimEndMs)
                                                    }
                                                }
                                                trimPreview = null
                                            }
                                        )
                                    }
                            }

                            // Drop marker while reordering: the start of the clip it will go before,
                            // or the end of the clip it will go after
                            reorder?.let { drag ->
                                val to = reorderTarget(drag)
                                if (to != drag.fromIndex) {
                                    val markerMs = if (to < drag.fromIndex) clipBoundariesMs[to] else clipBoundariesMs[to + 1]
                                    Box(
                                        modifier = Modifier
                                            .offset(x = msToDp(markerMs) - 2.dp)
                                            .width(4.dp)
                                            .fillMaxHeight()
                                            .clip(RoundedCornerShape(2.dp))
                                            .background(GoldChampagne)
                                    )
                                }
                            }
                            }

                            // Text & Sticker Track, positioned at each overlay's start/end time
                            if (textOverlays.isNotEmpty()) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(TRACK_HEIGHT_DP.dp)
                                        .background(DarkSurfaceVariant)
                                        .padding(vertical = 2.dp)
                                ) {
                                    textOverlays.forEach { overlay ->
                                        val (spanStart, spanEnd) = chipSpan(overlay.id, overlay.startTimeMs, overlay.endTimeMs)
                                        val startMs = spanStart.coerceIn(0L, totalDurationSafe)
                                        val endMs = spanEnd.coerceIn(startMs, totalDurationSafe)
                                        val isSelected = overlay.id == selectedOverlayId
                                        TimelineTrackChip(
                                            label = if (overlay.isSticker) overlay.text else "T  ${overlay.text}",
                                            color = if (overlay.isSticker) GoldChampagne else Color(0xFFFFDE03),
                                            isSelected = isSelected,
                                            isDragging = chipDrag?.id == overlay.id,
                                            modifier = Modifier
                                                .offset(x = msToDp(startMs))
                                                .width(msToDp(endMs - startMs).coerceAtLeast(MIN_CHIP_WIDTH_DP.dp))
                                                .dragAlongTimeline(
                                                    enabled = isSelected,
                                                    key = overlay.id,
                                                    onDrag = { dragChip(overlay.id, null, overlay.startTimeMs, overlay.endTimeMs, it) },
                                                    onEnd = { endChipDrag(onMoveOverlay) }
                                                ),
                                            // Selected text gets edge handles to change when it appears / disappears
                                            onEdgeDrag = if (isSelected) { edge, deltaPx ->
                                                dragChip(overlay.id, edge, overlay.startTimeMs, overlay.endTimeMs, deltaPx)
                                            } else null,
                                            onEdgeEnd = { endChipDrag(onMoveOverlay) },
                                            onClick = { onSelectOverlay(overlay.id) }
                                        )
                                    }
                                }
                            }

                            // Music, Voiceover & SFX Track, positioned where each one starts playing
                            if (displayAudioTracks.isNotEmpty()) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(TRACK_HEIGHT_DP.dp)
                                        .background(DarkSurfaceVariant)
                                        .padding(vertical = 2.dp)
                                ) {
                                    displayAudioTracks.forEach { track ->
                                        val isSelected = track.id == selectedAudioTrackId
                                        val lengthMs = track.effectiveDurationMs.takeIf { it > 0L && !track.isLooping }
                                        val chipLengthMs = lengthMs ?: (totalDurationSafe - track.timelineStartOffsetMs).coerceAtLeast(MIN_AUDIO_TRIM_MS)
                                        val (spanStart, spanEnd) = chipSpan(track.id, track.timelineStartOffsetMs, track.timelineStartOffsetMs + chipLengthMs)
                                        val startMs = spanStart.coerceIn(0L, totalDurationSafe)
                                        val endMs = spanEnd.coerceIn(startMs, totalDurationSafe)
                                        val chipDurMs = if (track.isLooping) (totalDurationSafe - startMs).coerceAtLeast(1L) else track.effectiveDurationMs.takeIf { it > 0 } ?: (endMs - startMs).coerceAtLeast(1L)
                                        TimelineTrackChip(
                                            label = track.title,
                                            icon = if (track.isVoiceover) Icons.Default.Mic else Icons.Default.MusicNote,
                                            color = if (track.isVoiceover) NeonCoral else HyperCyan,
                                            isSelected = isSelected,
                                            isDragging = chipDrag?.id == track.id,
                                            fadeInMs = track.fadeInMs,
                                            fadeOutMs = track.fadeOutMs,
                                            chipDurationMs = chipDurMs,
                                            modifier = Modifier
                                                .offset(x = msToDp(startMs))
                                                .width(msToDp(endMs - startMs).coerceAtLeast(MIN_CHIP_WIDTH_DP.dp))
                                                .dragAlongTimeline(
                                                    enabled = isSelected,
                                                    key = track.id,
                                                    onDrag = { dragChip(track.id, null, track.timelineStartOffsetMs, track.timelineStartOffsetMs + chipLengthMs, it) },
                                                    onEnd = { endChipDrag(onMoveAudioTrack) }
                                                ),
                                            onEdgeDrag = if (isSelected && !track.isLooping) { edge, deltaPx ->
                                                val base = audioTrimPreview ?: track
                                                audioTrimPreview = base.trimmedByDrag(edge == ChipEdge.START, (deltaPx / pxPerSecond * 1000f).toLong())
                                            } else null,
                                            onEdgeEnd = {
                                                audioTrimPreview?.let { preview ->
                                                    val original = audioTracks.find { it.id == preview.id }
                                                    if (original != null && (preview.timelineStartOffsetMs != original.timelineStartOffsetMs ||
                                                            preview.trimStartMs != original.trimStartMs || preview.trimEndMs != original.trimEndMs)) {
                                                        onTrimAudioTrack(preview.id, preview.timelineStartOffsetMs, preview.trimStartMs, preview.trimEndMs)
                                                    }
                                                }
                                                audioTrimPreview = null
                                            },
                                            onClick = { onSelectAudioTrack(track.id) }
                                        )
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
    thumbnailFrameMs: Long,
    widthDp: androidx.compose.ui.unit.Dp,
    isSelected: Boolean,
    onClick: () -> Unit,
    onTrimDrag: ((isStartEdge: Boolean, deltaPx: Float) -> Unit)? = null,
    onTrimEnd: () -> Unit = {},
    onReorderStart: (grabX: Float) -> Unit = {},
    onReorderDrag: (deltaPx: Float) -> Unit = {},
    onReorderEnd: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val latestOnReorderStart by rememberUpdatedState(onReorderStart)
    val latestOnReorderDrag by rememberUpdatedState(onReorderDrag)
    val latestOnReorderEnd by rememberUpdatedState(onReorderEnd)

    Box(
        modifier = modifier
            .width(widthDp)
            .padding(horizontal = 1.dp)
            .fillMaxHeight()
            .clip(RoundedCornerShape(8.dp))
            .background(DarkCard)
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                brush = if (isSelected) GoldRimGradient else Brush.linearGradient(listOf(Color.White.copy(alpha = 0.15f), Color.White.copy(alpha = 0.05f))),
                shape = RoundedCornerShape(8.dp)
            )
            .pointerInput(Unit) {
                detectDragGesturesAfterLongPress(
                    onDragStart = { latestOnReorderStart(it.x) },
                    onDragEnd = { latestOnReorderEnd() },
                    onDragCancel = { latestOnReorderEnd() }
                ) { change, dragAmount ->
                    change.consume()
                    latestOnReorderDrag(dragAmount.x)
                }
            }
            .clickable(onClick = onClick)
    ) {
        // Real Video Thumbnail Frame via Coil VideoFrameDecoder
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(clip.uri)
                .decoderFactory(VideoFrameDecoder.Factory())
                .videoFrameMillis(thumbnailFrameMs)
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
                    if (clip.isPhoto) {
                        Icon(
                            Icons.Default.Image,
                            contentDescription = "Photo",
                            tint = Color.White.copy(alpha = 0.85f),
                            modifier = Modifier.size(11.dp)
                        )
                    }
                    if (clip.isMuted && !clip.isPhoto) {
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

        // Gold trim handles: drag either edge of the selected clip to trim it
        if (isSelected && onTrimDrag != null) {
            TrimHandle(isStartEdge = true, onTrimDrag = onTrimDrag, onTrimEnd = onTrimEnd, modifier = Modifier.align(Alignment.CenterStart))
            TrimHandle(isStartEdge = false, onTrimDrag = onTrimDrag, onTrimEnd = onTrimEnd, modifier = Modifier.align(Alignment.CenterEnd))
        }
    }
}

@Composable
private fun TrimHandle(
    isStartEdge: Boolean,
    onTrimDrag: (isStartEdge: Boolean, deltaPx: Float) -> Unit,
    onTrimEnd: () -> Unit,
    modifier: Modifier = Modifier
) {
    val latestOnDrag by rememberUpdatedState(onTrimDrag)
    val latestOnEnd by rememberUpdatedState(onTrimEnd)
    Box(
        modifier = modifier
            .fillMaxHeight()
            .width(14.dp)
            .background(GoldChampagne)
            .pointerInput(isStartEdge) {
                detectHorizontalDragGestures(
                    onDragEnd = { latestOnEnd() },
                    onDragCancel = { latestOnEnd() }
                ) { change, dragAmount ->
                    change.consume()
                    latestOnDrag(isStartEdge, dragAmount)
                }
            },
        contentAlignment = Alignment.Center
    ) {
        // Grip line so the edge reads as something to grab
        Box(
            modifier = Modifier
                .width(2.dp)
                .height(18.dp)
                .clip(RoundedCornerShape(1.dp))
                .background(Color.Black.copy(alpha = 0.6f))
        )
    }
}

/** Lets a selected chip be dragged in time; unselected chips leave the drag to the timeline scroll. */
@Composable
private fun Modifier.dragAlongTimeline(
    enabled: Boolean,
    key: String,
    onDrag: (deltaPx: Float) -> Unit,
    onEnd: () -> Unit
): Modifier {
    val latestOnDrag by rememberUpdatedState(onDrag)
    val latestOnEnd by rememberUpdatedState(onEnd)
    return if (!enabled) this else this.pointerInput(key) {
        detectHorizontalDragGestures(
            onDragEnd = { latestOnEnd() },
            onDragCancel = { latestOnEnd() }
        ) { change, dragAmount ->
            change.consume()
            latestOnDrag(dragAmount)
        }
    }
}

@Composable
private fun TimelineTrackChip(
    label: String,
    color: Color,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    isDragging: Boolean = false,
    icon: ImageVector? = null,
    fadeInMs: Long = 0L,
    fadeOutMs: Long = 0L,
    chipDurationMs: Long = 0L,
    onEdgeDrag: ((ChipEdge, deltaPx: Float) -> Unit)? = null,
    onEdgeEnd: () -> Unit = {},
    onClick: (() -> Unit)? = null
) {
    Box(
        modifier = modifier
            .fillMaxHeight()
            .padding(horizontal = 1.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(color.copy(alpha = if (isDragging) 0.7f else if (isSelected) 0.45f else 0.22f))
            .border(if (isSelected) 1.5.dp else 0.8.dp, if (isDragging) Color.White else color, RoundedCornerShape(4.dp))
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
    ) {
        if ((fadeInMs > 0L || fadeOutMs > 0L) && chipDurationMs > 0L) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val chipW = size.width
                val chipH = size.height
                val clampedIn = fadeInMs.coerceIn(0L, chipDurationMs / 2).toFloat()
                val clampedOut = fadeOutMs.coerceIn(0L, chipDurationMs / 2).toFloat()

                val inW = (clampedIn / chipDurationMs * chipW).coerceIn(0f, chipW / 2f)
                if (inW > 1f) {
                    val inPath = Path().apply {
                        moveTo(0f, 0f)
                        lineTo(inW, 0f)
                        lineTo(0f, chipH)
                        close()
                    }
                    drawPath(inPath, color = Color.Black.copy(alpha = 0.5f))
                    drawLine(
                        color = Color.White.copy(alpha = 0.6f),
                        start = Offset(0f, chipH),
                        end = Offset(inW, 0f),
                        strokeWidth = 1.5.dp.toPx()
                    )
                }

                val outW = (clampedOut / chipDurationMs * chipW).coerceIn(0f, chipW / 2f)
                if (outW > 1f) {
                    val outPath = Path().apply {
                        moveTo(chipW - outW, 0f)
                        lineTo(chipW, 0f)
                        lineTo(chipW, chipH)
                        close()
                    }
                    drawPath(outPath, color = Color.Black.copy(alpha = 0.5f))
                    drawLine(
                        color = Color.White.copy(alpha = 0.6f),
                        start = Offset(chipW - outW, 0f),
                        end = Offset(chipW, chipH),
                        strokeWidth = 1.5.dp.toPx()
                    )
                }
            }
        }
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = if (onEdgeDrag != null) 12.dp else 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            if (icon != null) {
                Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(11.dp))
            }
            Text(
                text = label,
                color = TextPrimary,
                fontSize = 9.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Clip
            )
        }
        if (onEdgeDrag != null) {
            ChipEdgeHandle(ChipEdge.START, color, onEdgeDrag, onEdgeEnd, Modifier.align(Alignment.CenterStart))
            ChipEdgeHandle(ChipEdge.END, color, onEdgeDrag, onEdgeEnd, Modifier.align(Alignment.CenterEnd))
        }
    }
}

@Composable
private fun ChipEdgeHandle(
    edge: ChipEdge,
    color: Color,
    onEdgeDrag: (ChipEdge, deltaPx: Float) -> Unit,
    onEdgeEnd: () -> Unit,
    modifier: Modifier = Modifier
) {
    val latestOnDrag by rememberUpdatedState(onEdgeDrag)
    val latestOnEnd by rememberUpdatedState(onEdgeEnd)
    Box(
        modifier = modifier
            .fillMaxHeight()
            .width(10.dp)
            .background(color)
            .pointerInput(edge) {
                detectHorizontalDragGestures(
                    onDragEnd = { latestOnEnd() },
                    onDragCancel = { latestOnEnd() }
                ) { change, dragAmount ->
                    change.consume()
                    latestOnDrag(edge, dragAmount)
                }
            }
    )
}
