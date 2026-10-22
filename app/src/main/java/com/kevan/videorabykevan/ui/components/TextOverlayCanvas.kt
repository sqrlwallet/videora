package com.kevan.videorabykevan.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kevan.videorabykevan.model.TextOverlay
import com.kevan.videorabykevan.model.TextStylePreset
import com.kevan.videorabykevan.ui.theme.AccentCyan
import kotlin.math.roundToInt

@Composable
fun TextOverlayCanvas(
    textOverlays: List<TextOverlay>,
    currentPositionMs: Long,
    selectedOverlayId: String?,
    onSelectOverlay: (String?) -> Unit,
    onUpdatePosition: (id: String, newX: Float, newY: Float) -> Unit,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val canvasWidthPx = constraints.maxWidth.toFloat().coerceAtLeast(1f)
        val canvasHeightPx = constraints.maxHeight.toFloat().coerceAtLeast(1f)

        // Render each overlay that is active at the current playhead time
        textOverlays.forEach { overlay ->
            val isActive = currentPositionMs in overlay.startTimeMs..overlay.endTimeMs
            if (isActive) {
                DraggableTextItem(
                    overlay = overlay,
                    canvasWidthPx = canvasWidthPx,
                    canvasHeightPx = canvasHeightPx,
                    isSelected = overlay.id == selectedOverlayId,
                    onSelect = { onSelectOverlay(overlay.id) },
                    onPositionChanged = { newX, newY ->
                        onUpdatePosition(overlay.id, newX, newY)
                    }
                )
            }
        }
    }
}

@Composable
private fun DraggableTextItem(
    overlay: TextOverlay,
    canvasWidthPx: Float,
    canvasHeightPx: Float,
    isSelected: Boolean,
    onSelect: () -> Unit,
    onPositionChanged: (Float, Float) -> Unit
) {
    var offsetX by remember(overlay.posX) { mutableFloatStateOf(overlay.posX * canvasWidthPx) }
    var offsetY by remember(overlay.posY) { mutableFloatStateOf(overlay.posY * canvasHeightPx) }

    val textColor = Color(overlay.textColorHex)
    val bgColor = Color(overlay.backgroundColorHex)

    val shape = when (overlay.style) {
        TextStylePreset.VIRAL_HOOK, TextStylePreset.BOLD_WHITE_PILL -> RoundedCornerShape(12.dp)
        TextStylePreset.CLEAN_SUBTITLE -> RoundedCornerShape(6.dp)
        TextStylePreset.MINIMAL_ACCENT -> RoundedCornerShape(8.dp)
        else -> RoundedCornerShape(4.dp)
    }

    Box(
        modifier = Modifier
            .offset { IntOffset(offsetX.roundToInt(), offsetY.roundToInt()) }
            .pointerInput(overlay.id) {
                detectDragGestures(
                    onDragStart = { onSelect() },
                    onDragEnd = {
                        val normX = (offsetX / canvasWidthPx).coerceIn(0.05f, 0.95f)
                        val normY = (offsetY / canvasHeightPx).coerceIn(0.05f, 0.95f)
                        onPositionChanged(normX, normY)
                    }
                ) { change, dragAmount ->
                    change.consume()
                    offsetX = (offsetX + dragAmount.x).coerceIn(0f, canvasWidthPx - 100f)
                    offsetY = (offsetY + dragAmount.y).coerceIn(0f, canvasHeightPx - 50f)
                }
            }
            .clickable { onSelect() }
            .then(
                if (isSelected) {
                    Modifier.border(2.dp, AccentCyan, shape)
                } else {
                    Modifier
                }
            )
            .background(bgColor, shape)
            .padding(horizontal = 14.dp, vertical = 6.dp)
    ) {
        Text(
            text = overlay.text,
            color = textColor,
            fontSize = overlay.fontSizeSp.sp,
            fontWeight = when (overlay.style) {
                TextStylePreset.VIRAL_HOOK, TextStylePreset.MEME_IMPACT -> FontWeight.Black
                TextStylePreset.BOLD_WHITE_PILL -> FontWeight.Bold
                TextStylePreset.MINIMAL_ACCENT -> FontWeight.Medium
                else -> FontWeight.SemiBold
            },
            fontFamily = when (overlay.style) {
                TextStylePreset.MEME_IMPACT -> FontFamily.SansSerif
                else -> FontFamily.Default
            },
            textAlign = TextAlign.Center
        )
    }
}
