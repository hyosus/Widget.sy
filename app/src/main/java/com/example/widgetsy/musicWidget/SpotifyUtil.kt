package com.example.widgetsy.musicWidget

import android.content.Context
import android.content.pm.PackageManager

fun isSpotifyInstalled(context: Context): Boolean {
    return try {
        context.packageManager.getPackageInfo("com.spotify.music", 0)
        true
    } catch (e: PackageManager.NameNotFoundException) {
        false
    }
}