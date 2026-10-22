package com.kevan.videorabykevan.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kevan.videorabykevan.data.BuiltInAudioHelper
import com.kevan.videorabykevan.data.SoundEffectItem
import com.kevan.videorabykevan.model.AudioTrack
import com.kevan.videorabykevan.ui.theme.AccentCyan
import com.kevan.videorabykevan.ui.theme.AccentPink
import com.kevan.videorabykevan.ui.theme.DarkCard
import com.kevan.videorabykevan.ui.theme.DarkSurface
import com.kevan.videorabykevan.ui.theme.DarkSurfaceVariant

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AudioSheet(
    activeAudioTrack: AudioTrack?,
    allClipsMuted: Boolean,
    onDismiss: () -> Unit,
    onPickAudioFile: () -> Unit,
    onOpenVoiceover: () -> Unit,
    onAddSoundEffect: (SoundEffectItem) -> Unit,
    onUpdateAudioVolume: (Float) -> Unit,
    onToggleMuteOriginalAudio: () -> Unit,
    onRemoveAudioTrack: () -> Unit
) {
    var volume by remember { mutableFloatStateOf(activeAudioTrack?.volume ?: 0.8f) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = DarkSurface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            Text(
                text = "Audio & Soundtrack",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = "Add voiceover, sound effects, or background music",
                color = Color.White.copy(alpha = 0.6f),
                fontSize = 12.sp,
                modifier = Modifier.padding(top = 2.dp, bottom = 14.dp)
            )

            // Voiceover Studio Button & Pick Audio Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = {
                        onOpenVoiceover()
                        onDismiss()
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = AccentPink),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Mic, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Voiceover", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }

                Button(
                    onClick = onPickAudioFile,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = AccentCyan),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.MusicNote, contentDescription = null, tint = Color.Black, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Device Music", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Built-in Sound Effects Soundboard
            Text(
                text = "Viral Sound Effects (100% Offline)",
                color = Color.White.copy(alpha = 0.85f),
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp
            )
            Spacer(modifier = Modifier.height(6.dp))

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(BuiltInAudioHelper.availableEffects) { sfx ->
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(DarkCard)
                            .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                            .clickable {
                                onAddSoundEffect(sfx)
                                onDismiss()
                            }
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(sfx.iconEmoji, fontSize = 18.sp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Column {
                                Text(sfx.name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                Text("${sfx.category} • ${sfx.durationDesc}", color = AccentCyan, fontSize = 9.sp)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Original Audio Mute Toggle
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(DarkCard)
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        if (allClipsMuted) Icons.AutoMirrored.Filled.VolumeOff else Icons.AutoMirrored.Filled.VolumeUp,
                        contentDescription = null,
                        tint = if (allClipsMuted) Color(0xFFFF453A) else AccentCyan,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text("Original Video Sound", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        Text(
                            if (allClipsMuted) "Muted (great for voiceovers / music)" else "Active (ambient sound on)",
                            color = Color.White.copy(alpha = 0.6f),
                            fontSize = 11.sp
                        )
                    }
                }

                Switch(
                    checked = !allClipsMuted,
                    onCheckedChange = { onToggleMuteOriginalAudio() },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = AccentCyan,
                        checkedTrackColor = AccentCyan.copy(alpha = 0.3f)
                    )
                )
            }

            // Background Music Card if loaded
            if (activeAudioTrack != null) {
                Spacer(modifier = Modifier.height(14.dp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(DarkCard)
                        .padding(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .background(AccentCyan.copy(alpha = 0.2f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.GraphicEq, contentDescription = null, tint = AccentCyan, modifier = Modifier.size(18.dp))
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(activeAudioTrack.title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                Text("Attached to Timeline", color = Color.White.copy(alpha = 0.6f), fontSize = 10.sp)
                            }
                        }

                        IconButton(onClick = onRemoveAudioTrack) {
                            Icon(Icons.Default.Delete, contentDescription = "Remove", tint = Color(0xFFFF453A), modifier = Modifier.size(20.dp))
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Track Volume", color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)
                        Text("${(volume * 100).toInt()}%", color = AccentCyan, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }

                    Slider(
                        value = volume,
                        onValueChange = {
                            volume = it
                            onUpdateAudioVolume(it)
                        },
                        valueRange = 0f..1.5f,
                        colors = SliderDefaults.colors(thumbColor = AccentCyan, activeTrackColor = AccentCyan)
                    )
                }
            }

            Spacer(modifier = Modifier.height(28.dp))
        }
    }
}
