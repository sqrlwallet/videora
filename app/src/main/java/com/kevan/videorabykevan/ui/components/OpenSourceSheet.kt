package com.kevan.videorabykevan.ui.components

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.NoAccounts
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kevan.videorabykevan.ui.theme.AccentCyan
import com.kevan.videorabykevan.ui.theme.AccentPink
import com.kevan.videorabykevan.ui.theme.DarkCard
import com.kevan.videorabykevan.ui.theme.DarkSurface

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OpenSourceSheet(onDismiss: () -> Unit) {
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
                text = "100% Local & Open Source",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = "Videora by Kevan • Privacy-First Creator Studio",
                color = AccentCyan,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(top = 2.dp, bottom = 16.dp)
            )

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                PillarItem(
                    icon = Icons.Default.NoAccounts,
                    title = "Zero Sign-ups or Accounts",
                    description = "Never prompts for an email, phone number, password, or login. Launch the app and edit immediately."
                )

                PillarItem(
                    icon = Icons.Default.Lock,
                    title = "100% Local & Offline",
                    description = "Your video clips never leave your device. All rendering, trimming, and effects run entirely on your local GPU/NPU."
                )

                PillarItem(
                    icon = Icons.Default.Speed,
                    title = "Short-Form Platform Optimized",
                    description = "Pre-configured 9:16 vertical viewport, TikTok/Shorts/Reels safe-zone overlays, viral hook pill presets, and direct gallery export."
                )

                PillarItem(
                    icon = Icons.Default.Code,
                    title = "Open Source (Apache 2.0)",
                    description = "Built with Jetpack Compose & AndroidX Media3. Completely auditable, free, and community-driven."
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = AccentCyan),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Got It", color = Color.Black, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun PillarItem(
    icon: ImageVector,
    title: String,
    description: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(DarkCard)
            .padding(12.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .background(AccentCyan.copy(alpha = 0.15f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = AccentCyan, modifier = Modifier.size(18.dp))
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column {
            Text(title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            Spacer(modifier = Modifier.height(2.dp))
            Text(description, color = Color.White.copy(alpha = 0.7f), fontSize = 11.sp, lineHeight = 16.sp)
        }
    }
}
