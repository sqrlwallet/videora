package com.kevan.videorabykevan.ai

import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser

// Shared by both editions. Only the AI edition (src/ai) makes requests; this file is pure Kotlin
// (parsing, errors, model choices) so it can be unit tested without Android or a network.

/** What a feature needs from a model; the settings screen keeps one model per task. */
enum class AiTask(val title: String, val description: String, val requiredInput: String) {
    TEXT("Text model", "Hooks, post captions, rewriting and translating text", "text"),
    VISION("Vision model", "Auto color and ideas from your video's frames", "image"),
    AUDIO("Audio model", "Transcribing speech for auto-captions", "audio");

    // An OpenRouter alias that tracks the current Gemini Flash, which takes text and audio,
    // so the default doesn't go stale as model versions change
    val defaultModelId: String get() = "~google/gemini-flash-latest"
}

data class AiKeyInfo(
    val label: String,
    /** Spending cap on the key in USD; null = no cap. */
    val limitUsd: Double?,
    val remainingUsd: Double?,
    val usageUsd: Double,
    val isFreeTier: Boolean
)

data class AiModel(
    val id: String,
    val name: String,
    val inputModalities: List<String>,
    /** USD per prompt token; null when OpenRouter doesn't say. */
    val promptPricePerToken: Double?
) {
    val isFree: Boolean get() = promptPricePerToken == 0.0

    /** Price per million prompt tokens, as OpenRouter's site shows it. */
    val promptPricePerMillion: Double? get() = promptPricePerToken?.let { it * 1_000_000 }
}

/** Failures shown to the user; [message] is already user-facing. */
sealed class AiError(message: String) : Exception(message) {
    class NoKey : AiError("Add your OpenRouter key in AI settings first.")
    class InvalidKey : AiError("OpenRouter didn't accept this key. Check it was copied in full.")
    class NoCredits(detail: String?) : AiError("Out of OpenRouter credits${detail?.let { ": $it" } ?: "."}")
    class RateLimited(val retryAfterSeconds: Long?) :
        AiError("Too many requests right now${retryAfterSeconds?.let { "; try again in ${it}s" } ?: ", try again shortly"}.")
    class Offline : AiError("Couldn't reach OpenRouter. Check your internet connection.")
    class Server(code: Int, detail: String?) : AiError("OpenRouter error $code${detail?.let { ": $it" } ?: ""}")
    class BadResponse : AiError("OpenRouter sent a response Videora couldn't read.")
}

/** One part of a chat message: text, a JPEG frame or a WAV clip, both base64. */
sealed class AiContent {
    data class Text(val text: String) : AiContent()
    data class ImageJpeg(val base64: String) : AiContent()
    data class AudioWav(val base64: String) : AiContent()
}

data class AiMessage(val role: String, val content: List<AiContent>) {
    companion object {
        fun system(text: String) = AiMessage("system", listOf(AiContent.Text(text)))
        fun user(vararg content: AiContent) = AiMessage("user", content.toList())
    }
}

/** The client the AI features call; the AI edition implements it over HTTPS. */
interface AiClient {
    /** Checks the saved key and reads its credit. */
    suspend fun keyInfo(): AiKeyInfo

    /** Every model OpenRouter offers (public list; no key needed). */
    suspend fun models(): List<AiModel>

    /** The model the user picked for [task] (shown before anything is sent). */
    fun modelFor(task: AiTask): String

    /** Sends [messages] to the model for [task] and returns the reply text. */
    suspend fun chat(task: AiTask, messages: List<AiMessage>, maxTokens: Int = 1500): String
}

object OpenRouterJson {

    fun parseKeyInfo(body: String): AiKeyInfo {
        val data = parseObject(body).obj("data") ?: throw AiError.BadResponse()
        return AiKeyInfo(
            label = data.string("label").orEmpty(),
            limitUsd = data.double("limit"),
            remainingUsd = data.double("limit_remaining"),
            usageUsd = data.double("usage") ?: 0.0,
            isFreeTier = data.get("is_free_tier")?.takeIf { it.isJsonPrimitive }?.asBoolean ?: false
        )
    }

    fun parseModels(body: String): List<AiModel> {
        val data = parseObject(body).get("data")?.takeIf { it.isJsonArray }?.asJsonArray
            ?: throw AiError.BadResponse()
        return data.mapNotNull { element ->
            val model = element.takeIf { it.isJsonObject }?.asJsonObject ?: return@mapNotNull null
            val id = model.string("id") ?: return@mapNotNull null
            AiModel(
                id = id,
                name = model.string("name") ?: id,
                inputModalities = model.obj("architecture")?.get("input_modalities")
                    ?.takeIf { it.isJsonArray }?.asJsonArray
                    ?.mapNotNull { it.takeIf { m -> m.isJsonPrimitive }?.asString }
                    .orEmpty(),
                // Prices come as decimal strings ("0.0000003")
                promptPricePerToken = model.obj("pricing")?.string("prompt")?.toDoubleOrNull()
            )
        }
    }

