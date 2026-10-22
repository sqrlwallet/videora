package com.kevan.videorabykevan.data

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.kevan.videorabykevan.model.AspectRatioPreset
import com.kevan.videorabykevan.model.Project
import com.kevan.videorabykevan.model.SafeZoneType
import com.kevan.videorabykevan.model.TextOverlay
import com.kevan.videorabykevan.model.TextStylePreset
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class ProjectRepository(private val context: Context) {

    private val gson = Gson()
    private val projectsFile: File
        get() = File(context.filesDir, "videora_projects.json")

    suspend fun getProjects(): List<Project> = withContext(Dispatchers.IO) {
        if (!projectsFile.exists()) {
            val defaults = listOf(createWelcomeSampleProject())
            saveProjectsInternal(defaults)
            return@withContext defaults
        }

        try {
            val json = projectsFile.readText()
            val type = object : TypeToken<List<Project>>() {}.type
            val list: List<Project>? = gson.fromJson(json, type)
            list?.sortedByDescending { it.updatedAt } ?: emptyList()
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    suspend fun getProject(id: String): Project? = withContext(Dispatchers.IO) {
        getProjects().find { it.id == id }
    }

    suspend fun saveProject(project: Project) = withContext(Dispatchers.IO) {
        val current = getProjects().toMutableList()
        val index = current.indexOfFirst { it.id == project.id }
        val updated = project.copy(updatedAt = System.currentTimeMillis())
        if (index >= 0) {
            current[index] = updated
        } else {
            current.add(0, updated)
        }
        saveProjectsInternal(current)
    }

    suspend fun deleteProject(id: String) = withContext(Dispatchers.IO) {
        val current = getProjects().toMutableList()
        current.removeAll { it.id == id }
        saveProjectsInternal(current)
    }

    suspend fun duplicateProject(id: String): Project? = withContext(Dispatchers.IO) {
        val original = getProject(id) ?: return@withContext null
        val copy = original.copy(
            id = java.util.UUID.randomUUID().toString(),
            title = "${original.title} (Copy)",
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        saveProject(copy)
        copy
    }

    private fun saveProjectsInternal(projects: List<Project>) {
        try {
            val json = gson.toJson(projects)
            projectsFile.writeText(json)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun createWelcomeSampleProject(): Project {
        return Project(
            title = "My First Viral Short 🚀",
            aspectRatio = AspectRatioPreset.TIKTOK_REELS_SHORTS_9_16,
            safeZone = SafeZoneType.TIKTOK,
            textOverlays = listOf(
                TextOverlay(
                    text = "POV: 100% Local Video Editing ✨",
                    style = TextStylePreset.VIRAL_HOOK,
                    posY = 0.22f,
                    startTimeMs = 0L,
                    endTimeMs = 4500L
                ),
                TextOverlay(
                    text = "No Sign-up • No Cloud • Open Source",
                    style = TextStylePreset.BOLD_WHITE_PILL,
                    posY = 0.72f,
                    startTimeMs = 1000L,
                    endTimeMs = 6000L
                )
            )
        )
    }
}
