package it.fast4x.ritune.models

import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.PlayerConstants
import kotlinx.serialization.Serializable


@Serializable
data class PlayerState(
    val mediaId: String? = null,
    val isPlaying: Boolean = false,
    val currentTime: Float = 0f,
    val duration: Float = 0f,
    val title: String? = null,
    val state: PlayerConstants.PlayerState = PlayerConstants.PlayerState.UNSTARTED
)

@Serializable
data class RemoteCommand(
    val action: String,
    val mediaId: String? = null,
    val position: Float? = null
)