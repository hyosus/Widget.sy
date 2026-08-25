package com.example.widgetsy.musicWidget

import android.content.ComponentName
import android.graphics.Bitmap
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.service.notification.NotificationListenerService
import android.util.Log
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.appwidget.updateAll
import com.example.widgetsy.utils.blurBitmap
import com.example.widgetsy.musicWidget.vinyl.VinylWidget
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
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
        override fun onMetadataChanged(metadata: MediaMetadata?) {
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

        val title = metadata?.getString(MediaMetadata.METADATA_KEY_TITLE) ?: ""
        val artist = metadata?.getString(MediaMetadata.METADATA_KEY_ARTIST) ?: ""
        val isPlaying = controller?.playbackState?.state == PlaybackState.STATE_PLAYING
        val artBitmap = metadata?.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART)
            ?: metadata?.getBitmap(MediaMetadata.METADATA_KEY_ART)

        val artPath = artBitmap?.let { saveAlbumArtToFile(it) }
        val blurredArtPath = artBitmap?.let { saveBitmapToFile(blurBitmap(it, radius = 20), "blurred_art") }

        Log.d("MediaListener", "Updated info: title=$title, artist=$artist, playing=$isPlaying")
        Log.d("MediaListener", "artBitmap hash=${artBitmap?.let { System.identityHashCode(it) }}, size=${artBitmap?.byteCount}")

        serviceScope.launch {
            val manager = GlanceAppWidgetManager(applicationContext)
            val glanceIds = manager.getGlanceIds(VinylWidget::class.java)

            glanceIds.forEach { id ->
                updateAppWidgetState(applicationContext, id) { prefs ->
                    prefs[MusicWidgetKeys.TITLE] = title
                    prefs[MusicWidgetKeys.ARTIST] = artist
                    prefs[MusicWidgetKeys.IS_PLAYING] = isPlaying
                    if (artPath != null) {
                        prefs[MusicWidgetKeys.ALBUM_ART_PATH] = artPath
                    } else {
                        prefs.remove(MusicWidgetKeys.ALBUM_ART_PATH)
                    }
                    if (blurredArtPath != null) {
                        prefs[MusicWidgetKeys.BLURRED_ART_PATH] = blurredArtPath
                    } else {
                        prefs.remove(MusicWidgetKeys.BLURRED_ART_PATH)
                    }
                }
            }

            VinylWidget().updateAll(applicationContext)
        }
    }

    private var refreshJob: Job? = null

    private fun refreshFromController() {
        refreshJob?.cancel()
        refreshJob = serviceScope.launch {
            delay(1500.milliseconds) // wait for the burst to settle
            doRefresh()
        }
    }

    // Bitmaps can't be stored in Preferences directly — write to a file, store the path.
    private fun saveAlbumArtToFile(bitmap: Bitmap): String? =
        saveBitmapToFile(bitmap, "album_art")

    private fun saveBitmapToFile(bitmap: Bitmap, prefix: String): String? {
        return try {
            val file =
                File(applicationContext.cacheDir, "${prefix}_${System.currentTimeMillis()}.png")
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                out.flush()
            }
            // Clean up old files with the same prefix so cache doesn't grow forever
            applicationContext.cacheDir.listFiles { f -> f.name.startsWith("${prefix}_") && f.name != file.name }
                ?.forEach { it.delete() }

            file.absolutePath
        } catch (e: Exception) {
            Log.e("MediaListener", "Failed to save $prefix", e)
            null
        }
    }
}