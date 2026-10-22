package com.kevan.videorabykevan.ai

import android.content.Context
import androidx.compose.runtime.Composable

/**
 * Offline edition: no AI, no network code and no INTERNET permission. The AI edition's version
 * of this object (src/ai) has the same members, so shared code compiles against either.
 */
object AiEdition {
    const val isAvailable: Boolean = false

    fun client(context: Context): AiClient? = null

    @Composable
    fun SettingsScreen(onBack: () -> Unit) {
        // Never shown: the home screen only offers AI settings when isAvailable is true
    }
}
