package com.example.widgetsy.musicWidget

import android.util.Log
import com.spotify.protocol.types.Track
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import androidx.compose.runtime.mutableStateOf
import com.spotify.android.appremote.api.ConnectionParams
import com.spotify.android.appremote.api.Connector
import com.spotify.android.appremote.api.SpotifyAppRemote
import com.spotify.sdk.android.auth.AuthorizationClient
import com.spotify.sdk.android.auth.AuthorizationRequest
import com.spotify.sdk.android.auth.AuthorizationResponse
import com.example.widgetsy.BuildConfig
import androidx.compose.runtime.State
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.appwidget.updateAll
import androidx.palette.graphics.Palette
import com.spotify.protocol.types.ImageUri
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SpotifyService(private val context: Context) {
    private val clientId = BuildConfig.SPOTIFY_CLIENT_ID
    private val redirectUri = BuildConfig.SPOTIFY_REDIRECT_URI

    private val _currentTrack = mutableStateOf<Track?>(null)
    val currentTrack: State<Track?> get() = _currentTrack

    var spotifyAppRemote: SpotifyAppRemote? = null
    var isConnecting = false

    companion object {
        const val REQUEST_CODE = 1337
    }

    // Add a preference to store auth state
    private val prefs = context.getSharedPreferences("spotify_auth", Context.MODE_PRIVATE)

    // Check if user is already authorized
    fun isAuthorized(): Boolean {
        return prefs.getBoolean("is_authorized", false)
    }

    fun authenticateSpotify(activity: Activity) {

        if (!isAuthorized()) {
            val builder = AuthorizationRequest.Builder(
                clientId,
                AuthorizationResponse.Type.TOKEN,
                redirectUri
            )

            builder.setScopes(arrayOf(
                "streaming",
                "user-read-currently-playing",
                "user-read-playback-state"
            ))

            val request = builder.build()
            AuthorizationClient.openLoginActivity(activity, REQUEST_CODE, request)
        }
        else {
            connectSpotifyAppRemote()
        }
    }

    fun handleAuthResponse(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == REQUEST_CODE) {
            val response = AuthorizationClient.getResponse(resultCode, data)

            when (response.type) {
                AuthorizationResponse.Type.TOKEN -> {
                    val token = response.accessToken
                    prefs.edit().putBoolean("is_authorized", true).apply()
                    connectSpotifyAppRemote()
                }
                AuthorizationResponse.Type.ERROR -> {
                    Log.e("SpotifyService", "Auth error: ${response.error}")
                }
                else -> {
                    // Handle other cases
                }
            }
        }
    }

    fun connectSpotifyAppRemote(onConnected: (() -> Unit)? = null) {
        spotifyAppRemote?.let { remote ->
            if (remote.isConnected) {
                onConnected?.invoke()
                return
            }
        }

        if (isConnecting) {
            return
        }

        isConnecting = true
        val connectionParams = ConnectionParams.Builder(clientId)
            .setRedirectUri(redirectUri)
            .showAuthView(true)
            .build()

        SpotifyAppRemote.connect(context, connectionParams, object : Connector.ConnectionListener {
            override fun onConnected(appRemote: SpotifyAppRemote) {
                spotifyAppRemote = appRemote
                isConnecting = false
                onConnected?.invoke()
                Log.d("SpotifyService", "Connected to Spotify!")
                connected()
            }

            override fun onFailure(throwable: Throwable) {
                spotifyAppRemote = null
                isConnecting = false
                Log.e("SpotifyService", throwable.message, throwable)
            }
        })
    }

    fun connected() {
        subscribeToPlayerState()
    }

    fun disconnect() {
        SpotifyAppRemote.disconnect(spotifyAppRemote)
    }

    private fun subscribeToPlayerState() {
        spotifyAppRemote?.playerApi?.subscribeToPlayerState()?.setEventCallback { playerState ->
            _currentTrack.value = playerState.track

            playerState.track.imageUri?.let { uri ->
                spotifyAppRemote?.imagesApi?.getImage(uri)?.setResultCallback { bitmap ->
                    val palette = Palette.from(bitmap).generate()
                    val bgColor = palette.getDominantColor(0)

                    Log.d("SpotifyService", "Dominant color: $bgColor")

                    val imageUri = uri.raw?.replace("spotify:image:", "https://i.scdn.co/image/")
                    updateWidgetState(
                        context,
                        playerState.track.name,
                        playerState.track.artist.name,
                        imageUri ?: "",
                        playerState.isPaused,
                        bgColor
                    )
                }
            } ?: run {
                updateWidgetState(
                    context,
                    playerState.track.name,
                    playerState.track.artist.name,
                    "",
                    playerState.isPaused,
                    Color.White.toArgb()  // Default color when no image
                )
            }
        }
    }

    private fun updateWidgetState(
        context: Context,
        trackName: String,
        artistName: String,
        imageUri: String,
        isPaused: Boolean,
        bgColor: Int) {
        CoroutineScope(Dispatchers.Main).launch {
            try {
                // Obtain the GlanceId for the widget
                val glanceId = GlanceAppWidgetManager(context).getGlanceIds(MusicWidget::class.java).firstOrNull() ?: return@launch
                Log.d("SpotifyService", "GlanceId: $glanceId")

                // Update the widget state
                updateAppWidgetState(context, glanceId) { prefs ->
                    prefs[MusicWidget().trackNameKey] = trackName
                    prefs[MusicWidget().artistNameKey] = artistName
                    prefs[MusicWidget().albumArtUriKey] = imageUri
                    prefs[MusicWidget().isPausedKey] = isPaused
                    prefs[MusicWidget().backgroundColorKey] = bgColor
                }
                Log.d("SpotifyService", "Widget state updated: Track - $trackName, Artist - $artistName, Image - $imageUri, isPaused - $isPaused, bgColor - $bgColor")

                // Then update all widgets
                MusicWidget().updateAll(context)
                Log.d("SpotifyService", "Widget updated")

            } catch (e: Exception) {
                Log.e("SpotifyService", "Error updating widget", e)
            }
        }
    }

    // Playback control methods
    fun pause() = spotifyAppRemote?.playerApi?.pause()
    fun resume() = spotifyAppRemote?.playerApi?.resume()
    fun skipToNext() = spotifyAppRemote?.playerApi?.skipNext()
    fun skipToPrevious() = spotifyAppRemote?.playerApi?.skipPrevious()

    fun getTrackImage(callback: (ImageBitmap?) -> Unit) {
        _currentTrack.value?.imageUri?.let { uri ->
            spotifyAppRemote?.imagesApi?.getImage(uri)?.setResultCallback { bitmap ->
                callback(bitmap.asImageBitmap())
            }
        }
    }
}
