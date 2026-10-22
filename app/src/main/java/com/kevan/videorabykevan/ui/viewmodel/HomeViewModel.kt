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
