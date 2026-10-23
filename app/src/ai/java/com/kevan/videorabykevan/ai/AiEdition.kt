package com.kevan.videorabykevan.ai

import android.content.Context
import androidx.compose.runtime.Composable

/** AI edition entry points; see the offline edition's AiEdition for the no-AI counterpart. */
object AiEdition {
    const val isAvailable: Boolean = true

    /** Null until the user has saved a key, so features can prompt them to add one. */
    fun client(context: Context): AiClient? {
        val store = ApiKeyStore(context)
        return store.load()?.let { key -> OpenRouterClient(apiKey = key, models = store::modelFor) }
    }

    @Composable
    fun SettingsScreen(onBack: () -> Unit) = AiSettingsScreen(onBack = onBack)
}
