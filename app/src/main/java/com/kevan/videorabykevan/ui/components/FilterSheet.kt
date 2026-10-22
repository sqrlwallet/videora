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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
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
import com.kevan.videorabykevan.model.VideoFilter
import com.kevan.videorabykevan.ui.theme.AccentCyan
import com.kevan.videorabykevan.ui.theme.AccentPink
import com.kevan.videorabykevan.ui.theme.DarkCard
import com.kevan.videorabykevan.ui.theme.DarkSurface

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilterSheet(
    currentFilter: VideoFilter,
    onDismiss: () -> Unit,
    onSelectFilter: (VideoFilter) -> Unit
) {
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
                text = "Color Grade & Filters",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = "Hardware-accelerated color matrices optimized for TikTok & Reels",
                color = Color.White.copy(alpha = 0.6f),
                fontSize = 12.sp,
                modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
            )

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(VideoFilter.entries.toTypedArray()) { filter ->
                    val isSelected = filter == currentFilter
                    FilterCard(
                        filter = filter,
                        isSelected = isSelected,
                        onClick = {
                            onSelectFilter(filter)
                            onDismiss()
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(30.dp))
        }
    }
}

@Composable
private fun FilterCard(
    filter: VideoFilter,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(4.dp)
    ) {
        // Representative color swatch box
        val swatchColor = when (filter) {
            VideoFilter.NONE -> Color(0xFF607D8B)
            VideoFilter.VIBRANT -> Color(0xFFFF4081)
            VideoFilter.WARM_SUNSET -> Color(0xFFFF9800)
            VideoFilter.COOL_CINEMATIC -> Color(0xFF00BCD4)
            VideoFilter.VINTAGE_FILM -> Color(0xFF795548)
            VideoFilter.CYBERPUNK -> Color(0xFF9C27B0)
            VideoFilter.MONOCHROME -> Color(0xFF424242)
        }

        Box(
            modifier = Modifier
                .size(70.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(swatchColor)
                .border(
                    width = if (isSelected) 2.5.dp else 1.dp,
                    color = if (isSelected) AccentCyan else Color.White.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(12.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            if (isSelected) {
                Box(
                    modifier = Modifier
                        .size(26.dp)
                        .background(Color.Black.copy(alpha = 0.6f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Check,
                        contentDescription = "Selected",
                        tint = AccentCyan,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = filter.displayName,
            color = if (isSelected) AccentCyan else Color.White,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            fontSize = 11.sp
        )
    }
}
