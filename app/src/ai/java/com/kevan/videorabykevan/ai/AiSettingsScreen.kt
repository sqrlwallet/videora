package com.kevan.videorabykevan.ai

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldDefaults
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kevan.videorabykevan.ui.theme.AccentCyan
import com.kevan.videorabykevan.ui.theme.DarkBackground
import com.kevan.videorabykevan.ui.theme.DarkCard
import com.kevan.videorabykevan.ui.theme.DarkSurface
import com.kevan.videorabykevan.ui.theme.DarkSurfaceVariant
import com.kevan.videorabykevan.ui.theme.EmeraldAccent
import com.kevan.videorabykevan.ui.theme.GoldChampagne
import com.kevan.videorabykevan.ui.theme.TextPrimary
import com.kevan.videorabykevan.ui.theme.TextSecondary
import com.kevan.videorabykevan.ui.theme.TextTertiary
import java.util.Locale

private val ErrorRed = Color(0xFFFF453A)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiSettingsScreen(onBack: () -> Unit, viewModel: AiSettingsViewModel = viewModel()) {
    val state by viewModel.uiState.collectAsState()
    BackHandler(onBack = onBack)

    Scaffold(
        containerColor = DarkBackground,
        topBar = {
            TopAppBar(
                title = { Text("AI settings", color = TextPrimary, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkBackground)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            PrivacyCard()
            KeyCard(state, viewModel)
            SettingsCard {
                SectionTitle("Models")
                Text(
                    "Each feature uses its own model, billed to your OpenRouter account.",
                    color = TextSecondary,
                    fontSize = 12.sp
                )
                AiTask.entries.forEach { task ->
                    ModelRow(task, state.models[task] ?: task.defaultModelId, onChange = { viewModel.openModelPicker(task) })
                }
            }
            Spacer(modifier = Modifier.heightIn(min = 24.dp))
        }
    }

    state.pickingModelFor?.let { task ->
        ModelPickerSheet(
            task = task,
            currentModelId = state.models[task] ?: task.defaultModelId,
            modelList = state.modelList,
            onRetry = viewModel::loadModels,
            onPick = { viewModel.setModel(task, it) },
            onDismiss = viewModel::closeModelPicker
        )
    }
}

@Composable
private fun PrivacyCard() {
    SettingsCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Lock, contentDescription = null, tint = EmeraldAccent, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            SectionTitle("Off until you use it")
        }
        Bullet("Nothing leaves your phone until you tap an AI action. Editing and export stay fully offline.")
        Bullet("When you do, only what that action needs (for example a clip's audio, or your project title) goes to OpenRouter and the model provider you choose.")
        Bullet("Your key is encrypted on this phone and excluded from backups. Usage is billed to your own OpenRouter account.")
    }
}

@Composable
private fun KeyCard(state: AiSettingsUiState, viewModel: AiSettingsViewModel) {
    val context = LocalContext.current
    var confirmRemove by remember { mutableStateOf(false) }

    SettingsCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Key, contentDescription = null, tint = GoldChampagne, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            SectionTitle("OpenRouter key")
        }

        if (state.savedKeyMask != null) {
            Text(state.savedKeyMask, color = TextPrimary, fontFamily = FontFamily.Monospace, fontSize = 14.sp)
            KeyStatus(state.keyCheck)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(onClick = viewModel::checkKey, enabled = state.keyCheck != KeyCheck.Checking) {
                    Text("Check key", color = TextPrimary)
                }
                TextButton(onClick = { confirmRemove = true }) {
                    Text("Remove", color = ErrorRed)
                }
            }
        } else {
            Text(
                "Paste a key from your OpenRouter account. You can give it a spending limit there.",
                color = TextSecondary,
                fontSize = 12.sp
            )
            KeyInputField(state, viewModel)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                Button(
                    onClick = viewModel::saveKey,
                    enabled = state.keyInput.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = GoldChampagne)
                ) {
                    Text("Save key", color = Color.Black, fontWeight = FontWeight.Bold)
                }
                TextButton(onClick = {
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://openrouter.ai/keys")))
                }) {
                    Text("Get a key", color = AccentCyan)
                }
            }
        }
    }

    if (confirmRemove) {
        AlertDialog(
            onDismissRequest = { confirmRemove = false },
            containerColor = DarkSurface,
            title = { Text("Remove key?", color = TextPrimary) },
            text = { Text("AI features stop working until you add a key again. The key stays valid on OpenRouter.", color = TextSecondary) },
            confirmButton = {
                TextButton(onClick = {
                    confirmRemove = false
                    viewModel.removeKey()
                }) { Text("Remove", color = ErrorRed) }
            },
            dismissButton = {
                TextButton(onClick = { confirmRemove = false }) { Text("Cancel", color = TextPrimary) }
            }
        )
    }
}

@Composable
private fun KeyInputField(state: AiSettingsUiState, viewModel: AiSettingsViewModel) {
    var revealed by remember { mutableStateOf(false) }
    OutlinedTextField(
        value = state.keyInput,
        onValueChange = viewModel::onKeyInputChange,
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        placeholder = { Text("sk-or-…", color = TextTertiary) },
        isError = state.keyInputError != null,
        supportingText = state.keyInputError?.let { { Text(it, color = ErrorRed) } },
        visualTransformation = if (revealed) VisualTransformation.None else PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Password,
            autoCorrectEnabled = false,
            imeAction = ImeAction.Done
        ),
        trailingIcon = {
            IconButton(onClick = { revealed = !revealed }) {
                Icon(
                    if (revealed) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                    contentDescription = if (revealed) "Hide key" else "Show key",
                    tint = TextSecondary
                )
            }
        },
        colors = fieldColors()
    )
}

