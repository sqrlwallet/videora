package com.kevan.videorabykevan.data

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.webkit.MimeTypeMap
import com.kevan.videorabykevan.model.AudioTrack
import com.kevan.videorabykevan.model.Project
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

/**
 * Owns the media files a project depends on, under `filesDir/projects/<projectId>/`.
 *
 * Anything a project references from the cache directory (voiceovers, demo clips, built-in SFX)
 * or from a content:// URI (whose read permission expires) is copied here, because Android may
 * clear the cache and revoke URI grants at any time.
 */
object ProjectMediaStore {

    fun projectDir(context: Context, projectId: String): File =
        File(context.filesDir, "projects/$projectId")

    /**
     * Brings [file] into the project's media folder. [move] renames it instead of copying, for
     * one-off files like a fresh voiceover recording that nothing else uses.
     */
    suspend fun adoptFile(context: Context, projectId: String, file: File, move: Boolean): File =
        withContext(Dispatchers.IO) {
            val mediaDir = File(projectDir(context, projectId), "media").apply { mkdirs() }
            if (file.parentFile?.canonicalPath == mediaDir.canonicalPath) return@withContext file

            val target = File(mediaDir, "${UUID.randomUUID()}_${file.name}")
            if (move && file.renameTo(target)) return@withContext target
            file.copyTo(target, overwrite = true)
            if (move) file.delete()
            target
        }

    /**
     * Fills in the length of audio tracks saved without one (picked music and SFX used to be added
     * that way). Looping and fade-outs need it; tracks that can't be read are left unchanged.
     */
    suspend fun measureAudioTracks(context: Context, tracks: List<AudioTrack>): List<AudioTrack> = withContext(Dispatchers.IO) {
        tracks.map { track ->
            if (track.trimEndMs > track.trimStartMs) return@map track
            val retriever = MediaMetadataRetriever()
            try {
                retriever.setDataSource(context, Uri.parse(track.uri))
                val durationMs = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull()
                if (durationMs != null && durationMs > track.trimStartMs) {
                    track.copy(durationMs = durationMs, trimEndMs = durationMs)
                } else {
                    track
                }
            } catch (e: Exception) {
                track
            } finally {
                try { retriever.release() } catch (_: Exception) {}
            }
        }
    }

    /**
     * Rewrites every clip and audio URI that points at the cache or a content:// URI into a copy
     * owned by the project. URIs that can't be copied are left unchanged.
     */
    suspend fun localizeMedia(context: Context, project: Project): Project = withContext(Dispatchers.IO) {
        val cacheRoot = context.cacheDir.canonicalPath
        fun localize(uriString: String): String {
            val uri = Uri.parse(uriString)
            return try {
                when {
                    uri.scheme == "content" -> copyContentUri(context, project.id, uri)?.let { Uri.fromFile(it).toString() }
                    uri.scheme == "file" && uri.path?.let { File(it).canonicalPath.startsWith(cacheRoot) } == true -> {
                        val source = File(uri.path!!)
                        if (source.exists()) {
                            val mediaDir = File(projectDir(context, project.id), "media").apply { mkdirs() }
                            val target = File(mediaDir, "${UUID.randomUUID()}_${source.name}")
                            source.copyTo(target, overwrite = true)
                            Uri.fromFile(target).toString()
                        } else {
                            null
                        }
                    }
                    else -> null
                }
            } catch (e: Exception) {
                e.printStackTrace()
                null
            } ?: uriString
        }

        project.copy(
            clips = project.clips.map { it.copy(uri = localize(it.uri)) },
            audioTracks = project.audioTracks.map { it.copy(uri = localize(it.uri)) }
        )
    }

    /** Copies [source]'s media folder for a duplicated project and points the copy's URIs at it. */
    suspend fun copyMedia(context: Context, source: Project, copy: Project): Project = withContext(Dispatchers.IO) {
        val sourceDir = projectDir(context, source.id)
        val targetDir = projectDir(context, copy.id)
        if (!sourceDir.exists()) return@withContext copy

        sourceDir.copyRecursively(targetDir, overwrite = true)
        val sourcePrefix = Uri.fromFile(sourceDir).toString() + "/"
        val targetPrefix = Uri.fromFile(targetDir).toString() + "/"
        fun remap(uri: String) = if (uri.startsWith(sourcePrefix)) targetPrefix + uri.removePrefix(sourcePrefix) else uri

        copy.copy(
            clips = copy.clips.map { it.copy(uri = remap(it.uri)) },
            audioTracks = copy.audioTracks.map { it.copy(uri = remap(it.uri)) }
        )
    }

    suspend fun deleteMedia(context: Context, projectId: String) = withContext(Dispatchers.IO) {
        projectDir(context, projectId).deleteRecursively()
    }

    private fun copyContentUri(context: Context, projectId: String, uri: Uri): File? {
        val extension = context.contentResolver.getType(uri)
            ?.let { MimeTypeMap.getSingleton().getExtensionFromMimeType(it) }
            ?: "media"
        val mediaDir = File(projectDir(context, projectId), "media").apply { mkdirs() }
        val target = File(mediaDir, "${UUID.randomUUID()}.$extension")
        context.contentResolver.openInputStream(uri)?.use { input ->
            target.outputStream().use { output -> input.copyTo(output) }
        }
        return target.takeIf { it.exists() && it.length() > 0L } ?: run {
            target.delete()
            null
        }
    }
}
