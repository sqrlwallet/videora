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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kevan.videorabykevan.ai.AiTask
import com.kevan.videorabykevan.ai.ColorSuggestion
import com.kevan.videorabykevan.ai.RewriteStyle
import com.kevan.videorabykevan.model.Project
import com.kevan.videorabykevan.model.TextOverlay
import com.kevan.videorabykevan.model.VideoClip
import com.kevan.videorabykevan.ui.theme.AccentCyan
import com.kevan.videorabykevan.ui.theme.DarkCard
import com.kevan.videorabykevan.ui.theme.DarkSurface
import com.kevan.videorabykevan.ui.theme.GoldChampagne
import com.kevan.videorabykevan.ui.viewmodel.AiTool
import com.kevan.videorabykevan.ui.viewmodel.AiToolsUiState
import java.util.Locale

/** Everything the AI tools sheet can do to the project; each call is one undoable edit. */
class AiToolActions(
    val onAddHook: (String) -> Unit,
    val onAddOverlays: (overlays: List<TextOverlay>, replaceAutoCaptions: Boolean, message: String) -> Unit,
    val onSetTexts: (List<Pair<String, String>>) -> Unit,
    val onSetSelectedText: (String) -> Unit,
    val onApplyColor: (ColorSuggestion, toAllClips: Boolean) -> Unit,
    val onRename: (String) -> Unit
)

/**
 * The AI edition's tools. Each tool says what it will send and to which model before the user
 * taps it, and results are only added when the user picks one.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiToolsSheet(
    state: AiToolsUiState,
    project: Project,
    selectedClip: VideoClip?,
    selectedOverlay: TextOverlay?,
    modelFor: (AiTask) -> String,
    captionSeconds: Long,
    translatableCount: Int,
    storyLineCount: Int,
    onOpenTool: (AiTool?) -> Unit,
    onTopicChange: (String) -> Unit,
    onLanguageChange: (String) -> Unit,
    onUseFramesChange: (Boolean) -> Unit,
    onRewriteStyleChange: (RewriteStyle) -> Unit,
    onRun: (AiTool) -> Unit,
    onCancel: () -> Unit,
    actions: AiToolActions,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = DarkSurface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            val tool = state.tool
            if (tool == null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = GoldChampagne, modifier = Modifier.size(22.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("AI Tools", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Black)
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "Uses your OpenRouter key. Nothing is sent until you tap a tool's button, and results are only added when you pick them.",
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 12.sp
                )
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { onOpenTool(null) }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "All AI tools", tint = Color.White)
                    }
                    Text("${tool.emoji}  ${tool.title}", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Black)
                }
            }

            if (!state.hasKey) {
                Spacer(modifier = Modifier.height(10.dp))
                Notice("Add your OpenRouter key first: go back to the home screen and open AI settings (✨).", Color(0xFFFF9F0A))
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (tool == null) {
                ToolList(project, selectedClip, selectedOverlay, captionSeconds, translatableCount, onOpenTool)
            } else {
                ToolPanel(
                    tool, state, project, selectedClip, selectedOverlay, modelFor, captionSeconds, translatableCount,
                    storyLineCount, onTopicChange, onLanguageChange, onUseFramesChange, onRewriteStyleChange, onRun,
                    onCancel, actions, onDismiss
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

/** Why a tool can't run right now, or null when it can. */
private fun unavailableReason(
    tool: AiTool,
    project: Project,
    selectedClip: VideoClip?,
    selectedOverlay: TextOverlay?,
    captionSeconds: Long,
    translatableCount: Int
): String? = when {
    project.clips.isEmpty() && tool != AiTool.TITLE -> "Add a clip first"
    tool == AiTool.COLOR && selectedClip == null -> "Select a clip in the timeline"
    tool == AiTool.REWRITE && (selectedOverlay == null || selectedOverlay.isSticker) -> "Select a text on the timeline"
    tool == AiTool.TRANSLATE && translatableCount == 0 -> "Add some text first"
    tool == AiTool.CAPTIONS && captionSeconds == 0L -> "No clip or voiceover with sound"
    else -> null
}

