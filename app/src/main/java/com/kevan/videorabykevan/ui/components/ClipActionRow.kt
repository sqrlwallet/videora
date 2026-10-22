package com.kevan.videorabykevan.ui.components

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.automirrored.filled.RotateRight
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.AddReaction
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FilterAlt
import androidx.compose.material.icons.filled.Flip
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.ViewInAr
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kevan.videorabykevan.ui.theme.AccentCyan
import com.kevan.videorabykevan.ui.theme.AccentPink
import com.kevan.videorabykevan.ui.theme.DarkSurface
import com.kevan.videorabykevan.ui.theme.DarkSurfaceVariant

@Composable
fun ClipActionRow(
    hasSelectedClip: Boolean,
    isClipMuted: Boolean,
    onOpenTrim: () -> Unit,
    onOpenSpeed: () -> Unit,
    onOpenFilters: () -> Unit,
    onOpenText: () -> Unit,
    onOpenStickers: () -> Unit,
    onOpenAudio: () -> Unit,
    onOpenVoiceover: () -> Unit,
    onOpenSafeZones: () -> Unit,
    onMoveClipLeft: () -> Unit,
    onMoveClipRight: () -> Unit,
    onToggleFlip: () -> Unit,
    onRotate90: () -> Unit,
    onToggleMute: () -> Unit,
    onDuplicateClip: () -> Unit,
    onDeleteClip: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(DarkSurface)
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 12.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ActionButton(
            icon = Icons.Default.TextFields,
            label = "Text/Hook",
            accentColor = AccentCyan,
            onClick = onOpenText
        )

        ActionButton(
            icon = Icons.Default.AddReaction,
            label = "Stickers",
            accentColor = Color(0xFFFFDE03),
            onClick = onOpenStickers
        )

        ActionButton(
            icon = Icons.Default.FilterAlt,
            label = "Filters",
            accentColor = AccentPink,
            enabled = hasSelectedClip,
            onClick = onOpenFilters
        )

        ActionButton(
            icon = Icons.Default.ContentCut,
            label = "Trim",
            enabled = hasSelectedClip,
            onClick = onOpenTrim
        )

        ActionButton(
            icon = Icons.Default.Speed,
            label = "Speed",
            enabled = hasSelectedClip,
            onClick = onOpenSpeed
        )

        ActionButton(
            icon = Icons.Default.Mic,
            label = "Voiceover",
            accentColor = AccentPink,
            onClick = onOpenVoiceover
        )

        ActionButton(
            icon = Icons.Default.MusicNote,
            label = "Audio/SFX",
            onClick = onOpenAudio
        )

        ActionButton(
            icon = Icons.AutoMirrored.Filled.ArrowBack,
            label = "Move ◀",
            enabled = hasSelectedClip,
            onClick = onMoveClipLeft
        )

        ActionButton(
            icon = Icons.AutoMirrored.Filled.ArrowForward,
            label = "Move ▶",
            enabled = hasSelectedClip,
            onClick = onMoveClipRight
        )

        ActionButton(
            icon = Icons.Default.Flip,
            label = "Mirror",
            enabled = hasSelectedClip,
            onClick = onToggleFlip
        )

        ActionButton(
            icon = Icons.AutoMirrored.Filled.RotateRight,
            label = "Rotate",
            enabled = hasSelectedClip,
            onClick = onRotate90
        )

        ActionButton(
            icon = Icons.Default.ViewInAr,
            label = "Safe Zone",
            onClick = onOpenSafeZones
        )

        ActionButton(
            icon = if (isClipMuted) Icons.AutoMirrored.Filled.VolumeOff else Icons.AutoMirrored.Filled.VolumeUp,
            label = if (isClipMuted) "Unmute" else "Mute",
            enabled = hasSelectedClip,
            onClick = onToggleMute
        )

        ActionButton(
            icon = Icons.Default.ContentCopy,
            label = "Duplicate",
            enabled = hasSelectedClip,
            onClick = onDuplicateClip
        )

        ActionButton(
            icon = Icons.Default.Delete,
            label = "Delete",
            accentColor = Color(0xFFFF453A),
            enabled = hasSelectedClip,
            onClick = onDeleteClip
        )
    }
}

@Composable
private fun ActionButton(
    icon: ImageVector,
    label: String,
    accentColor: Color = Color.White,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    val alpha = if (enabled) 1f else 0.4f
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 6.dp, vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .background(DarkSurfaceVariant.copy(alpha = alpha), RoundedCornerShape(10.dp)),
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
            fontSize = 10.sp
        )
    }
}
