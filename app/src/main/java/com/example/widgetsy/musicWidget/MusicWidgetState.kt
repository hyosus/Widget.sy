package com.example.widgetsy.musicWidget

import android.graphics.Bitmap
import androidx.datastore.preferences.core.Preferences

sealed interface MusicWidgetState {

    /** No media info has ever been written yet (e.g. right after install). */
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
        val dynamicTextColor: Int?
    ) : MusicWidgetState
}

/**
 * Maps the persisted Glance Preferences into a typed widget state.
 * Keeps the composable free of raw preference reads / ad-hoc null fallbacks.
 */
fun Preferences.toMusicWidgetState(
    decodeBitmap: (String) -> Bitmap?
): MusicWidgetState {
    val title = this[MusicWidgetKeys.TITLE]
    val artist = this[MusicWidgetKeys.ARTIST]
    val isPlaying = this[MusicWidgetKeys.IS_PLAYING]
    val albumArtPath = this[MusicWidgetKeys.ALBUM_ART_PATH]
    val blurredArtPath = this[MusicWidgetKeys.BLURRED_ART_PATH]
    val dynamicBgColor = this[MusicWidgetKeys.DYNAMIC_BACKGROUND_COLOR]
    val dynamicTextColor = this[MusicWidgetKeys.DYNAMIC_TEXT_COLOR]
    val isLoading = this[MusicWidgetKeys.IS_LOADING]

    if (isLoading == true) return MusicWidgetState.Loading

    // We have state, but it represents "nothing playing".
    if (title.isNullOrEmpty()) {
        return MusicWidgetState.NoTrack
    }

    return MusicWidgetState.Completed(
        title = title,
        artist = artist.takeUnless { it.isNullOrEmpty() } ?: "Unknown artist",
        isPlaying = isPlaying ?: false,
        albumArt = albumArtPath?.let(decodeBitmap),
        blurredArt = blurredArtPath?.let(decodeBitmap),
        dynamicBgColor = dynamicBgColor,
        dynamicTextColor
    )
}