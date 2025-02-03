package com.example.widgetsy.musicWidget

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import androidx.compose.ui.graphics.Color
import android.net.Uri
import com.example.widgetsy.R.drawable
import com.example.widgetsy.R
import androidx.compose.ui.unit.dp
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.currentState
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.glance.text.Text
import android.util.Log
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.sp
import androidx.glance.Button
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.appwidget.ImageProvider
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.components.Scaffold
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.size
import androidx.glance.text.TextStyle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.toArgb
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.glance.action.Action
import androidx.glance.appwidget.CircularProgressIndicator
import androidx.glance.appwidget.background
import androidx.glance.appwidget.components.CircleIconButton
import androidx.glance.appwidget.cornerRadius
import androidx.glance.background
import androidx.glance.layout.ContentScale
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.unit.ColorProvider
import androidx.palette.graphics.Palette
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import coil3.imageLoader
import coil3.request.ErrorResult
import coil3.request.SuccessResult
import coil3.toBitmap
import com.example.widgetsy.MainActivity
import com.example.widgetsy.MyGlanceTheme
import kotlinx.coroutines.delay

class MusicWidget: GlanceAppWidget() {
    val trackNameKey = stringPreferencesKey("track_name")
    val artistNameKey = stringPreferencesKey("artist_name")
    val albumArtUriKey = stringPreferencesKey("album_art_uri")
    val isPausedKey = booleanPreferencesKey("is_paused")

    override val stateDefinition = PreferencesGlanceStateDefinition

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
        }

        provideContent {
            val trackName = currentState(key = trackNameKey) ?: "No track playing"
            val artistName = currentState(key = artistNameKey) ?: "No artists"
            val albumArtUri = currentState(key = albumArtUriKey) ?: ""
            val isPaused = currentState(key = isPausedKey) ?: true

            Log.d("MusicWidget", "Track: $trackName, Artist: $artistName, Album Art: $albumArtUri, Is Paused: $isPaused")

            GlanceTheme(
                colors = MyGlanceTheme.colors,
            ) {
                Scaffold(
                    modifier = GlanceModifier.fillMaxSize().padding(8.dp),
                    backgroundColor = MyGlanceTheme.colors.background
                )
                {
                    Row {
                        if (albumArtUri.isNotEmpty()) {
                            Log.d("MusicWidget", "Album Art Uri: ${Uri.parse(albumArtUri)}")

                            ImageWidgetUrlBackgroundThread(
                                albumArtUri,
                                modifier = GlanceModifier.size(100.dp).cornerRadius(12.dp)
                            )
                        }

                        Spacer(modifier = GlanceModifier.size(10.dp))

                        Column {
                            Text(
                                text = trackName,
                                style = TextStyle(fontSize = 20.sp, color = MyGlanceTheme.colors.primary),
                                maxLines = 1,
                                modifier = GlanceModifier.padding(start = 12.dp)
                            )
                            Text(
                                text = artistName,
                                style = TextStyle(color = MyGlanceTheme.colors.primary),
                                modifier = GlanceModifier.padding(start = 12.dp))

                            Row(
                                modifier = GlanceModifier.fillMaxWidth(),
                            ) {
                                CircleIconButton(
                                    imageProvider = ImageProvider(drawable.skip_previous),
                                    backgroundColor = null,
                                    contentDescription = "",
                                    onClick = actionRunCallback(SkipPreviousCallback::class.java),
                                )

                                Log.d("MusicWidget", "Is paused: $isPaused")

                                if (isPaused) {
                                    CircleIconButton(
                                        imageProvider = ImageProvider(drawable.play_arrow),
                                        backgroundColor = null,
                                        contentDescription = "",
                                        onClick = actionRunCallback(ResumeCallback::class.java),
                                    )
                                } else {
                                    CircleIconButton(
                                        imageProvider = ImageProvider(drawable.pause),
                                        backgroundColor = null,
                                        contentDescription = "",
                                        onClick = actionRunCallback(PauseCallback::class.java),
                                    )
                                }

                                CircleIconButton(
                                    imageProvider = ImageProvider(drawable.skip_next),
                                    backgroundColor = null,
                                    contentDescription = "",
                                    onClick = actionRunCallback(SkipNextCallback::class.java),
                                )
                            }
                        }
                    }

                }
            }
        }
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
                contentScale = ContentScale.Crop,
                modifier = modifier,
            )

            Palette.from(bitmap).generate { palette ->
                // Use generated instance.
                Log.d("MusicWidget", "Palette: $palette")
            }
        } else {
            CircularProgressIndicator(modifier)
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
        Log.d("SpotifyCallback", "Is authorized: ${prefs.getBoolean("is_authorized", false)}")

        if (prefs.getBoolean("is_authorized", false)) {
            withContext(Dispatchers.Main) {
                val spotifyService = SpotifyService(context)
                spotifyService.connectSpotifyAppRemote()

                var retryCount = 0
                while (spotifyService.spotifyAppRemote == null && retryCount < 5) {
                    delay(100)
                    retryCount++
                }

                if (spotifyService.spotifyAppRemote != null) {
                    executeSpotifyCommand(spotifyService)
                    Log.d("SpotifyCallback", "Command executed")
                } else {
                    Log.e("SpotifyCallback", "Failed to connect to Spotify")
                }
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