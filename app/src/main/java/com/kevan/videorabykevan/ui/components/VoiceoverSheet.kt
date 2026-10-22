package com.kevan.videorabykevan.ui.components

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.kevan.videorabykevan.data.VoiceRecorderHelper
import com.kevan.videorabykevan.model.AudioTrack
import com.kevan.videorabykevan.ui.theme.AccentCyan
import com.kevan.videorabykevan.ui.theme.DarkCard
import com.kevan.videorabykevan.ui.theme.DarkSurface
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoiceoverSheet(
    currentTimelinePositionMs: Long,
    onDismiss: () -> Unit,
    onAddVoiceoverTrack: (AudioTrack) -> Unit
) {
    val context = LocalContext.current
    val voiceRecorder = remember { VoiceRecorderHelper(context) }

    var isRecording by remember { mutableStateOf(false) }
    var recordedFile by remember { mutableStateOf<File?>(null) }
    var recordedDurationMs by remember { mutableLongStateOf(0L) }
    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasPermission = granted
    }

    // Timer updater while recording
    LaunchedEffect(isRecording) {
        while (isRecording && isActive) {
            recordedDurationMs = voiceRecorder.getElapsedDurationMs()
            delay(100)
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(600),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    ModalBottomSheet(
        onDismissRequest = {
            if (isRecording) {
                voiceRecorder.stopRecording()
            }
            onDismiss()
        },
        containerColor = DarkSurface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Voiceover Studio",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = "Record microphone audio synced to timeline at ${formatTime(currentTimelinePositionMs)}",
                color = Color.White.copy(alpha = 0.6f),
                fontSize = 12.sp,
                modifier = Modifier.padding(top = 2.dp, bottom = 20.dp)
            )

            if (!hasPermission) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Microphone permission required for voiceovers",
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Button(
                        onClick = { permissionLauncher.launch(Manifest.permission.RECORD_AUDIO) },
                        colors = ButtonDefaults.buttonColors(containerColor = AccentCyan),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Grant Permission", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            } else {
                // Record Button Container
                Box(
                    modifier = Modifier
                        .size(96.dp)
                        .clip(CircleShape)
                        .background(
                            if (isRecording) Color.Red.copy(alpha = 0.2f) else DarkCard
                        )
                        .then(
                            if (isRecording) Modifier.scale(pulseScale) else Modifier
                        )
                        .clickable {
                            if (isRecording) {
                                recordedFile = voiceRecorder.stopRecording()
                                isRecording = false
                            } else {
                                val file = voiceRecorder.startRecording()
                                if (file != null) {
                                    isRecording = true
                                    recordedFile = null
                                }
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .background(if (isRecording) Color.Red else AccentCyan, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isRecording) Icons.Default.Stop else Icons.Default.Mic,
                            contentDescription = if (isRecording) "Stop" else "Record",
                            tint = Color.Black,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Timer
                Text(
                    text = formatTime(recordedDurationMs),
                    color = if (isRecording) Color.Red else Color.White,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )

                Text(
                    text = if (isRecording) "Recording... Tap button to stop" else if (recordedFile != null) "Recording saved! Ready to apply." else "Tap mic to start recording",
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 12.sp,
                    modifier = Modifier.padding(top = 4.dp, bottom = 20.dp)
                )

                if (recordedFile != null && !isRecording) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                recordedFile?.delete()
                                recordedFile = null
                                recordedDurationMs = 0L
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Re-record", color = Color.White)
                        }

                        Button(
                            onClick = {
                                val file = recordedFile
                                if (file != null) {
                                    val track = AudioTrack(
                                        uri = Uri.fromFile(file).toString(),
                                        title = "Voiceover",
                                        durationMs = recordedDurationMs,
                                        timelineStartOffsetMs = currentTimelinePositionMs,
                                        volume = 1.0f
                                    )
                                    onAddVoiceoverTrack(track)
                                    onDismiss()
                                }
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = AccentCyan),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Attach Voiceover", color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(28.dp))
        }
    }
}

private fun formatTime(ms: Long): String {
    val sec = ms / 1000
    val min = sec / 60
    val secRem = sec % 60
    val tenth = (ms % 1000) / 100
    return String.format(java.util.Locale.US, "%02d:%02d.%d", min, secRem, tenth)
}
