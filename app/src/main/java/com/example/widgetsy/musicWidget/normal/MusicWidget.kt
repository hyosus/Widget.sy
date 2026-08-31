package com.example.widgetsy.musicWidget.normal

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.Preferences
import androidx.glance.ColorFilter
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionSendBroadcast
import androidx.glance.appwidget.components.Scaffold
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.currentState
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.example.widgetsy.R
import com.example.widgetsy.musicWidget.MediaControlReceiver
import com.example.widgetsy.musicWidget.MusicWidgetKeys
import com.example.widgetsy.musicWidget.MusicWidgetState
import com.example.widgetsy.musicWidget.toMusicWidgetState

class MusicWidget: GlanceAppWidget() {
    override val sizeMode = SizeMode.Exact
    override val stateDefinition = PreferencesGlanceStateDefinition

    @SuppressLint("RestrictedApi")
    override suspend fun provideGlance(
        context: Context,
        id: GlanceId
    ) {
        provideContent {
            val prefs = currentState<Preferences>()
            val state = prefs.toMusicWidgetState { path -> BitmapFactory.decodeFile(path) }


            when (state) {
                is MusicWidgetState.Loading -> {
                    LoadingSkeletonLayout(bgColor = prefs[MusicWidgetKeys.DYNAMIC_BACKGROUND_COLOR])
                }

                is MusicWidgetState.NoTrack -> {
                    Log.d("MusicWidget", "FUCK")
                    WidgetLayout("No track playing", "No artist", false, null, Color.Black, null)
                }

                is MusicWidgetState.Completed -> {
                    Log.d("MusicWidget", "BGCOLOR argbColor: ${state.dynamicBgColor}")
                    Log.d("MusicWidget", "Track: ${state.title}, Artist: ${state.artist}, Album Art: ${state.albumArt}, Is Paused: ${state.isPlaying}")
                    state.dynamicTextColor?.let { WidgetLayout(state.title, state.artist, state.isPlaying, state.albumArt, Color(it), state.dynamicBgColor) }
                }
            }
        }
    }

    override suspend fun providePreview(context: Context, widgetCategory: Int) {
        provideContent {
            WidgetLayout("Song Title", "Artist", false, null, Color.Black, null)
        }
    }

    override val previewSizeMode = SizeMode.Responsive(
        setOf(
            DpSize(250.dp, 80.dp),
        )
    )
}

@SuppressLint("RestrictedApi")
@Composable
private fun LoadingSkeletonLayout(bgColor: Int?) {
    val height = LocalSize.current.height
    val skeletonColor = if (bgColor != null) Color.White.copy(alpha = 0.35f) else Color(0xFFBDBDBD)

    Scaffold(
        modifier = GlanceModifier.fillMaxSize().padding(vertical = 12.dp),
        backgroundColor = if (bgColor != null) ColorProvider(Color(bgColor)) else ColorProvider(Color.White)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = GlanceModifier
                    .size(height * 0.75f)
                    .cornerRadius(12.dp)
                    .background(skeletonColor)
            ) {}

            Spacer(modifier = GlanceModifier.size(12.dp))

            Column(
                modifier = GlanceModifier.fillMaxSize(),
                verticalAlignment = Alignment.Bottom
            ) {
                Column(
                    modifier = GlanceModifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Top
                ) {
                    Box(
                        modifier = GlanceModifier
                            .fillMaxWidth()
                            .height(18.dp)
                            .cornerRadius(4.dp)
                            .background(skeletonColor)
                    ) {}
                    Spacer(modifier = GlanceModifier.size(8.dp))
                    Box(
                        modifier = GlanceModifier
                            .width(100.dp)
                            .height(14.dp)
                            .cornerRadius(4.dp)
                            .background(skeletonColor)
                    ) {}
                }

                Box(modifier = GlanceModifier.defaultWeight()) {}

                Row(modifier = GlanceModifier.fillMaxWidth()) {
                    Box(modifier = GlanceModifier.size(26.dp).cornerRadius(13.dp).background(skeletonColor)) {}
                    Spacer(modifier = GlanceModifier.width(34.dp))
                    Box(modifier = GlanceModifier.size(26.dp).cornerRadius(13.dp).background(skeletonColor)) {}
                    Spacer(modifier = GlanceModifier.width(34.dp))
                    Box(modifier = GlanceModifier.size(26.dp).cornerRadius(13.dp).background(skeletonColor)) {}
                }
            }
        }
    }
}

