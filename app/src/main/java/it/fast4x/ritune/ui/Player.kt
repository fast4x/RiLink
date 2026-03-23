package it.fast4x.ritune.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.ui.graphics.Brush
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
import it.fast4x.ritune.MainActivity
import it.fast4x.ritune.R
import it.fast4x.ritune.models.PlayerState
import it.fast4x.ritune.service.CommandService
import it.fast4x.ritune.ui.customui.CustomDefaultPlayerUiController
import it.fast4x.ritune.utils.DeviceInfo
import it.fast4x.ritune.utils.getDeviceInfo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import timber.log.Timber
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex

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
    var mediaId by remember { mutableStateOf("Tmod0giDy0o") }

    val commandService = remember {
        CommandService(
            context as MainActivity,
            onCommandLoad = { id, position ->
                Timber.d("RiTune Player Web Command Load: $id @ $position")
                mediaId = id
                player.value?.loadVideo(id, position)
            },
            onCommandPlay = { id ->
                Timber.d("RiTune PlayerWeb Command Play id $id")
                if (mediaId != id) {
                    mediaId = id
                    player.value?.loadVideo(id, 0f)
                } else {
                    player.value?.play()
                }
            },
            onCommandPause = {
                Timber.d("RiTune Player Web Command Pause")
                player.value?.pause()
            },
            onCommandSeek = { time ->
                Timber.d("RiTune Player Web Command Seek: $time")
                player.value?.seekTo(time)
            }
        )
    }

    var deviceInfo: DeviceInfo? by remember { mutableStateOf(null) }
    LaunchedEffect(Unit) {
        commandService.start()
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
            //.padding(innerPadding)
            .fillMaxSize()
            .background(Color.Transparent)
    ) {


        AnimatedVisibility(
            modifier = Modifier
                .zIndex(1f)
                .align(Alignment.Center),
            visible = (showPanel || playerState.value != PlayerConstants.PlayerState.PLAYING),
            enter = fadeIn(animationSpec = tween(1000)),
            exit = fadeOut(animationSpec = tween(500))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    // 1. SFONDO: Gradiente profondo invece di nero pieno
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFF0a0a0a), // Nero profondo
                                Color(0xFF101010), // Grigio molto scuro
                                Color(0xFF000000)  // Nero puro in basso
                            )
                        )
                    )
            ) {
                // 2. INFO DISPOSITIVO (In basso a sinistra, stile terminale)
                // Spostato qui per non occupare spazio visivo in alto
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(24.dp)
                        .alpha(0.6f) // Leggermente trasparente per non distrarre
                ) {
                    Text(
                        text = deviceInfo?.let { "${it.deviceBrand} ${it.deviceModel}" } ?: "Device Unknown",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace, // Stile "coder" molto professionale
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = commandService.ipAddress()?.let { "IP: $it" } ?: "IP: Resolving...",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }

                // 3. CENTRO: Brand e Logo (Hero Section)
                Column(
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.align(Alignment.Center)
                ) {
                    // Logo più grande con "Glow" (bagliore)
                    Image(
                        painter = painterResource(R.drawable.ic_launcher_foreground),
                        contentDescription = "RiTune Logo",
                        modifier = Modifier
                            .size(120.dp) // Più grande per le TV
                            .padding(bottom = 16.dp),
                        colorFilter = ColorFilter.tint(Color.White) // Assicura che sia bianco
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "RiTune",
                        fontSize = 48.sp, // Dimensione cinematografica
                        fontWeight = FontWeight.Light, // Peso leggero per eleganza
                        color = Color.White,
                        letterSpacing = 4.sp // Spaziatura lettere per modernità
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Stato
                    Text(
                        text = "Ready to Cast",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Normal,
                        color = Color.White.copy(alpha = 0.5f),
                        letterSpacing = 2.sp
                    )
                }

                // 4. ICONA CAST ANIMATA (In basso a destra)
                // Animazione "Breathing" per indicare attesa attiva
                val infiniteTransition = rememberInfiniteTransition(label = "castPulse")
                val scale by infiniteTransition.animateFloat(
                    initialValue = 1f,
                    targetValue = 1.1f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(1000, easing = LinearEasing),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "scale"
                )
                val alpha by infiniteTransition.animateFloat(
                    initialValue = 0.6f,
                    targetValue = 1f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(1000, easing = LinearEasing),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "alpha"
                )

                Image(
                    painter = painterResource(if (commandService.connections.size > 0) R.drawable.cast_connected else R.drawable.cast_disconnected),
                    contentDescription = "Cast Status",
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(24.dp)
                        .size(42.dp)
                        .scale(scale) // Animazione dimensione
                        .alpha(alpha), // Animazione opacità
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
                            commandService.broadcastState(PlayerState(mediaId, false, 0f, 0f, state = playerState.value))
                        }


                    }

                    override fun onCurrentSecond(
                        youTubePlayer: YouTubePlayer,
                        second: Float
                    ) {
                        super.onCurrentSecond(youTubePlayer, second)
                        currentSecond = second

                        if (playerState.value == PlayerConstants.PlayerState.PLAYING) {
                            CoroutineScope(Dispatchers.IO).launch {
                                commandService.broadcastState(
                                    PlayerState(
                                        mediaId = mediaId,
                                        isPlaying = true,
                                        currentTime = second,
                                        duration = currentDuration,
                                        state = playerState.value
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
                            commandService.broadcastState(
                                PlayerState(
                                    mediaId = mediaId,
                                    isPlaying = isPlaying,
                                    currentTime = currentSecond,
                                    duration = currentDuration,
                                    state = playerState.value
                                )
                            )
                        }

                    }

                    override fun onPlaybackQualityChange(
                        youTubePlayer: YouTubePlayer,
                        playbackQuality: PlayerConstants.PlaybackQuality
                    ) {
                        //super.onPlaybackQualityChange(youTubePlayer, playbackQuality)
                        Timber.d("RiTune Player onPlaybackQualityChange $playbackQuality")
                    }

                    override fun onError(
                        youTubePlayer: YouTubePlayer,
                        error: PlayerConstants.PlayerError
                    ) {
                        CoroutineScope(Dispatchers.IO).launch {
                            commandService.broadcastState(
                                PlayerState(
                                    mediaId = mediaId,
                                    isPlaying = false,
                                    currentTime = currentSecond,
                                    duration = currentDuration,
                                    state = playerState.value
                                )
                            )
                        }
                        //super.onError(youTubePlayer, error)
                        Timber.d("RiTune Player onError $error")
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