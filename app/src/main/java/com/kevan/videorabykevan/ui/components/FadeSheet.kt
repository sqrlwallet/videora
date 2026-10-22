package com.kevan.videorabykevan.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kevan.videorabykevan.model.AudioTrack
import com.kevan.videorabykevan.ui.theme.DarkSurface
import com.kevan.videorabykevan.ui.theme.GoldChampagne
import java.util.Locale
import kotlin.math.roundToLong

private const val MAX_FADE_SECONDS = 5f

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FadeSheet(
    track: AudioTrack,
    onDismiss: () -> Unit,
    onSetFades: (fadeInMs: Long, fadeOutMs: Long) -> Unit
) {
    var fadeIn by remember { mutableFloatStateOf((track.fadeInMs / 1000f).coerceIn(0f, MAX_FADE_SECONDS)) }
    var fadeOut by remember { mutableFloatStateOf((track.fadeOutMs / 1000f).coerceIn(0f, MAX_FADE_SECONDS)) }
    // Commit once when a drag ends, so one adjustment is one undo step
    val commit = { onSetFades((fadeIn * 1000).roundToLong(), (fadeOut * 1000).roundToLong()) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = DarkSurface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            Text(
                text = "Fade ${track.title}",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Black
            )
            Text(
                text = "Ease the sound in where the track starts and out where it ends.",
                color = Color.White.copy(alpha = 0.6f),
                fontSize = 12.sp,
                modifier = Modifier.padding(top = 4.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))
            FadeSlider("Fade in", fadeIn, onChange = { fadeIn = it }, onFinished = commit)
            FadeSlider("Fade out", fadeOut, onChange = { fadeOut = it }, onFinished = commit)
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun FadeSlider(label: String, seconds: Float, onChange: (Float) -> Unit, onFinished: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Text(label, color = Color.White.copy(alpha = 0.8f), fontSize = 13.sp, modifier = Modifier.weight(1f))
        Text(
            text = if (seconds <= 0f) "Off" else "%.1fs".format(Locale.US, seconds),
            color = GoldChampagne,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
        )
    }
    Slider(
        value = seconds,
        onValueChange = { onChange((it * 2).roundToLong() / 2f) }, // half-second steps
        onValueChangeFinished = onFinished,
        valueRange = 0f..MAX_FADE_SECONDS,
        colors = SliderDefaults.colors(thumbColor = GoldChampagne, activeTrackColor = GoldChampagne)
    )
}
