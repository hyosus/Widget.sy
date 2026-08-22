package com.example.widgetsy.dapWidget

import android.content.ComponentName
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import androidx.glance.GlanceId
import androidx.glance.appwidget.GlanceAppWidgetManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MediaListenerService : NotificationListenerService() {

    companion object {
        var instance: MediaListenerService? = null
        var isConnected: Boolean = false
        private const val TARGET_PACKAGE = "com.hiby.music"
    }

    private val serviceScope = CoroutineScope(Dispatchers.Main)
    private var activeController: MediaController? = null

    private val controllerCallback = object : MediaController.Callback() {
        override fun onMetadataChanged(metadata: android.media.MediaMetadata?) {
            refreshFromController()
        }

        override fun onPlaybackStateChanged(state: PlaybackState?) {
            refreshFromController()
        }
    }

    private val sessionsChangedListener =
        MediaSessionManager.OnActiveSessionsChangedListener { controllers ->
            attachToBestController(controllers ?: emptyList())
        }

    override fun onListenerConnected() {
        super.onListenerConnected()
        instance = this
        isConnected = true
        Log.d("MediaListener", "Listener connected!")

        val sessionManager = getSystemService(MEDIA_SESSION_SERVICE) as MediaSessionManager
        val componentName = ComponentName(this, MediaListenerService::class.java)

        try {
            sessionManager.addOnActiveSessionsChangedListener(sessionsChangedListener, componentName)
            attachToBestController(sessionManager.getActiveSessions(componentName))
        } catch (e: SecurityException) {
            Log.e("MediaListener", "Missing permission to get sessions", e)
        }
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        isConnected = false
        activeController?.unregisterCallback(controllerCallback)
        activeController = null
        Log.d("MediaListener", "Listener disconnected")
    }

    override fun onDestroy() {
        super.onDestroy()
        instance = null
        isConnected = false
        activeController?.unregisterCallback(controllerCallback)
        activeController = null
    }

    // A new/removed notification can mean a media app started or stopped —
    // re-check active sessions so we pick up new controllers.
    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        val sessionManager = getSystemService(MEDIA_SESSION_SERVICE) as MediaSessionManager
        val componentName = ComponentName(this, MediaListenerService::class.java)
        try {
            attachToBestController(sessionManager.getActiveSessions(componentName))
        } catch (e: SecurityException) {
            Log.e("MediaListener", "Missing permission to get sessions", e)
        }
    }

    private fun attachToBestController(controllers: List<MediaController>) {
        val hibyController = controllers.firstOrNull {
            it.packageName == TARGET_PACKAGE && it.playbackState?.state == PlaybackState.STATE_PLAYING
        }
        val playingController = controllers.firstOrNull {
            it.playbackState?.state == PlaybackState.STATE_PLAYING
        }
        val targetController = hibyController ?: playingController ?: controllers.firstOrNull()

        if (targetController?.sessionToken == activeController?.sessionToken) {
            // Same controller already tracked, just refresh values
            refreshFromController()
            return
        }

        activeController?.unregisterCallback(controllerCallback)
        activeController = targetController
        activeController?.registerCallback(controllerCallback)

        Log.d("MediaListener", "Now tracking package: ${targetController?.packageName}")
        refreshFromController()
    }

    private fun refreshFromController() {
        val controller = activeController
        val metadata = controller?.metadata

        val info = if (controller == null || metadata == null) {
            null
        } else {
            val artBitmap = metadata.getBitmap(android.media.MediaMetadata.METADATA_KEY_ALBUM_ART)
                ?: metadata.getBitmap(android.media.MediaMetadata.METADATA_KEY_ART)

            MediaInfo(
                title = metadata.getString(android.media.MediaMetadata.METADATA_KEY_TITLE) ?: "",
                artist = metadata.getString(android.media.MediaMetadata.METADATA_KEY_ARTIST) ?: "",
                isPlaying = controller.playbackState?.state == PlaybackState.STATE_PLAYING,
                albumArt = artBitmap
            )
        }

        MediaInfoHolder.currentInfo = info
        Log.d("MediaListener", "Updated info: $info")

        serviceScope.launch {
            val manager = GlanceAppWidgetManager(applicationContext)
            val glanceIds: List<GlanceId> = manager.getGlanceIds(DapWidget::class.java)
            glanceIds.forEach { id ->
                DapWidget().update(applicationContext, id)
            }
        }
    }

    // Kept for any spot that still calls this directly (e.g. MainActivity onStart)
    fun getCurrentMediaInfo(): MediaInfo? {
        return MediaInfoHolder.currentInfo
    }
}

data class MediaInfo(
    val title: String,
    val artist: String,
    val isPlaying: Boolean,
    val albumArt: android.graphics.Bitmap? = null
)