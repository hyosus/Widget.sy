package com.example.widgetsy.vinylWidget

import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import com.example.widgetsy.vinylWidget.VinylWidget

class VinylWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = VinylWidget()
}