@Composable
private fun ToolList(
    project: Project,
    selectedClip: VideoClip?,
    selectedOverlay: TextOverlay?,
    captionSeconds: Long,
    translatableCount: Int,
    onOpenTool: (AiTool?) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        AiTool.entries.forEach { tool ->
            val reason = unavailableReason(tool, project, selectedClip, selectedOverlay, captionSeconds, translatableCount)
            val alpha = if (reason == null) 1f else 0.45f
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(DarkCard)
                    .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(12.dp))
                    .clickable(enabled = reason == null) { onOpenTool(tool) }
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(tool.emoji, fontSize = 20.sp, color = Color.White.copy(alpha = alpha))
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(tool.title, color = Color.White.copy(alpha = alpha), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Text(reason ?: tool.subtitle, color = Color.White.copy(alpha = 0.6f * alpha), fontSize = 11.sp)
                }
            }
        }
    }
}

@Composable
private fun ToolPanel(
    tool: AiTool,
    state: AiToolsUiState,
    project: Project,
    selectedClip: VideoClip?,
    selectedOverlay: TextOverlay?,
    modelFor: (AiTask) -> String,
    captionSeconds: Long,
    translatableCount: Int,
    storyLineCount: Int,
    onTopicChange: (String) -> Unit,
    onLanguageChange: (String) -> Unit,
    onUseFramesChange: (Boolean) -> Unit,
    onRewriteStyleChange: (RewriteStyle) -> Unit,
    onRun: (AiTool) -> Unit,
    onCancel: () -> Unit,
    actions: AiToolActions,
    onDismiss: () -> Unit
) {
    Text(tool.subtitle, color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)
    Spacer(modifier = Modifier.height(12.dp))

    // ---- Inputs ----
    val usesTopic = tool in setOf(AiTool.HOOKS, AiTool.POST, AiTool.TITLE, AiTool.STORY)
    val usesFrames = tool == AiTool.HOOKS || tool == AiTool.POST
    val needsLanguage = tool == AiTool.TRANSLATE || (tool == AiTool.REWRITE && state.rewriteStyle == RewriteStyle.TRANSLATE)

    if (tool == AiTool.REWRITE && selectedOverlay != null) {
        Notice("\"${selectedOverlay.text}\"", AccentCyan)
        Spacer(modifier = Modifier.height(10.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(RewriteStyle.entries) { style ->
                Chip(style.title, style == state.rewriteStyle) { onRewriteStyleChange(style) }
            }
        }
        Spacer(modifier = Modifier.height(10.dp))
    }
    if (usesTopic) {
        InputField(state.topic, onTopicChange, if (tool == AiTool.STORY) "What's the story about?" else "What's the video about? (optional)")
        Spacer(modifier = Modifier.height(8.dp))
    }
    if (needsLanguage) {
        InputField(state.language, onLanguageChange, "Language, e.g. Spanish")
        Spacer(modifier = Modifier.height(8.dp))
    }
    if (usesFrames) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("Let AI look at 4 frames of the video", color = Color.White, fontSize = 13.sp, modifier = Modifier.weight(1f))
            Switch(
                checked = state.useFrames,
                onCheckedChange = onUseFramesChange,
                colors = SwitchDefaults.colors(checkedThumbColor = AccentCyan, checkedTrackColor = AccentCyan.copy(alpha = 0.3f))
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
    }
    if (tool == AiTool.CAPTIONS && project.textOverlays.any { it.isAutoCaption }) {
        Notice("Your earlier auto captions will be replaced when you add the new ones.", Color.White.copy(alpha = 0.7f))
        Spacer(modifier = Modifier.height(8.dp))
    }

    // ---- What's sent, before anything is ----
    val sent = when (tool) {
        AiTool.CAPTIONS -> "about ${formatSeconds(captionSeconds)} of audio from your clips and voiceovers" to AiTask.AUDIO
        AiTool.COLOR -> "one frame of \"${selectedClip?.name.orEmpty()}\"" to AiTask.VISION
        AiTool.HOOKS, AiTool.POST ->
            (if (state.useFrames) "4 small frames, your project title and on-screen text" else "your project title and on-screen text") to
                (if (state.useFrames) AiTask.VISION else AiTask.TEXT)
        AiTool.REWRITE -> "the selected text" to AiTask.TEXT
        AiTool.TRANSLATE -> "the $translatableCount text${if (translatableCount == 1) "" else "s"} on your video" to AiTask.TEXT
        AiTool.STORY -> "your topic and project title ($storyLineCount lines)" to AiTask.TEXT
        AiTool.TITLE -> "your project's on-screen text and title" to AiTask.TEXT
    }
    Text(
        "Sends ${sent.first} to ${modelFor(sent.second)}. Billed to your OpenRouter key.",
        color = Color.White.copy(alpha = 0.5f),
        fontSize = 11.sp
    )
    Spacer(modifier = Modifier.height(10.dp))

    // ---- Run / progress ----
    if (state.isWorking) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            CircularProgressIndicator(color = GoldChampagne, strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(10.dp))
            Text(state.progress ?: "Working…", color = Color.White, fontSize = 13.sp, modifier = Modifier.weight(1f))
            OutlinedButton(onClick = onCancel, shape = RoundedCornerShape(10.dp)) { Text("Cancel", color = Color.White) }
        }
    } else {
        val canRun = state.hasKey && !(needsLanguage && state.language.isBlank()) && !(tool == AiTool.STORY && state.topic.isBlank())
        Button(
            onClick = { onRun(tool) },
            enabled = canRun,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = GoldChampagne, disabledContainerColor = GoldChampagne.copy(alpha = 0.3f)),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(runLabel(tool, hasResults(state)), color = Color.Black, fontWeight = FontWeight.Bold)
        }
    }

    state.error?.let {
        Spacer(modifier = Modifier.height(10.dp))
        Notice(it, Color(0xFFFF453A))
    }

    // ---- Results: nothing changes until one is picked ----
    Spacer(modifier = Modifier.height(12.dp))
    if (state.options.isNotEmpty()) {
        Text(
            when (tool) {
                AiTool.HOOKS -> "Tap one to add it at the start"
                AiTool.TITLE -> "Tap one to rename the project"
                else -> "Tap one to use it"
            },
            color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp
        )
        Spacer(modifier = Modifier.height(6.dp))
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            state.options.forEach { option ->
                ResultRow(option) {
                    when (tool) {
                        AiTool.HOOKS -> actions.onAddHook(option)
                        AiTool.REWRITE -> actions.onSetSelectedText(option)
                        AiTool.TITLE -> actions.onRename(option)
                        else -> {}
                    }
                    onDismiss()
                }
            }
        }
    }

    state.postCaption?.let { caption ->
        val clipboard = LocalClipboardManager.current
        Notice(caption.fullText, Color.White)
        Spacer(modifier = Modifier.height(8.dp))
        Button(
            onClick = { clipboard.setText(AnnotatedString(caption.fullText)) },
            colors = ButtonDefaults.buttonColors(containerColor = AccentCyan),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.ContentCopy, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Copy caption & hashtags", color = Color.Black, fontWeight = FontWeight.Bold)
        }
    }

    if (state.translations.isNotEmpty()) {
        val originals = project.textOverlays.associate { it.id to it.text }
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            state.translations.forEach { (id, text) ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(DarkCard)
                        .padding(10.dp)
                ) {
                    Text(originals[id].orEmpty(), color = Color.White.copy(alpha = 0.5f), fontSize = 11.sp)
                    Text(text, color = Color.White, fontSize = 13.sp)
                }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        ApplyButton("Apply translations") {
            actions.onSetTexts(state.translations)
            onDismiss()
        }
    }

    state.color?.let { color ->
        Notice(colorSummary(color), Color.White)
        Spacer(modifier = Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(
                onClick = {
                    actions.onApplyColor(color, true)
                    onDismiss()
                },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.weight(1f)
            ) { Text("Apply to all clips", color = Color.White) }
            Button(
                onClick = {
                    actions.onApplyColor(color, false)
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(containerColor = AccentCyan),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.weight(1f)
            ) { Text("Apply", color = Color.Black, fontWeight = FontWeight.Bold) }
        }
    }

    if (state.overlays.isNotEmpty()) {
        val preview = state.overlays.take(6)
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            preview.forEach { overlay ->
                Text(
                    "${formatTimestamp(overlay.startTimeMs)}  ${overlay.text}",
                    color = Color.White,
                    fontSize = 12.sp
                )
            }
            if (state.overlays.size > preview.size) {
                Text("…and ${state.overlays.size - preview.size} more", color = Color.White.copy(alpha = 0.5f), fontSize = 11.sp)
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        val isCaptions = tool == AiTool.CAPTIONS
        ApplyButton(if (isCaptions) "Add ${state.overlays.size} captions" else "Add story to video") {
            actions.onAddOverlays(
                state.overlays,
                isCaptions,
                if (isCaptions) "${state.overlays.size} captions added" else "Text story added"
            )
            onDismiss()
        }
    }
}

private fun hasResults(state: AiToolsUiState) =
    state.options.isNotEmpty() || state.postCaption != null || state.translations.isNotEmpty() ||
        state.color != null || state.overlays.isNotEmpty()

private fun runLabel(tool: AiTool, again: Boolean): String = if (again) "Try again" else when (tool) {
    AiTool.CAPTIONS -> "Transcribe"
    AiTool.TRANSLATE -> "Translate"
    AiTool.COLOR -> "Suggest a grade"
    AiTool.REWRITE -> "Rewrite"
    AiTool.POST -> "Write caption"
    else -> "Generate"
}

private fun colorSummary(color: ColorSuggestion): String {
    val values = listOf(
        "Exposure" to color.exposure, "Brightness" to color.brightness, "Contrast" to color.contrast,
        "Warmth" to color.temperature, "Tint" to color.tint, "Saturation" to color.saturation, "Hue" to color.hue
    ).filter { it.second != 0f }
        .joinToString("\n") { (name, value) -> "$name ${if (value > 0) "+" else ""}${(value * 100).toInt()}" }
    return listOf(color.reason, values.ifEmpty { "No changes needed." }).filter { it.isNotBlank() }.joinToString("\n\n")
}

private fun formatSeconds(seconds: Long) = if (seconds < 60) "${seconds}s" else "${seconds / 60}m ${seconds % 60}s"

private fun formatTimestamp(ms: Long) = String.format(Locale.US, "%d:%04.1f", ms / 60_000, (ms % 60_000) / 1000f)

@Composable
private fun Notice(text: String, color: Color) {
    Text(
        text = text,
        color = color,
        fontSize = 12.sp,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(DarkCard)
            .padding(10.dp)
    )
}

@Composable
private fun InputField(value: String, onChange: (String) -> Unit, label: String) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = Color.White,
            unfocusedTextColor = Color.White,
            focusedBorderColor = AccentCyan,
            focusedLabelColor = AccentCyan
        )
    )
}

@Composable
private fun Chip(label: String, isSelected: Boolean, onClick: () -> Unit) {
    Text(
        text = label,
        color = if (isSelected) AccentCyan else Color.White,
        fontSize = 12.sp,
        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (isSelected) AccentCyan.copy(alpha = 0.2f) else DarkCard)
            .border(1.dp, if (isSelected) AccentCyan else Color.White.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp)
    )
}

@Composable
private fun ResultRow(text: String, onClick: () -> Unit) {
    Text(
        text = text,
        color = Color.White,
        fontSize = 14.sp,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(DarkCard)
            .border(1.dp, GoldChampagne.copy(alpha = 0.3f), RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp)
    )
}

@Composable
private fun ApplyButton(label: String, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(containerColor = AccentCyan),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(label, color = Color.Black, fontWeight = FontWeight.Bold)
    }
}
