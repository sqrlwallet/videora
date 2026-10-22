package com.kevan.videorabykevan.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RangeSlider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kevan.videorabykevan.model.VideoClip
import com.kevan.videorabykevan.ui.theme.AccentCyan
import com.kevan.videorabykevan.ui.theme.DarkSurface
import kotlin.math.roundToLong

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrimSheet(
    clip: VideoClip,
    onDismiss: () -> Unit,
    onSaveTrim: (startMs: Long, endMs: Long) -> Unit
) {
    val duration = if (clip.durationMs > 0) clip.durationMs.toFloat() else 10_000f
    var sliderPosition by remember {
        mutableStateOf(
            clip.trimStartMs.toFloat()..if (clip.trimEndMs > 0) clip.trimEndMs.toFloat() else duration
        )
    }

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
                text = "Trim Clip: ${clip.name}",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Time markers
            val startMs = sliderPosition.start.roundToLong()
            val endMs = sliderPosition.endInclusive.roundToLong()
            val trimmedMs = (endMs - startMs).coerceAtLeast(0L)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Start Cut", color = Color.White.copy(alpha = 0.6f), fontSize = 12.sp)
                    Text(
                        text = formatMillis(startMs),
                        color = AccentCyan,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Duration", color = Color.White.copy(alpha = 0.6f), fontSize = 12.sp)
                    Text(
                        text = "${String.format("%.1f", trimmedMs / 1000f)}s",
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text("End Cut", color = Color.White.copy(alpha = 0.6f), fontSize = 12.sp)
                    Text(
                        text = formatMillis(endMs),
                        color = AccentCyan,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Range slider
            RangeSlider(
                value = sliderPosition,
                onValueChange = { range ->
                    // Enforce minimum 0.5s clip duration
                    if (range.endInclusive - range.start >= 500f) {
                        sliderPosition = range
                    }
                },
                valueRange = 0f..duration,
                colors = SliderDefaults.colors(
                    thumbColor = AccentCyan,
                    activeTrackColor = AccentCyan,
                    inactiveTrackColor = Color.DarkGray
                )
            )

            Spacer(modifier = Modifier.height(20.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = { sliderPosition = 0f..duration },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Reset Full", color = Color.White)
                }

                Button(
                    onClick = {
                        onSaveTrim(startMs, endMs)
                        onDismiss()
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = AccentCyan),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Apply Trim", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

private fun formatMillis(ms: Long): String {
    val totalSec = ms / 1000
    val min = totalSec / 60
    val sec = totalSec % 60
    val tenth = (ms % 1000) / 100
    return String.format("%02d:%02d.%d", min, sec, tenth)
}
