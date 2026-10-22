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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
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
import com.kevan.videorabykevan.model.TextOverlay
import com.kevan.videorabykevan.model.TextStylePreset
import com.kevan.videorabykevan.ui.theme.AccentCyan
import com.kevan.videorabykevan.ui.theme.DarkCard
import com.kevan.videorabykevan.ui.theme.DarkSurface
import com.kevan.videorabykevan.ui.theme.DarkSurfaceVariant
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TextEditorSheet(
    existingOverlay: TextOverlay?,
    totalDurationMs: Long,
    currentPositionMs: Long = 0L,
    onDismiss: () -> Unit,
    onSaveOverlay: (TextOverlay) -> Unit,
    onDeleteOverlay: ((String) -> Unit)? = null
) {
    var textContent by remember { mutableStateOf(existingOverlay?.text ?: "Viral Hook ✨") }
    var selectedPreset by remember { mutableStateOf(existingOverlay?.style ?: TextStylePreset.VIRAL_HOOK) }
    var fontSize by remember { mutableFloatStateOf(existingOverlay?.fontSizeSp ?: selectedPreset.defaultFontSizeSp) }
    var posY by remember { mutableFloatStateOf(existingOverlay?.posY ?: 0.25f) }
    var posX by remember { mutableFloatStateOf(existingOverlay?.posX ?: 0.5f) }

    val maxSec = (totalDurationMs / 1000f).coerceAtLeast(5f)
    var textColorHex by remember { mutableStateOf(existingOverlay?.textColorHex ?: selectedPreset.defaultTextColorHex) }

    // New text starts at the playhead (where the user is looking) and shows for 3 seconds
    val newStartMs = currentPositionMs.coerceIn(0L, (totalDurationMs - 500L).coerceAtLeast(0L))
    val newEndMs = (newStartMs + 3000L).coerceAtMost(totalDurationMs.coerceAtLeast(newStartMs + 500L))
    var startSec by remember { mutableFloatStateOf((existingOverlay?.startTimeMs ?: newStartMs) / 1000f) }
    var endSec by remember { mutableFloatStateOf((existingOverlay?.endTimeMs ?: newEndMs) / 1000f) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = DarkSurface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            Text(
                text = if (existingOverlay != null) "Edit Text Hook" else "Add Viral Text Hook",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Quick Viral Hook Templates
            Text("Viral Hook Ideas", color = Color.White.copy(alpha = 0.7f), fontSize = 11.sp)
            Spacer(modifier = Modifier.height(4.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                items(TextOverlay.VIRAL_HOOK_TEMPLATES) { template ->
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(DarkCard)
                            .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                            .clickable { textContent = template }
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                    ) {
                        Text(template, color = Color.White, fontSize = 11.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Text input
            OutlinedTextField(
                value = textContent,
                onValueChange = { textContent = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Enter hook or caption...") },
                colors = TextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedContainerColor = DarkSurfaceVariant,
                    unfocusedContainerColor = DarkSurfaceVariant,
                    cursorColor = AccentCyan,
                    focusedIndicatorColor = AccentCyan,
                    unfocusedIndicatorColor = Color.Transparent
                ),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Style preset selector
            Text("Style Presets", color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)
            Spacer(modifier = Modifier.height(6.dp))

            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(TextStylePreset.entries.toTypedArray()) { preset ->
                    val isSelected = preset == selectedPreset
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(preset.defaultBackgroundColorHex))
                            .border(
                                width = if (isSelected) 2.dp else 1.dp,
                                color = if (isSelected) AccentCyan else Color.White.copy(alpha = 0.2f),
                                shape = RoundedCornerShape(8.dp)
                            )
                            .clickable {
                                selectedPreset = preset
                                fontSize = preset.defaultFontSizeSp
                                textColorHex = preset.defaultTextColorHex
                            }
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = preset.title,
                            color = Color(preset.defaultTextColorHex),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Text color
            Text("Text Color", color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)
            Spacer(modifier = Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                (listOf(selectedPreset.defaultTextColorHex) + TEXT_COLOR_SWATCHES).distinct().forEach { hex ->
                    val isSelected = hex == textColorHex
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(Color(hex))
                            .border(
                                width = if (isSelected) 3.dp else 1.dp,
                                color = if (isSelected) AccentCyan else Color.White.copy(alpha = 0.3f),
                                shape = CircleShape
                            )
                            .clickable { textColorHex = hex }
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Text size
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Size", color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)
                Text("${fontSize.roundToInt()}", color = AccentCyan, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
            Slider(
                value = fontSize,
                onValueChange = { fontSize = it },
                valueRange = 12f..72f,
                colors = SliderDefaults.colors(thumbColor = AccentCyan, activeTrackColor = AccentCyan)
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Quick Position Placement
            Text("Position Placement", color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                PositionPill(label = "Top Hook", isSelected = posY in 0.15f..0.35f) {
                    posY = 0.22f
                    posX = 0.5f
                }
                PositionPill(label = "Center Title", isSelected = posY in 0.45f..0.55f) {
                    posY = 0.5f
                    posX = 0.5f
                }
                PositionPill(label = "Subtitle", isSelected = posY in 0.65f..0.85f) {
                    posY = 0.72f
                    posX = 0.5f
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Timing Sliders
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Display: ${String.format(java.util.Locale.US, "%.1fs", startSec)} to ${String.format(java.util.Locale.US, "%.1fs", endSec)}",
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 12.sp
                )
                Text(
                    text = "Duration: ${String.format(java.util.Locale.US, "%.1fs", (endSec - startSec).coerceAtLeast(0f))}",
                    color = AccentCyan,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Slider(
                value = startSec,
                onValueChange = { newStart ->
                    startSec = newStart.coerceAtMost(endSec - 0.5f)
                },
                valueRange = 0f..maxSec,
                colors = SliderDefaults.colors(thumbColor = AccentCyan, activeTrackColor = AccentCyan)
            )

            Slider(
                value = endSec,
                onValueChange = { newEnd ->
                    endSec = newEnd.coerceAtLeast(startSec + 0.5f)
                },
                valueRange = 0f..maxSec,
                colors = SliderDefaults.colors(thumbColor = Color.White, activeTrackColor = Color.White)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (existingOverlay != null && onDeleteOverlay != null) {
                    OutlinedButton(
                        onClick = {
                            onDeleteOverlay(existingOverlay.id)
                            onDismiss()
                        },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFFF453A)),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFF453A)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Delete")
                    }
                }

                Button(
                    onClick = {
                        val overlay = (existingOverlay ?: TextOverlay()).copy(
                            text = textContent.trim(),
                            style = selectedPreset,
                            fontSizeSp = fontSize,
                            textColorHex = textColorHex,
                            backgroundColorHex = selectedPreset.defaultBackgroundColorHex,
                            posX = posX,
                            posY = posY,
                            startTimeMs = (startSec * 1000).toLong(),
                            endTimeMs = (endSec * 1000).toLong()
                        )
                        onSaveOverlay(overlay)
                        onDismiss()
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = AccentCyan),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        if (existingOverlay != null) "Update Hook" else "Add Hook",
                        color = Color.Black,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun PositionPill(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (isSelected) AccentCyan.copy(alpha = 0.2f) else DarkCard)
            .border(
                width = if (isSelected) 1.5.dp else 1.dp,
                color = if (isSelected) AccentCyan else Color.White.copy(alpha = 0.15f),
                shape = RoundedCornerShape(8.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 6.dp)
    ) {
        Text(
            text = label,
            color = if (isSelected) AccentCyan else Color.White,
            fontSize = 11.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
        )
    }
}

private val TEXT_COLOR_SWATCHES = listOf(
    0xFFFFFFFF, 0xFF000000, 0xFFFFDE03, 0xFF00F2FE, 0xFFFF2A85, 0xFFFF453A, 0xFF34C759
)
