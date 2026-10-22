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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.decode.VideoFrameDecoder
import coil.request.ImageRequest
import coil.request.videoFrameMillis
import com.kevan.videorabykevan.model.AspectRatioPreset
import com.kevan.videorabykevan.model.Project
import com.kevan.videorabykevan.ui.components.ExportProgressBanner
import com.kevan.videorabykevan.ui.components.OpenSourceSheet
import com.kevan.videorabykevan.ui.theme.DarkBackground
import com.kevan.videorabykevan.ui.theme.DarkCard
import com.kevan.videorabykevan.ui.theme.DarkElevated
import com.kevan.videorabykevan.ui.theme.DarkStudioCardGradient
import com.kevan.videorabykevan.ui.theme.DarkSurface
import com.kevan.videorabykevan.ui.theme.DarkSurfaceVariant
import com.kevan.videorabykevan.ui.theme.EmeraldAccent
import com.kevan.videorabykevan.ui.theme.GoldChampagne
import com.kevan.videorabykevan.ui.theme.GoldMetallic
import com.kevan.videorabykevan.ui.theme.GoldRimGradient
import com.kevan.videorabykevan.ui.theme.HyperCyan
import com.kevan.videorabykevan.ui.theme.LuxuryAmethystGradient
import com.kevan.videorabykevan.ui.theme.LuxuryGoldGradient
import com.kevan.videorabykevan.ui.theme.NeonCoral
import com.kevan.videorabykevan.ui.theme.RoyalAmethyst
import com.kevan.videorabykevan.ui.theme.TextPrimary
import com.kevan.videorabykevan.ui.theme.TextSecondary
import com.kevan.videorabykevan.ui.theme.TextTertiary
import com.kevan.videorabykevan.ui.viewmodel.HomeViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onOpenProject: (String) -> Unit
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val exportState by viewModel.exportState.collectAsState()
    var showOpenSourceInfo by remember { mutableStateOf(false) }
    var projectPendingDelete by remember { mutableStateOf<Project?>(null) }
    var projectBeingRenamed by remember { mutableStateOf<Project?>(null) }

    Scaffold(
        containerColor = DarkBackground,
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Brand Monogram Badge
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(DarkSurfaceVariant)
                                .border(1.5.dp, GoldRimGradient, RoundedCornerShape(10.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "V",
                                color = GoldChampagne,
                                fontWeight = FontWeight.Black,
                                fontSize = 20.sp,
                                fontFamily = FontFamily.SansSerif
                            )
                        }

                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "VIDEORA",
                                    color = TextPrimary,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 17.sp,
                                    letterSpacing = 1.2.sp
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(Brush.horizontalGradient(listOf(GoldChampagne, GoldMetallic)))
                                        .padding(horizontal = 5.dp, vertical = 1.dp)
                                ) {
                                    Text(
                                        text = "PRO STUDIO",
                                        color = Color.Black,
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Black
                                    )
                                }
                            }
                            Text(
                                text = "Shorts • TikTok • Reels Engine",
                                color = TextSecondary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                },
                actions = {
                    // Privacy & Offline Signal Pill
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(DarkSurfaceVariant)
                            .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(16.dp))
                            .clickable { showOpenSourceInfo = true }
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(EmeraldAccent)
                        )
                        Text(
                            text = "100% Local",
                            color = TextPrimary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Icon(
                            Icons.Default.Info,
                            contentDescription = "Info",
                            tint = TextSecondary,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
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
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = GoldChampagne, strokeWidth = 3.dp)
                    Spacer(modifier = Modifier.height(14.dp))
                    Text("Loading Creator Studio...", color = TextSecondary, fontSize = 13.sp)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                if (exportState.isExporting) {
                    item {
                        ExportProgressBanner(exportState = exportState, onCancel = { viewModel.cancelExport() })
                    }
                }

                // Section 1: Privacy Guarantee Ribbon
                item {
                    PrivacyBannerRibbon(onClick = { showOpenSourceInfo = true })
                }

                // Section 2: Hero 9:16 Creation Master Card
                item {
                    HeroCreationCard(
                        onCreateProject = {
                            viewModel.createNewProject(
                                title = "Viral Reel ${uiState.projects.size + 1}",
                                aspectRatio = AspectRatioPreset.TIKTOK_REELS_SHORTS_9_16,
                                onCreated = onOpenProject
                            )
                        }
                    )
                }

                // Section 3: Quick Aspect Ratio Presets
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = "Canvas Aspect Ratios",
                            color = TextSecondary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )
                        AspectRatioPresetRow(
                            onSelect = { preset ->
                                viewModel.createNewProject(
                                    title = "${preset.label} Project",
                                    aspectRatio = preset,
                                    onCreated = onOpenProject
                                )
                            }
                        )
                    }
                }

                // Section 4: Creator Studio Toolkit Highlights
                item {
                    StudioToolkitRow()
                }

                // Section 5: Drafts & Projects Header
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Your Projects",
                                color = TextPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 17.sp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(DarkSurfaceVariant)
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "${uiState.projects.size}",
                                    color = GoldChampagne,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        if (uiState.projects.isNotEmpty()) {
                            Text(
                                text = "Auto-saved locally",
                                color = TextTertiary,
                                fontSize = 11.sp
                            )
                        }
                    }
                }

                // Section 6: Projects List or Empty State
                if (uiState.projects.isEmpty()) {
                    item {
                        EmptyStudioPlaceholder(
                            onCreateNew = {
                                viewModel.createNewProject(onCreated = onOpenProject)
                            },
                            onLoadDemo = {
                                viewModel.createDemoProject(context = context, onCreated = onOpenProject)
                            }
                        )
                    }
                } else {
                    items(uiState.projects, key = { it.id }) { project ->
                        ProjectItemCard(
                            project = project,
                            onClick = { onOpenProject(project.id) },
                            onRename = { projectBeingRenamed = project },
                            onDuplicate = { viewModel.duplicateProject(project.id) },
                            onDelete = { projectPendingDelete = project }
                        )
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(28.dp))
                }
            }
        }

        if (showOpenSourceInfo) {
            OpenSourceSheet(onDismiss = { showOpenSourceInfo = false })
        }

        // Deleting removes the project's clips and recordings too, so always confirm
        projectPendingDelete?.let { project ->
            AlertDialog(
                onDismissRequest = { projectPendingDelete = null },
                containerColor = DarkCard,
                title = { Text("Delete \"${project.title}\"?", color = TextPrimary, fontWeight = FontWeight.Bold) },
                text = {
                    Text(
                        "This removes the project and its imported clips and voiceovers from the app. " +
                            "Videos you already exported to your gallery are kept.",
                        color = TextSecondary
                    )
                },
                confirmButton = {
                    TextButton(onClick = {
                        viewModel.deleteProject(project.id)
                        projectPendingDelete = null
                    }) {
                        Text("Delete", color = Color(0xFFFF5252), fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { projectPendingDelete = null }) {
                        Text("Cancel", color = TextSecondary)
                    }
                }
            )
        }

        projectBeingRenamed?.let { project ->
            var title by remember(project.id) { mutableStateOf(project.title) }
            AlertDialog(
                onDismissRequest = { projectBeingRenamed = null },
                containerColor = DarkCard,
                title = { Text("Rename project", color = TextPrimary, fontWeight = FontWeight.Bold) },
                text = {
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedBorderColor = GoldChampagne,
                            cursorColor = GoldChampagne
                        )
                    )
                },
                confirmButton = {
                    TextButton(
                        enabled = title.isNotBlank(),
                        onClick = {
                            viewModel.renameProject(project.id, title)
                            projectBeingRenamed = null
                        }
                    ) {
                        Text("Save", color = GoldChampagne, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { projectBeingRenamed = null }) {
                        Text("Cancel", color = TextSecondary)
                    }
                }
            )
        }
    }
}

