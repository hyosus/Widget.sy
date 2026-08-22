package com.example.widgetsy.dapWidget

import android.content.Context
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.fillMaxHeight
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.example.widgetsy.R


@Suppress("RestrictedApi")
class DapWidget: GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {

        // In this method, load data needed to render the AppWidget.
        // Use `withContext` to switch to another thread for long running
        // operations.

        provideContent {
            // create your AppWidget here
            Row(
                modifier = GlanceModifier.fillMaxSize().background(ImageProvider(R.drawable.dap_widget_bg)),
                verticalAlignment = Alignment.Bottom
            ) {
                Column(
                    modifier = GlanceModifier.defaultWeight().padding(bottom = 15.dp, start = 15.dp),
                    verticalAlignment = Alignment.Bottom
                ) {

                    Text(
                        text = MediaInfoHolder.currentInfo?.artist ?: "Unknown artist",
                        modifier = GlanceModifier.fillMaxWidth(),
                        style = TextStyle(
                            color = ColorProvider(Color.White.copy(alpha = 0.8f)),
                            fontSize = 14.sp
                        )
                    )

                    Text(
                        text = MediaInfoHolder.currentInfo?.title ?: "No track playing",
                        modifier = GlanceModifier.fillMaxWidth(),
                        style = TextStyle(
                            color = ColorProvider(Color.White),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    )
                }

                Box(
                    modifier = GlanceModifier.width(110.dp).fillMaxHeight(),
                    contentAlignment = Alignment.Center
                ) {
                    val albumArt = MediaInfoHolder.currentInfo?.albumArt
                    Image(
                        modifier = GlanceModifier.size(90.dp).cornerRadius(180.dp),
                        provider = ImageProvider(R.drawable.vinyl),
                        contentDescription = "Vinyl Background"
                    )
                    Image(
                        modifier = GlanceModifier.size(60.dp).cornerRadius(80.dp),
                        provider = if (albumArt != null) {
                            ImageProvider(albumArt)
                        } else {
                            ImageProvider(R.drawable.cloud_widget_bg)
                        },
                        contentDescription = "Album Art"
                    )
                    Image(
                        modifier = GlanceModifier.size(90.dp),
                        provider = ImageProvider(R.drawable.turntable_arm),
                        contentDescription = "Foreground"
                    )
                }
            }

        }
    }
}