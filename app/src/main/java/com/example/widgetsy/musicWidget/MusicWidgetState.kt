package com.example.widgetsy.musicWidget


import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow

// Define preference keys
object PreferenceKeys {
    val TRACK_NAME = stringPreferencesKey("track_name")
    val ARTIST_NAME = stringPreferencesKey("artist_name")
    val ALBUM_ART_URI = stringPreferencesKey("album_art_uri")
}

class MusicRepository(context: Context) {
    // create datastore instance
    private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "spotify_track")
    private val dataStore = context.dataStore

    companion object {
        val trackNameKey = stringPreferencesKey("TRACK_NAME_KEY")
    }

    suspend fun setTrackName(trackName: String) {
        dataStore.edit { pref ->
            pref[trackNameKey] = trackName
        }
    }

}