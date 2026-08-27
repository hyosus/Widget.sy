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
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
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

    private var lastMetadataKey: String? = null
    private var lastPlaybackState: Int? = null
    private var hasArtForCurrentTrack: Boolean = false

    private val controllerCallback = object : MediaController.Callback() {
        override fun onMetadataChanged(metadata: MediaMetadata?) {
            val title = metadata?.getString(MediaMetadata.METADATA_KEY_TITLE) ?: ""
            val artist = metadata?.getString(MediaMetadata.METADATA_KEY_ARTIST) ?: ""
            val key = "$title|$artist"
            val hasArt = metadata?.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART) != null
                    || metadata?.getBitmap(MediaMetadata.METADATA_KEY_ART) != null
            Log.d("MediaListener", "onMetadataChanged: title=$title, artist=$artist (lastKey=$lastMetadataKey, newKey=$key, hasArt=$hasArt, hadArt=$hasArtForCurrentTrack)")
            if (key == lastMetadataKey && (hasArtForCurrentTrack || !hasArt)) {
                Log.d("MediaListener", "onMetadataChanged: same track, no new art, skipping")
                return
            }
            if (key != lastMetadataKey) {
                hasArtForCurrentTrack = false
            }
            lastMetadataKey = key
            scheduleRefresh()
        }
        override fun onPlaybackStateChanged(state: PlaybackState?) {
            val stateCode = state?.state
            Log.d("MediaListener", "onPlaybackStateChanged: state=$stateCode (PLAYING=${PlaybackState.STATE_PLAYING}, PAUSED=${PlaybackState.STATE_PAUSED})")
            if (stateCode == lastPlaybackState) return
            lastPlaybackState = stateCode
            schedulePlaybackRefresh()
        }
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
        playbackRefreshJob?.cancel()
        playbackRefreshJob = null
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
        lastMetadataKey = null
        lastPlaybackState = null
        hasArtForCurrentTrack = false

        Log.d("MediaListener", "Now tracking package: ${target?.packageName}")
        scheduleRefresh()
    }

    private data class ArtResult(
        val artPath: String?,
        val blurredArtPath: String?,
        val dynamicBgColor: Int?,
        val dynamicTextColor: Color?
    )

    private suspend fun doRefresh() {
        val controller = activeController
        val metadata = controller?.metadata

        val title = metadata?.getString(MediaMetadata.METADATA_KEY_TITLE) ?: ""
        val artist = metadata?.getString(MediaMetadata.METADATA_KEY_ARTIST) ?: ""
        val isPlaying = controller?.playbackState?.state == PlaybackState.STATE_PLAYING
        val artBitmap = metadata?.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART)
            ?: metadata?.getBitmap(MediaMetadata.METADATA_KEY_ART)

        Log.d("MediaListener", "doRefresh: title=$title, artist=$artist, playing=$isPlaying, hasArt=${artBitmap != null}")

        val manager = GlanceAppWidgetManager(applicationContext)
        val vinylGlanceIds = manager.getGlanceIds(VinylWidget::class.java)
        val normalGlanceIds = manager.getGlanceIds(MusicWidget::class.java)

        if (artBitmap == null) {
            Log.d("MediaListener", "doRefresh: no art, clearing stale art and updating text")
            vinylGlanceIds.forEach { id ->
                updateAppWidgetState(applicationContext, id) { prefs ->
                    prefs[MusicWidgetKeys.TITLE] = title
                    prefs[MusicWidgetKeys.ARTIST] = artist
                    prefs[MusicWidgetKeys.IS_PLAYING] = isPlaying
                    prefs.remove(MusicWidgetKeys.ALBUM_ART_PATH)
                    prefs.remove(MusicWidgetKeys.BLURRED_ART_PATH)
                }
            }
            normalGlanceIds.forEach { id ->
                updateAppWidgetState(applicationContext, id) { prefs ->
                    prefs[MusicWidgetKeys.TITLE] = title
                    prefs[MusicWidgetKeys.ARTIST] = artist
                    prefs[MusicWidgetKeys.IS_PLAYING] = isPlaying
                    prefs.remove(MusicWidgetKeys.ALBUM_ART_PATH)
                    prefs.remove(MusicWidgetKeys.DYNAMIC_BACKGROUND_COLOR)
                    prefs.remove(MusicWidgetKeys.DYNAMIC_TEXT_COLOR)
                }
            }
            MusicWidget().updateAll(applicationContext)
            VinylWidget().updateAll(applicationContext)
            return
        }

        hasArtForCurrentTrack = true
        Log.d("MediaListener", "doRefresh: has art, entering loading skeleton")
        coroutineScope {
            // Kick off heavy IO immediately — runs concurrently with the loading-state write below.
            val artDeferred = async(Dispatchers.IO) {
                val artPath = saveAlbumArtToFile(artBitmap)
                val blurredArtPath = saveBitmapToFile(blurBitmap(artBitmap, radius = 20), "blurred_art")
                val dynamicBgColor = getPrimaryColorFromImage(artBitmap)
                val dynamicTextColor = getTextColor(Color(dynamicBgColor))
                ArtResult(artPath, blurredArtPath, dynamicBgColor, dynamicTextColor)
            }

            // Show skeleton while IO is in flight. Title/artist intentionally stay stale so
            // the skeleton is the only thing visible — no text flash mid-transition.
            vinylGlanceIds.forEach { id ->
                updateAppWidgetState(applicationContext, id) { prefs ->
                    prefs[MusicWidgetKeys.IS_LOADING] = true
                }
            }
            normalGlanceIds.forEach { id ->
                updateAppWidgetState(applicationContext, id) { prefs ->
                    prefs[MusicWidgetKeys.IS_LOADING] = true
                }
            }
            MusicWidget().updateAll(applicationContext)
            VinylWidget().updateAll(applicationContext)

            // Wait for IO, then write all final state together at once so the widget
            // transitions from skeleton to complete UI in a single update.
            val art = artDeferred.await()
            val isPlayingNow = activeController?.playbackState?.state == PlaybackState.STATE_PLAYING

            vinylGlanceIds.forEach { id ->
                updateAppWidgetState(applicationContext, id) { prefs ->
                    prefs[MusicWidgetKeys.IS_LOADING] = false
                    prefs[MusicWidgetKeys.TITLE] = title
                    prefs[MusicWidgetKeys.ARTIST] = artist
                    prefs[MusicWidgetKeys.IS_PLAYING] = isPlayingNow
                    if (art.artPath != null) prefs[MusicWidgetKeys.ALBUM_ART_PATH] = art.artPath
                    else prefs.remove(MusicWidgetKeys.ALBUM_ART_PATH)
                    if (art.blurredArtPath != null) prefs[MusicWidgetKeys.BLURRED_ART_PATH] = art.blurredArtPath
                    else prefs.remove(MusicWidgetKeys.BLURRED_ART_PATH)
                }
            }
            normalGlanceIds.forEach { id ->
                updateAppWidgetState(applicationContext, id) { prefs ->
                    prefs[MusicWidgetKeys.IS_LOADING] = false
                    prefs[MusicWidgetKeys.TITLE] = title
                    prefs[MusicWidgetKeys.ARTIST] = artist
                    prefs[MusicWidgetKeys.IS_PLAYING] = isPlayingNow
                    if (art.artPath != null) prefs[MusicWidgetKeys.ALBUM_ART_PATH] = art.artPath
                    else prefs.remove(MusicWidgetKeys.ALBUM_ART_PATH)
                    if (art.dynamicBgColor != null) prefs[MusicWidgetKeys.DYNAMIC_BACKGROUND_COLOR] = art.dynamicBgColor
                    else prefs.remove(MusicWidgetKeys.DYNAMIC_BACKGROUND_COLOR)
                    prefs[MusicWidgetKeys.DYNAMIC_TEXT_COLOR] = art.dynamicTextColor?.toArgb() ?: 1
                }
            }
            MusicWidget().updateAll(applicationContext)
            VinylWidget().updateAll(applicationContext)
        }
    }

    private var refreshJob: Job? = null
    private var playbackRefreshJob: Job? = null

    private fun schedulePlaybackRefresh() {
        if (refreshJob?.isActive == true) return
        playbackRefreshJob?.cancel()
        playbackRefreshJob = serviceScope.launch {
            delay(150.milliseconds)
            val isPlaying = activeController?.playbackState?.state == PlaybackState.STATE_PLAYING
            val manager = GlanceAppWidgetManager(applicationContext)
            val vinylGlanceIds = manager.getGlanceIds(VinylWidget::class.java)
            val normalGlanceIds = manager.getGlanceIds(MusicWidget::class.java)
            vinylGlanceIds.forEach { id ->
                updateAppWidgetState(applicationContext, id) { prefs ->
                    prefs[MusicWidgetKeys.IS_PLAYING] = isPlaying
                }
            }
            normalGlanceIds.forEach { id ->
                updateAppWidgetState(applicationContext, id) { prefs ->
                    prefs[MusicWidgetKeys.IS_PLAYING] = isPlaying
                }
            }
            MusicWidget().updateAll(applicationContext)
            VinylWidget().updateAll(applicationContext)
        }
    }

    private fun scheduleRefresh() {
        refreshJob?.cancel()
        playbackRefreshJob?.cancel()
        refreshJob = serviceScope.launch {
            delay(300.milliseconds)
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