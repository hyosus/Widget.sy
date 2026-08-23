package com.example.widgetsy.dapWidget

import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey

object WidgetKeys {
    val TITLE = stringPreferencesKey("title")
    val ARTIST = stringPreferencesKey("artist")
    val IS_PLAYING = booleanPreferencesKey("is_playing")
    val ALBUM_ART_PATH = stringPreferencesKey("album_art_path")
}   