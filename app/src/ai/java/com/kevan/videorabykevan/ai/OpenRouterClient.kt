package com.kevan.videorabykevan.ai

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

/**
 * [AiClient] over OpenRouter's HTTPS API, using only the platform's HttpURLConnection so the app
 * gains no networking dependency. Every call is made by an explicit user action.
 */
class OpenRouterClient(private val apiKey: String?) : AiClient {

    override suspend fun keyInfo(): AiKeyInfo {
        if (apiKey == null) throw AiError.NoKey()
        return OpenRouterJson.parseKeyInfo(get("/key"))
    }

    override suspend fun models(): List<AiModel> = OpenRouterJson.parseModels(get("/models"))

    private suspend fun get(path: String): String = withContext(Dispatchers.IO) {
        val connection = (URL(BASE_URL + path).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 15_000
            readTimeout = 30_000
            // The model list is public, so it can be browsed before a key is saved
            apiKey?.let { setRequestProperty("Authorization", "Bearer $it") }
            setRequestProperty("Accept", "application/json")
            // Optional OpenRouter attribution headers; they identify the app, not the user
            setRequestProperty("HTTP-Referer", "https://github.com/sqrlwallet/videora")
            setRequestProperty("X-Title", "Videora")
        }
        try {
            val code = connection.responseCode
            if (code in 200..299) {
                connection.inputStream.bufferedReader().use { it.readText() }
            } else {
                val body = connection.errorStream?.bufferedReader()?.use { it.readText() }
                throw OpenRouterJson.errorFor(code, body, connection.getHeaderField("Retry-After"))
            }
        } catch (e: IOException) {
            throw AiError.Offline()
        } finally {
            connection.disconnect()
        }
    }

    private companion object {
        const val BASE_URL = "https://openrouter.ai/api/v1"
    }
}
