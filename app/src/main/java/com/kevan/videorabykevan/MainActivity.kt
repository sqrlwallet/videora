package com.kevan.videorabykevan

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.view.WindowCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kevan.videorabykevan.ui.screens.EditorScreen
import com.kevan.videorabykevan.ui.screens.HomeScreen
import com.kevan.videorabykevan.ui.theme.DarkBackground
import com.kevan.videorabykevan.ui.theme.VideoraByKevanTheme
import com.kevan.videorabykevan.ui.viewmodel.EditorViewModel
import com.kevan.videorabykevan.ui.viewmodel.HomeViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        try {
            WindowCompat.getInsetsController(window, window.decorView).apply {
                isAppearanceLightStatusBars = false
                isAppearanceLightNavigationBars = false
            }
        } catch (_: Exception) {}

        setContent {
            VideoraByKevanTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = DarkBackground
                ) {
                    VideoraApp()
                }
            }
        }
    }
}

@Composable
fun VideoraApp() {
    var activeProjectId by remember { mutableStateOf<String?>(null) }
    val homeViewModel: HomeViewModel = viewModel()

    AnimatedContent(
        targetState = activeProjectId,
        transitionSpec = {
            if (targetState != null) {
                (slideInHorizontally { width -> width } + fadeIn()).togetherWith(
                    slideOutHorizontally { width -> -width } + fadeOut()
                )
            } else {
                (slideInHorizontally { width -> -width } + fadeIn()).togetherWith(
                    slideOutHorizontally { width -> width } + fadeOut()
                )
            }
        },
        label = "AppScreenTransition"
    ) { projectId ->
        if (projectId == null) {
            HomeScreen(
                viewModel = homeViewModel,
                onOpenProject = { id ->
                    activeProjectId = id
                }
            )
        } else {
            BackHandler {
                activeProjectId = null
                homeViewModel.loadProjects()
            }

            // Scoped specifically to this active project
            val editorViewModel: EditorViewModel = viewModel(key = projectId)

            EditorScreen(
                projectId = projectId,
                viewModel = editorViewModel,
                onNavigateBack = {
                    activeProjectId = null
                    homeViewModel.loadProjects()
                }
            )
        }
    }
}
