package com.example.widgetsy.musicWidget.normal

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.background
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionSendBroadcast
import androidx.glance.appwidget.components.CircleIconButton
import androidx.glance.appwidget.components.Scaffold
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.state.getAppWidgetState
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
import com.example.widgetsy.musicWidget.MusicWidgetState
import com.example.widgetsy.musicWidget.toMusicWidgetState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MusicWidget: GlanceAppWidget() {
    override val sizeMode = SizeMode.Exact
    override val stateDefinition = PreferencesGlanceStateDefinition

    @SuppressLint("RestrictedApi")
    override suspend fun provideGlance(
        context: Context,
        id: GlanceId
    ) {
        val prefs = getAppWidgetState(context, PreferencesGlanceStateDefinition, id)
        val state = withContext(Dispatchers.IO) {
            prefs.toMusicWidgetState { path -> BitmapFactory.decodeFile(path) }
        }

        provideContent {
            when (state) {
                is MusicWidgetState.Loading -> {
                    WidgetLayout("Loading…", "", false, null, Color.Black, null, isRefreshing = false)
                }

                is MusicWidgetState.NoTrack -> {
                    WidgetLayout("No track playing", "No artist", false, null, Color.Black, null, isRefreshing = false)
                }

                is MusicWidgetState.Completed -> {
                    val textColor = state.dynamicTextColor?.let { Color(it) } ?: Color.Black
                    WidgetLayout(
                        state.title,
                        state.artist,
                        state.isPlaying,
                        state.albumArt,
                        textColor,
                        state.dynamicBgColor,
                        isRefreshing = state.isRefreshing
                    )
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
    bgColor: Int?,
    isRefreshing: Boolean
) {
    val size = LocalSize.current
    val height = size.height
    val context = LocalContext.current

    Scaffold(
        modifier = GlanceModifier.fillMaxSize()
            .padding(vertical = 12.dp),
        backgroundColor = if (bgColor != null) {
            ColorProvider(Color(bgColor))
        } else {
            ColorProvider(Color.White)
        }
    )
    {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (isRefreshing) {
                Box(
                    modifier = GlanceModifier.size(height * 0.75f)
                        .cornerRadius(12.dp)
                        .background(textColor.copy(alpha = 0.15f))
                ) {}
            } else {
                Image(
                    modifier = GlanceModifier.size(height * 0.75f).cornerRadius(12.dp),
                    provider = if (albumArtBitmap != null) {
                        ImageProvider(albumArtBitmap)
                    } else {
                        ImageProvider(R.drawable.vinyl)
                    },
                    contentDescription = "Album cover"
                )
            }

            Spacer(modifier = GlanceModifier.size(12.dp))

            Column(
                modifier = GlanceModifier.fillMaxSize(),
                verticalAlignment = Alignment.Bottom
            ) {
                Column(
                    modifier = GlanceModifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Top
                ) {
                    if (isRefreshing) {
                        Box(
                            modifier = GlanceModifier.width(120.dp).height(14.dp)
                                .cornerRadius(4.dp)
                                .background(textColor.copy(alpha = 0.15f))
                        ) {}
                        Spacer(modifier = GlanceModifier.size(6.dp))
                        Box(
                            modifier = GlanceModifier.width(80.dp).height(12.dp)
                                .cornerRadius(4.dp)
                                .background(textColor.copy(alpha = 0.15f))
                        ) {}
                    } else {
                        Text(
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
                }

                Box(modifier = GlanceModifier.defaultWeight()) {} // spacer pushes the rest down

                Row(
                    modifier = GlanceModifier.fillMaxWidth(),
                ) {
                    CircleIconButton(
                        imageProvider = ImageProvider(android.R.drawable.ic_media_previous),
                        contentDescription = "Previous track",
                        backgroundColor = null,
                        contentColor = ColorProvider(textColor),
                        onClick = actionSendBroadcast(
                            Intent(MediaControlReceiver.ACTION_PREVIOUS)
                                .setClass(context, MediaControlReceiver::class.java)
                        )
                    )
                    CircleIconButton(
                        imageProvider = ImageProvider(
                            if (isPlaying) android.R.drawable.ic_media_pause
                            else android.R.drawable.ic_media_play
                        ),
                        contentDescription = if (isPlaying) "Pause" else "Play",
                        backgroundColor = null,
                        contentColor = ColorProvider(textColor),
                        onClick = actionSendBroadcast(
                            Intent(MediaControlReceiver.ACTION_PLAY_PAUSE)
                                .setClass(context, MediaControlReceiver::class.java)
                        )
                    )
                    CircleIconButton(
                        imageProvider = ImageProvider(android.R.drawable.ic_media_next),
                        contentDescription = "Next track",
                        backgroundColor = null,
                        contentColor = ColorProvider(textColor),
                        onClick = actionSendBroadcast(
                            Intent(MediaControlReceiver.ACTION_NEXT)
                                .setClass(context, MediaControlReceiver::class.java)
                        )
                    )
                }
            }
        }
    }
}