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
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
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
    var spotifyAppRemote: SpotifyAppRemote? = null

    private val _currentTrack = mutableStateOf<Track?>(null)
    val currentTrack: State<Track?> get() = _currentTrack

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

    fun connectSpotifyAppRemote() {
        val connectionParams = ConnectionParams.Builder(clientId)
            .setRedirectUri(redirectUri)
            .showAuthView(true)
            .build()

        SpotifyAppRemote.connect(context, connectionParams, object : Connector.ConnectionListener {
            override fun onConnected(appRemote: SpotifyAppRemote) {
                spotifyAppRemote = appRemote
                Log.d("SpotifyService", "Connected to Spotify!")
                connected()
            }

            override fun onFailure(throwable: Throwable) {
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
            // Set currentTrack from playerState
            _currentTrack.value = playerState.track
            Log.d(
                "SpotifyService", """
            Current Track:
            Name: ${playerState.track.name}
            Artist: ${playerState.track.artist.name}
            Album Image: ${playerState.track.imageUri}
            URI: ${playerState.track.uri}
            Playback Position: ${playerState.playbackPosition}
            Is Playing: ${!playerState.isPaused}
        """.trimIndent()
            )



            val imageUri = playerState.track.imageUri
            var bgColor = 0
            _currentTrack.value?.imageUri?.let { uri ->
                spotifyAppRemote?.imagesApi?.getImage(uri)?.setResultCallback { bitmap ->
                    bgColor = extractDominantColor(bitmap)
                    Log.d("SpotifyService", "Dominant color: $bgColor")
                }
            }
            if (imageUri != null) {
                getAlbumArtUrl(imageUri) { imageUrl ->
                    updateWidgetState(
                        context,
                        playerState.track.name,
                        playerState.track.artist.name,
                        imageUrl,
                        playerState.isPaused,
                    )
                }
            } else {
                updateWidgetState(
                    context,
                    playerState.track.name,
                    playerState.track.artist.name,
                    "",
                    playerState.isPaused,
                )
            }
        }
    }

    private fun getAlbumArtUrl(imageUri: ImageUri, callback: (String) -> Unit) {
        spotifyAppRemote?.imagesApi?.getImage(imageUri)?.setResultCallback { bitmap ->
            val imageUrl = imageUri.raw?.replace("spotify:image:", "https://i.scdn.co/image/")
            if (imageUrl != null){
                callback(imageUrl)
            }
        }
    }

    fun extractDominantColor(bitmap: Bitmap): Int {
        val palette = Palette.from(bitmap).generate()
        return palette.getDominantColor(0)
    }

    private fun updateWidgetState(
        context: Context,
        trackName: String,
        artistName: String,
        imageUri: String,
        isPaused: Boolean) {
        CoroutineScope(Dispatchers.Main).launch {
            try {
                // Obtain the GlanceId for the widget
                val glanceId = GlanceAppWidgetManager(context).getGlanceIds(MusicWidget::class.java).firstOrNull()
                Log.d("SpotifyService", "GlanceId: $glanceId")

                if (glanceId != null) {
                    // Update the widget state
                    updateAppWidgetState(context, glanceId) { prefs ->
                        prefs[MusicWidget().trackNameKey] = trackName
                        prefs[MusicWidget().artistNameKey] = artistName
                        prefs[MusicWidget().albumArtUriKey] = imageUri
                        prefs[MusicWidget().isPausedKey] = isPaused
                    }
                    Log.d("SpotifyService", "Widget state updated: Track - $trackName, Artist - $artistName, Image - $imageUri, isPaused - $isPaused")

                    // Then update all widgets
                    MusicWidget().updateAll(context)
                    Log.d("SpotifyService", "Widget updated")

                }else {
                    Log.e("SpotifyService", "GlanceId is null, widget not found")
                }
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
