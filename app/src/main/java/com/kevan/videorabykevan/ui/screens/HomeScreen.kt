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
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Movie
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
import androidx.compose.ui.graphics.Color
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
import com.kevan.videorabykevan.ui.theme.DarkBackground
import com.kevan.videorabykevan.ui.theme.DarkCard
import com.kevan.videorabykevan.ui.theme.DarkSurfaceVariant
import com.kevan.videorabykevan.ui.theme.GoldChampagne
import com.kevan.videorabykevan.ui.theme.GoldRimGradient
import com.kevan.videorabykevan.ui.theme.TextPrimary
import com.kevan.videorabykevan.ui.theme.TextSecondary
import com.kevan.videorabykevan.ui.theme.TextTertiary
import com.kevan.videorabykevan.ui.viewmodel.HomeViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onOpenProject: (String) -> Unit,
    // Null in the offline edition, which has no AI settings
    onOpenAiSettings: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val exportState by viewModel.exportState.collectAsState()
    var projectPendingDelete by remember { mutableStateOf<Project?>(null) }
    var projectBeingRenamed by remember { mutableStateOf<Project?>(null) }

    Scaffold(
        containerColor = DarkBackground,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Videora",
                        color = TextPrimary,
                        fontWeight = FontWeight.Black,
                        fontSize = 20.sp
                    )
                },
                actions = {
                    if (onOpenAiSettings != null) {
                        IconButton(onClick = onOpenAiSettings) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = "AI settings", tint = GoldChampagne)
                        }
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
                CircularProgressIndicator(color = GoldChampagne, strokeWidth = 3.dp)
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

                // New video: pick its shape
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = "New video",
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp
                        )
                        AspectRatioPresetRow(
                            onSelect = { preset ->
                                viewModel.createNewProject(
                                    title = "${preset.label} ${uiState.projects.size + 1}",
                                    aspectRatio = preset,
                                    onCreated = onOpenProject
                                )
                            }
                        )
                    }
                }

                item {
                    Text(
                        text = "Your projects",
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp
                    )
                }

                if (uiState.projects.isEmpty()) {
                    item {
                        EmptyStudioPlaceholder(
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
private fun AspectRatioPresetRow(onSelect: (AspectRatioPreset) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        AspectRatioCard(
            preset = AspectRatioPreset.TIKTOK_REELS_SHORTS_9_16,
            ratioText = "9:16",
            platformText = "TikTok",
            isFeatured = true,
            modifier = Modifier.weight(1f),
            onClick = { onSelect(AspectRatioPreset.TIKTOK_REELS_SHORTS_9_16) }
        )
        AspectRatioCard(
            preset = AspectRatioPreset.SQUARE_1_1,
            ratioText = "1:1",
            platformText = "Square",
            isFeatured = false,
            modifier = Modifier.weight(1f),
            onClick = { onSelect(AspectRatioPreset.SQUARE_1_1) }
        )
        AspectRatioCard(
            preset = AspectRatioPreset.LANDSCAPE_16_9,
            ratioText = "16:9",
            platformText = "YouTube",
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
        Icon(
            Icons.Default.Add,
            contentDescription = null,
            tint = if (isFeatured) GoldChampagne else TextSecondary,
            modifier = Modifier.size(20.dp)
        )
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
            // Thumbnail of the first clip
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
                } else {
                    Icon(Icons.Default.Movie, contentDescription = null, tint = TextTertiary, modifier = Modifier.size(20.dp))
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

                Text(
                    text = project.formattedDuration,
                    color = TextSecondary,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp
                )
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
private fun EmptyStudioPlaceholder(onLoadDemo: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(DarkCard)
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "No projects yet",
            color = TextSecondary,
            fontSize = 13.sp,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            lineHeight = 18.sp
        )

        Spacer(modifier = Modifier.height(14.dp))

        Button(
            onClick = onLoadDemo,
            colors = ButtonDefaults.buttonColors(containerColor = DarkSurfaceVariant),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(
                text = "Open demo project",
                color = TextPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp
            )
        }
    }
}
