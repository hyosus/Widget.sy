package com.example.widgetsy.musicWidget

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.session.PlaybackState
import android.util.Log

class MediaControlReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_PREVIOUS = "com.example.widgetsy.MEDIA_PREVIOUS"
        const val ACTION_PLAY_PAUSE = "com.example.widgetsy.MEDIA_PLAY_PAUSE"
        const val ACTION_NEXT = "com.example.widgetsy.MEDIA_NEXT"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val controls = MediaListenerService.instance?.currentController?.transportControls
        if (controls == null) {
            Log.w("MediaControlReceiver", "Received ${intent.action} but service is not connected")
            return
        }

        Log.w("MediaControlReceiver", "Received ${intent.action}")

        when (intent.action) {
            ACTION_PREVIOUS -> controls.skipToPrevious()
            ACTION_PLAY_PAUSE -> {
                val state = MediaListenerService.instance?.currentController?.playbackState?.state
                if (state == PlaybackState.STATE_PLAYING) controls.pause() else controls.play()
            }
            ACTION_NEXT -> controls.skipToNext()
        }
    }
}