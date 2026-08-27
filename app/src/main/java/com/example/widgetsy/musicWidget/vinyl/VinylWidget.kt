package com.example.widgetsy.musicWidget.vinyl

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.Preferences
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalSize
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.currentState
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.ContentScale
import androidx.glance.layout.Row
import androidx.glance.layout.fillMaxHeight
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.example.widgetsy.R
import com.example.widgetsy.musicWidget.MusicWidgetState
import com.example.widgetsy.musicWidget.toMusicWidgetState

@Suppress("RestrictedApi")
class VinylWidget : GlanceAppWidget() {

    override val stateDefinition = PreferencesGlanceStateDefinition

    override val sizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            val prefs = currentState<Preferences>()
            val state = prefs.toMusicWidgetState { path -> BitmapFactory.decodeFile(path) }

            val size = LocalSize.current
            // Tall/roughly-square layout kicks in once height catches up to width
            // (your 3x1/4x1 wide layout has height << width; 2x2 is closer to square/taller)
            val isTallLayout = size.height >= size.width * 0.6f

            when (state) {
                is MusicWidgetState.Loading -> {
                    if (isTallLayout) {
                        SkeletonTallVinylLayout()
                    } else {
                        SkeletonWideVinylLayout()
                    }
                }

                is MusicWidgetState.NoTrack -> {
                    if (isTallLayout) {
                        TallVinylLayout("No track playing", "No artist", null, null)
                    } else {
                        WideVinylLayout("No track playing", "No artist", isPlaying = false, albumArtBitmap = null)
                    }
                }

                is MusicWidgetState.Completed -> {
                    if (isTallLayout) {
                        TallVinylLayout(state.title, state.artist, state.albumArt, state.blurredArt)
                    } else {
                        WideVinylLayout(state.title, state.artist, state.isPlaying, state.albumArt)
                    }
                }
            }
        }
    }
}

@SuppressLint("RestrictedApi")

@Composable
private fun WideVinylLayout(
    title: String,
    artist: String,
    isPlaying: Boolean,
    albumArtBitmap: Bitmap?,
) {
    val size = LocalSize.current
    val widgetPadding = 12.dp
    val vinylBoxWidth = (size.height - widgetPadding)
    val vinylSize = vinylBoxWidth * 0.9f
    val artSize = vinylSize * 0.6f

    Row(
        modifier = GlanceModifier.fillMaxSize().background(ImageProvider(R.drawable.dap_widget_bg))
            .padding(widgetPadding),
        verticalAlignment = Alignment.Bottom
    ) {
        Column(
            modifier = GlanceModifier.fillMaxSize().defaultWeight(),
            verticalAlignment = Alignment.Bottom
        ) {
            Column(
                modifier = GlanceModifier.defaultWeight(),
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
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                    ),
                    maxLines = 2
                )
            }
        }

        Box(
            modifier = GlanceModifier.width(vinylBoxWidth).fillMaxHeight(),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = GlanceModifier.padding(end = 5.dp),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    modifier = GlanceModifier.size(vinylSize).cornerRadius(180.dp),
                    provider = ImageProvider(R.drawable.vinyl),
                    contentDescription = "Vinyl Background"
                )
                Image(
                    modifier = GlanceModifier.size(artSize).cornerRadius(80.dp),
                    provider = if (albumArtBitmap != null) {
                        ImageProvider(albumArtBitmap)
                    } else {
                        ImageProvider(R.drawable.cloud_widget_bg)
                    },
                    contentDescription = "Album Art"
                )
            }
            Box(
                modifier = GlanceModifier.padding(start = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    modifier = GlanceModifier.size(vinylSize),
                    provider = ImageProvider(R.drawable.turntable_arm),
                    contentDescription = "Foreground"
                )
            }

        }
    }
}

