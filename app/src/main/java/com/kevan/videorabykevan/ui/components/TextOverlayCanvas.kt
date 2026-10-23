package com.kevan.videorabykevan.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.kevan.videorabykevan.engine.OverlayMetrics
import com.kevan.videorabykevan.model.TextAnimationFrame
import com.kevan.videorabykevan.model.TextOverlay
import com.kevan.videorabykevan.model.TextStylePreset
import com.kevan.videorabykevan.ui.theme.AccentCyan
import kotlin.math.abs
import kotlin.math.roundToInt

// Same range as the size slider in the text editor
private const val MIN_FONT_SIZE_SP = 12f
private const val MAX_FONT_SIZE_SP = 72f
// Dragged text snaps to the vertical center line within this distance
private const val CENTER_SNAP_DP = 8

@Composable
fun TextOverlayCanvas(
    textOverlays: List<TextOverlay>,
    currentPositionMs: Long,
    selectedOverlayId: String?,
    onSelectOverlay: (String?) -> Unit,
    onEditOverlay: () -> Unit,
    onUpdatePosition: (id: String, newX: Float, newY: Float) -> Unit,
    onResizeOverlay: (id: String, fontSizeSp: Float) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier
) {
    val selectedVisible = textOverlays.any { it.id == selectedOverlayId && currentPositionMs in it.startTimeMs..it.endTimeMs }
    // Live pinch factor for the selected overlay; saved as its font size when the fingers lift
    var pinchScale by remember { mutableFloatStateOf(1f) }
    var showCenterGuide by remember { mutableStateOf(false) }
    val latestSelected by rememberUpdatedState(textOverlays.find { it.id == selectedOverlayId })
    val latestOnResize by rememberUpdatedState(onResizeOverlay)
    val latestOnSelect by rememberUpdatedState(onSelectOverlay)

    // Only while a text/sticker is selected and on screen does the canvas take touches itself:
    // otherwise taps fall through to the video (play/pause).
    val selectionGestures = if (selectedVisible) {
        Modifier
            // Pinch anywhere on the video resizes the selected overlay, so small text is easy to grow
            .pointerInput(Unit) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    var pinched = false
                    do {
                        val event = awaitPointerEvent(PointerEventPass.Initial)
                        if (event.changes.count { it.pressed } >= 2) {
                            pinched = true
                            pinchScale *= event.calculateZoom()
                            // Consuming stops the overlay drag and the tap underneath
                            event.changes.forEach { it.consume() }
                        }
                    } while (event.changes.any { it.pressed })
                    if (pinched) {
                        latestSelected?.let { overlay ->
                            val newSize = (overlay.fontSizeSp * pinchScale).coerceIn(MIN_FONT_SIZE_SP, MAX_FONT_SIZE_SP)
                            latestOnResize(overlay.id, newSize)
                        }
                        pinchScale = 1f
                    }
                }
            }
            // Tapping empty video deselects (taps on an overlay are consumed by the overlay first)
            .pointerInput(Unit) { detectTapGestures { latestOnSelect(null) } }
    } else {
        Modifier
    }

    BoxWithConstraints(modifier = modifier.fillMaxSize().then(selectionGestures)) {
        val canvasWidthPx = constraints.maxWidth.toFloat().coerceAtLeast(1f)
        val canvasHeightPx = constraints.maxHeight.toFloat().coerceAtLeast(1f)

        if (showCenterGuide) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawLine(
                    color = AccentCyan.copy(alpha = 0.8f),
                    start = Offset(size.width / 2f, 0f),
                    end = Offset(size.width / 2f, size.height),
                    strokeWidth = 1.dp.toPx()
                )
            }
        }

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
                    // The selected overlay holds still so it can always be seen and dragged
                    animationFrame = if (overlay.id == selectedOverlayId) TextAnimationFrame.STATIC else overlay.animationFrameAt(currentPositionMs),
                    sizeScale = if (overlay.id == selectedOverlayId) pinchScale else 1f,
                    onSnapChanged = { showCenterGuide = it },
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
    animationFrame: TextAnimationFrame,
    sizeScale: Float,
    onSnapChanged: (Boolean) -> Unit,
    onSelect: () -> Unit,
    onEdit: () -> Unit,
    onPositionChanged: (Float, Float) -> Unit
) {
    // posX / posY are the overlay's center, so presets like "Center Title" are truly centered
    var centerX by remember(overlay.posX, canvasWidthPx) { mutableFloatStateOf(overlay.posX * canvasWidthPx) }
    var centerY by remember(overlay.posY, canvasHeightPx) { mutableFloatStateOf(overlay.posY * canvasHeightPx) }
    // Where the finger has dragged it, before snapping to the center line
    var rawCenterX by remember { mutableFloatStateOf(0f) }
    val haptics = LocalHapticFeedback.current

    val textColor = Color(overlay.textColorHex)
    val bgColor = Color(overlay.backgroundColorHex)

    // Sized relative to the canvas (not device dp/sp) so the preview matches the exported video
    val density = LocalDensity.current
    val shape = RoundedCornerShape(with(density) { (OverlayMetrics.cornerRadius(overlay.style) * pxPerUnit).toDp() })
    val padH = with(density) { (OverlayMetrics.PADDING_HORIZONTAL * pxPerUnit).toDp() }
    val padV = with(density) { (OverlayMetrics.PADDING_VERTICAL * pxPerUnit).toDp() }
    val fontSizeUnits = (overlay.fontSizeSp * sizeScale).coerceIn(MIN_FONT_SIZE_SP, MAX_FONT_SIZE_SP)
    val fontSize = with(density) { (fontSizeUnits * pxPerUnit).toSp() }
    val snapDistancePx = with(density) { CENTER_SNAP_DP.dp.toPx() }
    val visibleText = animationFrame.visibleChars?.let { overlay.text.take(it) } ?: overlay.text

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
            // Same animation values as the export's TextOverlayBitmapOverlay
            .graphicsLayer {
                alpha = if (visibleText.isEmpty()) 0f else animationFrame.alpha
                scaleX = animationFrame.scale
                scaleY = animationFrame.scale
                translationY = animationFrame.offsetY * canvasHeightPx
            }
            // Restarted when a move is saved so the gesture works on the fresh position state
            .pointerInput(overlay.id, overlay.posX, overlay.posY, canvasWidthPx, canvasHeightPx) {
                fun finish() {
                    onSnapChanged(false)
                    onPositionChanged(centerX / canvasWidthPx, centerY / canvasHeightPx)
                }
                detectDragGestures(
                    onDragStart = {
                        rawCenterX = centerX
                        onSelect()
                    },
                    onDragEnd = { finish() },
                    onDragCancel = { finish() }
                ) { change, dragAmount ->
                    change.consume()
                    rawCenterX = (rawCenterX + dragAmount.x).coerceIn(canvasWidthPx * 0.02f, canvasWidthPx * 0.98f)
                    val snapped = abs(rawCenterX - canvasWidthPx / 2f) < snapDistancePx
                    if (snapped && centerX != canvasWidthPx / 2f) {
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    }
                    onSnapChanged(snapped)
                    centerX = if (snapped) canvasWidthPx / 2f else rawCenterX
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
            text = visibleText,
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
