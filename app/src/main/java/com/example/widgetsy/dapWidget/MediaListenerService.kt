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
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.appwidget.updateAll
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import kotlin.time.Duration.Companion.milliseconds

class MediaListenerService : NotificationListenerService() {

    companion object {
        var instance: MediaListenerService? = null
        var isConnected: Boolean = false
        private const val TARGET_PACKAGE = "com.hiby.music"
        private const val ALBUM_ART_FILENAME = "widget_album_art.png"
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
//    override fun onNotificationPosted(sbn: StatusBarNotification?) {
//        super.onNotificationPosted(sbn)
//        val sessionManager = getSystemService(MEDIA_SESSION_SERVICE) as MediaSessionManager
//        val componentName = ComponentName(this, MediaListenerService::class.java)
//        try {
//            attachToBestController(sessionManager.getActiveSessions(componentName))
//        } catch (e: SecurityException) {
//            Log.e("MediaListener", "Missing permission to get sessions", e)
//        }
//    }

    private fun attachToBestController(controllers: List<MediaController>) {
        val hibyController = controllers.firstOrNull {
            it.packageName == TARGET_PACKAGE && it.playbackState?.state == PlaybackState.STATE_PLAYING
        }
        val playingController = controllers.firstOrNull {
            it.playbackState?.state == PlaybackState.STATE_PLAYING
        }
        val targetController = hibyController ?: playingController ?: controllers.firstOrNull()

        if (targetController?.sessionToken == activeController?.sessionToken) {
            refreshFromController()
            return
        }

        activeController?.unregisterCallback(controllerCallback)
        activeController = targetController
        activeController?.registerCallback(controllerCallback)

        Log.d("MediaListener", "Now tracking package: ${targetController?.packageName}")
        refreshFromController()
    }

    private fun doRefresh() {
        val controller = activeController
        val metadata = controller?.metadata

        val title = metadata?.getString(android.media.MediaMetadata.METADATA_KEY_TITLE) ?: ""
        val artist = metadata?.getString(android.media.MediaMetadata.METADATA_KEY_ARTIST) ?: ""
        val isPlaying = controller?.playbackState?.state == PlaybackState.STATE_PLAYING
        val artBitmap = metadata?.getBitmap(android.media.MediaMetadata.METADATA_KEY_ALBUM_ART)
            ?: metadata?.getBitmap(android.media.MediaMetadata.METADATA_KEY_ART)

        val artPath = artBitmap?.let { saveAlbumArtToFile(it) }

        Log.d("MediaListener", "Updated info: title=$title, artist=$artist, playing=$isPlaying")
        Log.d("MediaListener", "artBitmap hash=${artBitmap?.let { System.identityHashCode(it) }}, size=${artBitmap?.byteCount}")

        serviceScope.launch {
            val manager = GlanceAppWidgetManager(applicationContext)
            val glanceIds = manager.getGlanceIds(DapWidget::class.java)

            glanceIds.forEach { id ->
                updateAppWidgetState(applicationContext, id) { prefs ->
                    prefs[WidgetKeys.TITLE] = title
                    prefs[WidgetKeys.ARTIST] = artist
                    prefs[WidgetKeys.IS_PLAYING] = isPlaying
                    if (artPath != null) {
                        prefs[WidgetKeys.ALBUM_ART_PATH] = artPath
                    } else {
                        prefs.remove(WidgetKeys.ALBUM_ART_PATH)
                    }
                }
            }

            DapWidget().updateAll(applicationContext)
        }
    }

    private var refreshJob: kotlinx.coroutines.Job? = null

    private fun refreshFromController() {
        refreshJob?.cancel()
        refreshJob = serviceScope.launch {
            kotlinx.coroutines.delay(1500.milliseconds) // wait for the burst to settle
            doRefresh()
        }
    }

    // Bitmaps can't be stored in Preferences directly — write to a file, store the path.
    private fun saveAlbumArtToFile(bitmap: android.graphics.Bitmap): String? {
        return try {
            val file = File(applicationContext.cacheDir, "album_art_${System.currentTimeMillis()}.png")
            FileOutputStream(file).use { out ->
                bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, out)
                out.flush()
            }
            // Clean up old art files so cache doesn't grow forever
            applicationContext.cacheDir.listFiles { f -> f.name.startsWith("album_art_") && f.name != file.name }
                ?.forEach { it.delete() }

            file.absolutePath
        } catch (e: Exception) {
            Log.e("MediaListener", "Failed to save album art", e)
            null
        }
    }
}