    /** Models usable for [task], cheapest first; "batch" variants (async, not for an app) are left out. */
    fun modelsFor(task: AiTask, models: List<AiModel>): List<AiModel> =
        models
            .filter { task.requiredInput in it.inputModalities && !it.id.endsWith(":batch") }
            .sortedWith(compareBy(nullsLast()) { it.promptPricePerToken })

    /**
     * Chat completion request body. Content is always sent as a parts array (text, image_url with
     * a data URL, input_audio) because that's the shape every multimodal model accepts.
     */
    fun chatRequest(model: String, messages: List<AiMessage>, maxTokens: Int): String {
        val body = JsonObject()
        body.addProperty("model", model)
        body.addProperty("max_tokens", maxTokens)
        val array = JsonArray()
        for (message in messages) {
            val parts = JsonArray()
            for (content in message.content) {
                parts.add(
                    JsonObject().apply {
                        when (content) {
                            is AiContent.Text -> {
                                addProperty("type", "text")
                                addProperty("text", content.text)
                            }
                            is AiContent.ImageJpeg -> {
                                addProperty("type", "image_url")
                                add("image_url", JsonObject().apply { addProperty("url", "data:image/jpeg;base64,${content.base64}") })
                            }
                            is AiContent.AudioWav -> {
                                addProperty("type", "input_audio")
                                add(
                                    "input_audio",
                                    JsonObject().apply {
                                        addProperty("data", content.base64)
                                        addProperty("format", "wav")
                                    }
                                )
                            }
                        }
                    }
                )
            }
            array.add(
                JsonObject().apply {
                    addProperty("role", message.role)
                    add("content", parts)
                }
            )
        }
        body.add("messages", array)
        return body.toString()
    }

    /** The assistant's text from a chat completion (content may be a string or a parts array). */
    fun parseChatReply(body: String): String {
        val root = parseObject(body)
        // Some providers report failures inside a 200 response
        root.obj("error")?.string("message")?.let { throw AiError.Server(200, it) }
        val message = root.get("choices")?.takeIf { it.isJsonArray }?.asJsonArray
            ?.firstOrNull()?.takeIf { it.isJsonObject }?.asJsonObject?.obj("message")
            ?: throw AiError.BadResponse()
        val content = message.get("content") ?: throw AiError.BadResponse()
        val text = when {
            content.isJsonPrimitive -> content.asString
            content.isJsonArray -> content.asJsonArray.mapNotNull {
                it.takeIf { part -> part.isJsonObject }?.asJsonObject?.string("text")
            }.joinToString("")
            else -> null
        }
        return text?.takeIf { it.isNotBlank() } ?: throw AiError.BadResponse()
    }

    /** Maps a failed HTTP response to the error the user sees. */
    fun errorFor(code: Int, body: String?, retryAfterHeader: String?): AiError {
        val detail = body?.let {
            runCatching { parseObject(it).obj("error")?.string("message") }.getOrNull()
        }?.takeIf { it.isNotBlank() }
        return when (code) {
            401, 403 -> AiError.InvalidKey()
            402 -> AiError.NoCredits(detail)
            429 -> AiError.RateLimited(retryAfterHeader?.trim()?.toLongOrNull())
            else -> AiError.Server(code, detail)
        }
    }

    private fun parseObject(body: String): JsonObject {
        val json = try {
            JsonParser.parseString(body)
        } catch (e: Exception) {
            throw AiError.BadResponse()
        }
        return json.takeIf { it.isJsonObject }?.asJsonObject ?: throw AiError.BadResponse()
    }

    private fun JsonObject.obj(key: String): JsonObject? = get(key)?.takeIf { it.isJsonObject }?.asJsonObject
    private fun JsonObject.string(key: String): String? = get(key)?.primitive()?.asString
    private fun JsonObject.double(key: String): Double? =
        get(key)?.primitive()?.let { runCatching { it.asDouble }.getOrNull() }

    private fun JsonElement.primitive() = takeIf { it.isJsonPrimitive }?.asJsonPrimitive
}

object AiKeyFormat {
    /** OpenRouter keys start with "sk-or-"; anything else is almost certainly a paste mistake. */
    fun looksValid(key: String): Boolean = key.trim().let { it.startsWith("sk-or-") && it.length >= 20 }

    /** "sk-or-…a1b2": enough to recognise the key without showing it. */
    fun mask(key: String): String = key.trim().let { if (it.length <= 10) "••••" else "${it.take(6)}…${it.takeLast(4)}" }
}
