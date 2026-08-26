package com.example.widgetsy.musicWidget

import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey

object MusicWidgetKeys {
    val TITLE = stringPreferencesKey("title")
    val ARTIST = stringPreferencesKey("artist")
    val IS_PLAYING = booleanPreferencesKey("is_playing")
    val IS_LOADING = booleanPreferencesKey("is_loading")
    val ALBUM_ART_PATH = stringPreferencesKey("album_art_path")

    val BLURRED_ART_PATH = stringPreferencesKey("blurred_art_path")

    val DYNAMIC_BACKGROUND_COLOR = intPreferencesKey("dynamic_background_color")
    val DYNAMIC_TEXT_COLOR = intPreferencesKey("dynamic_text_color")
}