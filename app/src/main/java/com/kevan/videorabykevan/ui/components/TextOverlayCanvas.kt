package com.kevan.videorabykevan.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.kevan.videorabykevan.engine.OverlayMetrics
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
    onEditOverlay: () -> Unit,
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
                    pxPerUnit = OverlayMetrics.pxPerUnit(canvasWidthPx, canvasHeightPx),
                    canvasWidthPx = canvasWidthPx,
                    canvasHeightPx = canvasHeightPx,
                    isSelected = overlay.id == selectedOverlayId,
                    onSelect = { onSelectOverlay(overlay.id) },
                    onEdit = onEditOverlay,
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
    pxPerUnit: Float,
    canvasWidthPx: Float,
    canvasHeightPx: Float,
    isSelected: Boolean,
    onSelect: () -> Unit,
    onEdit: () -> Unit,
    onPositionChanged: (Float, Float) -> Unit
) {
    // posX / posY are the overlay's center, so presets like "Center Title" are truly centered
    var centerX by remember(overlay.posX, canvasWidthPx) { mutableFloatStateOf(overlay.posX * canvasWidthPx) }
    var centerY by remember(overlay.posY, canvasHeightPx) { mutableFloatStateOf(overlay.posY * canvasHeightPx) }

    val textColor = Color(overlay.textColorHex)
    val bgColor = Color(overlay.backgroundColorHex)

    // Sized relative to the canvas (not device dp/sp) so the preview matches the exported video
    val density = LocalDensity.current
    val shape = RoundedCornerShape(with(density) { (OverlayMetrics.cornerRadius(overlay.style) * pxPerUnit).toDp() })
    val padH = with(density) { (OverlayMetrics.PADDING_HORIZONTAL * pxPerUnit).toDp() }
    val padV = with(density) { (OverlayMetrics.PADDING_VERTICAL * pxPerUnit).toDp() }
    val fontSize = with(density) { (overlay.fontSizeSp * pxPerUnit).toSp() }

    Box(
        modifier = Modifier
            .layout { measurable, constraints ->
                val placeable = measurable.measure(constraints)
                layout(placeable.width, placeable.height) {
                    placeable.place(
                        (centerX - placeable.width / 2f).roundToInt(),
                        (centerY - placeable.height / 2f).roundToInt()
                    )
                }
            }
            .pointerInput(overlay.id) {
                detectDragGestures(
                    onDragStart = { onSelect() },
                    onDragEnd = {
                        onPositionChanged(centerX / canvasWidthPx, centerY / canvasHeightPx)
                    }
                ) { change, dragAmount ->
                    change.consume()
                    centerX = (centerX + dragAmount.x).coerceIn(canvasWidthPx * 0.02f, canvasWidthPx * 0.98f)
                    centerY = (centerY + dragAmount.y).coerceIn(canvasHeightPx * 0.02f, canvasHeightPx * 0.98f)
                }
            }
            // First tap selects, a second tap on the selected overlay opens the editor
            .clickable { if (isSelected) onEdit() else onSelect() }
            .then(
                if (isSelected) {
                    Modifier.border(2.dp, AccentCyan, shape)
                } else {
                    Modifier
                }
            )
            .background(bgColor, shape)
            .padding(horizontal = padH, vertical = padV)
    ) {
        Text(
            text = overlay.text,
            color = textColor,
            fontSize = fontSize,
            fontWeight = FontWeight(OverlayMetrics.fontWeight(overlay.style)),
            fontFamily = when (overlay.style) {
                TextStylePreset.MEME_IMPACT -> FontFamily.SansSerif
                else -> FontFamily.Default
            },
            textAlign = TextAlign.Center
        )
    }
}
