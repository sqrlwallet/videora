package com.kevan.videorabykevan.engine

import android.content.Context
import android.net.Uri
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.kevan.videorabykevan.model.AudioTrack
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
import kotlin.math.abs

data class PlayerState(
    val isPlaying: Boolean = false,
    val currentPositionMs: Long = 0L,
    val totalDurationMs: Long = 0L,
    val activeClipIndex: Int = 0,
    val isBuffering: Boolean = false
)

private const val AUDIO_DRIFT_TOLERANCE_MS = 150L

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
                syncAudioTracks(forceSeek = isPlaying)
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
                // Playback rolled into the next clip on its own: apply that clip's speed and volume,
                // otherwise it would keep the previous clip's settings
                currentClips.getOrNull(exoPlayer.currentMediaItemIndex)?.let { applyClipParameters(it) }
                updateState()
                syncAudioTracks(forceSeek = true)
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

                applyClipParameters(clip)
                exoPlayer.seekTo(i, sourceOffset)
                break
            }
            accumulatedMs += clipDuration
        }
        updateState()
        syncAudioTracks(forceSeek = true)
    }

    private fun applyClipParameters(clip: VideoClip) {
        exoPlayer.playbackParameters = PlaybackParameters(clip.speed)
        exoPlayer.volume = if (clip.isMuted) 0f else clip.volume
    }

    // ---------------------------------------------------------------
    // Music / voiceover / SFX preview: one lightweight player per track, kept in sync with the
    // timeline position of the main video player.
    // ---------------------------------------------------------------

    private class AudioTrackPlayer(val track: AudioTrack, val player: ExoPlayer)

    private var audioPlayers: List<AudioTrackPlayer> = emptyList()

    fun setAudioTracks(tracks: List<AudioTrack>) {
        if (tracks == audioPlayers.map { it.track }) return

        // Reuse players whose source is unchanged so editing volume doesn't restart audio
        val reusable = audioPlayers.associateBy { it.track.id }.toMutableMap()
        audioPlayers = tracks.map { track ->
            val existing = reusable.remove(track.id)
            if (existing != null && existing.track.uri == track.uri && existing.track.trimStartMs == track.trimStartMs &&
                existing.track.trimEndMs == track.trimEndMs && existing.track.isLooping == track.isLooping
            ) {
                existing.player.volume = track.volume.coerceIn(0f, 1f)
                AudioTrackPlayer(track, existing.player)
            } else {
                existing?.player?.release()
                AudioTrackPlayer(track, createAudioPlayer(track))
            }
        }
        reusable.values.forEach { it.player.release() }
        syncAudioTracks(forceSeek = true)
    }

    private fun createAudioPlayer(track: AudioTrack): ExoPlayer {
        val player = ExoPlayer.Builder(context).build()
        // Audio focus stays with the main player; these follow its play/pause state
        player.setAudioAttributes(
            AudioAttributes.Builder().setUsage(C.USAGE_MEDIA).setContentType(C.AUDIO_CONTENT_TYPE_MUSIC).build(),
            false
        )
        val builder = MediaItem.Builder().setUri(Uri.parse(track.uri))
        if (track.trimStartMs > 0L || track.trimEndMs > track.trimStartMs) {
            val clipping = MediaItem.ClippingConfiguration.Builder().setStartPositionMs(track.trimStartMs)
            if (track.trimEndMs > track.trimStartMs) clipping.setEndPositionMs(track.trimEndMs)
            builder.setClippingConfiguration(clipping.build())
        }
        player.setMediaItem(builder.build())
        player.repeatMode = if (track.isLooping) Player.REPEAT_MODE_ONE else Player.REPEAT_MODE_OFF
        player.volume = track.volume.coerceIn(0f, 1f)
        player.prepare()
        return player
    }

    /**
     * Starts, stops and repositions each audio player to match the timeline. Small drift is
     * tolerated; [forceSeek] repositions regardless (after seeks and clip changes).
     */
    private fun syncAudioTracks(forceSeek: Boolean = false) {
        if (audioPlayers.isEmpty()) return
        val timelineMs = _playerState.value.currentPositionMs
        val mainPlaying = exoPlayer.isPlaying

        for (entry in audioPlayers) {
            val player = entry.player
            val offsetMs = timelineMs - entry.track.timelineStartOffsetMs
            val lengthMs = player.duration.takeIf { it != C.TIME_UNSET && it > 0L }
            val active = offsetMs >= 0L && (entry.track.isLooping || lengthMs == null || offsetMs < lengthMs)

            if (!active) {
                if (player.playWhenReady) player.pause()
                // Park at the start so it's ready the moment the playhead reaches it
                if (offsetMs < 0L && player.currentPosition != 0L) player.seekTo(0L)
                continue
            }

            // Volume follows the fade ramps; the export applies the same gains (AudioTrack.fadeGainAt)
            val trackLengthMs = entry.track.timelineLengthMs(_playerState.value.totalDurationMs)
            player.volume = entry.track.volume.coerceIn(0f, 1f) * entry.track.fadeGainAt(offsetMs, trackLengthMs)

            val targetMs = if (entry.track.isLooping && lengthMs != null) offsetMs % lengthMs else offsetMs
            if (forceSeek || abs(player.currentPosition - targetMs) > AUDIO_DRIFT_TOLERANCE_MS) {
                player.seekTo(targetMs)
            }
            if (mainPlaying != player.playWhenReady) {
                player.playWhenReady = mainPlaying
            }
        }
    }

    private fun releaseAudioPlayers() {
        audioPlayers.forEach { it.player.release() }
        audioPlayers = emptyList()
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
                syncAudioTracks()
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
        releaseAudioPlayers()
        exoPlayer.release()
    }
}
