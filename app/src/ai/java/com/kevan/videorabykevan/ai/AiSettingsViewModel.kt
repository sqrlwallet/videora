package com.kevan.videorabykevan.ai

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface KeyCheck {
    data object Idle : KeyCheck
    data object Checking : KeyCheck
    data class Valid(val info: AiKeyInfo) : KeyCheck
    data class Failed(val message: String) : KeyCheck
}

sealed interface ModelList {
    data object NotLoaded : ModelList
    data object Loading : ModelList
    data class Loaded(val models: List<AiModel>) : ModelList
    data class Failed(val message: String) : ModelList
}

data class AiSettingsUiState(
    /** Masked form of the saved key; null when none is saved. */
    val savedKeyMask: String? = null,
    val keyInput: String = "",
    val keyInputError: String? = null,
    val keyCheck: KeyCheck = KeyCheck.Idle,
    val models: Map<AiTask, String> = emptyMap(),
    val modelList: ModelList = ModelList.NotLoaded,
    /** The task whose model picker is open, if any. */
    val pickingModelFor: AiTask? = null
)

class AiSettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val store = ApiKeyStore(application)

    private val _uiState = MutableStateFlow(
        AiSettingsUiState(
            savedKeyMask = store.load()?.let(AiKeyFormat::mask),
            models = AiTask.entries.associateWith { store.modelFor(it) }
        )
    )
    val uiState: StateFlow<AiSettingsUiState> = _uiState.asStateFlow()

    private var checkJob: Job? = null

    fun onKeyInputChange(value: String) = _uiState.update { it.copy(keyInput = value, keyInputError = null) }

    /** Saves the pasted key, then checks it straight away so a typo shows up immediately. */
    fun saveKey() {
        val key = _uiState.value.keyInput.trim()
        if (!AiKeyFormat.looksValid(key)) {
            _uiState.update { it.copy(keyInputError = "OpenRouter keys start with \"sk-or-\"") }
            return
        }
        store.save(key)
        _uiState.update { it.copy(savedKeyMask = AiKeyFormat.mask(key), keyInput = "", keyInputError = null) }
        checkKey()
    }

    fun checkKey() {
        val key = store.load() ?: return
        checkJob?.cancel()
        _uiState.update { it.copy(keyCheck = KeyCheck.Checking) }
        checkJob = viewModelScope.launch {
            val result = try {
                KeyCheck.Valid(OpenRouterClient(key).keyInfo())
            } catch (e: AiError) {
                KeyCheck.Failed(e.message.orEmpty())
            }
            _uiState.update { it.copy(keyCheck = result) }
        }
    }

    fun removeKey() {
        checkJob?.cancel()
        store.clear()
        _uiState.update { it.copy(savedKeyMask = null, keyCheck = KeyCheck.Idle) }
    }

    fun openModelPicker(task: AiTask) {
        _uiState.update { it.copy(pickingModelFor = task) }
        if (_uiState.value.modelList !is ModelList.Loaded) loadModels()
    }

    fun closeModelPicker() = _uiState.update { it.copy(pickingModelFor = null) }

    fun loadModels() {
        _uiState.update { it.copy(modelList = ModelList.Loading) }
        viewModelScope.launch {
            val result = try {
                ModelList.Loaded(OpenRouterClient(store.load()).models())
            } catch (e: AiError) {
                ModelList.Failed(e.message.orEmpty())
            }
            _uiState.update { it.copy(modelList = result) }
        }
    }

    fun setModel(task: AiTask, modelId: String) {
        store.setModel(task, modelId)
        _uiState.update { it.copy(models = it.models + (task to store.modelFor(task)), pickingModelFor = null) }
    }
}
