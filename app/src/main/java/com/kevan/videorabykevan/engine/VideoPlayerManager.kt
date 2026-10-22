package com.kevan.videorabykevan.engine

import android.content.Context
import android.net.Uri
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.kevan.videorabykevan.model.VideoClip
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class PlayerState(
    val isPlaying: Boolean = false,
    val currentPositionMs: Long = 0L,
    val totalDurationMs: Long = 0L,
    val activeClipIndex: Int = 0,
    val isBuffering: Boolean = false
)

class VideoPlayerManager(
    private val context: Context,
    private val coroutineScope: CoroutineScope
) {
    val exoPlayer: ExoPlayer = ExoPlayer.Builder(context).build()

    private val _playerState = MutableStateFlow(PlayerState())
    val playerState: StateFlow<PlayerState> = _playerState.asStateFlow()

    private var currentClips: List<VideoClip> = emptyList()
    private var progressTrackingJob: Job? = null

    init {
        exoPlayer.repeatMode = Player.REPEAT_MODE_OFF

        // Configure audio attributes with automatic audio focus handling.
        // When another app plays sound or the user navigates away, playback pauses cleanly.
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(C.USAGE_MEDIA)
            .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
            .build()
        exoPlayer.setAudioAttributes(audioAttributes, true)

        exoPlayer.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                updateState()
                if (isPlaying) {
                    startProgressTracker()
                } else {
                    stopProgressTracker()
                }
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                updateState()
                if (playbackState == Player.STATE_ENDED) {
                    // When playback completes, stop and reset to start (do not play in background or endlessly loop)
                    seekToTimeline(0L)
                    exoPlayer.pause()
                }
            }

            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                updateState()
            }
        })
    }

    fun setClips(clips: List<VideoClip>, keepPosition: Boolean = true) {
        val previousPosition = _playerState.value.currentPositionMs
        this.currentClips = clips

        if (clips.isEmpty()) {
            exoPlayer.clearMediaItems()
            _playerState.value = PlayerState()
            return
        }

        val mediaItems = clips.map { clip ->
            val builder = MediaItem.Builder().setUri(Uri.parse(clip.uri))
            val isTrimmed = clip.trimStartMs > 0L || (clip.trimEndMs > 0L && clip.trimEndMs < clip.durationMs)
            if (isTrimmed) {
                val endMs = if (clip.trimEndMs > 0L) clip.trimEndMs else clip.durationMs
                val clippingConfig = MediaItem.ClippingConfiguration.Builder()
                    .setStartPositionMs(clip.trimStartMs)
                    .setEndPositionMs(endMs)
                    .build()
                builder.setClippingConfiguration(clippingConfig)
            }
            builder.build()
        }

        exoPlayer.setMediaItems(mediaItems)
        exoPlayer.prepare()

        if (keepPosition && previousPosition > 0) {
            seekToTimeline(previousPosition)
        } else {
            seekToTimeline(0L)
        }
        updateState()
    }

    fun togglePlayPause() {
        if (exoPlayer.isPlaying) {
            exoPlayer.pause()
        } else {
            if (exoPlayer.playbackState == Player.STATE_ENDED) {
                seekToTimeline(0L)
            }
            exoPlayer.play()
        }
    }

    fun play() {
        if (exoPlayer.playbackState == Player.STATE_ENDED) {
            seekToTimeline(0L)
        }
        exoPlayer.play()
    }

    fun pause() {
        exoPlayer.pause()
    }

    fun seekToTimeline(positionMs: Long) {
        if (currentClips.isEmpty()) return

        val totalDuration = currentClips.sumOf { it.playbackDurationMs }
        val clamped = positionMs.coerceIn(0L, totalDuration)

        var accumulatedMs = 0L
        for (i in currentClips.indices) {
            val clip = currentClips[i]
            val clipDuration = clip.playbackDurationMs
            if (clamped <= accumulatedMs + clipDuration || i == currentClips.lastIndex) {
                val offsetWithinClip = (clamped - accumulatedMs).coerceAtLeast(0L)
                val sourceOffset = (offsetWithinClip * clip.speed).toLong()

                // Update clip-specific playback speed and volume
                exoPlayer.playbackParameters = PlaybackParameters(clip.speed)
                exoPlayer.volume = if (clip.isMuted) 0f else clip.volume

                exoPlayer.seekTo(i, sourceOffset)
                break
            }
            accumulatedMs += clipDuration
        }
        updateState()
    }

    fun stepBy(deltaMs: Long) {
        val current = _playerState.value.currentPositionMs
        seekToTimeline(current + deltaMs)
    }

    private fun startProgressTracker() {
        progressTrackingJob?.cancel()
        progressTrackingJob = coroutineScope.launch(Dispatchers.Main) {
            while (isActive) {
                updateState()
                delay(33) // ~30 fps update
            }
        }
    }

    private fun stopProgressTracker() {
        progressTrackingJob?.cancel()
        progressTrackingJob = null
        updateState()
    }

    private fun updateState() {
        if (currentClips.isEmpty()) {
            _playerState.value = PlayerState()
            return
        }

        val totalDuration = currentClips.sumOf { it.playbackDurationMs }
        val currentMediaIndex = exoPlayer.currentMediaItemIndex.coerceIn(0, currentClips.lastIndex)

        // Calculate composite timeline position
        var precedingMs = 0L
        for (i in 0 until currentMediaIndex) {
            precedingMs += currentClips[i].playbackDurationMs
        }

        val activeClip = currentClips[currentMediaIndex]
        val activeClipPos = (exoPlayer.currentPosition / if (activeClip.speed > 0f) activeClip.speed else 1f).toLong()
        val compositePos = (precedingMs + activeClipPos).coerceIn(0L, totalDuration)

        _playerState.value = PlayerState(
            isPlaying = exoPlayer.isPlaying,
            currentPositionMs = compositePos,
            totalDurationMs = totalDuration,
            activeClipIndex = currentMediaIndex,
            isBuffering = exoPlayer.playbackState == Player.STATE_BUFFERING
        )
    }

    fun release() {
        stopProgressTracker()
        exoPlayer.release()
    }
}
