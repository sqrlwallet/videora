package com.kevan.videorabykevan.ui.components

import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
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
import androidx.media3.ui.PlayerView
import com.kevan.videorabykevan.model.AspectRatioPreset
import com.kevan.videorabykevan.model.SafeZoneType
import com.kevan.videorabykevan.model.TextOverlay
import com.kevan.videorabykevan.ui.theme.AccentCyan
import com.kevan.videorabykevan.ui.theme.AccentPink
import com.kevan.videorabykevan.ui.theme.DarkBackground
import com.kevan.videorabykevan.ui.theme.DarkSurfaceVariant

@OptIn(UnstableApi::class)
@Composable
fun VideoPlayerView(
    exoPlayer: ExoPlayer,
    aspectRatioPreset: AspectRatioPreset,
    safeZoneType: SafeZoneType,
    textOverlays: List<TextOverlay>,
    currentPositionMs: Long,
    isPlaying: Boolean,
    isBuffering: Boolean,
    hasClips: Boolean,
    selectedOverlayId: String?,
    onTogglePlayPause: () -> Unit,
    onSelectOverlay: (String?) -> Unit,
    onUpdateOverlayPosition: (String, Float, Float) -> Unit,
    onAddClipClicked: () -> Unit,
    onGenerateDemoClipClicked: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(DarkBackground),
        contentAlignment = Alignment.Center
    ) {
        // Enforce aspect ratio viewport (9:16 portrait standard for TikTok / Shorts / Reels)
        Box(
            modifier = Modifier
                .aspectRatio(aspectRatioPreset.aspectRatio)
                .clip(RoundedCornerShape(16.dp))
                .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(16.dp))
                .background(Color.Black)
        ) {
            if (hasClips) {
                // ExoPlayer surface
                AndroidView(
                    factory = { ctx ->
                        PlayerView(ctx).apply {
                            useController = false
                            player = exoPlayer
                            layoutParams = FrameLayout.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT
                            )
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )

                // Interactive Text Overlays Layer
                TextOverlayCanvas(
                    textOverlays = textOverlays,
                    currentPositionMs = currentPositionMs,
                    selectedOverlayId = selectedOverlayId,
                    onSelectOverlay = onSelectOverlay,
                    onUpdatePosition = onUpdateOverlayPosition,
                    modifier = Modifier.fillMaxSize()
                )

                // Safe Zone Overlay Layer (TikTok, Reels, Shorts UI guides)
                SafeZoneOverlay(
                    safeZoneType = safeZoneType,
                    modifier = Modifier.fillMaxSize()
                )

                // Buffering indicator
                if (isBuffering) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            color = AccentCyan,
                            modifier = Modifier.size(44.dp)
                        )
                    }
                }

                // Tap canvas to play/pause with animated indicator
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            onTogglePlayPause()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    AnimatedVisibility(
                        visible = !isPlaying && !isBuffering,
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                                .border(1.5.dp, Color.White.copy(alpha = 0.8f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.PlayArrow,
                                contentDescription = "Play",
                                tint = Color.White,
                                modifier = Modifier.size(32.dp)
                            )
                        }
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
                        colors = ButtonDefaults.buttonColors(containerColor = AccentCyan),
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
