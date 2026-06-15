package it.fast4x.ritune.ui

import android.graphics.Bitmap
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
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
import it.fast4x.ritune.utils.TrackMetadata
import it.fast4x.ritune.utils.TrackMetadataResolver
import it.fast4x.ritune.utils.getDeviceInfo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber

@Composable
fun Player(innerPadding: PaddingValues) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val inflatedView = remember(context) {
        LayoutInflater.from(context).inflate(R.layout.youtube_player, null, false)
    }
    val onlinePlayerView = inflatedView as YouTubePlayerView

    val player = remember { mutableStateOf<YouTubePlayer?>(null) }
    val playerState = remember { mutableStateOf(PlayerConstants.PlayerState.UNSTARTED) }

    var currentSecond by remember { mutableFloatStateOf(0f) }
    var currentDuration by remember { mutableFloatStateOf(0f) }
    var enableBackgroundPlayback by remember { mutableStateOf(true) }

    var mediaId by remember { mutableStateOf("Tmod0giDy0o") }
    var deviceInfo: DeviceInfo? by remember { mutableStateOf(null) }
    var trackMetadata by remember { mutableStateOf<TrackMetadata?>(null) }

    val commandService = remember {
        CommandService(
            context as MainActivity,
            onCommandLoad = { id, position ->
                Timber.d("RiTune Player Web Command Load: $id @ $position")
                mediaId = id
                player.value?.loadVideo(id, position)
            },
            onCommandPlay = { id ->
                Timber.d("RiTune Player Web Command Play id $id")
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

    LaunchedEffect(Unit) {
        commandService.start()
        deviceInfo = getDeviceInfo()
    }

    LaunchedEffect(mediaId) {
        trackMetadata = TrackMetadataResolver.resolve(mediaId)
    }

    val displayTitle = trackMetadata?.title ?: "Riproduzione in corso"
    val displayArtist = trackMetadata?.artist ?: "YouTube"
    val coverBitmap = trackMetadata?.coverBitmap

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(innerPadding)
    ) {
        AndroidView(
            modifier = Modifier
                .size(1.dp)
                .alpha(0f),
            factory = {
                if (onlinePlayerView.parent != null) {
                    (onlinePlayerView.parent as ViewGroup).removeView(onlinePlayerView)
                }

                val iFramePlayerOptions = IFramePlayerOptions.Builder()
                    .controls(0)
                    .listType("playlist")
                    .origin("https://music.youtube.com")
                    .build()

                val listener = object : AbstractYouTubePlayerListener() {
                    override fun onReady(youTubePlayer: YouTubePlayer) {
                        player.value = youTubePlayer

                        val customUiController = CustomDefaultPlayerUiController(
                            onlinePlayerView,
                            youTubePlayer,
                            onTap = { }
                        )

                        customUiController.showUi(false)
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

                        CoroutineScope(Dispatchers.IO).launch {
                            commandService.broadcastState(
                                PlayerState(
                                    mediaId = mediaId,
                                    isPlaying = false,
                                    currentTime = 0f,
                                    duration = 0f,
                                    title = displayTitle,
                                    state = playerState.value
                                )
                            )
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
                                        title = displayTitle,
                                        state = playerState.value
                                    )
                                )
                            }
                        }

                        if (commandService.connections.isEmpty()) {
                            player.value?.pause()
                        }
                    }

                    override fun onVideoDuration(
                        youTubePlayer: YouTubePlayer,
                        duration: Float
                    ) {
                        currentDuration = duration
                    }

                    override fun onStateChange(
                        youTubePlayer: YouTubePlayer,
                        state: PlayerConstants.PlayerState
                    ) {
                        playerState.value = state

                        val isPlaying = state == PlayerConstants.PlayerState.PLAYING
                        CoroutineScope(Dispatchers.IO).launch {
                            commandService.broadcastState(
                                PlayerState(
                                    mediaId = mediaId,
                                    isPlaying = isPlaying,
                                    currentTime = currentSecond,
                                    duration = currentDuration,
                                    title = displayTitle,
                                    state = playerState.value
                                )
                            )
                        }
                    }

                    override fun onPlaybackQualityChange(
                        youTubePlayer: YouTubePlayer,
                        playbackQuality: PlayerConstants.PlaybackQuality
                    ) {
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
                                    title = displayTitle,
                                    state = playerState.value
                                )
                            )
                        }
                        Timber.d("RiTune Player onError $error")
                    }
                }

                onlinePlayerView.apply {
                    enableAutomaticInitialization = false
                    if (enableBackgroundPlayback) {
                        enableBackgroundPlayback(true)
                    } else {
                        lifecycleOwner.lifecycle.addObserver(this)
                    }
                    initialize(listener, iFramePlayerOptions)
                }
            },
            update = {
                it.enableBackgroundPlayback(enableBackgroundPlayback)
                it.layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
            }
        )

        if (playerState.value == PlayerConstants.PlayerState.PLAYING) {
            NowPlayingScreen(
                title = displayTitle,
                artist = displayArtist,
                coverBitmap = coverBitmap,
                currentSecond = currentSecond,
                currentDuration = currentDuration,
                mediaId = mediaId,
                commandService = commandService,
                deviceInfo = deviceInfo
            )
        } else {
            WelcomeScreen(
                commandService = commandService,
                deviceInfo = deviceInfo
            )
        }
    }
}