@Composable
private fun SkeletonWideVinylLayout() {
    val size = LocalSize.current
    val widgetPadding = 12.dp
    val vinylBoxWidth = (size.height - widgetPadding)
    val vinylSize = vinylBoxWidth * 0.9f
    val artSize = vinylSize * 0.6f

    val skeletonColor = Color(0xFFBDBDBD)

    Row(
        modifier = GlanceModifier.fillMaxSize().background(ImageProvider(R.drawable.dap_widget_bg))
            .padding(widgetPadding),
        verticalAlignment = Alignment.Bottom
    ) {
        Column(
            modifier = GlanceModifier.fillMaxSize().defaultWeight(),
            verticalAlignment = Alignment.Bottom
        ) {
            Column(
                modifier = GlanceModifier.defaultWeight(),
                verticalAlignment = Alignment.Top
            ) {
                Box(
                    modifier = GlanceModifier
                        .width(50.dp)
                        .height(14.dp)
                        .cornerRadius(4.dp)
                        .background(skeletonColor)
                ) {}

                Box(modifier = GlanceModifier.defaultWeight()) {} // spacer pushes the rest down

                Box(
                    modifier = GlanceModifier
                        .width(100.dp)
                        .height(14.dp)
                        .cornerRadius(4.dp)
                        .background(skeletonColor)
                ) {}

                Box(
                    modifier = GlanceModifier
                        .width(140.dp)
                        .height(14.dp)
                        .cornerRadius(4.dp)
                        .background(skeletonColor)
                ) {}
            }
        }

        Box(
            modifier = GlanceModifier.width(vinylBoxWidth).fillMaxHeight(),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = GlanceModifier.padding(end = 5.dp),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    modifier = GlanceModifier.size(vinylSize).cornerRadius(180.dp),
                    provider = ImageProvider(R.drawable.vinyl),
                    contentDescription = "Vinyl Background"
                )
                Image(
                    modifier = GlanceModifier.size(artSize).cornerRadius(80.dp),
                    provider = ImageProvider(R.drawable.cloud_widget_bg),
                    contentDescription = "Album Art"
                )
            }
            Box(
                modifier = GlanceModifier.padding(start = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    modifier = GlanceModifier.size(vinylSize),
                    provider = ImageProvider(R.drawable.turntable_arm),
                    contentDescription = "Foreground"
                )
            }

        }
    }
}
@SuppressLint("RestrictedApi")
@Composable
private fun TallVinylLayout(
    title: String,
    artist: String,
    albumArtBitmap: Bitmap?,
    blurredArtBitmap: Bitmap?,
) {
    val size = LocalSize.current
    var vinylSize: Dp
    var vinylBoxSize: Dp
    var artSize: Dp
    var vinylBg: Dp

    if (size.width > size.height) {
        vinylSize = size.height
        vinylBoxSize = vinylSize * 0.7f
        vinylBg = vinylBoxSize
        artSize = vinylSize * 0.45f
    } else {
        vinylSize = size.width
        vinylBoxSize = vinylSize * 0.8f
        vinylBg = vinylBoxSize * 0.9f
        artSize = vinylBg * 0.6f
    }

    Box(modifier = GlanceModifier.fillMaxSize()) {
        Image(
            modifier = GlanceModifier.fillMaxSize(),
            provider = if (blurredArtBitmap != null) {
                ImageProvider(blurredArtBitmap)
            } else {
                ImageProvider(R.drawable.dap_widget_bg)
            },
            contentScale = ContentScale.Crop,
            contentDescription = null
        )

        // Layer 2: dark scrim so white text stays readable over bright/busy art
        Box(
            modifier = GlanceModifier.fillMaxSize()
                .background(Color.Black.copy(alpha = 0.70f))
        ) {}

        // Layer 3: actual content
        Column(
            modifier = GlanceModifier.fillMaxSize().padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Vinyl + album art, centered, taking most of the vertical space
            Box(
                modifier = GlanceModifier.defaultWeight().height(vinylBoxSize).fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    modifier = GlanceModifier.size(vinylBg),
                    provider = ImageProvider(R.drawable.vinyl),
                    contentDescription = "Vinyl Background"
                )
                Image(
                    modifier = GlanceModifier.size(artSize).cornerRadius(95.dp),
                    provider = if (albumArtBitmap != null) {
                        ImageProvider(albumArtBitmap)
                    } else {
                        ImageProvider(R.drawable.cloud_widget_bg)
                    },
                    contentDescription = "Album Art"
                )

                Box(
                    modifier = GlanceModifier.padding(start = vinylSize * 0.15f),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        modifier = GlanceModifier.size(vinylSize * 0.7f),
                        provider = ImageProvider(R.drawable.turntable_arm),
                        contentDescription = "Foreground"
                    )
                }
            }

            Column(
                modifier = GlanceModifier.defaultWeight(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = title,
                    modifier = GlanceModifier.fillMaxWidth(),
                    style = TextStyle(
                        color = ColorProvider(Color.White),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    ),
                    maxLines = 2
                )
                Text(
                    text = artist,
                    modifier = GlanceModifier.fillMaxWidth().padding(top = 2.dp),
                    style = TextStyle(
                        color = ColorProvider(Color.White.copy(alpha = 0.75f)),
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center
                    )
                )
            }


        }
    }
}

