package com.kevan.videorabykevan.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import com.kevan.videorabykevan.model.SafeZoneType
import com.kevan.videorabykevan.ui.theme.AccentCyan
import com.kevan.videorabykevan.ui.theme.DarkCard
import com.kevan.videorabykevan.ui.theme.DarkSurface

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SafeZoneSheet(
    currentSafeZone: SafeZoneType,
    onDismiss: () -> Unit,
    onSelectSafeZone: (SafeZoneType) -> Unit
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
                text = "Safe Zone Guides",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = "Preview real platform UI buttons so critical text isn't covered",
                color = Color.White.copy(alpha = 0.6f),
                fontSize = 12.sp,
                modifier = Modifier.padding(top = 2.dp, bottom = 14.dp)
            )

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SafeZoneType.entries.forEach { safeZone ->
                    val isSelected = safeZone == currentSafeZone
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isSelected) AccentCyan.copy(alpha = 0.15f) else DarkCard)
                            .border(
                                width = if (isSelected) 1.5.dp else 1.dp,
                                color = if (isSelected) AccentCyan else Color.White.copy(alpha = 0.1f),
                                shape = RoundedCornerShape(12.dp)
                            )
                            .clickable {
                                onSelectSafeZone(safeZone)
                                onDismiss()
                            }
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = safeZone.title,
                                color = if (isSelected) AccentCyan else Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = safeZone.description,
                                color = Color.White.copy(alpha = 0.6f),
                                fontSize = 11.sp
                            )
                        }

                        if (isSelected) {
                            Text(
                                text = "ACTIVE",
                                color = AccentCyan,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(28.dp))
        }
    }
}
