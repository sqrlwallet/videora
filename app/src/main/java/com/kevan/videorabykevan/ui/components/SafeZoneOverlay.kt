package com.kevan.videorabykevan.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.NearMe
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ThumbDown
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kevan.videorabykevan.model.SafeZoneType
import com.kevan.videorabykevan.ui.theme.AccentCyan
import com.kevan.videorabykevan.ui.theme.AccentPink
import com.kevan.videorabykevan.ui.theme.AccentYellow

@Composable
fun SafeZoneOverlay(
    safeZoneType: SafeZoneType,
    modifier: Modifier = Modifier
) {
    if (safeZoneType == SafeZoneType.NONE) return

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val width = maxWidth
        val height = maxHeight

        // Draw dashed safe zone boundary lines
        Canvas(modifier = Modifier.fillMaxSize()) {
            val dashEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f), 0f)

            // Top boundary (avoid top navigation)
            drawLine(
                color = Color.Yellow.copy(alpha = 0.5f),
                start = Offset(0f, size.height * 0.12f),
                end = Offset(size.width, size.height * 0.12f),
                strokeWidth = 2f,
                pathEffect = dashEffect
            )

            // Bottom boundary (avoid captions/sound ticker)
            drawLine(
                color = Color.Yellow.copy(alpha = 0.5f),
                start = Offset(0f, size.height * 0.82f),
                end = Offset(size.width, size.height * 0.82f),
                strokeWidth = 2f,
                pathEffect = dashEffect
            )

            // Right action rail boundary (avoid like/comment buttons)
            drawLine(
                color = Color.Red.copy(alpha = 0.45f),
                start = Offset(size.width * 0.82f, size.height * 0.12f),
                end = Offset(size.width * 0.82f, size.height * 0.82f),
                strokeWidth = 2f,
                pathEffect = dashEffect
            )
        }

        // Overlay Platform specific UI mockups
        when (safeZoneType) {
            SafeZoneType.TIKTOK -> TikTokUiMockup()
            SafeZoneType.INSTAGRAM_REELS -> InstagramReelsUiMockup()
            SafeZoneType.YOUTUBE_SHORTS -> YouTubeShortsUiMockup()
            SafeZoneType.NONE -> {}
        }

        // Safe zone badge
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 8.dp)
                .background(Color.Black.copy(alpha = 0.7f), RoundedCornerShape(12.dp))
                .border(1.dp, AccentCyan.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                .padding(horizontal = 8.dp, vertical = 2.dp)
        ) {
            Text(
                text = "${safeZoneType.badge} Safe Zone Overlay",
                color = AccentCyan,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun TikTokUiMockup() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .alpha(0.65f)
            .padding(12.dp)
    ) {
        // Top Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 28.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("LIVE", color = Color.White.copy(alpha = 0.8f), fontSize = 13.sp, fontWeight = FontWeight.Bold)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Following", color = Color.White.copy(alpha = 0.6f), fontSize = 13.sp)
                Text("For You", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
            Icon(Icons.Default.Search, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
        }

        // Right Rail
        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 60.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Profile with (+)
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Color.Gray)
                    .border(1.5.dp, Color.White, CircleShape)
            )

            // Like
            ActionItem(icon = Icons.Default.Favorite, count = "124K", tint = AccentPink)
            // Comment
            ActionItem(icon = Icons.Default.ChatBubble, count = "1.8K")
            // Bookmark
            ActionItem(icon = Icons.Default.Bookmark, count = "12K")
            // Share
            ActionItem(icon = Icons.Default.Share, count = "4.2K")
            // Rotating sound disc
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(Color.DarkGray)
                    .border(2.dp, Color.White, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.GraphicEq, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
            }
        }

        // Bottom Captions
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth(0.78f)
                .padding(bottom = 16.dp)
        ) {
            Text("@creator_handle", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            Spacer(modifier = Modifier.height(2.dp))
            Text("Wait for the end! 🔥 #fyp #viral #shorts", color = Color.White.copy(alpha = 0.85f), fontSize = 12.sp)
            Spacer(modifier = Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.GraphicEq, contentDescription = null, tint = Color.White, modifier = Modifier.size(12.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Original Sound - Viral Audio", color = Color.White.copy(alpha = 0.7f), fontSize = 11.sp)
            }
        }
    }
}

@Composable
private fun InstagramReelsUiMockup() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .alpha(0.65f)
            .padding(12.dp)
    ) {
        // Top Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 28.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Reels", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Icon(Icons.Default.Search, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
        }

        // Right Rail
        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 50.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            ActionItem(icon = Icons.Default.Favorite, count = "88K")
            ActionItem(icon = Icons.Default.ChatBubble, count = "650")
            ActionItem(icon = Icons.Default.NearMe, count = "Share")
            Icon(Icons.Default.MoreVert, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
        }

        // Bottom Captions
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth(0.78f)
                .padding(bottom = 16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(Color.Gray)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("creator.official", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Spacer(modifier = Modifier.width(6.dp))
                Text("• Follow", color = AccentCyan, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text("Best video editing workflow on Android ✨ #reels", color = Color.White.copy(alpha = 0.9f), fontSize = 12.sp)
        }
    }
}

@Composable
private fun YouTubeShortsUiMockup() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .alpha(0.65f)
            .padding(12.dp)
    ) {
        // Top Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 28.dp),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Search, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(16.dp))
            Icon(Icons.Default.MoreVert, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
        }

        // Right Rail
        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            ActionItem(icon = Icons.Default.ThumbUp, count = "54K")
            ActionItem(icon = Icons.Default.ThumbDown, count = "Dislike")
            ActionItem(icon = Icons.Default.ChatBubble, count = "1.2K")
            ActionItem(icon = Icons.Default.Share, count = "Share")
            ActionItem(icon = Icons.Default.Repeat, count = "Remix")
        }

        // Bottom Bar
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth(0.76f)
                .padding(bottom = 16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(26.dp).clip(CircleShape).background(Color.Red))
                Spacer(modifier = Modifier.width(8.dp))
                Text("@ChannelName", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                Spacer(modifier = Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .background(Color.White, RoundedCornerShape(12.dp))
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text("Subscribe", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text("How to create viral shorts 100% offline!", color = Color.White, fontSize = 12.sp)
        }
    }
}

@Composable
private fun ActionItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    count: String,
    tint: Color = Color.White
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(24.dp))
        Spacer(modifier = Modifier.height(2.dp))
        Text(count, color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
    }
}
