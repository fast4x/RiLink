package it.fast4x.rilink.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.PlayerConstants
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.YouTubePlayer
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.listeners.AbstractYouTubePlayerListener
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.options.IFramePlayerOptions
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.views.YouTubePlayerView
import it.fast4x.rilink.MainActivity
import it.fast4x.rilink.R
import it.fast4x.rilink.models.PlayerState
import it.fast4x.rilink.service.LinkServiceWeb
import it.fast4x.rilink.service.LinkServiceWebWS
import it.fast4x.rilink.ui.customui.CustomDefaultPlayerUiController
import it.fast4x.rilink.utils.DeviceInfo
import it.fast4x.rilink.utils.getDeviceInfo
import it.fast4x.rilink.utils.isLandscape
import it.fast4x.rilink.utils.lastVideoIdKey
import it.fast4x.rilink.utils.lastVideoSecondsKey
import it.fast4x.rilink.utils.rememberPreference
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import timber.log.Timber

@Composable
fun Player(
    innerPadding: PaddingValues
) {

    val context = LocalContext.current
    val inflatedView = LayoutInflater.from(context).inflate(R.layout.youtube_player, null, false)
    val onlinePlayerView: YouTubePlayerView = inflatedView as YouTubePlayerView
    val player = remember { mutableStateOf<YouTubePlayer?>(null) }
    val playerState = remember { mutableStateOf(PlayerConstants.PlayerState.UNSTARTED) }
    var currentSecond by remember { mutableFloatStateOf(0f) }
    var currentDuration by remember { mutableFloatStateOf(0f) }
    var enableBackgroundPlayback by remember { mutableStateOf(true) }
    val lifecycleOwner = LocalLifecycleOwner.current
    val isLandscape = isLandscape
    var mediaId by remember { mutableStateOf("Tmod0giDy0o") }
    var lastYTVideoId by rememberPreference(key = lastVideoIdKey, defaultValue = "")
    var lastYTVideoSeconds by rememberPreference(key = lastVideoSecondsKey, defaultValue = 0f)

    val linkService = remember {
        LinkServiceWebWS(
            context as MainActivity,
            onCommandLoad = { id, position ->
                Timber.d("RiLink Player Web Command Load: $id @ $position")
                mediaId = id
                player.value?.loadVideo(id, position)
            },
            onCommandPlay = {
                Timber.d("RiLink PlayerWeb Command Play")
                player.value?.play()
            },
            onCommandPause = {
                Timber.d("RiLink Player Web Command Pause")
                player.value?.pause()
            },
            onCommandSeek = { time ->
                Timber.d("RiLink Player Web Command Seek: $time")
                player.value?.seekTo(time)
            }
        )
    }

    var deviceInfo: DeviceInfo? by remember { mutableStateOf(null) }
    LaunchedEffect(Unit) {
        linkService.start()
        deviceInfo = getDeviceInfo()
    }
    

    var showPanel by remember { mutableStateOf(true) }
    LaunchedEffect(showPanel) {
        if (showPanel) {
            delay(10000)
            showPanel = false
        }
    }

    Box(
        modifier = Modifier
            .padding(innerPadding)
            .fillMaxSize()
            .background(Color.Transparent)
    ) {
        AnimatedVisibility(
            modifier = Modifier
                .zIndex(1f)
                .align(Alignment.Center),
            visible = (showPanel || playerState.value != PlayerConstants.PlayerState.PLAYING),
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Box(
                modifier = Modifier
                    .background(Color.Black)
                    .fillMaxSize()
//                    .fillMaxWidth(0.9f)
//                    .fillMaxHeight(0.8f)
            ) {

                Text(
                    text = deviceInfo?.let {
                        "Device:${it.deviceBrand} ${it.deviceModel} (${it.deviceName})"
                    } ?: "",
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(top = 5.dp)
                )
                Text(
                    text = linkService.ipAddress()?.let { "Address:$it" } ?: "",
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(top = 30.dp)
                )
                Text(
                    text = "RiLink",
                    fontSize = MaterialTheme.typography.titleLarge.fontSize,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(top = 5.dp)
                )
                Image(
                    painter = painterResource(R.drawable.cast_connected),
                    contentDescription = "Link Connected",
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 5.dp, end = 5.dp)
                        .size(36.dp),
                    colorFilter = ColorFilter.tint(Color.White)
                )
            }
        }

        AndroidView(
            modifier = Modifier
                .background(Color.Transparent)
                .zIndex(0f),
            factory = {
                if (onlinePlayerView.parent != null) {
                    (onlinePlayerView.parent as ViewGroup).removeView(onlinePlayerView) // <- fix
                }
//                val iFramePlayerOptions = IFramePlayerOptions.Builder()
//                    .controls(1) // show/hide controls
//                    .rel(0) // related video at the end
//                    .ivLoadPolicy(0) // show/hide annotations
//                    .ccLoadPolicy(0) // show/hide captions
//                    // Play a playlist by id
//                    //.listType("playlist")
//                    //.list(PLAYLIST_ID)
//                    .build()

                // Disable default view controls to set custom view
                val iFramePlayerOptions = IFramePlayerOptions.Builder()
                    .controls(0) // show/hide controls
                    .listType("playlist")
                    .origin("https://music.youtube.com")
                    .build()

                val listener = object : AbstractYouTubePlayerListener() {

                    override fun onReady(youTubePlayer: YouTubePlayer) {
                        player.value = youTubePlayer

                        /* Used to show custom player ui with uiController as listener
//                            val customPlayerUiController = CustomBasePlayerUiControllerAsListener(
//                                it,
//                                customPLayerUi,
//                                youTubePlayer,
//                                onlinePlayerView,
//                                onTap = {
//                                    showControls = !showControls
//                                }
//                            )
//                            youTubePlayer.addListener(customPlayerUiController)
*/

// Used to show default player ui with defaultPlayerUiController as custom view
                        val customUiController =
                            CustomDefaultPlayerUiController(
                                onlinePlayerView,
                                youTubePlayer,
                                onTap = {
                                    showPanel = !showPanel
                                }
                            )
                        customUiController.showUi(false) // disable all default controls and buttons
                        customUiController.showMenuButton(false)
                        customUiController.showVideoTitle(false)
                        customUiController.showPlayPauseButton(false)
                        customUiController.showDuration(false)
                        customUiController.showCurrentTime(false)
                        customUiController.showSeekBar(false)
                        customUiController.showBufferingProgress(false)
                        customUiController.showYouTubeButton(false)
                        customUiController.showFullscreenButton(false)
                        onlinePlayerView.setCustomPlayerUi(customUiController.rootView)

                        // not required to load by default
//                        if (playerState.value == PlayerConstants.PlayerState.UNSTARTED
//                            || playerState.value != PlayerConstants.PlayerState.BUFFERING
//                        )
//                            youTubePlayer.loadVideo(
//                                mediaId,
//                                if (mediaId == getLastYTVideoId()) getLastYTVideoSeconds() else 0f
//                            )

                        //youTubePlayer.cueVideo(mediaId, 0f)

                        CoroutineScope(Dispatchers.IO).launch {
                            linkService.broadcastState(PlayerState(mediaId, false, 0f, 0f))
                        }


                    }

                    override fun onCurrentSecond(
                        youTubePlayer: YouTubePlayer,
                        second: Float
                    ) {
                        super.onCurrentSecond(youTubePlayer, second)
                        currentSecond = second
                        lastYTVideoSeconds = second
                        lastYTVideoId = mediaId

                        if (playerState.value == PlayerConstants.PlayerState.PLAYING) {
                            CoroutineScope(Dispatchers.IO).launch {
                                linkService.broadcastState(
                                    PlayerState(
                                        mediaId = mediaId,
                                        isPlaying = true,
                                        currentTime = second,
                                        duration = currentDuration
                                    )
                                )
                            }
                        }
                    }

                    override fun onVideoDuration(
                        youTubePlayer: YouTubePlayer,
                        duration: Float
                    ) {
                        //super.onVideoDuration(youTubePlayer, duration)
                        currentDuration = duration
                    }

                    override fun onStateChange(
                        youTubePlayer: YouTubePlayer,
                        state: PlayerConstants.PlayerState
                    ) {
                        //super.onStateChange(youTubePlayer, state)
//                        if (state == PlayerConstants.PlayerState.ENDED) {
//                            onVideoEnded()
//                        }
                        playerState.value = state

                        val isPlaying = state == PlayerConstants.PlayerState.PLAYING
                        val isEnded = state == PlayerConstants.PlayerState.ENDED

                        CoroutineScope(Dispatchers.IO).launch {
                            linkService.broadcastState(
                                PlayerState(
                                    mediaId = mediaId,
                                    isPlaying = isPlaying,
                                    currentTime = currentSecond,
                                    duration = currentDuration
                                )
                            )
                        }

                    }

                    override fun onPlaybackQualityChange(
                        youTubePlayer: YouTubePlayer,
                        playbackQuality: PlayerConstants.PlaybackQuality
                    ) {
                        //super.onPlaybackQualityChange(youTubePlayer, playbackQuality)
                        Timber.d("RiLink Player onPlaybackQualityChange $playbackQuality")
                    }

                    override fun onError(
                        youTubePlayer: YouTubePlayer,
                        error: PlayerConstants.PlayerError
                    ) {
                        //super.onError(youTubePlayer, error)
                        Timber.d("RiLink Player onError $error")
                    }


                }

                onlinePlayerView.apply {
                    enableAutomaticInitialization = false

                    if (enableBackgroundPlayback)
                        enableBackgroundPlayback(true)
                    else
                        lifecycleOwner.lifecycle.addObserver(this)

                    initialize(listener, iFramePlayerOptions)
                }

            },
            update = {
                it.enableBackgroundPlayback(enableBackgroundPlayback)
                it.layoutParams =
                    ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                    )
            }
        )
    }

}