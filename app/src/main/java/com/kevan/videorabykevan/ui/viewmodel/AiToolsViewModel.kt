package com.kevan.videorabykevan.ui.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.kevan.videorabykevan.ai.AiClient
import com.kevan.videorabykevan.ai.AiContent
import com.kevan.videorabykevan.ai.AiEdition
import com.kevan.videorabykevan.ai.AiError
import com.kevan.videorabykevan.ai.AiMedia
import com.kevan.videorabykevan.ai.AiMessage
import com.kevan.videorabykevan.ai.AiProjectContext
import com.kevan.videorabykevan.ai.AiPrompts
import com.kevan.videorabykevan.ai.AiTask
import com.kevan.videorabykevan.ai.ColorSuggestion
import com.kevan.videorabykevan.ai.PostCaption
import com.kevan.videorabykevan.ai.RewriteStyle
import com.kevan.videorabykevan.model.Project
import com.kevan.videorabykevan.model.TextOverlay
import com.kevan.videorabykevan.model.VideoClip
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** The AI tools offered in the editor (AI edition only). */
enum class AiTool(val title: String, val subtitle: String, val emoji: String) {
    CAPTIONS("Auto captions", "Subtitles from the speech in your clips and voiceovers", "💬"),
    HOOKS("Hook ideas", "Scroll-stopping opening lines for your video", "🪝"),
    STORY("Text story", "A sequence of on-screen lines that tells your story", "📖"),
    REWRITE("Rewrite text", "Punchier, shorter, fixed, with emojis or translated", "✍️"),
    TRANSLATE("Translate all text", "Every text on the video into another language", "🌍"),
    COLOR("Auto color", "A color correction suggested from the clip's frame", "🎨"),
    POST("Post caption", "Caption and hashtags to paste when you post", "#️⃣"),
    TITLE("Name project", "Short names for this project", "🏷️")
}

data class AiToolsUiState(
    val tool: AiTool? = null,
    // Checked when the sheet opens: reading the key decrypts it, so not on every recomposition
    val hasKey: Boolean = false,
    val models: Map<AiTask, String> = emptyMap(),
    val topic: String = "",
    val language: String = "",
    val useFrames: Boolean = true,
    val rewriteStyle: RewriteStyle = RewriteStyle.PUNCHIER,
    val isWorking: Boolean = false,
    val progress: String? = null,
    val error: String? = null,
    // Results; one tool's at a time, cleared when another tool opens
    val options: List<String> = emptyList(),
    val postCaption: PostCaption? = null,
    val translations: List<Pair<String, String>> = emptyList(),
    val color: ColorSuggestion? = null,
    val overlays: List<TextOverlay> = emptyList()
)

/**
 * Runs the AI tools and holds their results. Nothing is applied here: the sheet shows each result
 * and the user taps to add it, which goes through EditorViewModel as an ordinary undoable edit.
 */
class AiToolsViewModel(application: Application) : AndroidViewModel(application) {

    private val _state = MutableStateFlow(AiToolsUiState())
    val state: StateFlow<AiToolsUiState> = _state.asStateFlow()

    private var job: Job? = null

    private val context get() = getApplication<Application>().applicationContext

    /** Read fresh each time: the key or model may have changed in settings. */
    private fun client(): AiClient? = AiEdition.client(context)

    fun modelFor(task: AiTask): String = _state.value.models[task] ?: task.defaultModelId

    fun open(tool: AiTool?) {
        cancel()
        val client = client()
        _state.value = _state.value.copy(
            tool = tool, hasKey = client != null,
            models = AiTask.entries.associateWith { client?.modelFor(it) ?: it.defaultModelId },
            error = null, progress = null, options = emptyList(), postCaption = null,
            translations = emptyList(), color = null, overlays = emptyList()
        )
    }

    fun setTopic(topic: String) = update { it.copy(topic = topic) }
    fun setLanguage(language: String) = update { it.copy(language = language) }
    fun setUseFrames(useFrames: Boolean) = update { it.copy(useFrames = useFrames) }
    fun setRewriteStyle(style: RewriteStyle) = update { it.copy(rewriteStyle = style) }

    fun cancel() {
        job?.cancel()
        job = null
        update { it.copy(isWorking = false, progress = null) }
    }

    fun hooks(project: Project) = launchTool(AiTask.TEXT) { client ->
        val frames = if (_state.value.useFrames) frames(project) else emptyList()
        val prompt = AiPrompts.hooks(contextOf(project), _state.value.topic, frames.isNotEmpty())
        val reply = client.chat(if (frames.isEmpty()) AiTask.TEXT else AiTask.VISION, messages(prompt, frames))
        update { it.copy(options = AiPrompts.parseStringList(reply, "hooks")) }
    }

    fun postCaption(project: Project) = launchTool(AiTask.TEXT) { client ->
        val frames = if (_state.value.useFrames) frames(project) else emptyList()
        val prompt = AiPrompts.postCaption(contextOf(project), _state.value.topic, frames.isNotEmpty())
        val reply = client.chat(if (frames.isEmpty()) AiTask.TEXT else AiTask.VISION, messages(prompt, frames))
        update { it.copy(postCaption = AiPrompts.parsePostCaption(reply)) }
    }

    fun rewrite(text: String) = launchTool(AiTask.TEXT) { client ->
        val prompt = AiPrompts.rewrite(text, _state.value.rewriteStyle, _state.value.language)
        update { it.copy(options = AiPrompts.parseStringList(client.chat(AiTask.TEXT, messages(prompt)), "options")) }
    }