@Composable
private fun PrivacyBannerRibbon(onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(DarkSurfaceVariant)
            .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(Icons.Default.Shield, contentDescription = null, tint = EmeraldAccent, modifier = Modifier.size(16.dp))
            Text(
                text = "Zero Cloud Uploads • No Accounts • Offline Engine",
                color = TextSecondary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium
            )
        }

        Icon(
            Icons.AutoMirrored.Filled.ArrowForward,
            contentDescription = "Details",
            tint = TextTertiary,
            modifier = Modifier.size(14.dp)
        )
    }
}

@Composable
private fun HeroCreationCard(onCreateProject: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(DarkStudioCardGradient)
            .border(1.5.dp, GoldRimGradient, RoundedCornerShape(22.dp))
            .clickable(onClick = onCreateProject)
            .padding(22.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(Brush.horizontalGradient(listOf(GoldChampagne, GoldMetallic)))
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = "9:16 VERTICAL",
                                color = Color.Black,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Black
                            )
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color.White.copy(alpha = 0.1f))
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = "TIKTOK • REELS • SHORTS",
                                color = TextPrimary,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "New Short / Reel",
                        color = TextPrimary,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Black
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "Hardware-accelerated editing with multi-track timeline, safe zones & audio studio.",
                        color = TextSecondary,
                        fontSize = 12.sp,
                        lineHeight = 16.sp
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(LuxuryGoldGradient),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = "New Project",
                        tint = Color.Black,
                        modifier = Modifier.size(30.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Pro Tags
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                StudioPillTag("🎙️ Voiceover")
                StudioPillTag("🔊 SFX Board")
                StudioPillTag("⚡ Speed Ramp")
                StudioPillTag("↶ Undo/Redo")
            }
        }
    }
}

