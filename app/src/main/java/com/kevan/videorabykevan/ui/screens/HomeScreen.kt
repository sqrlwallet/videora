package com.kevan.videorabykevan.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kevan.videorabykevan.model.AspectRatioPreset
import com.kevan.videorabykevan.model.Project
import com.kevan.videorabykevan.ui.components.OpenSourceSheet
import com.kevan.videorabykevan.ui.theme.AccentCyan
import com.kevan.videorabykevan.ui.theme.AccentPink
import com.kevan.videorabykevan.ui.theme.AccentPurple
import com.kevan.videorabykevan.ui.theme.DarkBackground
import com.kevan.videorabykevan.ui.theme.DarkCard
import com.kevan.videorabykevan.ui.theme.DarkSurface
import com.kevan.videorabykevan.ui.theme.DarkSurfaceVariant
import com.kevan.videorabykevan.ui.viewmodel.HomeViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onOpenProject: (String) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    var showOpenSourceInfo by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = DarkBackground,
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    Brush.linearGradient(listOf(AccentCyan, AccentPink))
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.PlayCircle,
                                contentDescription = null,
                                tint = Color.Black,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Videora",
                                color = Color.White,
                                fontWeight = FontWeight.Black,
                                fontSize = 18.sp
                            )
                            Text(
                                text = "Shorts • TikTok • Reels",
                                color = AccentCyan,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                },
                actions = {
                    IconButton(onClick = { showOpenSourceInfo = true }) {
                        Icon(
                            Icons.Default.Info,
                            contentDescription = "Open Source & Privacy",
                            tint = Color.White.copy(alpha = 0.8f)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkBackground)
            )
        }
    ) { innerPadding ->
        if (uiState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = AccentCyan)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    // Privacy & Offline Hero Banner
                    PrivacyPillBanner(onClick = { showOpenSourceInfo = true })
                }

                item {
                    // Create New 9:16 Project Hero Card
                    CreateProjectHeroCard(
                        onCreateProject = {
                            viewModel.createNewProject(
                                title = "Viral Short ${uiState.projects.size + 1}",
                                aspectRatio = AspectRatioPreset.TIKTOK_REELS_SHORTS_9_16,
                                onCreated = onOpenProject
                            )
                        }
                    )
                }

                item {
                    // Platform Quick Cheat Sheet
                    CreatorCheatSheet()
                }

                item {
                    // Section Title
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Drafts & Projects (${uiState.projects.size})",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }
                }

                if (uiState.projects.isEmpty()) {
                    item {
                        EmptyDraftsPlaceholder(
                            onCreateNew = {
                                viewModel.createNewProject(onCreated = onOpenProject)
                            }
                        )
                    }
                } else {
                    items(uiState.projects, key = { it.id }) { project ->
                        ProjectItemCard(
                            project = project,
                            onClick = { onOpenProject(project.id) },
                            onDuplicate = { viewModel.duplicateProject(project.id) },
                            onDelete = { viewModel.deleteProject(project.id) }
                        )
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(30.dp))
                }
            }
        }

        if (showOpenSourceInfo) {
            OpenSourceSheet(onDismiss = { showOpenSourceInfo = false })
        }
    }
}

@Composable
private fun PrivacyPillBanner(onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(DarkSurfaceVariant)
            .border(1.dp, BorderSubtle(), RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Lock, contentDescription = null, tint = AccentCyan, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "100% Local • No Sign-ups • Zero Tracking",
                color = Color.White,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium
            )
        }

        Text(
            text = "Learn more →",
            color = AccentCyan,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun CreateProjectHeroCard(onCreateProject: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(
                Brush.horizontalGradient(
                    listOf(
                        Color(0xFF2E0854),
                        Color(0xFF13132B),
                        Color(0xFF0F1E36)
                    )
                )
            )
            .border(
                1.5.dp,
                Brush.horizontalGradient(listOf(AccentPink, AccentCyan)),
                RoundedCornerShape(20.dp)
            )
            .clickable(onClick = onCreateProject)
            .padding(20.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column {
                    Box(
                        modifier = Modifier
                            .background(AccentPink, RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "9:16 VERTICAL",
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "New Short / Reel",
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black
                    )

                    Text(
                        text = "Optimized for TikTok, Shorts & Instagram Reels",
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 12.sp
                    )
                }

                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .background(AccentCyan, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = "New Project",
                        tint = Color.Black,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FeatureBadge(label = "Safe Zone Guides")
                FeatureBadge(label = "Viral Text Hooks")
                FeatureBadge(label = "Split & Trim")
                FeatureBadge(label = "LUT Filters")
            }
        }
    }
}

@Composable
private fun FeatureBadge(label: String) {
    Box(
        modifier = Modifier
            .background(Color.White.copy(alpha = 0.12f), RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(label, color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun CreatorCheatSheet() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(DarkCard)
            .padding(14.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        PlatformTipItem(
            name = "TikTok",
            duration = "15-60s",
            hint = "Hook in 3s"
        )
        PlatformTipItem(
            name = "Shorts",
            duration = "30-50s",
            hint = "Loop ending"
        )
        PlatformTipItem(
            name = "Reels",
            duration = "15-30s",
            hint = "Audio sync"
        )
    }
}

@Composable
private fun PlatformTipItem(name: String, duration: String, hint: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(name, color = AccentCyan, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(2.dp))
        Text(duration, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold)
        Text(hint, color = Color.White.copy(alpha = 0.5f), fontSize = 10.sp)
    }
}

@Composable
private fun ProjectItemCard(
    project: Project,
    onClick: () -> Unit,
    onDuplicate: () -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(DarkCard)
            .border(1.dp, BorderSubtle(), RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(DarkSurfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Smartphone,
                    contentDescription = null,
                    tint = AccentCyan,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column {
                Text(
                    text = project.title,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )

                Spacer(modifier = Modifier.height(3.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = project.aspectRatio.label,
                        color = AccentPink,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    )
                    Text(" • ", color = Color.Gray, fontSize = 11.sp)
                    Text(
                        text = "${project.clipCount} clips",
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 11.sp
                    )
                    Text(" • ", color = Color.Gray, fontSize = 11.sp)
                    Text(
                        text = project.formattedDuration,
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 11.sp
                    )
                }
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onDuplicate, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Default.ContentCopy, contentDescription = "Duplicate", tint = Color.White.copy(alpha = 0.7f), modifier = Modifier.size(16.dp))
            }
            IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color(0xFFFF453A).copy(alpha = 0.8f), modifier = Modifier.size(16.dp))
            }
        }
    }
}

@Composable
private fun EmptyDraftsPlaceholder(onCreateNew: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(DarkCard)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            Icons.Default.VideoLibrary,
            contentDescription = null,
            tint = AccentCyan,
            modifier = Modifier.size(40.dp)
        )
        Spacer(modifier = Modifier.height(10.dp))
        Text("No drafts yet", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
        Text(
            "Start your first short-form video now. Zero sign-up required.",
            color = Color.White.copy(alpha = 0.6f),
            fontSize = 12.sp,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            modifier = Modifier.padding(top = 4.dp, bottom = 14.dp)
        )
        Button(
            onClick = onCreateNew,
            colors = ButtonDefaults.buttonColors(containerColor = AccentCyan),
            shape = RoundedCornerShape(10.dp)
        ) {
            Text("Create New Short", color = Color.Black, fontWeight = FontWeight.Bold)
        }
    }
}

private fun BorderSubtle(): Color = Color(0xFF2C2C38)
