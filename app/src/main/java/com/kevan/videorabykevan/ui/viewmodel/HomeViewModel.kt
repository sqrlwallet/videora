package com.kevan.videorabykevan.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.kevan.videorabykevan.data.ProjectRepository
import com.kevan.videorabykevan.model.AspectRatioPreset
import com.kevan.videorabykevan.model.Project
import com.kevan.videorabykevan.model.SafeZoneType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class HomeUiState(
    val projects: List<Project> = emptyList(),
    val isLoading: Boolean = true
)

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = ProjectRepository(application.applicationContext)

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        loadProjects()
    }

    fun loadProjects() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val list = repository.getProjects()
            _uiState.value = HomeUiState(projects = list, isLoading = false)
        }
    }

    fun createNewProject(
        title: String = "Viral Short",
        aspectRatio: AspectRatioPreset = AspectRatioPreset.TIKTOK_REELS_SHORTS_9_16,
        onCreated: (String) -> Unit
    ) {
        viewModelScope.launch {
            val newProject = Project(
                title = title,
                aspectRatio = aspectRatio,
                safeZone = SafeZoneType.TIKTOK
            )
            repository.saveProject(newProject)
            loadProjects()
            onCreated(newProject.id)
        }
    }

    fun createDemoProject(
        context: android.content.Context,
        onCreated: (String) -> Unit
    ) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                val clipFile = com.kevan.videorabykevan.data.SampleMediaHelper.generateSampleVideoClip(
                    context = context,
                    clipTitle = "Viral Demo Scene",
                    durationSeconds = 5
                )
                val clip = com.kevan.videorabykevan.model.VideoClip(
                    uri = android.net.Uri.fromFile(clipFile).toString(),
                    name = "Starter Clip (5s)",
                    durationMs = 5000L,
                    trimEndMs = 5000L
                )
                val starterOverlay = com.kevan.videorabykevan.model.TextOverlay(
                    text = "🔥 STOP SCROLLING!",
                    startTimeMs = 0L,
                    endTimeMs = 3000L,
                    fontSizeSp = 28f,
                    style = com.kevan.videorabykevan.model.TextStylePreset.VIRAL_HOOK
                )
                val demoProject = Project(
                    title = "Demo Viral Short",
                    aspectRatio = AspectRatioPreset.TIKTOK_REELS_SHORTS_9_16,
                    safeZone = SafeZoneType.TIKTOK,
                    clips = listOf(clip),
                    textOverlays = listOf(starterOverlay)
                )
                repository.saveProject(demoProject)
                loadProjects()
                onCreated(demoProject.id)
            } catch (_: Exception) {
                // Fallback to empty project
                createNewProject("Demo Short", AspectRatioPreset.TIKTOK_REELS_SHORTS_9_16, onCreated)
            }
        }
    }

    fun duplicateProject(id: String) {
        viewModelScope.launch {
            repository.duplicateProject(id)
            loadProjects()
        }
    }

    fun deleteProject(id: String) {
        viewModelScope.launch {
            repository.deleteProject(id)
            loadProjects()
        }
    }
}