@Composable
private fun StudioPillTag(label: String) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(DarkElevated)
            .border(0.8.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            text = label,
            color = TextSecondary,
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun AspectRatioPresetRow(onSelect: (AspectRatioPreset) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        AspectRatioCard(
            preset = AspectRatioPreset.TIKTOK_REELS_SHORTS_9_16,
            ratioText = "9:16",
            platformText = "Reels / TikTok",
            isFeatured = true,
            modifier = Modifier.weight(1f),
            onClick = { onSelect(AspectRatioPreset.TIKTOK_REELS_SHORTS_9_16) }
        )
        AspectRatioCard(
            preset = AspectRatioPreset.SQUARE_1_1,
            ratioText = "1:1",
            platformText = "Square Feed",
            isFeatured = false,
            modifier = Modifier.weight(1f),
            onClick = { onSelect(AspectRatioPreset.SQUARE_1_1) }
        )
        AspectRatioCard(
            preset = AspectRatioPreset.LANDSCAPE_16_9,
            ratioText = "16:9",
            platformText = "YouTube Cinema",
            isFeatured = false,
            modifier = Modifier.weight(1f),
            onClick = { onSelect(AspectRatioPreset.LANDSCAPE_16_9) }
        )
    }
}

@Composable
private fun AspectRatioCard(
    preset: AspectRatioPreset,
    ratioText: String,
    platformText: String,
    isFeatured: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(DarkCard)
            .border(
                1.dp,
                if (isFeatured) GoldChampagne.copy(alpha = 0.4f) else Color.White.copy(alpha = 0.08f),
                RoundedCornerShape(14.dp)
            )
            .clickable(onClick = onClick)
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = ratioText,
            color = if (isFeatured) GoldChampagne else TextPrimary,
            fontSize = 15.sp,
            fontWeight = FontWeight.Black
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = platformText,
            color = TextTertiary,
            fontSize = 10.sp,
            maxLines = 1
        )
    }
}

@Composable
private fun StudioToolkitRow() {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            ToolkitItem(
                icon = Icons.Default.Mic,
                title = "Voiceover Studio",
                subtitle = "Mic commentary sync",
                tint = NeonCoral
            )
        }
        item {
            ToolkitItem(
                icon = Icons.Default.GraphicEq,
                title = "SFX Soundboard",
                subtitle = "6 offline synthesized audio",
                tint = HyperCyan
            )
        }
        item {
            ToolkitItem(
                icon = Icons.Default.Speed,
                title = "Speed Ramping",
                subtitle = "0.25x Slo-Mo to 3x Hyper",
                tint = GoldChampagne
            )
        }
        item {
            ToolkitItem(
                icon = Icons.Default.TextFields,
                title = "Viral Text Hooks",
                subtitle = "Retention templates",
                tint = RoyalAmethyst
            )
        }
    }
}

