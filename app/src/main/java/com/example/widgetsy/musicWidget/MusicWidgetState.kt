package com.example.widgetsy.musicWidget

import android.graphics.Bitmap
import androidx.datastore.preferences.core.Preferences

sealed interface MusicWidgetState {

    /** A refresh is in-flight; data is not yet ready to display. */
    data object Loading : MusicWidgetState

    /** We have data, but nothing is currently tracked/playing. */
    data object NoTrack : MusicWidgetState

    data class Completed(
        val title: String,
        val artist: String,
        val isPlaying: Boolean,
        val albumArt: Bitmap?,
        val blurredArt: Bitmap?,
        val dynamicBgColor: Int?,
        val dynamicTextColor: Int?,
        /** A refresh (e.g. track skip) is in-flight; keep showing this data but as a skeleton. */
        val isRefreshing: Boolean = false
    ) : MusicWidgetState
}

/**
 * Maps the persisted Glance Preferences into a typed widget state.
 * Keeps the composable free of raw preference reads / ad-hoc null fallbacks.
 */
fun Preferences.toMusicWidgetState(
    decodeBitmap: (String) -> Bitmap?
): MusicWidgetState {
    val isLoading = this[MusicWidgetKeys.IS_LOADING] == true

    val title = this[MusicWidgetKeys.TITLE]
    val artist = this[MusicWidgetKeys.ARTIST]
    val isPlaying = this[MusicWidgetKeys.IS_PLAYING]
    val albumArtPath = this[MusicWidgetKeys.ALBUM_ART_PATH]
    val blurredArtPath = this[MusicWidgetKeys.BLURRED_ART_PATH]
    val dynamicBgColor = this[MusicWidgetKeys.DYNAMIC_BACKGROUND_COLOR]
    val dynamicTextColor = this[MusicWidgetKeys.DYNAMIC_TEXT_COLOR]

    // Nothing has ever been shown yet — fall back to the generic loading/empty UI.
    if (title.isNullOrEmpty()) {
        return if (isLoading) MusicWidgetState.Loading else MusicWidgetState.NoTrack
    }

    // Keep showing the last known track/colors while a refresh is in-flight, so a
    // skip doesn't flash the widget back to a blank default background.
    return MusicWidgetState.Completed(
        title = title,
        artist = artist.takeUnless { it.isNullOrEmpty() } ?: "Unknown artist",
        isPlaying = isPlaying ?: false,
        albumArt = albumArtPath?.let(decodeBitmap),
        blurredArt = blurredArtPath?.let(decodeBitmap),
        dynamicBgColor = dynamicBgColor,
        dynamicTextColor = dynamicTextColor,
        isRefreshing = isLoading
    )
}