@SuppressLint("RestrictedApi")
@Composable
private fun WidgetLayout(
    title: String,
    artist: String,
    isPlaying: Boolean,
    albumArtBitmap: Bitmap?,
    textColor: Color,
    bgColor: Int?
) {
    val size = LocalSize.current
    val height = size.height
    val context = LocalContext.current

    Scaffold(
        modifier = GlanceModifier.fillMaxSize()
            .padding(vertical = 12.dp, horizontal = 2.dp),
        backgroundColor = if (bgColor != null) {
            ColorProvider(Color(bgColor))
        } else {
            ColorProvider(Color.White)
        }
    )
    {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Image(
                modifier = GlanceModifier.size(height * 0.75f).cornerRadius(12.dp),
                provider = if (albumArtBitmap != null) {
                    ImageProvider(albumArtBitmap)
                } else {
                    ImageProvider(R.drawable.default_cover)
                },
                contentDescription = "Album cover"
            )

            Spacer(modifier = GlanceModifier.size(12.dp))

            Column(
                modifier = GlanceModifier.fillMaxSize(),
                verticalAlignment = Alignment.Bottom
            ) {
                Column(
                    modifier = GlanceModifier.fillMaxWidth().padding(start = 4.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Text(
                        modifier = GlanceModifier.fillMaxWidth(),
                        text = title,
                        style = TextStyle(
                            color = ColorProvider(textColor),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                        ),
                        maxLines = 2
                    )
                    Text(
                        text = artist,
                        style = TextStyle(
                            color = ColorProvider(textColor.copy(alpha = 0.8f)),
                            fontSize = 14.sp
                        )
                    )

                }

                Box(modifier = GlanceModifier.defaultWeight()) {} // spacer pushes the rest down

                Row(
                    modifier = GlanceModifier.fillMaxWidth(),
                ) {
                    Image(
                        modifier = GlanceModifier.size(26.dp)
                            .clickable(actionSendBroadcast(
                                Intent(MediaControlReceiver.ACTION_PREVIOUS)
                                    .setClass(context, MediaControlReceiver::class.java)
                            )),
                        provider = ImageProvider(R.drawable.skip_previous),
                        contentDescription = "Previous track",
                        colorFilter = ColorFilter.tint(ColorProvider(textColor))
                    )
                    Spacer(modifier = GlanceModifier.width(34.dp))
                    Image(
                        modifier = GlanceModifier.size(26.dp)
                            .clickable(actionSendBroadcast(
                                Intent(MediaControlReceiver.ACTION_PLAY_PAUSE)
                                    .setClass(context, MediaControlReceiver::class.java)
                            )),
                        provider = ImageProvider(
                            if (isPlaying) R.drawable.pause
                            else R.drawable.play_arrow
                        ),
                        contentDescription = if (isPlaying) "Pause" else "Play",
                        colorFilter = ColorFilter.tint(ColorProvider(textColor))
                    )
                    Spacer(modifier = GlanceModifier.width(34.dp))
                    Image(
                        modifier = GlanceModifier.size(26.dp)
                            .clickable(actionSendBroadcast(
                                Intent(MediaControlReceiver.ACTION_NEXT)
                                    .setClass(context, MediaControlReceiver::class.java)
                            )),
                        provider = ImageProvider(R.drawable.skip_next),
                        contentDescription = "Next track",
                        colorFilter = ColorFilter.tint(ColorProvider(textColor))
                    )
                }
            }

        }
    }
}