@Composable
private fun SkeletonTallVinylLayout() {
    val size = LocalSize.current
    var vinylSize: Dp
    var vinylBoxSize: Dp
    var artSize: Dp
    var vinylBg: Dp

    val skeletonColor = Color(0xFFBDBDBD)

    if (size.width > size.height) {
        vinylSize = size.height
        vinylBoxSize = vinylSize * 0.7f
        vinylBg = vinylBoxSize
        artSize = vinylSize * 0.45f
    } else {
        vinylSize = size.width
        vinylBoxSize = vinylSize * 0.8f
        vinylBg = vinylBoxSize * 0.9f
        artSize = vinylBg * 0.6f
    }

    Box(modifier = GlanceModifier.fillMaxSize()) {
        Image(
            modifier = GlanceModifier.fillMaxSize(),
            provider = ImageProvider(R.drawable.dap_widget_bg),
            contentScale = ContentScale.Crop,
            contentDescription = null
        )

        // Layer 2: dark scrim so white text stays readable over bright/busy art
        Box(
            modifier = GlanceModifier.fillMaxSize()
                .background(Color.Black.copy(alpha = 0.70f))
        ) {}

        // Layer 3: actual content
        Column(
            modifier = GlanceModifier.fillMaxSize().padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Vinyl + album art, centered, taking most of the vertical space
            Box(
                modifier = GlanceModifier.defaultWeight().height(vinylBoxSize).fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    modifier = GlanceModifier.size(vinylBg),
                    provider = ImageProvider(R.drawable.vinyl),
                    contentDescription = "Vinyl Background"
                )
                Image(
                    modifier = GlanceModifier.size(artSize).cornerRadius(95.dp),
                    provider = ImageProvider(R.drawable.cloud_widget_bg),
                    contentDescription = "Album Art"
                )

                Box(
                    modifier = GlanceModifier.padding(start = vinylSize * 0.15f),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        modifier = GlanceModifier.size(vinylSize * 0.7f),
                        provider = ImageProvider(R.drawable.turntable_arm),
                        contentDescription = "Foreground"
                    )
                }
            }

            Column(
                modifier = GlanceModifier.defaultWeight(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = GlanceModifier
                        .width(140.dp)
                        .height(14.dp)
                        .cornerRadius(4.dp)
                        .background(skeletonColor)
                ) {}
                Box(
                    modifier = GlanceModifier
                        .width(100.dp)
                        .height(14.dp)
                        .cornerRadius(4.dp)
                        .background(skeletonColor)
                ) {}
            }


        }
    }
}