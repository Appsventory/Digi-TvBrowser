package com.appsventory.digiapps.scour.ui.player

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.media3.common.C
import androidx.media3.common.PlaybackException
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.TrackSelectionParameters
import androidx.media3.common.Tracks
import androidx.media3.common.VideoSize
import com.appsventory.digiapps.scour.player.PlayerConfig
import kotlinx.coroutines.delay

/** Compose-observable mirror of a [Player]. Read-only: the screen sends commands to the player. */
@Stable
class PlayerState(private val player: Player) {
    var isPlaying by mutableStateOf(player.isPlaying)
        private set
    var playbackState by mutableIntStateOf(player.playbackState)
        private set
    var positionMs by mutableLongStateOf(0L)
        private set
    var bufferedMs by mutableLongStateOf(0L)
        private set
    /** 0 when unknown (live / not prepared yet). */
    var durationMs by mutableLongStateOf(0L)
        private set
    var isLive by mutableStateOf(false)
        private set
    var isSeekable by mutableStateOf(false)
        private set
    var tracks by mutableStateOf(player.currentTracks)
        private set
    var params by mutableStateOf(player.trackSelectionParameters)
        private set
    var speed by mutableFloatStateOf(player.playbackParameters.speed)
        private set
    var error by mutableStateOf<PlaybackException?>(null)
        private set
    var videoAspect by mutableFloatStateOf(PlayerConfig.PIP_DEFAULT_RATIO)
        private set

    val listener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            this@PlayerState.isPlaying = isPlaying
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            this@PlayerState.playbackState = playbackState
            if (playbackState == Player.STATE_READY) error = null
        }

        override fun onTracksChanged(tracks: Tracks) {
            this@PlayerState.tracks = tracks
        }

        override fun onTrackSelectionParametersChanged(parameters: TrackSelectionParameters) {
            params = parameters
        }

        override fun onPlaybackParametersChanged(playbackParameters: PlaybackParameters) {
            speed = playbackParameters.speed
        }

        override fun onPlayerError(error: PlaybackException) {
            this@PlayerState.error = error
        }

        override fun onVideoSizeChanged(videoSize: VideoSize) {
            if (videoSize.width > 0 && videoSize.height > 0) {
                videoAspect = videoSize.width * videoSize.pixelWidthHeightRatio / videoSize.height
            }
        }
    }

    fun refreshProgress() {
        positionMs = player.currentPosition
        bufferedMs = player.bufferedPosition
        val d = player.duration
        durationMs = if (d == C.TIME_UNSET || d < 0L) 0L else d
        isLive = player.isCurrentMediaItemLive
        isSeekable = player.isCurrentMediaItemSeekable
    }

    fun clearError() {
        error = null
    }
}

@Composable
fun rememberPlayerState(player: Player): PlayerState {
    val state = remember(player) { PlayerState(player) }
    DisposableEffect(player) {
        player.addListener(state.listener)
        state.refreshProgress()
        onDispose { player.removeListener(state.listener) }
    }
    LaunchedEffect(player) {
        while (true) {
            state.refreshProgress()
            delay(PlayerConfig.PROGRESS_POLL_MS)
        }
    }
    return state
}
