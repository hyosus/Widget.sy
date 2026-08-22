package com.example.widgetsy.dapWidget

import android.content.ComponentName
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.service.notification.NotificationListenerService
import android.util.Log

class MediaListenerService : NotificationListenerService() {

    companion object {
        var instance: MediaListenerService? = null
        var isConnected: Boolean = false
        private const val TARGET_PACKAGE = "com.hiby.music"
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        instance = this
        isConnected = true
        Log.d("MediaListener", "Listener connected!")
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        isConnected = false
        Log.d("MediaListener", "Listener disconnected")
    }

    override fun onDestroy() {
        super.onDestroy()
        instance = null
        isConnected = false
    }

    fun getCurrentMediaInfo(): MediaInfo? {
        if (!isConnected) {
            Log.d("MediaListener", "Not connected yet, skipping")
            return null
        }

        val sessionManager = getSystemService(MEDIA_SESSION_SERVICE) as MediaSessionManager
        val componentName = ComponentName(this, MediaListenerService::class.java)

        val controllers = try {
            sessionManager.getActiveSessions(componentName)
        } catch (e: SecurityException) {
            Log.e("MediaListener", "Missing permission to get sessions", e)
            return null
        }

        // Prefer HiBy specifically if it's playing, then any playing session, then any active session
        val hibyController = controllers.firstOrNull {
            it.packageName == TARGET_PACKAGE && it.playbackState?.state == PlaybackState.STATE_PLAYING
        }
        val playingController = controllers.firstOrNull {
            it.playbackState?.state == PlaybackState.STATE_PLAYING
        }
        val targetController = hibyController ?: playingController ?: controllers.firstOrNull() ?: return null

        if (targetController.playbackState?.state == PlaybackState.STATE_PLAYING) {
            Log.d("MediaListener", "Found playing media session for package: ${targetController.packageName}")
        } else {
            Log.d("MediaListener", "Found active media session (not playing) for package: ${targetController.packageName}")
        }

        val metadata = targetController.metadata ?: return null
        val playbackState = targetController.playbackState

        val info = MediaInfo(
            title = metadata.getString(android.media.MediaMetadata.METADATA_KEY_TITLE) ?: "",
            artist = metadata.getString(android.media.MediaMetadata.METADATA_KEY_ARTIST) ?: "",
            isPlaying = playbackState?.state == PlaybackState.STATE_PLAYING
        )

        Log.d("MediaListener", "Title: ${info.title}, Artist: ${info.artist}, Playing: ${info.isPlaying}")
        return info
    }
}

data class MediaInfo(
    val title: String,
    val artist: String,
    val isPlaying: Boolean
)