package com.kevan.videorabykevan.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CallSplit
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kevan.videorabykevan.model.VideoClip
import com.kevan.videorabykevan.model.VideoFilter
import com.kevan.videorabykevan.ui.theme.AccentCyan
import com.kevan.videorabykevan.ui.theme.AccentPink
import com.kevan.videorabykevan.ui.theme.DarkCard
import com.kevan.videorabykevan.ui.theme.DarkSurface
import com.kevan.videorabykevan.ui.theme.DarkSurfaceVariant
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
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(DarkSurface)
            .padding(vertical = 8.dp)
    ) {
        // Timeline Header: Timecode and Transport Controls
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Timecode: Current / Total
            val curSec = currentPositionMs / 1000
            val curMin = curSec / 60
            val curSecRem = curSec % 60
            val curMillis = (currentPositionMs % 1000) / 100

            val totSec = totalDurationMs / 1000
            val totMin = totSec / 60
            val totSecRem = totSec % 60

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = String.format(java.util.Locale.US, "%02d:%02d.%d", curMin, curSecRem, curMillis),
                    color = AccentCyan,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = String.format(java.util.Locale.US, " / %02d:%02d", totMin, totSecRem),
                    color = Color.White.copy(alpha = 0.5f),
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace
                )
            }

            // Transport Buttons
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Step -1s
                IconButton(onClick = onStepBackward, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.FastRewind, contentDescription = "-1s", tint = Color.White, modifier = Modifier.size(18.dp))
                }

                // Play / Pause
                IconButton(
                    onClick = onTogglePlayPause,
                    modifier = Modifier
                        .size(36.dp)
                        .background(AccentCyan, CircleShape)
                ) {
                    Icon(
                        if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = "Play/Pause",
                        tint = Color.Black,
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Step +1s
                IconButton(onClick = onStepForward, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.FastForward, contentDescription = "+1s", tint = Color.White, modifier = Modifier.size(18.dp))
                }

                // Split at playhead
                IconButton(
                    onClick = onSplitCurrentClip,
                    enabled = clips.isNotEmpty(),
                    modifier = Modifier
                        .size(32.dp)
                        .background(DarkSurfaceVariant, CircleShape)
                ) {
                    Icon(Icons.AutoMirrored.Filled.CallSplit, contentDescription = "Split", tint = AccentPink, modifier = Modifier.size(16.dp))
                }

                // Add clip
                IconButton(
                    onClick = onAddClipClicked,
                    modifier = Modifier
                        .size(32.dp)
                        .background(DarkSurfaceVariant, CircleShape)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Clip", tint = Color.White, modifier = Modifier.size(18.dp))
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Visual Multi-clip Timeline Strip
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .height(72.dp)
                .background(DarkSurfaceVariant)
                .padding(vertical = 4.dp)
        ) {
            val totalWidthPx = constraints.maxWidth.toFloat().coerceAtLeast(1f)

            if (clips.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No clips in timeline. Tap (+) to import video clips.",
                        color = Color.White.copy(alpha = 0.5f),
                        fontSize = 12.sp
                    )
                }
            } else {
                // Timeline track
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(totalDurationMs) {
                            detectTapGestures { offset ->
                                val fraction = (offset.x / totalWidthPx).coerceIn(0f, 1f)
                                onSeekTo((fraction * totalDurationMs).toLong())
                            }
                        }
                        .pointerInput(totalDurationMs) {
                            detectHorizontalDragGestures { change, _ ->
                                change.consume()
                                val fraction = (change.position.x / totalWidthPx).coerceIn(0f, 1f)
                                onSeekTo((fraction * totalDurationMs).toLong())
                            }
                        }
                ) {
                    // Clip blocks row
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        clips.forEachIndexed { index, clip ->
                            val clipWeight = max(0.05f, (clip.playbackDurationMs.toFloat() / max(1L, totalDurationMs)))
                            val isSelected = index == selectedClipIndex

                            Box(
                                modifier = Modifier
                                    .weight(clipWeight)
                                    .fillMaxHeight()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) DarkCard else Color(0xFF1E1E24))
                                    .border(
                                        width = if (isSelected) 2.dp else 1.dp,
                                        color = if (isSelected) AccentCyan else Color.White.copy(alpha = 0.15f),
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
                                            color = Color.White,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            maxLines = 1
                                        )

                                        if (clip.speed != 1.0f) {
                                            Text(
                                                text = "${clip.speed}x",
                                                color = AccentPink,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = String.format(java.util.Locale.US, "%.1fs", clip.playbackDurationMs / 1000f),
                                            color = Color.White.copy(alpha = 0.7f),
                                            fontSize = 10.sp
                                        )

                                        if (clip.filter != VideoFilter.NONE) {
                                            Box(
                                                modifier = Modifier
                                                    .background(AccentCyan.copy(alpha = 0.2f), RoundedCornerShape(4.dp))
                                                    .padding(horizontal = 4.dp, vertical = 1.dp)
                                            ) {
                                                Text(
                                                    text = clip.filter.displayName,
                                                    color = AccentCyan,
                                                    fontSize = 8.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Real-time Playhead Cursor (Red vertical line)
                    val progressFraction = if (totalDurationMs > 0) {
                        (currentPositionMs.toFloat() / totalDurationMs).coerceIn(0f, 1f)
                    } else 0f

                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val playheadX = progressFraction * size.width

                        // Playhead line
                        drawLine(
                            color = Color.Red,
                            start = Offset(playheadX, 0f),
                            end = Offset(playheadX, size.height),
                            strokeWidth = 3.dp.toPx()
                        )

                        // Playhead top cap / needle head
                        drawCircle(
                            color = Color.Red,
                            radius = 5.dp.toPx(),
                            center = Offset(playheadX, 5.dp.toPx())
                        )
                    }
                }
            }
        }
    }
}