@Composable
private fun WelcomeScreen(
    commandService: CommandService,
    deviceInfo: DeviceInfo?
) {
    val infiniteTransition = rememberInfiniteTransition(label = "castPulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.08f,
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

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF0A0A0A),
                        Color(0xFF101010),
                        Color(0xFF000000)
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(24.dp)
                .alpha(0.6f)
        ) {
            Text(
                text = deviceInfo?.let { "${it.deviceBrand} ${it.deviceModel}" } ?: "Device Unknown",
                color = Color.White,
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = commandService.ipAddress()?.let { "IP: $it" } ?: "IP: Resolving...",
                color = Color.White,
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace
            )
        }

        Column(
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.align(Alignment.Center)
        ) {
            Image(
                painter = painterResource(R.drawable.ic_launcher_foreground),
                contentDescription = "RiTune Logo",
                modifier = Modifier
                    .size(120.dp)
                    .padding(bottom = 16.dp),
                colorFilter = ColorFilter.tint(Color.White)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "RiTune",
                fontSize = 48.sp,
                fontWeight = FontWeight.Light,
                color = Color.White,
                letterSpacing = 4.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Ready to Cast",
                fontSize = 14.sp,
                fontWeight = FontWeight.Normal,
                color = Color.White.copy(alpha = 0.5f),
                letterSpacing = 2.sp
            )
        }

        StatusIcon(
            connected = commandService.connections.isNotEmpty(),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(24.dp)
        )
    }
}

@Composable
private fun NowPlayingScreen(
    title: String,
    artist: String,
    coverBitmap: Bitmap?,
    currentSecond: Float,
    currentDuration: Float,
    mediaId: String,
    commandService: CommandService,
    deviceInfo: DeviceInfo?
) {
    val progress = if (currentDuration > 0f) {
        (currentSecond / currentDuration).coerceIn(0f, 1f)
    } else {
        0f
    }

    val infiniteTransition = rememberInfiniteTransition(label = "coverPulse")
    val coverScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.02f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "coverScale"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF090909),
                        Color(0xFF111111),
                        Color(0xFF000000)
                    )
                )
            )
    ) {
        Row(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth()
                .padding(horizontal = 56.dp),
            horizontalArrangement = Arrangement.spacedBy(44.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(320.dp)
                    .scale(coverScale)
                    .clip(RoundedCornerShape(32.dp))
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                Color(0xFF2A2A2A),
                                Color(0xFF161616),
                                Color(0xFF0E0E0E)
                            )
                        )
                    )
                    .border(
                        width = 1.dp,
                        color = Color.White.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(32.dp)
                    )
            ) {
                if (coverBitmap != null) {
                    Image(
                        bitmap = coverBitmap.asImageBitmap(),
                        contentDescription = "Cover art",
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Text(
                        text = title.trim().firstOrNull()?.uppercaseChar()?.toString() ?: "♪",
                        modifier = Modifier.align(Alignment.Center),
                        fontSize = 96.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White.copy(alpha = 0.10f)
                    )

                    Image(
                        painter = painterResource(R.drawable.ic_launcher_foreground),
                        contentDescription = "Cover art",
                        modifier = Modifier
                            .size(160.dp)
                            .align(Alignment.Center),
                        colorFilter = ColorFilter.tint(Color.White)
                    )
                }
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "NOW PLAYING",
                    color = Color.White.copy(alpha = 0.55f),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 3.sp
                )

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = title,
                    color = Color.White,
                    fontSize = 46.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = artist,
                    color = Color.White.copy(alpha = 0.75f),
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Normal,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(28.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .background(Color.White.copy(alpha = 0.12f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(progress)
                            .fillMaxHeight()
                            .background(Color.White.copy(alpha = 0.95f))
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                Text(
                    text = formatDuration(currentSecond) + " / " + formatDuration(currentDuration),
                    color = Color.White.copy(alpha = 0.50f),
                    fontSize = 14.sp
                )

            }
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(24.dp)
                .alpha(0.6f)
        ) {
            Text(
                text = deviceInfo?.let { "${it.deviceBrand} ${it.deviceModel}" } ?: "Device Unknown",
                color = Color.White,
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = commandService.ipAddress()?.let { "IP: $it" } ?: "IP: Resolving...",
                color = Color.White,
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace
            )
        }

        StatusIcon(
            connected = commandService.connections.isNotEmpty(),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(24.dp)
        )
    }
}

private fun formatDuration(seconds: Float): String {
    val totalSeconds = seconds.toInt()
    val minutes = totalSeconds / 60
    val remainingSeconds = totalSeconds % 60
    return String.format("%d:%02d", minutes, remainingSeconds)
}

@Composable
private fun StatusIcon(
    connected: Boolean,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "statusPulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.08f,
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
        painter = painterResource(
            if (connected) R.drawable.cast_connected else R.drawable.cast_disconnected
        ),
        contentDescription = "Cast Status",
        modifier = modifier
            .size(42.dp)
            .scale(scale)
            .alpha(alpha),
        colorFilter = ColorFilter.tint(Color.White)
    )
}