@Composable
private fun ToolkitItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    tint: Color
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .background(DarkSurfaceVariant)
            .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(14.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(RoundedCornerShape(8.dp))
            .background(tint.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(18.dp))
        }

        Column {
            Text(title, color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Text(subtitle, color = TextTertiary, fontSize = 10.sp)
        }
    }
}

@Composable
private fun ProjectItemCard(
    project: Project,
    onClick: () -> Unit,
    onRename: () -> Unit,
    onDuplicate: () -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(DarkCard)
            .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            // Thumbnail of the first clip, with the aspect ratio and clip count on top
            val firstClip = project.clips.firstOrNull()
            val context = LocalContext.current
            Box(
                modifier = Modifier
                    .size(width = 46.dp, height = 58.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(DarkSurfaceVariant)
                    .border(1.dp, GoldRimGradient, RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                if (firstClip != null) {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(firstClip.uri)
                            .decoderFactory(VideoFrameDecoder.Factory())
                            .videoFrameMillis(firstClip.trimStartMs)
                            .crossfade(true)
                            .build(),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.35f)))
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = project.aspectRatio.label,
                        color = GoldChampagne,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black
                    )
                    Text(
                        text = "${project.clipCount}c",
                        color = TextSecondary,
                        fontSize = 9.sp
                    )
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column {
                Text(
                    text = project.title,
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Duration Monospace Pill
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(DarkElevated)
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = project.formattedDuration,
                            color = GoldChampagne,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp
                        )
                    }

                    if (project.audioTracks.isNotEmpty()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.MusicNote, contentDescription = null, tint = HyperCyan, modifier = Modifier.size(12.dp))
                            Text(" Audio", color = HyperCyan, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }

                    if (project.textOverlays.isNotEmpty()) {
                        Text("• ${project.textOverlays.size} text", color = TextTertiary, fontSize = 10.sp)
                    }
                }
            }
        }

        // Action Buttons
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            IconButton(
                onClick = onRename,
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(DarkSurfaceVariant)
            ) {
                Icon(
                    Icons.Default.Edit,
                    contentDescription = "Rename",
                    tint = TextSecondary,
                    modifier = Modifier.size(16.dp)
                )
            }

            IconButton(
                onClick = onDuplicate,
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(DarkSurfaceVariant)
            ) {
                Icon(
                    Icons.Default.ContentCopy,
                    contentDescription = "Duplicate",
                    tint = TextSecondary,
                    modifier = Modifier.size(16.dp)
                )
            }

            IconButton(
                onClick = onDelete,
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(DarkSurfaceVariant)
            ) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Delete",
                    tint = Color(0xFFFF5252),
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
private fun EmptyStudioPlaceholder(
    onCreateNew: () -> Unit,
    onLoadDemo: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(DarkCard)
            .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(20.dp))
            .padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(LuxuryGoldGradient),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.PlayArrow,
                contentDescription = null,
                tint = Color.Black,
                modifier = Modifier.size(36.dp)
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        Text(
            text = "Your Studio Canvas Is Ready",
            color = TextPrimary,
            fontWeight = FontWeight.Black,
            fontSize = 17.sp
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = "Import clips from your phone gallery or start instantly with a sample viral demo reel.",
            color = TextSecondary,
            fontSize = 12.sp,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            lineHeight = 16.sp
        )

        Spacer(modifier = Modifier.height(18.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Button(
                onClick = onCreateNew,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = GoldChampagne),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = "+ Blank Project",
                    color = Color.Black,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
            }

            Button(
                onClick = onLoadDemo,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = DarkSurfaceVariant),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = GoldChampagne, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Demo Reel",
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}
