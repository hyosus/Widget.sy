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
    internal var activeController: MediaController? = null

    // Every live session we've registered controllerCallback on. Tracking all of them
    // (not just the active one) is what lets us notice when a *different* app that already
    // held a session starts playing — that emits onPlaybackStateChanged, not a session-list change.
    private val registeredControllers = mutableListOf<MediaController>()

    // Identifies the track currently reflected in the widget UI, so a bare play/pause
    // (same track, no metadata change) can skip the loading/skeleton treatment entirely.
    private var lastAppliedTrackKey: String? = null

    // Registered on every session, so a state/metadata change from ANY app re-triggers
    // selection. We can't tell which controller fired, so we just re-read the live session
    // list and let selectBest() decide — that's cheap and keeps the logic in one place.
    private val controllerCallback = object : MediaController.Callback() {
        override fun onMetadataChanged(metadata: MediaMetadata?) {
            selectBest(currentSessions())
        }

        override fun onPlaybackStateChanged(state: PlaybackState?) {
            selectBest(currentSessions())
        }

        override fun onSessionDestroyed() {
            onSessions(currentSessions())
        }
    }

    private fun currentTrackKey(): String? {
        val metadata = activeController?.metadata ?: return null
        val title = metadata.getString(MediaMetadata.METADATA_KEY_TITLE) ?: ""
        val artist = metadata.getString(MediaMetadata.METADATA_KEY_ARTIST) ?: ""
        return "$title|$artist"
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
        registeredControllers.forEach { it.unregisterCallback(controllerCallback) }
        registeredControllers.clear()
        activeController = null
        lastAppliedTrackKey = null
        Log.d("MediaListener", "Listener disconnected")
    }

    override fun onDestroy() {
        super.onDestroy()
        instance = null
        isConnected = false
        registeredControllers.forEach { it.unregisterCallback(controllerCallback) }
        registeredControllers.clear()
        activeController = null
        serviceScope.cancel()
    }


    /** Session set changed (app added/removed): re-register callbacks on all, then re-select. */
    private fun onSessions(controllers: List<MediaController>) {
        syncCallbacks(controllers)
        selectBest(controllers)
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

    private fun syncCallbacks(controllers: List<MediaController>) {
        registeredControllers.forEach { it.unregisterCallback(controllerCallback) }
        registeredControllers.clear()
        controllers.forEach {
            it.registerCallback(controllerCallback)
            registeredControllers.add(it)
        }
    }

    /**
     * Pick which session drives the widget: whatever is actually playing. The currently
     * tracked session wins ties while it keeps playing, so two playing apps don't flap.
     * Falls back to the current (now paused) session, then any session, when nothing plays.
     */
    private fun selectBest(controllers: List<MediaController>) {
        val currentToken = activeController?.sessionToken
        val currentStillPlaying = controllers.any {
            it.sessionToken == currentToken && it.playbackState?.state == PlaybackState.STATE_PLAYING
        }

        val target = when {
            currentStillPlaying -> controllers.first { it.sessionToken == currentToken }
            else -> controllers.firstOrNull { it.playbackState?.state == PlaybackState.STATE_PLAYING }
                ?: controllers.firstOrNull { it.sessionToken == currentToken }
                ?: controllers.firstOrNull()
        }

        if (target?.sessionToken == currentToken) {
            // Same session we already track — refresh the handle and update in place.
            activeController = target
            val state = target?.playbackState?.state
            when {
                target == null -> return
                currentTrackKey() != lastAppliedTrackKey -> refreshFromController()
                state == PlaybackState.STATE_PLAYING || state == PlaybackState.STATE_PAUSED ->
                    updatePlaybackStateOnly(state == PlaybackState.STATE_PLAYING)
            }
            return
        }

        // Switching to a different session.
        activeController = target
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

        lastAppliedTrackKey = "$title|$artist"

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
                prefs[MusicWidgetKeys.IS_LOADING] = false
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
                prefs[MusicWidgetKeys.IS_LOADING] = false
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

    private fun updatePlaybackStateOnly(isPlaying: Boolean) {
        serviceScope.launch {
            val manager = GlanceAppWidgetManager(applicationContext)
            manager.getGlanceIds(VinylWidget::class.java).forEach { id ->
                updateAppWidgetState(applicationContext, id) { prefs ->
                    prefs[MusicWidgetKeys.IS_PLAYING] = isPlaying
                }
            }
            manager.getGlanceIds(MusicWidget::class.java).forEach { id ->
                updateAppWidgetState(applicationContext, id) { prefs ->
                    prefs[MusicWidgetKeys.IS_PLAYING] = isPlaying
                }
            }
            MusicWidget().updateAll(applicationContext)
            VinylWidget().updateAll(applicationContext)
        }
    }

    private var refreshJob: Job? = null

    fun requestRefresh() {
        refreshFromController()
    }

    private fun refreshFromController() {
        refreshJob?.cancel()
        refreshJob = serviceScope.launch {
            setLoadingState(true)
            delay(1500.milliseconds) // wait for the burst to settle
            doRefresh()
        }
    }

    private suspend fun setLoadingState(isLoading: Boolean) {
        val manager = GlanceAppWidgetManager(applicationContext)
        manager.getGlanceIds(VinylWidget::class.java).forEach { id ->
            updateAppWidgetState(applicationContext, id) { prefs ->
                prefs[MusicWidgetKeys.IS_LOADING] = isLoading
            }
        }
        manager.getGlanceIds(MusicWidget::class.java).forEach { id ->
            updateAppWidgetState(applicationContext, id) { prefs ->
                prefs[MusicWidgetKeys.IS_LOADING] = isLoading
            }
        }
        MusicWidget().updateAll(applicationContext)
        VinylWidget().updateAll(applicationContext)
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