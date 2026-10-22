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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kevan.videorabykevan.model.VideoClip
import com.kevan.videorabykevan.ui.theme.AccentCyan
import com.kevan.videorabykevan.ui.theme.DarkCard
import com.kevan.videorabykevan.ui.theme.DarkSurface

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpeedSheet(
    clip: VideoClip,
    onDismiss: () -> Unit,
    onSelectSpeed: (Float) -> Unit
) {
    val speedOptions = listOf(
        0.5f to "0.5x Slow-mo",
        0.75f to "0.75x Relaxed",
        1.0f to "1.0x Normal",
        1.25f to "1.25x Dynamic",
        1.5f to "1.5x Brisk",
        2.0f to "2.0x Timelapse"
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = DarkSurface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            Text(
                text = "Clip Playback Speed",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(16.dp))

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                speedOptions.forEach { (factor, label) ->
                    val isSelected = clip.speed == factor
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isSelected) AccentCyan.copy(alpha = 0.15f) else DarkCard)
                            .border(
                                width = if (isSelected) 1.5.dp else 1.dp,
                                color = if (isSelected) AccentCyan else Color.White.copy(alpha = 0.1f),
                                shape = RoundedCornerShape(12.dp)
                            )
                            .clickable {
                                onSelectSpeed(factor)
                                onDismiss()
                            }
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = label,
                            color = if (isSelected) AccentCyan else Color.White,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 15.sp
                        )

                        if (isSelected) {
                            Text(
                                text = "ACTIVE",
                                color = AccentCyan,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