    fun titles(project: Project) = launchTool(AiTask.TEXT) { client ->
        val prompt = AiPrompts.titles(contextOf(project), _state.value.topic)
        update { it.copy(options = AiPrompts.parseStringList(client.chat(AiTask.TEXT, messages(prompt)), "titles")) }
    }

    fun translateAll(project: Project) = launchTool(AiTask.TEXT) { client ->
        val texts = translatableTexts(project)
        if (texts.isEmpty()) throw IllegalStateException("There's no text on the video to translate.")
        val prompt = AiPrompts.translateAll(texts.map { it.text }, _state.value.language)
        val translated = AiPrompts.parseTranslations(client.chat(AiTask.TEXT, messages(prompt), maxTokens = 3000), texts.size)
        update { it.copy(translations = texts.map { o -> o.id }.zip(translated)) }
    }

    fun story(project: Project) = launchTool(AiTask.TEXT) { client ->
        val lineCount = storyLineCount(project)
        val prompt = AiPrompts.script(contextOf(project), _state.value.topic, lineCount)
        val lines = AiPrompts.parseStringList(client.chat(AiTask.TEXT, messages(prompt)), "lines")
        update { it.copy(overlays = AiPrompts.storyOverlays(lines, project.totalDurationMs)) }
    }

    fun color(clip: VideoClip) = launchTool(AiTask.VISION) { client ->
        update { it.copy(progress = "Reading the frame…") }
        val frame = AiMedia.frameJpeg(context, clip, (clip.trimStartMs + clip.trimEndMs) / 2)
            ?: throw IllegalStateException("Couldn't read a frame from this clip.")
        update { it.copy(progress = "Asking for a grade…") }
        val reply = client.chat(AiTask.VISION, messages(AiPrompts.color(), listOf(frame)))
        update { it.copy(color = AiPrompts.parseColor(reply)) }
    }

    fun captions(project: Project) = launchTool(AiTask.AUDIO) { client ->
        val pieces = AiPrompts.speechPieces(project)
        if (pieces.isEmpty()) throw IllegalStateException("No clip or voiceover with sound to caption.")
        val segments = mutableListOf<com.kevan.videorabykevan.ai.TranscriptSegment>()
        pieces.forEachIndexed { i, speech ->
            val label = if (pieces.size > 1) " (${i + 1}/${pieces.size})" else ""
            update { it.copy(progress = "Preparing audio$label…") }
            val pcm = AiMedia.decodeSpeechPcm(
                context, Uri.parse(speech.uri), speech.sourceStartMs, speech.sourceStartMs + speech.piece.lengthMs
            ) ?: return@forEachIndexed
            update { it.copy(progress = "Transcribing$label…") }
            val audio = AiContent.AudioWav(AiMedia.base64(AiMedia.wav(pcm)))
            val reply = client.chat(AiTask.AUDIO, messages(AiPrompts.transcribe(), audio = audio), maxTokens = 4000)
            segments += AiPrompts.mapToTimeline(AiPrompts.parseTranscript(reply), speech.piece)
        }
        val overlays = AiPrompts.captionOverlays(segments, project.totalDurationMs)
        if (overlays.isEmpty()) throw IllegalStateException("No speech was found.")
        update { it.copy(overlays = overlays) }
    }

    /** Seconds of audio auto-captions would send, for the "what's sent" line. */
    fun captionAudioSeconds(project: Project): Long = AiPrompts.speechPieces(project).sumOf { it.piece.lengthMs } / 1000

    fun translatableTexts(project: Project): List<TextOverlay> = project.textOverlays.filter { !it.isSticker && it.text.isNotBlank() }

    fun storyLineCount(project: Project): Int = (project.totalDurationMs / 2500L).toInt().coerceIn(3, 8)

    private suspend fun frames(project: Project): List<String> {
        update { it.copy(progress = "Reading frames…") }
        return AiMedia.projectFrames(context, project).also { update { s -> s.copy(progress = "Asking AI…") } }
    }

    private fun messages(prompt: String, frames: List<String> = emptyList(), audio: AiContent? = null): List<AiMessage> =
        listOf(
            AiMessage.system(AiPrompts.SYSTEM),
            AiMessage("user", listOf<AiContent>(AiContent.Text(prompt)) + frames.map { AiContent.ImageJpeg(it) } + listOfNotNull(audio))
        )

    private fun contextOf(project: Project) = AiProjectContext(
        title = project.title,
        durationMs = project.totalDurationMs,
        clipCount = project.clips.size,
        onScreenText = project.textOverlays.filter { !it.isSticker && !it.isAutoCaption }.map { it.text }.take(10)
    )

    /** One tool run: shows progress, turns failures into a readable message, can be cancelled. */
    private fun launchTool(task: AiTask, block: suspend (AiClient) -> Unit) {
        job?.cancel()
        val client = client()
        if (client == null) {
            update { it.copy(error = AiError.NoKey().message) }
            return
        }
        update {
            it.copy(
                isWorking = true, error = null, progress = "Asking ${client.modelFor(task)}…", options = emptyList(),
                postCaption = null, translations = emptyList(), color = null, overlays = emptyList()
            )
        }
        job = viewModelScope.launch {
            try {
                block(client)
                update { it.copy(isWorking = false, progress = null) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: AiError) {
                update { it.copy(isWorking = false, progress = null, error = e.message) }
            } catch (e: IllegalStateException) {
                update { it.copy(isWorking = false, progress = null, error = e.message) }
            } catch (e: Exception) {
                e.printStackTrace()
                update { it.copy(isWorking = false, progress = null, error = "Something went wrong. Try again.") }
            }
        }
    }

    private inline fun update(transform: (AiToolsUiState) -> AiToolsUiState) {
        _state.value = transform(_state.value)
    }
}