@Composable
private fun KeyStatus(check: KeyCheck) {
    when (check) {
        KeyCheck.Idle -> Unit
        KeyCheck.Checking -> Row(verticalAlignment = Alignment.CenterVertically) {
            CircularProgressIndicator(color = GoldChampagne, strokeWidth = 2.dp, modifier = Modifier.size(14.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Checking with OpenRouter…", color = TextSecondary, fontSize = 12.sp)
        }
        is KeyCheck.Valid -> Text(describeCredit(check.info), color = EmeraldAccent, fontSize = 12.sp)
        is KeyCheck.Failed -> Text(check.message, color = ErrorRed, fontSize = 12.sp)
    }
}

private fun describeCredit(info: AiKeyInfo): String {
    val parts = mutableListOf("Key works")
    when {
        info.remainingUsd != null && info.limitUsd != null ->
            parts += "${usd(info.remainingUsd)} left of its ${usd(info.limitUsd)} limit"
        else -> parts += "${usd(info.usageUsd)} used, no limit on this key"
    }
    if (info.isFreeTier) parts += "free tier"
    return parts.joinToString(" · ")
}

private fun usd(amount: Double) = "$%.2f".format(Locale.US, amount)

@Composable
private fun ModelRow(task: AiTask, modelId: String, onChange: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(DarkSurfaceVariant)
            .clickable(onClick = onChange)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(task.title, color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
            Text(task.description, color = TextSecondary, fontSize = 11.sp)
            Text(
                modelId,
                color = GoldChampagne,
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Text("Change", color = AccentCyan, fontSize = 12.sp)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ModelPickerSheet(
    task: AiTask,
    currentModelId: String,
    modelList: ModelList,
    onRetry: () -> Unit,
    onPick: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var query by remember { mutableStateOf("") }

    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = DarkSurface) {
        Column(modifier = Modifier.padding(horizontal = 16.dp).padding(bottom = 16.dp)) {
            Text(task.title, color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Text(
                "Models that accept ${task.requiredInput}, cheapest first.",
                color = TextSecondary,
                fontSize = 12.sp,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                placeholder = { Text("Search, or type a model ID", color = TextTertiary) },
                colors = fieldColors()
            )
            Spacer(modifier = Modifier.heightIn(min = 8.dp))

            when (modelList) {
                ModelList.NotLoaded, ModelList.Loading -> Box(
                    modifier = Modifier.fillMaxWidth().padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = GoldChampagne)
                }
                is ModelList.Failed -> Column {
                    Text(modelList.message, color = ErrorRed, fontSize = 12.sp)
                    TextButton(onClick = onRetry) { Text("Try again", color = AccentCyan) }
                }
                is ModelList.Loaded -> {
                    val needle = query.trim()
                    val matches = OpenRouterJson.modelsFor(task, modelList.models).filter {
                        needle.isEmpty() || it.id.contains(needle, ignoreCase = true) || it.name.contains(needle, ignoreCase = true)
                    }
                    LazyColumn(modifier = Modifier.heightIn(max = 420.dp)) {
                        if (needle.isEmpty()) {
                            item {
                                PickerRow(
                                    title = "Default (recommended)",
                                    subtitle = task.defaultModelId,
                                    detail = null,
                                    isCurrent = currentModelId == task.defaultModelId,
                                    onClick = { onPick(task.defaultModelId) }
                                )
                            }
                        } else if (matches.none { it.id == needle }) {
                            // Lets people use a model the list doesn't show (new, or an alias)
                            item {
                                PickerRow(
                                    title = "Use \"$needle\"",
                                    subtitle = "Custom model ID",
                                    detail = null,
                                    isCurrent = false,
                                    onClick = { onPick(needle) }
                                )
                            }
                        }
                        items(matches, key = { it.id }) { model ->
                            PickerRow(
                                title = model.name,
                                subtitle = model.id,
                                detail = priceLabel(model),
                                isCurrent = model.id == currentModelId,
                                onClick = { onPick(model.id) }
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun priceLabel(model: AiModel): String? = when {
    model.isFree -> "Free"
    model.promptPricePerMillion != null -> "$%.2f/M".format(Locale.US, model.promptPricePerMillion)
    else -> null
}

@Composable
private fun PickerRow(title: String, subtitle: String, detail: String?, isCurrent: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(if (isCurrent) DarkCard else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = if (isCurrent) GoldChampagne else TextPrimary, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(subtitle, color = TextTertiary, fontSize = 11.sp, fontFamily = FontFamily.Monospace, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        detail?.let { Text(it, color = TextSecondary, fontSize = 11.sp) }
    }
}

@Composable
private fun SettingsCard(content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(DarkCard)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        content()
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text, color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
}

@Composable
private fun Bullet(text: String) {
    Row {
        Text("•", color = TextSecondary, fontSize = 12.sp, modifier = Modifier.padding(end = 6.dp))
        Text(text, color = TextSecondary, fontSize = 12.sp)
    }
}

@Composable
private fun fieldColors() = TextFieldDefaults.colors(
    focusedTextColor = Color.White,
    unfocusedTextColor = Color.White,
    focusedContainerColor = DarkSurfaceVariant,
    unfocusedContainerColor = DarkSurfaceVariant,
    cursorColor = AccentCyan,
    focusedIndicatorColor = AccentCyan,
    unfocusedIndicatorColor = Color.Transparent
)
