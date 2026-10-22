package com.kevan.videorabykevan.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kevan.videorabykevan.model.VideoClip
import com.kevan.videorabykevan.ui.theme.AccentCyan
import com.kevan.videorabykevan.ui.theme.DarkCard
import com.kevan.videorabykevan.ui.theme.DarkSurface
import com.kevan.videorabykevan.ui.theme.GoldChampagne
import kotlin.math.abs
import kotlin.math.roundToInt

private enum class AdjustTab(val label: String) { LIGHT("Light"), COLOR("Color") }

/**
 * Color correction for the selected clip: exposure, brightness, contrast, white balance
 * (temperature / tint), saturation and hue. Changes show live in the preview (no dimming scrim,
 * so the video stays visible), each slider gesture is one undo step, and the export applies the
 * exact same correction.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdjustSheet(
    clip: VideoClip,
    clipCount: Int,
    onDismiss: () -> Unit,
    onAdjust: ((VideoClip) -> VideoClip) -> Unit,
    onCommit: () -> Unit,
    onReset: () -> Unit,
    onApplyToAll: () -> Unit,
    onCompare: (Boolean) -> Unit
) {
    var tab by remember { mutableStateOf(AdjustTab.LIGHT) }

    ModalBottomSheet(
        onDismissRequest = {
            onCompare(false)
            onDismiss()
        },
        containerColor = DarkSurface,
        scrimColor = Color.Transparent
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 380.dp)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 4.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "🎨 Color Correction",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black,
                    modifier = Modifier.weight(1f)
                )
                SheetChip(
                    label = "Reset",
                    enabled = clip.hasColorAdjustments,
                    onClick = onReset
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Tabs keep the sheet short so the video above stays visible
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AdjustTab.entries.forEach { entry ->
                    val selected = entry == tab
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (selected) GoldChampagne else DarkCard)
                            .clickable { tab = entry }
                            .padding(horizontal = 16.dp, vertical = 7.dp)
                    ) {
                        Text(
                            entry.label,
                            color = if (selected) Color.Black else Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            when (tab) {
                AdjustTab.LIGHT -> {
                    AdjustSlider("Exposure", clip.exposure, onCommit) { v -> onAdjust { it.copy(exposure = v) } }
                    AdjustSlider("Brightness", clip.brightness, onCommit) { v -> onAdjust { it.copy(brightness = v) } }
                    AdjustSlider("Contrast", clip.contrast, onCommit) { v -> onAdjust { it.copy(contrast = v) } }
                }
                AdjustTab.COLOR -> {
                    AdjustSlider("Temperature", clip.temperature, onCommit, ends = "Cool" to "Warm") { v ->
                        onAdjust { it.copy(temperature = v) }
                    }
                    AdjustSlider("Tint", clip.tint, onCommit, ends = "Green" to "Magenta") { v ->
                        onAdjust { it.copy(tint = v) }
                    }
                    AdjustSlider("Saturation", clip.saturation, onCommit) { v -> onAdjust { it.copy(saturation = v) } }
                    AdjustSlider("Hue", clip.hue, onCommit) { v -> onAdjust { it.copy(hue = v) } }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                // Press and hold to see the clip before correction
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .border(1.dp, Color.White.copy(alpha = 0.25f), RoundedCornerShape(12.dp))
                        .pointerInput(Unit) {
                            detectTapGestures(onPress = {
                                onCompare(true)
                                tryAwaitRelease()
                                onCompare(false)
                            })
                        }
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Hold to compare", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }

                if (clipCount > 1) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(AccentCyan)
                            .clickable(onClick = onApplyToAll)
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Apply to all clips", color = Color.Black, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@Composable
private fun SheetChip(label: String, enabled: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Text(label, color = if (enabled) AccentCyan else Color.White.copy(alpha = 0.3f), fontSize = 13.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun AdjustSlider(
    label: String,
    value: Float,
    onCommit: () -> Unit,
    ends: Pair<String, String>? = null,
    onValueChange: (Float) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
        val percent = (value * 100).roundToInt()
        // Tap the value to reset just this slider
        Text(
            text = if (percent > 0) "+$percent" else "$percent",
            color = if (percent == 0) Color.White.copy(alpha = 0.5f) else GoldChampagne,
            fontSize = 13.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .clickable(enabled = percent != 0) {
                    onValueChange(0f)
                    onCommit()
                }
                .padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
    Slider(
        value = value,
        // Snap to 0 near the middle so "no change" is easy to hit
        onValueChange = { onValueChange(if (abs(it) < 0.03f) 0f else it) },
        onValueChangeFinished = onCommit,
        valueRange = -1f..1f,
        colors = SliderDefaults.colors(thumbColor = GoldChampagne, activeTrackColor = GoldChampagne),
        modifier = Modifier.height(28.dp)
    )
    if (ends != null) {
        Row(modifier = Modifier.fillMaxWidth()) {
            Text(ends.first, color = Color.White.copy(alpha = 0.45f), fontSize = 10.sp)
            Spacer(modifier = Modifier.weight(1f).width(0.dp))
            Text(ends.second, color = Color.White.copy(alpha = 0.45f), fontSize = 10.sp)
        }
    }
}
