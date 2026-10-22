package com.kevan.videorabykevan.ui.components

import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.view.LayoutInflater
import android.view.TextureView
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CropFree
import androidx.compose.material.icons.filled.FitScreen
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.kevan.videorabykevan.R
import kotlin.math.max
import kotlin.math.min
import com.kevan.videorabykevan.engine.ClipColorMatrix
import com.kevan.videorabykevan.model.AspectRatioPreset
import com.kevan.videorabykevan.model.SafeZoneType
import com.kevan.videorabykevan.model.VideoClip
import com.kevan.videorabykevan.model.TextOverlay
import com.kevan.videorabykevan.ui.theme.AccentCyan
import com.kevan.videorabykevan.ui.theme.AccentPink
import com.kevan.videorabykevan.ui.theme.DarkBackground
import com.kevan.videorabykevan.ui.theme.DarkSurfaceVariant
import com.kevan.videorabykevan.ui.theme.GoldChampagne
import com.kevan.videorabykevan.ui.theme.GoldRimGradient

@OptIn(UnstableApi::class)
@Composable
fun VideoPlayerView(
    exoPlayer: ExoPlayer,
    aspectRatioPreset: AspectRatioPreset,
    safeZoneType: SafeZoneType,
    textOverlays: List<TextOverlay>,
    activeClip: VideoClip?,
    showOriginalColors: Boolean = false,
    currentPositionMs: Long,
    isPlaying: Boolean,
    isBuffering: Boolean,
    hasClips: Boolean,
    selectedOverlayId: String?,
    isFitMode: Boolean,
    onToggleFitMode: () -> Unit,
    onTogglePlayPause: () -> Unit,
    onSelectOverlay: (String?) -> Unit,
    onEditOverlay: () -> Unit,
    onUpdateOverlayPosition: (String, Float, Float) -> Unit,
    onAddClipClicked: () -> Unit,
    onGenerateDemoClipClicked: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Toggles between TikTok/Reels full-bleed crop (Fill) and letterboxed view (Fit); saved on the project and used by export
    val isFillMode = !isFitMode

    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(DarkBackground),
        contentAlignment = Alignment.Center
    ) {
        // Enforce aspect ratio viewport (9:16 portrait standard for TikTok / Shorts / Reels)
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .aspectRatio(aspectRatioPreset.aspectRatio, matchHeightConstraintsFirst = true)
                .clip(RoundedCornerShape(16.dp))
                .border(1.5.dp, GoldRimGradient, RoundedCornerShape(16.dp))
                .background(Color.Black)
        ) {
            if (hasClips) {
                // Media3 PlayerView inflated from XML with TextureView surface
                // TextureView prevents black screen inside Compose, allows rounded clipping, and lets overlays display properly!
                AndroidView(
                    factory = { ctx ->
                        val view = LayoutInflater.from(ctx).inflate(R.layout.media3_player_view, null) as PlayerView
                        view.player = exoPlayer
                        view.resizeMode = if (isFillMode) AspectRatioFrameLayout.RESIZE_MODE_ZOOM else AspectRatioFrameLayout.RESIZE_MODE_FIT
                        view.setOnClickListener { onTogglePlayPause() }
                        // Re-apply the clip look whenever the video frame is re-laid out (new clip size, Fit/Fill)
                        view.addOnLayoutChangeListener { v, _, _, _, _, _, _, _, _ ->
                            (v.getTag(R.id.media3_player_view) as? PreviewLook)?.let { applyPreviewLook(v as PlayerView, it) }
                        }
                        view
                    },
                    update = { view ->
                        if (view.player != exoPlayer) {
                            view.player = exoPlayer
                        }
                        val targetResizeMode = if (isFillMode) AspectRatioFrameLayout.RESIZE_MODE_ZOOM else AspectRatioFrameLayout.RESIZE_MODE_FIT
                        if (view.resizeMode != targetResizeMode) {
                            view.resizeMode = targetResizeMode
                        }
                        val look = PreviewLook(activeClip, isFillMode, showOriginalColors)
                        view.setTag(R.id.media3_player_view, look)
                        applyPreviewLook(view, look)
                    },
                    modifier = Modifier.fillMaxSize()
                )

                // Interactive Text Overlays Layer
                TextOverlayCanvas(
                    textOverlays = textOverlays,
                    currentPositionMs = currentPositionMs,
                    selectedOverlayId = selectedOverlayId,
                    onSelectOverlay = onSelectOverlay,
                    onEditOverlay = onEditOverlay,
                    onUpdatePosition = onUpdateOverlayPosition,
                    modifier = Modifier.fillMaxSize()
                )

                // Safe Zone Overlay Layer (TikTok, Reels, Shorts UI guides)
                SafeZoneOverlay(
                    safeZoneType = safeZoneType,
                    modifier = Modifier.fillMaxSize()
                )

                // Fit / Fill Aspect Mode Pill (Top-Right)
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(10.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.Black.copy(alpha = 0.65f))
                        .border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                        .clickable { onToggleFitMode() }
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            if (isFillMode) Icons.Default.CropFree else Icons.Default.FitScreen,
                            contentDescription = "Aspect mode",
                            tint = GoldChampagne,
                            modifier = Modifier.size(12.dp)
                        )
                        Text(
                            text = if (isFillMode) "Fill" else "Fit",
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Buffering indicator
                if (isBuffering) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            color = GoldChampagne,
                            modifier = Modifier.size(44.dp)
                        )
                    }
                }

                // Center Floating Play Indicator (only when paused)
                AnimatedVisibility(
                    visible = !isPlaying && !isBuffering,
                    enter = fadeIn(),
                    exit = fadeOut(),
                    modifier = Modifier.align(Alignment.Center)
                ) {
                    Box(
                        modifier = Modifier
                            .size(62.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.65f))
                            .border(2.dp, GoldRimGradient, CircleShape)
                            .clickable { onTogglePlayPause() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.PlayArrow,
                            contentDescription = "Play",
                            tint = GoldChampagne,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                }
            } else {
                // Empty state when no clips imported yet
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .background(DarkSurfaceVariant, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.VideoLibrary,
                            contentDescription = null,
                            tint = AccentCyan,
                            modifier = Modifier.size(32.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "9:16 Canvas Ready",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )

                    Text(
                        text = "Import video clips to begin crafting your viral short",
                        color = Color.White.copy(alpha = 0.6f),
                        fontSize = 12.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        modifier = Modifier.padding(top = 4.dp, bottom = 18.dp)
                    )

                    Button(
                        onClick = onAddClipClicked,
                        colors = ButtonDefaults.buttonColors(containerColor = GoldChampagne),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, tint = Color.Black, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Add Video Clips", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedButton(
                        onClick = onGenerateDemoClipClicked,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = AccentPink),
                        border = androidx.compose.foundation.BorderStroke(1.dp, AccentPink.copy(alpha = 0.6f))
                    ) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = AccentPink, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Generate Demo Clip", fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

/** What the preview needs to mimic the export for the clip under the playhead. */
private data class PreviewLook(val clip: VideoClip?, val isFillMode: Boolean, val showOriginalColors: Boolean)

/**
 * Shows the active clip's filter, color adjustments, mirror and rotation in the live preview using
 * cheap view-level transforms on the TextureView, so the user sees what the export will produce.
 */
@OptIn(UnstableApi::class)
private fun applyPreviewLook(playerView: PlayerView, look: PreviewLook) {
    val textureView = playerView.videoSurfaceView as? TextureView ?: return
    val clip = look.clip

    // "Compare" shows the clip without filter and color correction
    val colorMatrix = clip?.let { ClipColorMatrix.colorMatrixFor(it, includeColor = !look.showOriginalColors) }
    textureView.setLayerPaint(colorMatrix?.let { Paint().apply { colorFilter = ColorMatrixColorFilter(it) } })

    val degrees = ((clip?.rotationDegrees ?: 0) % 360 + 360) % 360
    val w = textureView.width.toFloat()
    val h = textureView.height.toFloat()
    val cw = playerView.width.toFloat()
    val ch = playerView.height.toFloat()
    // A quarter turn swaps the frame's width and height; rescale so it still fits / fills the viewport
    val scale = if ((degrees == 90 || degrees == 270) && w > 0f && h > 0f && cw > 0f && ch > 0f) {
        if (look.isFillMode) max(cw / h, ch / w) else min(cw / h, ch / w)
    } else {
        1f
    }
    textureView.rotation = degrees.toFloat()
    textureView.scaleX = scale * if (clip?.isFlippedHorizontal == true) -1f else 1f
    textureView.scaleY = scale
}
