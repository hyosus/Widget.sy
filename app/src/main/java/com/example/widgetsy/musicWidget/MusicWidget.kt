package com.example.widgetsy.musicWidget

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.CircularProgressIndicator
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.components.CircleIconButton
import androidx.glance.appwidget.components.Scaffold
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.currentState
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.ContentScale
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import coil3.imageLoader
import coil3.request.CachePolicy
import coil3.request.ErrorResult
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import coil3.toBitmap
import com.example.widgetsy.MainActivity
import com.example.widgetsy.R.drawable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MusicWidget: GlanceAppWidget() {
    override val sizeMode = SizeMode.Exact
    val trackNameKey = stringPreferencesKey("track_name")
    val artistNameKey = stringPreferencesKey("artist_name")
    val albumArtUriKey = stringPreferencesKey("album_art_uri")
    val isPausedKey = booleanPreferencesKey("is_paused")
    val backgroundColorKey = intPreferencesKey("background_color")

    override val stateDefinition = PreferencesGlanceStateDefinition

    @SuppressLint("RestrictedApi")
    override suspend fun provideGlance(
        context: Context,
        id: GlanceId
    ) {
        // Initialize state with default values if not already set
        updateAppWidgetState(context, id) { prefs ->
            if (prefs[trackNameKey] == null) {
                prefs[trackNameKey] = "No track playing"
            }
            if (prefs[artistNameKey] == null) {
                prefs[artistNameKey] = "No artist"
            }
            if (prefs[albumArtUriKey] == null) {
                prefs[albumArtUriKey] = ""
            }
            if (prefs[isPausedKey] == null) {
                prefs[isPausedKey] = true
            }
            if (prefs[backgroundColorKey] == null) {
                prefs[backgroundColorKey] = Color.White.toArgb()
            }
        }

        provideContent {
            val trackName = currentState(key = trackNameKey) ?: "No track playing"
            val artistName = currentState(key = artistNameKey) ?: "No artists"
            val albumArtUri = currentState(key = albumArtUriKey) ?: ""
            val isPaused = currentState(key = isPausedKey) ?: true
            val bgColor = currentState(key = backgroundColorKey) ?: Color.Red.toArgb()

            val textColor = getTextColor(Color(bgColor))

            val size = LocalSize.current.height // Get the current widget size
            Log.d("MusicWidget", "Widget size: $size")

            // Calculate image size based on widget size
            val imageHeight = size - 24.dp

            Log.d("MusicWidget", "BGCOLOR argbColor: $bgColor")
            Log.d("MusicWidget", "Track: $trackName, Artist: $artistName, Album Art: $albumArtUri, Is Paused: $isPaused")

            Scaffold(
                modifier = GlanceModifier.fillMaxSize().padding(vertical = 12.dp, horizontal = 4.dp),
                backgroundColor = ColorProvider(Color(bgColor))
            )
            {
                Row (verticalAlignment = Alignment.CenterVertically) {
                    if (albumArtUri.isNotEmpty()) {
                        Log.d("MusicWidget", "Album Art Uri: ${Uri.parse(albumArtUri)}")

                        ImageWidgetUrlBackgroundThread(
                            albumArtUri,
                            modifier = GlanceModifier.size(imageHeight).cornerRadius(12.dp)
                        )
                    }

                    Spacer(modifier = GlanceModifier.size(10.dp))

                    Column (
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = trackName,
                            style = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.Bold, color = ColorProvider(textColor)),
                            maxLines = 1,
                            modifier = GlanceModifier.padding(start = 14.dp),
                        )
                        Text(
                            text = artistName,
                            style = TextStyle(color = ColorProvider(textColor)),
                            modifier = GlanceModifier.padding(start = 14.dp))

                        Row(
                            modifier = GlanceModifier.fillMaxWidth(),

                        ) {
                            CircleIconButton(
                                imageProvider = ImageProvider(drawable.skip_previous),
                                backgroundColor = null,
                                contentDescription = "",
                                contentColor = ColorProvider(textColor),
                                onClick = actionRunCallback(SkipPreviousCallback::class.java),
                            )

                            Log.d("MusicWidget", "Is paused: $isPaused")

                            if (isPaused) {
                                CircleIconButton(
                                    imageProvider = ImageProvider(drawable.play_arrow),
                                    backgroundColor = null,
                                    contentDescription = "",
                                    contentColor = ColorProvider(textColor),
                                    onClick = actionRunCallback(ResumeCallback::class.java),
                                )
                            } else {
                                CircleIconButton(
                                    imageProvider = ImageProvider(drawable.pause),
                                    backgroundColor = null,
                                    contentDescription = "",
                                    contentColor = ColorProvider(textColor),
                                    onClick = actionRunCallback(PauseCallback::class.java),
                                )
                            }

                            CircleIconButton(
                                imageProvider = ImageProvider(drawable.skip_next),
                                backgroundColor = null,
                                contentDescription = "",
                                contentColor = ColorProvider(textColor),
                                onClick = actionRunCallback(SkipNextCallback::class.java),
                            )
                        }
                    }
                }

            }
        }
    }
}

