package com.example.widgetsy.musicWidget

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.appwidget.updateAll
import androidx.palette.graphics.Palette
import com.example.widgetsy.BuildConfig
import com.spotify.android.appremote.api.ConnectionParams
import com.spotify.android.appremote.api.Connector
import com.spotify.android.appremote.api.SpotifyAppRemote
import com.spotify.protocol.types.Track
import com.spotify.sdk.android.auth.AuthorizationClient
import com.spotify.sdk.android.auth.AuthorizationRequest
import com.spotify.sdk.android.auth.AuthorizationResponse
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SpotifyService(private val context: Context) {
    private val clientId = BuildConfig.SPOTIFY_CLIENT_ID
    private val redirectUri = BuildConfig.SPOTIFY_REDIRECT_URI
    private val prefs = context.getSharedPreferences("spotify_auth", Context.MODE_PRIVATE)

    private val _currentTrack = mutableStateOf<Track?>(null)
    val currentTrack: State<Track?> get() = _currentTrack

    var spotifyAppRemote: SpotifyAppRemote? = null

    companion object {
        const val REQUEST_CODE = 1337
    }

    fun isAuthorized(): Boolean = prefs.getBoolean("is_authorized", false)

    fun authorizeIfNeeded(activity: Activity) {
        if (isAuthorized()) {
            Log.d("SpotifyService", "Already authorized")
            connectSpotifyAppRemote()
            return
        }
        val scopes = arrayOf(
            "streaming",
            "user-read-currently-playing",
            "user-read-playback-state"
        )
        val request = AuthorizationRequest.Builder(
            clientId,
            AuthorizationResponse.Type.TOKEN,
            redirectUri
        ).setScopes(scopes).build()
        Log.d("SpotifyService", "Starting auth flow")
        AuthorizationClient.openLoginActivity(activity, REQUEST_CODE, request)
    }

    fun handleAuthResponse(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode != REQUEST_CODE) return
        val response = AuthorizationClient.getResponse(resultCode, data)
        when (response.type) {
            AuthorizationResponse.Type.TOKEN -> {
                prefs.edit().putBoolean("is_authorized", true).apply()
                Log.d("SpotifyService", "Auth success; token len=${response.accessToken?.length ?: 0}")
                connectSpotifyAppRemote()
            }
            AuthorizationResponse.Type.ERROR -> {
                Log.e("SpotifyService", "Auth error: ${response.error}")
            }
            else -> Log.w("SpotifyService", "Auth result: ${response.type}")
        }
    }

    fun connectSpotifyAppRemote(onConnected: (() -> Unit)? = null) {
        spotifyAppRemote?.let { if (it.isConnected) { onConnected?.invoke(); return } }
        val params = ConnectionParams.Builder(clientId)
            .setRedirectUri(redirectUri)
            .showAuthView(false) // already did explicit auth
            .build()
        Log.d("SpotifyService", "Connecting App Remote")
        SpotifyAppRemote.connect(context, params, object : Connector.ConnectionListener {
            override fun onConnected(appRemote: SpotifyAppRemote) {
                spotifyAppRemote = appRemote
                Log.d("SpotifyService", "App Remote connected")
                subscribeToPlayerState()
                onConnected?.invoke()
            }
            override fun onFailure(t: Throwable) {
                Log.e("SpotifyService", "Connect failed: ${t.message}", t)
                spotifyAppRemote = null
            }
        })
    }

    fun connected() {
        subscribeToPlayerState()
    }

    fun disconnect() {
        spotifyAppRemote?.let { SpotifyAppRemote.disconnect(it) }
        spotifyAppRemote = null
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
