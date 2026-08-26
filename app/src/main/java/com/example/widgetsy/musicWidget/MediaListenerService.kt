package com.example.widgetsy.musicWidget

import android.content.ComponentName
import android.graphics.Bitmap
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.service.notification.NotificationListenerService
import android.util.Log
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.appwidget.updateAll
import com.example.widgetsy.musicWidget.normal.MusicWidget
import com.example.widgetsy.musicWidget.vinyl.VinylWidget
import com.example.widgetsy.utils.blurBitmap
import com.example.widgetsy.utils.getPrimaryColorFromImage
import com.example.widgetsy.utils.getTextColor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import kotlin.time.Duration.Companion.milliseconds

class MediaListenerService : NotificationListenerService() {

    companion object {
        var instance: MediaListenerService? = null
        var isConnected: Boolean = false
    }

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var activeController: MediaController? = null

    /** Read-only access for MediaControlReceiver — callers should never mutate this. */
    val currentController: MediaController?
        get() = activeController

    private val controllerCallback = object : MediaController.Callback() {
        override fun onMetadataChanged(metadata: MediaMetadata?) = refreshFromController()
        override fun onPlaybackStateChanged(state: PlaybackState?) = refreshFromController()
        override fun onSessionDestroyed() = onSessions(currentSessions())
    }

    private val sessionsChangedListener =
        MediaSessionManager.OnActiveSessionsChangedListener { controllers ->
            onSessions(controllers ?: emptyList())
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
            onSessions(sessionManager.getActiveSessions(componentName))
        } catch (e: SecurityException) {
            Log.e("MediaListener", "Missing permission to get sessions", e)
        }
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        isConnected = false
        refreshJob?.cancel()
        refreshJob = null
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
        serviceScope.cancel()
    }

    private fun currentSessions(): List<MediaController> {
        return try {
            val sessionManager = getSystemService(MEDIA_SESSION_SERVICE) as MediaSessionManager
            val componentName = ComponentName(this, MediaListenerService::class.java)
            sessionManager.getActiveSessions(componentName)
        } catch (e: SecurityException) {
            Log.e("MediaListener", "Missing permission to get sessions", e)
            emptyList()
        }
    }

    /** Whatever's playing wins, from any app. Falls back to any active session if nothing is. */
    private fun onSessions(controllers: List<MediaController>) {
        val target = controllers.firstOrNull { it.playbackState?.state == PlaybackState.STATE_PLAYING }
            ?: controllers.firstOrNull()

        if (target?.sessionToken == activeController?.sessionToken) return

        activeController?.unregisterCallback(controllerCallback)
        activeController = target
        activeController?.registerCallback(controllerCallback)

        Log.d("MediaListener", "Now tracking package: ${target?.packageName}")
        refreshFromController()
    }

    private suspend fun doRefresh() {
        val controller = activeController
        val metadata = controller?.metadata

        val title = metadata?.getString(MediaMetadata.METADATA_KEY_TITLE) ?: ""
        val artist = metadata?.getString(MediaMetadata.METADATA_KEY_ARTIST) ?: ""
        val isPlaying = controller?.playbackState?.state == PlaybackState.STATE_PLAYING
        val artBitmap = metadata?.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART)
            ?: metadata?.getBitmap(MediaMetadata.METADATA_KEY_ART)

//        lastAppliedTrackKey = "$title|$artist"

        Log.d("MediaListener", "Updated info: title=$title, artist=$artist, playing=$isPlaying")
        Log.d("MediaListener", "artBitmap hash=${artBitmap?.let { System.identityHashCode(it) }}, size=${artBitmap?.byteCount}")

        var artPath: String? = null
        var blurredArtPath: String? = null
        var dynamicBgColor: Int? = null
        var dynamicTextColor: Color? = null

        withContext(Dispatchers.IO) {
            artPath = artBitmap?.let { saveAlbumArtToFile(it) }
            blurredArtPath = artBitmap?.let { saveBitmapToFile(blurBitmap(it, radius = 20), "blurred_art") }
            dynamicBgColor = artBitmap?.let { getPrimaryColorFromImage(it) }
            dynamicTextColor = dynamicBgColor?.let { getTextColor(Color(it)) }
        }

        val manager = GlanceAppWidgetManager(applicationContext)
        val vinylGlanceIds = manager.getGlanceIds(VinylWidget::class.java)
        val normalGlanceIds = manager.getGlanceIds(MusicWidget::class.java)

        vinylGlanceIds.forEach { id ->
            updateAppWidgetState(applicationContext, id) { prefs ->
                prefs[MusicWidgetKeys.TITLE] = title
                prefs[MusicWidgetKeys.ARTIST] = artist
                prefs[MusicWidgetKeys.IS_PLAYING] = isPlaying
//                prefs[MusicWidgetKeys.IS_LOADING] = false
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

        normalGlanceIds.forEach { id ->
            updateAppWidgetState(applicationContext, id) { prefs ->
                prefs[MusicWidgetKeys.TITLE] = title
                prefs[MusicWidgetKeys.ARTIST] = artist
                prefs[MusicWidgetKeys.IS_PLAYING] = isPlaying
//                prefs[MusicWidgetKeys.IS_LOADING] = false
                if (artPath != null) {
                    prefs[MusicWidgetKeys.ALBUM_ART_PATH] = artPath
                } else {
                    prefs.remove(MusicWidgetKeys.ALBUM_ART_PATH)
                }
                if (dynamicBgColor != null) {
                    prefs[MusicWidgetKeys.DYNAMIC_BACKGROUND_COLOR] = dynamicBgColor
                } else {
                    prefs.remove(MusicWidgetKeys.DYNAMIC_BACKGROUND_COLOR)
                }
                prefs[MusicWidgetKeys.DYNAMIC_TEXT_COLOR] = dynamicTextColor?.toArgb() ?: 1
            }
        }

        MusicWidget().updateAll(applicationContext)
        VinylWidget().updateAll(applicationContext)
    }

    private var refreshJob: Job? = null

    fun requestRefresh() = refreshFromController()

    private fun refreshFromController() {
        refreshJob?.cancel()
        refreshJob = serviceScope.launch {
            delay(1500.milliseconds) // let a burst of callbacks settle before doing real work
            doRefresh()
        }
    }

    // Bitmaps can't be stored in Preferences directly — write to a file, store the path.
    private fun saveAlbumArtToFile(bitmap: Bitmap): String? =
        saveBitmapToFile(bitmap, "album_art")

    private fun saveBitmapToFile(bitmap: Bitmap, prefix: String): String? {
        return try {
            val file = File(applicationContext.cacheDir, "${prefix}_${System.currentTimeMillis()}.png")
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                out.flush()
            }
            applicationContext.cacheDir.listFiles { f -> f.name.startsWith("${prefix}_") && f.name != file.name }
                ?.forEach { it.delete() }
            file.absolutePath
        } catch (e: Exception) {
            Log.e("MediaListener", "Failed to save $prefix", e)
            null
        }
    }
}