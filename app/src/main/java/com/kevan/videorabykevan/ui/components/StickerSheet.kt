package com.kevan.videorabykevan.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import com.kevan.videorabykevan.model.TextOverlay
import com.kevan.videorabykevan.model.TextStylePreset
import com.kevan.videorabykevan.ui.theme.DarkCard
import com.kevan.videorabykevan.ui.theme.DarkSurface

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StickerSheet(
    totalDurationMs: Long,
    currentPositionMs: Long,
    onDismiss: () -> Unit,
    onAddSticker: (TextOverlay) -> Unit
) {
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
                text = "Reaction Stickers & Emojis",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = "Tap to place a reaction sticker at the current playhead",
                color = Color.White.copy(alpha = 0.6f),
                fontSize = 12.sp,
                modifier = Modifier.padding(top = 2.dp, bottom = 14.dp)
            )

            LazyVerticalGrid(
                columns = GridCells.Fixed(6),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(TextOverlay.TRENDING_STICKERS) { emoji ->
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(DarkCard)
                            .clickable {
                                val startTime = currentPositionMs
                                val endTime = (startTime + 3000L).coerceAtMost(
                                    if (totalDurationMs > 0) totalDurationMs else 5000L
                                )
                                val sticker = TextOverlay(
                                    text = emoji,
                                    style = TextStylePreset.STICKER_EMOJI,
                                    fontSizeSp = 46f,
                                    textColorHex = 0xFFFFFFFF,
                                    backgroundColorHex = 0x00000000,
                                    posX = 0.5f,
                                    posY = 0.45f,
                                    startTimeMs = startTime,
                                    endTimeMs = endTime,
                                    isSticker = true
                                )
                                onAddSticker(sticker)
                                onDismiss()
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = emoji, fontSize = 28.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(28.dp))
        }
    }
}
