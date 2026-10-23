package com.kevan.videorabykevan.data

import android.content.Context
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.kevan.videorabykevan.model.AspectRatioPreset
import com.kevan.videorabykevan.model.AudioTrack
import com.kevan.videorabykevan.model.Project
import com.kevan.videorabykevan.model.SafeZoneType
import com.kevan.videorabykevan.model.TextOverlay
import com.kevan.videorabykevan.model.TextAnimation
import com.kevan.videorabykevan.model.TextStylePreset
import com.kevan.videorabykevan.model.VideoClip
import com.kevan.videorabykevan.model.VideoFilter
import com.kevan.videorabykevan.model.VoiceEffect
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption

class ProjectRepository(private val context: Context) {

    private val gson = Gson()
    private val projectsFile: File
        get() = File(context.filesDir, "videora_projects.json")

    suspend fun getProjects(): List<Project> = withContext(Dispatchers.IO) {
        fileLock.withLock { loadProjectsInternal() }
    }

    suspend fun getProject(id: String): Project? = withContext(Dispatchers.IO) {
        getProjects().find { it.id == id }
    }

    suspend fun saveProject(project: Project) = withContext(Dispatchers.IO) {
        fileLock.withLock {
            val current = loadProjectsInternal().toMutableList()
            val index = current.indexOfFirst { it.id == project.id }
            val updated = project.copy(updatedAt = System.currentTimeMillis())
            if (index >= 0) {
                current[index] = updated
            } else {
                current.add(0, updated)
            }
            saveProjectsInternal(current)
        }
    }

    suspend fun deleteProject(id: String) = withContext(Dispatchers.IO) {
        fileLock.withLock {
            val current = loadProjectsInternal().toMutableList()
            current.removeAll { it.id == id }
            saveProjectsInternal(current)
        }
        ProjectMediaStore.deleteMedia(context, id)
    }

    suspend fun duplicateProject(id: String): Project? = withContext(Dispatchers.IO) {
        val original = getProject(id) ?: return@withContext null
        val copy = original.copy(
            id = java.util.UUID.randomUUID().toString(),
            title = "${original.title} (Copy)",
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        // The copy gets its own media files so deleting either project never breaks the other
        val withOwnMedia = ProjectMediaStore.copyMedia(context, original, copy)
        saveProject(withOwnMedia)
        withOwnMedia
    }

    // Callers must hold fileLock
    private fun loadProjectsInternal(): List<Project> {
        if (!projectsFile.exists()) {
            val defaults = listOf(createWelcomeSampleProject())
            saveProjectsInternal(defaults)
            return defaults
        }

        return try {
            ProjectJson.decode(projectsFile.readText()).sortedByDescending { it.updatedAt }
        } catch (t: Throwable) {
            t.printStackTrace()
            // Never overwrite unreadable data: keep it aside so it can be recovered, then start fresh
            val backup = File(context.filesDir, "videora_projects.corrupt-${System.currentTimeMillis()}.json")
            projectsFile.renameTo(backup)
            val defaults = listOf(createWelcomeSampleProject())
            saveProjectsInternal(defaults)
            defaults
        }
    }

    // Callers must hold fileLock. Writes to a temp file then atomically swaps it in, so a crash
    // mid-write can't leave a half-written projects file.
    private fun saveProjectsInternal(projects: List<Project>) {
        try {
            val tempFile = File(context.filesDir, "videora_projects.json.tmp")
            tempFile.writeText(gson.toJson(projects))
            Files.move(
                tempFile.toPath(),
                projectsFile.toPath(),
                StandardCopyOption.REPLACE_EXISTING,
                StandardCopyOption.ATOMIC_MOVE
            )
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

    companion object {
        // Shared across instances: the home and editor screens each create their own repository
        private val fileLock = Mutex()
    }
}

/** Reads the saved projects JSON defensively; see [withDefaults]. */
internal object ProjectJson {
    private val gson = Gson()

    fun decode(text: String): List<Project> {
        val json = JsonParser.parseString(text)
        if (!json.isJsonArray) throw IllegalStateException("Projects file is not a list")
        return json.asJsonArray
            .filter { it.isJsonObject }
            .map { gson.fromJson(withDefaults(it.asJsonObject), Project::class.java) }
    }

    /**
     * Gson bypasses Kotlin constructors, so a field missing from older saves (or an enum value
     * this version doesn't know) would come back as null in a non-null property and crash later.
     * Fill every missing/null/unknown field from a default instance before deserializing.
     */
    private fun withDefaults(saved: JsonObject): JsonObject {
        val project = mergeDefaults(saved, gson.toJsonTree(Project()).asJsonObject)
        project.validateEnum("aspectRatio", AspectRatioPreset.entries, AspectRatioPreset.TIKTOK_REELS_SHORTS_9_16.name)
        project.validateEnum("safeZone", SafeZoneType.entries, SafeZoneType.TIKTOK.name)

        project.mapArray("clips", gson.toJsonTree(VideoClip(uri = "")).asJsonObject) {
            it.validateEnum("filter", VideoFilter.entries, VideoFilter.NONE.name)
            it.validateEnum("voiceEffect", VoiceEffect.entries, VoiceEffect.NONE.name)
        }
        project.mapArray("audioTracks", gson.toJsonTree(AudioTrack(uri = "")).asJsonObject) {
            it.validateEnum("voiceEffect", VoiceEffect.entries, VoiceEffect.NONE.name)
        }
        project.mapArray("textOverlays", gson.toJsonTree(TextOverlay()).asJsonObject) {
            it.validateEnum("style", TextStylePreset.entries, TextStylePreset.VIRAL_HOOK.name)
            it.validateEnum("animation", TextAnimation.entries, TextAnimation.NONE.name)
        }
        return project
    }

    private fun mergeDefaults(saved: JsonObject, defaults: JsonObject): JsonObject {
        val merged = defaults.deepCopy()
        for ((key, value) in saved.entrySet()) {
            if (!value.isJsonNull) merged.add(key, value)
        }
        return merged
    }

    private inline fun JsonObject.mapArray(key: String, defaults: JsonObject, fix: (JsonObject) -> Unit) {
        val items = get(key)?.takeIf { it.isJsonArray }?.asJsonArray ?: JsonArray()
        val fixed = JsonArray()
        for (item in items) {
            if (!item.isJsonObject) continue
            fixed.add(mergeDefaults(item.asJsonObject, defaults).also(fix))
        }
        add(key, fixed)
    }

    private fun JsonObject.validateEnum(key: String, entries: List<Enum<*>>, fallback: String) {
        val value = get(key)?.takeIf { it.isJsonPrimitive }?.asString
        if (entries.none { it.name == value }) addProperty(key, fallback)
    }
}