fun getTextColor(bgColor: Color): Color {
    return if (bgColor.luminance() > 0.5) {
        Color.Black
    } else {
        Color.White
    }
}

@Composable
fun ImageWidgetUrlBackgroundThread(
    imageUrl: String,
    modifier: GlanceModifier = GlanceModifier,
) {
    // Image from a url fetched in a background thread
    val context = LocalContext.current
    var loadedBitmap by remember(imageUrl) { mutableStateOf<Bitmap?>(null) }

    LaunchedEffect(imageUrl) {
        withContext(Dispatchers.IO) {
            val request = ImageRequest.Builder(context).data(imageUrl).apply {
                memoryCachePolicy(CachePolicy.DISABLED)
                diskCachePolicy(CachePolicy.DISABLED)
            }.build()

            // Request the image to be loaded and return null if an error has occurred
            loadedBitmap = when (val result = context.imageLoader.execute(request)) {
                is ErrorResult -> null
                is SuccessResult -> result.image.toBitmap()
            }
        }
    }

    loadedBitmap.let { bitmap ->
        Log.d("MusicWidget", "Bitmap width: ${bitmap?.width}, height: ${bitmap?.height}")

        if (bitmap != null) {
            Image(
                provider = ImageProvider(bitmap),
                contentDescription = null,
                contentScale = ContentScale.FillBounds,
                modifier = modifier,
            )
        } else {
            CircularProgressIndicator(modifier)
        }
    }
}

class SpotifyCallbackManager(private val context: Context) {
    private var spotifyService: SpotifyService? = null
    private var connecting = false
    private val pending = mutableListOf<(SpotifyService) -> Unit>()

    suspend fun executeCommand(command: (SpotifyService) -> Unit) {
        val service = spotifyService ?: SpotifyService(context).also { spotifyService = it }

        if (service.spotifyAppRemote?.isConnected == true) {
            command(service)
            return
        }

        // Queue command until connected
        pending += command

        if (connecting) return
        connecting = true

        withContext(Dispatchers.Main) {
            service.connectSpotifyAppRemote {
                connecting = false
                // Drain queued commands
                val toRun = pending.toList()
                pending.clear()
                toRun.forEach { it(service) }
            }
        }
    }
}

abstract class BaseSpotifyCallback : ActionCallback {
    abstract fun executeSpotifyCommand(spotifyService: SpotifyService)

    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        val prefs = context.getSharedPreferences("spotify_auth", Context.MODE_PRIVATE)

        if (prefs.getBoolean("is_authorized", false)) {
            // Create a new manager instance each time, but reuse the SpotifyAppRemote connection
            SpotifyCallbackManager(context).executeCommand { service ->
                executeSpotifyCommand(service)
            }
        } else {
            withContext(Dispatchers.Main) {
                val intent = Intent(context, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(intent)
            }
        }
    }
}
// Specific callback implementations
class SkipNextCallback : BaseSpotifyCallback() {
    override fun executeSpotifyCommand(spotifyService: SpotifyService) {
        spotifyService.skipToNext()
    }
}

class SkipPreviousCallback : BaseSpotifyCallback() {
    override fun executeSpotifyCommand(spotifyService: SpotifyService) {
        spotifyService.skipToPrevious()
    }
}

class PauseCallback : BaseSpotifyCallback() {
    override fun executeSpotifyCommand(spotifyService: SpotifyService) {
        spotifyService.pause()
    }
}

class ResumeCallback : BaseSpotifyCallback() {
    override fun executeSpotifyCommand(spotifyService: SpotifyService) {
        spotifyService.resume()
    }
}