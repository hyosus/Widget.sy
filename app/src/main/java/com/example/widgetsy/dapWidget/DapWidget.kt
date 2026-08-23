package com.example.widgetsy.dapWidget

import android.content.Context
import android.graphics.BitmapFactory
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.Preferences
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.background
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.currentState
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
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.example.widgetsy.R

@Suppress("RestrictedApi")
class DapWidget : GlanceAppWidget() {

    override val stateDefinition = PreferencesGlanceStateDefinition

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            val prefs = currentState<Preferences>()
            val title = prefs[WidgetKeys.TITLE] ?: "No track playing"
            val artist = prefs[WidgetKeys.ARTIST] ?: "Unknown artist"
            val isPlaying = prefs[WidgetKeys.IS_PLAYING] ?: false
            val albumArtPath = prefs[WidgetKeys.ALBUM_ART_PATH]
            val albumArtBitmap = albumArtPath?.let { BitmapFactory.decodeFile(it) }

            Row(
                modifier = GlanceModifier.fillMaxSize().background(ImageProvider(R.drawable.dap_widget_bg)),
                verticalAlignment = Alignment.Bottom
            ) {
                Column(
                    modifier = GlanceModifier.fillMaxSize().defaultWeight(),
                    verticalAlignment = Alignment.Bottom
                ) {
                    Column(
                        modifier = GlanceModifier.defaultWeight().padding(start = 20.dp, top = 15.dp, bottom = 15.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Text(
                            text = if (isPlaying) "Now Playing" else "Paused",
                            modifier = GlanceModifier.fillMaxWidth(),
                            style = TextStyle(
                                color = ColorProvider(Color.White.copy(alpha = 0.5f)),
                                fontSize = 12.sp
                            )
                        )

                        Box(modifier = GlanceModifier.defaultWeight()) {} // spacer pushes the rest down

                        Text(
                            text = artist,
                            modifier = GlanceModifier.fillMaxWidth(),
                            style = TextStyle(
                                color = ColorProvider(Color.White.copy(alpha = 0.8f)),
                                fontSize = 14.sp
                            )
                        )

                        Text(
                            text = title,
                            modifier = GlanceModifier.fillMaxWidth(),
                            style = TextStyle(
                                color = ColorProvider(Color.White),
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                            )
                        )
                    }
                }

                Box(
                    modifier = GlanceModifier.width(110.dp).fillMaxHeight(),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = GlanceModifier.padding(end = 15.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            modifier = GlanceModifier.size(90.dp).cornerRadius(180.dp),
                            provider = ImageProvider(R.drawable.vinyl),
                            contentDescription = "Vinyl Background"
                        )
                        Image(
                            modifier = GlanceModifier.size(55.dp).cornerRadius(80.dp),
                            provider = if (albumArtBitmap != null) {
                                ImageProvider(albumArtBitmap)
                            } else {
                                ImageProvider(R.drawable.cloud_widget_bg)
                            },
                            contentDescription = "Album Art"
                        )
                    }
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