package com.example.widgetsy.musicWidget.normal

import android.appwidget.AppWidgetManager
import android.content.Context
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import com.example.widgetsy.musicWidget.MediaListenerService

class MusicWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = MusicWidget()

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)
        MediaListenerService.instance?.requestRefresh()
    }

    override fun onDisabled(context: Context) {
        super.onDisabled(context)
        context.cacheDir.listFiles { f ->
            f.name.startsWith("album_art_") || f.name.startsWith("blurred_art_")
        }?.forEach { it.delete() }
    }
}
