package app.openflow.audio

import android.media.MediaPlayer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File

enum class PlaybackStatus {
    IDLE,
    PLAYING,
    PAUSED,
    COMPLETED,
    ERROR
}

data class AudioPlaybackState(
    val sessionId: String? = null,
    val status: PlaybackStatus = PlaybackStatus.IDLE,
    val currentPositionMs: Int = 0,
    val totalDurationMs: Int = 0,
)

/**
 * Controller for audio playback in History.
 */
class AudioPlayerController(
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Main)
) {
    private var mediaPlayer: MediaPlayer? = null
    private var progressJob: Job? = null

    private val _state = MutableStateFlow(AudioPlaybackState())
    val state: StateFlow<AudioPlaybackState> = _state.asStateFlow()

    fun play(sessionId: String, audioFile: File) {
        if (!audioFile.exists() || audioFile.length() == 0L) {
            _state.value = AudioPlaybackState(sessionId = sessionId, status = PlaybackStatus.ERROR)
            return
        }

        // If same session is paused, resume
        if (_state.value.sessionId == sessionId && _state.value.status == PlaybackStatus.PAUSED) {
            mediaPlayer?.start()
            _state.value = _state.value.copy(status = PlaybackStatus.PLAYING)
            startProgressUpdates()
            return
        }

        // Stop current
        stop()

        try {
            val player = MediaPlayer().apply {
                setDataSource(audioFile.absolutePath)
                prepare()
                setOnCompletionListener {
                    _state.value = _state.value.copy(
                        status = PlaybackStatus.COMPLETED,
                        currentPositionMs = duration
                    )
                    progressJob?.cancel()
                }
                setOnErrorListener { _, _, _ ->
                    _state.value = _state.value.copy(status = PlaybackStatus.ERROR)
                    progressJob?.cancel()
                    true
                }
            }
            mediaPlayer = player
            player.start()
            _state.value = AudioPlaybackState(
                sessionId = sessionId,
                status = PlaybackStatus.PLAYING,
                currentPositionMs = 0,
                totalDurationMs = player.duration
            )
            startProgressUpdates()
        } catch (e: Exception) {
            _state.value = AudioPlaybackState(sessionId = sessionId, status = PlaybackStatus.ERROR)
        }
    }

    fun pause() {
        if (_state.value.status == PlaybackStatus.PLAYING) {
            mediaPlayer?.pause()
            progressJob?.cancel()
            _state.value = _state.value.copy(status = PlaybackStatus.PAUSED)
        }
    }

    fun seekTo(positionMs: Int) {
        mediaPlayer?.let {
            val safePos = positionMs.coerceIn(0, it.duration)
            it.seekTo(safePos)
            _state.value = _state.value.copy(currentPositionMs = safePos)
        }
    }

    fun stop() {
        progressJob?.cancel()
        progressJob = null
        try {
            mediaPlayer?.stop()
            mediaPlayer?.release()
        } catch (_: Exception) {}
        mediaPlayer = null
        _state.value = AudioPlaybackState()
    }

    private fun startProgressUpdates() {
        progressJob?.cancel()
        progressJob = scope.launch {
            while (isActive) {
                mediaPlayer?.let { player ->
                    if (player.isPlaying) {
                        _state.value = _state.value.copy(
                            currentPositionMs = player.currentPosition,
                            totalDurationMs = player.duration
                        )
                    }
                }
                delay(100)
            }
        }
    }
}
