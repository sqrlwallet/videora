package com.kevan.videorabykevan.ui.components

import android.content.Context
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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.kevan.videorabykevan.engine.ExportProgressState
import com.kevan.videorabykevan.engine.VideoExportManager
import com.kevan.videorabykevan.model.ExportPreset
import com.kevan.videorabykevan.ui.theme.DarkCard
import com.kevan.videorabykevan.ui.theme.DarkSurface
import com.kevan.videorabykevan.ui.theme.DarkSurfaceVariant
import com.kevan.videorabykevan.ui.theme.EmeraldAccent
import com.kevan.videorabykevan.ui.theme.GoldChampagne
import com.kevan.videorabykevan.ui.theme.GoldRimGradient
import com.kevan.videorabykevan.ui.theme.LuxuryGoldGradient

@Composable
fun ExportDialog(
    exportState: ExportProgressState,
    onStartExport: (ExportPreset) -> Unit,
    onCancelExport: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var selectedPreset by remember { mutableStateOf(ExportPreset.TIKTOK_OPTIMIZED) }

    Dialog(
        onDismissRequest = {
            if (!exportState.isExporting) onDismiss()
        },
        properties = DialogProperties(
            dismissOnBackPress = !exportState.isExporting,
            dismissOnClickOutside = !exportState.isExporting
        )
    ) {
        Surface(
            shape = RoundedCornerShape(22.dp),
            color = DarkSurface,
            modifier = Modifier
                .fillMaxWidth()
                .border(1.5.dp, GoldRimGradient, RoundedCornerShape(22.dp))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (exportState.isSuccess) "Export Success! 🎉" else "Export Video",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )

                    if (!exportState.isExporting) {
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                when {
                    // Export is in progress
                    exportState.isExporting -> {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp)
                        ) {
                            LinearProgressIndicator(
                                progress = { exportState.progress },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .clip(RoundedCornerShape(4.dp)),
                                color = GoldChampagne,
                                trackColor = DarkSurfaceVariant
                            )

                            Spacer(modifier = Modifier.height(14.dp))

                            Text(
                                text = exportState.statusText,
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold
                            )

                            Text(
                                text = "Encoding on this phone. You can leave the app; it keeps going in the background.",
                                color = Color.White.copy(alpha = 0.6f),
                                fontSize = 11.sp,
                                modifier = Modifier.padding(top = 4.dp)
                            )

                            Spacer(modifier = Modifier.height(20.dp))

                            OutlinedButton(
                                onClick = onCancelExport,
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Cancel", color = Color.White)
                            }
                        }
                    }

                    // Export finished successfully
                    exportState.isSuccess -> {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 10.dp)
                        ) {
                            Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = Color(0xFF34C759),
                                modifier = Modifier.size(54.dp)
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            Text(
                                text = "Saved to Gallery / Movies/Videora",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )

                            Text(
                                text = "Ready to publish to TikTok, Shorts, or Reels!",
                                color = Color.White.copy(alpha = 0.7f),
                                fontSize = 12.sp,
                                modifier = Modifier.padding(top = 2.dp, bottom = 18.dp)
                            )

                            // Share to Social Apps
                            Button(
                                onClick = {
                                    val shareIntent = VideoExportManager.createShareIntent(
                                        context,
                                        exportState.exportedFile,
                                        exportState.mediaStoreUri
                                    )
                                    context.startActivity(android.content.Intent.createChooser(shareIntent, "Share to TikTok / Reels / Shorts"))
                                },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(containerColor = GoldChampagne),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.Share, contentDescription = null, tint = Color.Black, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Share to TikTok / Reels / Shorts", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            OutlinedButton(
                                onClick = onDismiss,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Done", color = Color.White)
                            }
                        }
                    }

                    // Export error
                    exportState.error != null -> {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 10.dp)
                        ) {
                            Icon(
                                Icons.Default.ErrorOutline,
                                contentDescription = null,
                                tint = Color(0xFFFF453A),
                                modifier = Modifier.size(48.dp)
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = "Export Error",
                                color = Color(0xFFFF453A),
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )

                            Text(
                                text = exportState.error,
                                color = Color.White.copy(alpha = 0.8f),
                                fontSize = 12.sp,
                                modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
                            )

                            Button(
                                onClick = { onStartExport(selectedPreset) },
                                colors = ButtonDefaults.buttonColors(containerColor = GoldChampagne),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Retry Export", color = Color.Black, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    // Preset selection mode
                    else -> {
                        Text(
                            text = "Choose target platform preset for optimal quality:",
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 12.sp
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            ExportPreset.entries.forEach { preset ->
                                val isSelected = preset == selectedPreset
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(if (isSelected) GoldChampagne.copy(alpha = 0.15f) else DarkCard)
                                        .border(
                                            width = if (isSelected) 1.5.dp else 1.dp,
                                            color = if (isSelected) GoldChampagne else Color.White.copy(alpha = 0.1f),
                                            shape = RoundedCornerShape(12.dp)
                                        )
                                        .clickable { selectedPreset = preset }
                                        .padding(horizontal = 14.dp, vertical = 10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = preset.title,
                                            color = if (isSelected) GoldChampagne else Color.White,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = preset.description,
                                            color = Color.White.copy(alpha = 0.6f),
                                            fontSize = 11.sp
                                        )
                                    }

                                    Box(
                                        modifier = Modifier
                                            .background(
                                                if (isSelected) GoldChampagne else DarkSurfaceVariant,
                                                RoundedCornerShape(6.dp)
                                            )
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = preset.badge,
                                            color = if (isSelected) Color.Black else Color.White,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(18.dp))

                        Button(
                            onClick = { onStartExport(selectedPreset) },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = GoldChampagne),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Videocam, contentDescription = null, tint = Color.Black, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Export Now (100% Local)", color = Color.Black, fontWeight = FontWeight.Black, fontSize = 14.sp)
                        }
                    }
                }
            }
        }
    }
}
