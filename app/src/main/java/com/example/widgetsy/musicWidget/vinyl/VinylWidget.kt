package com.example.widgetsy.musicWidget.vinyl

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
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

    override suspend fun providePreview(context: Context, widgetCategory: Int) {
        provideContent {
            val size = LocalSize.current  // will be each DpSize from previewSizeMode in turn
            val isTallLayout = size.height >= size.width
            if (isTallLayout) {
                TallVinylLayout("Song Title", "Artist", null, null)
            } else {
                WideVinylLayout("Song Title", "Artist", false, null)
            }
        }
    }

    override val previewSizeMode = SizeMode.Responsive(
        setOf(
            DpSize(250.dp, 80.dp),   // wide → WideVinylLayout
            DpSize(120.dp, 200.dp),  // tall → TallVinylLayout
        )
    )
}

// ── Dimension helpers ────────────────────────────────────────────────
private data class WideDims(
    val widgetPadding: Dp,
    val colPadding: Dp,
    val vinylBoxWidth: Dp,
    val vinylSize: Dp,
    val artSize: Dp,
    val circleBottomPadding: Dp,
    val shiftVinylPadding: Dp,
    val armDrawable: Int,
    val armStartPadding: Dp,
    val armBottomPadding: Dp,
    val armSize: Dp
)

private fun getWideDims(size: DpSize): WideDims {
    val height = size.height
    val widgetPadding: Dp
    val colPadding: Dp
    val vinylBoxWidth: Dp
    val vinylSize: Dp
    val artSize: Dp
    val circleBottomPadding: Dp
    val shiftVinylPadding: Dp
    val armDrawable: Int
    val armStartPadding: Dp
    val armBottomPadding: Dp
    val armSize: Dp

    if (height > 110.dp) {
        widgetPadding = 24.dp
        colPadding = 6.dp
        vinylBoxWidth = height * 0.85f
        vinylSize = vinylBoxWidth * 0.9f
        artSize = vinylSize * 0.5f
        armDrawable = R.drawable.turntable_arm_2
        armSize = vinylSize * 1.03f
        armStartPadding = vinylBoxWidth * 0.08f
        armBottomPadding = vinylBoxWidth * 0.18f
        circleBottomPadding = vinylBoxWidth * 0.15f
        shiftVinylPadding = vinylBoxWidth * 0.06f
    } else {
        widgetPadding = 12.dp
        colPadding = 0.dp
        vinylBoxWidth = height
        vinylSize = height - 24.dp
        artSize = vinylSize * 0.46f
        armDrawable = R.drawable.turntable_arm
        armSize = vinylSize * 1.18f
        armStartPadding = vinylBoxWidth * 0.09f
        armBottomPadding = 0.dp
        circleBottomPadding = 0.dp
        shiftVinylPadding = 2.dp
    }

    return WideDims(
        widgetPadding = widgetPadding,
        colPadding = colPadding,
        vinylBoxWidth = vinylBoxWidth,
        vinylSize = vinylSize,
        artSize = artSize,
        circleBottomPadding = circleBottomPadding,
        shiftVinylPadding = shiftVinylPadding,
        armDrawable = armDrawable,
        armStartPadding = armStartPadding,
        armBottomPadding = armBottomPadding,
        armSize = armSize
    )
}

private data class TallDims(
    val vinylSize: Dp,
    val vinylBoxSize: Dp,
    val artSize: Dp,
    val vinylBg: Dp,
    val circleBottomPadding: Dp,
    val circleStartPadding: Dp,
    val armStartPadding: Dp,
    val armBottomPadding: Dp
)

private fun getTallDims(size: DpSize): TallDims {
    val width = size.width
    val height = size.height
    val vinylSize: Dp
    val vinylBoxSize: Dp
    val artSize: Dp
    val vinylBg: Dp
    val circleBottomPadding: Dp
    val circleStartPadding: Dp
    val armStartPadding: Dp
    val armBottomPadding: Dp

    if (width > height) {
        vinylSize = height
        vinylBoxSize = vinylSize * 0.7f
        vinylBg = vinylBoxSize * 0.95f
        artSize = vinylSize * 0.45f
        circleBottomPadding = 0.dp
        circleStartPadding = vinylSize * 0.04f
        armStartPadding = vinylBoxSize * 0.12f
        armBottomPadding = vinylBoxSize * 0.1f
    } else {
        vinylSize = width
        vinylBoxSize = vinylSize * 0.76f
        vinylBg = vinylBoxSize * 0.88f
        artSize = vinylBg * 0.6f
        circleBottomPadding = vinylSize * 0.04f
        circleStartPadding = vinylSize * 0.04f
        armStartPadding = vinylBoxSize * 0.02f
        armBottomPadding = vinylBoxSize * 0.08f
    }

    return TallDims(
        vinylSize = vinylSize,
        vinylBoxSize = vinylBoxSize,
        artSize = artSize,
        vinylBg = vinylBg,
        circleBottomPadding = circleBottomPadding,
        circleStartPadding = circleStartPadding,
        armStartPadding = armStartPadding,
        armBottomPadding = armBottomPadding
    )
}

// ── Real Wide Layout ─────────────────────────────────────────────────
@SuppressLint("RestrictedApi")
@Composable
private fun WideVinylLayout(
    title: String,
    artist: String,
    isPlaying: Boolean,
    albumArtBitmap: Bitmap?,
) {
    val size = LocalSize.current
    val dims = getWideDims(size)

    Row(
        modifier = GlanceModifier.fillMaxSize().background(ImageProvider(R.drawable.dap_widget_bg))
            .padding(dims.widgetPadding),
        verticalAlignment = Alignment.Bottom
    ) {
        Column(
            modifier = GlanceModifier.fillMaxSize().defaultWeight().padding(start = dims.colPadding, top = dims.colPadding, bottom = dims.colPadding),
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

                Box(modifier = GlanceModifier.defaultWeight()) {} // spacer

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
            modifier = GlanceModifier.width(dims.vinylBoxWidth).fillMaxHeight(),
            contentAlignment = Alignment.Center
        ) {
            // Circle
            Box(
                modifier = GlanceModifier.padding(start = 8.dp, bottom = dims.circleBottomPadding),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    provider = ImageProvider(R.drawable.turntable_circle),
                    contentDescription = "Arm Circle"
                )
            }

            // Vinyl + album art
            Box(
                modifier = GlanceModifier.padding(end = dims.shiftVinylPadding),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    modifier = GlanceModifier.size(dims.artSize).cornerRadius(80.dp),
                    provider = if (albumArtBitmap != null) {
                        ImageProvider(albumArtBitmap)
                    } else {
                        ImageProvider(R.drawable.cloud_widget_bg)
                    },
                    contentDescription = "Album Art"
                )

                Image(
                    modifier = GlanceModifier.size(dims.vinylSize),
                    provider = ImageProvider(R.drawable.vinyl),
                    contentDescription = "Vinyl Background"
                )
            }

            // Arm
            Box(
                modifier = GlanceModifier.padding(start = dims.armStartPadding, bottom = dims.armBottomPadding),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    modifier = GlanceModifier.size(dims.armSize),
                    provider = ImageProvider(dims.armDrawable),
                    contentDescription = "Turntable Arm"
                )
            }
        }
    }
}

// ── Skeleton Wide Layout ─────────────────────────────────────────────
@SuppressLint("RestrictedApi")
@Composable
private fun SkeletonWideVinylLayout() {
    val size = LocalSize.current
    val dims = getWideDims(size)
    val skeletonColor = Color(0xFFBDBDBD)

    Row(
        modifier = GlanceModifier.fillMaxSize().background(ImageProvider(R.drawable.dap_widget_bg))
            .padding(dims.widgetPadding),
        verticalAlignment = Alignment.Bottom
    ) {
        Column(
            modifier = GlanceModifier.fillMaxSize().defaultWeight().padding(start = dims.colPadding, top = dims.colPadding, bottom = dims.colPadding),
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

                Box(modifier = GlanceModifier.defaultWeight()) {} // spacer

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
            modifier = GlanceModifier.width(dims.vinylBoxWidth).fillMaxHeight(),
            contentAlignment = Alignment.Center
        ) {
            // Circle (real image)
            Box(
                modifier = GlanceModifier.padding(start = 8.dp, bottom = dims.circleBottomPadding),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    provider = ImageProvider(R.drawable.turntable_circle),
                    contentDescription = "Arm Circle"
                )
            }

            // Vinyl + skeleton album art
            Box(
                modifier = GlanceModifier.padding(end = dims.shiftVinylPadding),
                contentAlignment = Alignment.Center
            ) {
                // Skeleton art placeholder
                Box(
                    modifier = GlanceModifier.size(dims.artSize).cornerRadius(80.dp).background(skeletonColor)
                ) {}

                // Real vinyl
                Image(
                    modifier = GlanceModifier.size(dims.vinylSize),
                    provider = ImageProvider(R.drawable.vinyl),
                    contentDescription = "Vinyl Background"
                )
            }

            // Arm (real image)
            Box(
                modifier = GlanceModifier.padding(start = dims.armStartPadding, bottom = dims.armBottomPadding),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    modifier = GlanceModifier.size(dims.armSize),
                    provider = ImageProvider(dims.armDrawable),
                    contentDescription = "Turntable Arm"
                )
            }
        }
    }
}

// ── Real Tall Layout ─────────────────────────────────────────────────
@SuppressLint("RestrictedApi")
@Composable
private fun TallVinylLayout(
    title: String,
    artist: String,
    albumArtBitmap: Bitmap?,
    blurredArtBitmap: Bitmap?,
) {
    val size = LocalSize.current
    val dims = getTallDims(size)

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

        Box(
            modifier = GlanceModifier.fillMaxSize()
                .background(Color(0xff2b2b2b).copy(0.76f))
        ) {}

        Column(
            modifier = GlanceModifier.fillMaxSize().padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = GlanceModifier.defaultWeight().height(dims.vinylBoxSize).fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = GlanceModifier.padding(end = dims.vinylSize * 0.05f),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        modifier = GlanceModifier.size(dims.artSize).cornerRadius(95.dp),
                        provider = if (albumArtBitmap != null) {
                            ImageProvider(albumArtBitmap)
                        } else {
                            ImageProvider(R.drawable.cloud_widget_bg)
                        },
                        contentDescription = "Album Art"
                    )

                    Box(
                        modifier = GlanceModifier.padding(start = dims.circleStartPadding, bottom = dims.circleBottomPadding),
                    ) {
                        Image(
                            provider = ImageProvider(R.drawable.turntable_circle),
                            contentDescription = "Turntable Circle"
                        )
                    }

                    Image(
                        modifier = GlanceModifier.size(dims.vinylBg),
                        provider = ImageProvider(R.drawable.vinyl),
                        contentDescription = "Vinyl Background"
                    )
                }

                Box(
                    modifier = GlanceModifier.defaultWeight().height(dims.vinylBoxSize * 0.95f).fillMaxWidth().padding(dims.armStartPadding, bottom = dims.armBottomPadding),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        modifier = GlanceModifier,
                        provider = ImageProvider(R.drawable.turntable_arm),
                        contentDescription = "Turntable Arm"
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

// ── Skeleton Tall Layout ─────────────────────────────────────────────
@SuppressLint("RestrictedApi")
@Composable
private fun SkeletonTallVinylLayout() {
    val size = LocalSize.current
    val dims = getTallDims(size)
    val skeletonColor = Color(0xFFBDBDBD)

    Box(modifier = GlanceModifier.fillMaxSize()) {
        Image(
            modifier = GlanceModifier.fillMaxSize(),
            provider = ImageProvider(R.drawable.dap_widget_bg),
            contentScale = ContentScale.Crop,
            contentDescription = null
        )

        Box(
            modifier = GlanceModifier.fillMaxSize()
                .background(Color.Black.copy(alpha = 0.70f))
        ) {}

        Column(
            modifier = GlanceModifier.fillMaxSize().padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = GlanceModifier.defaultWeight().height(dims.vinylBoxSize).fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = GlanceModifier.padding(end = dims.vinylSize * 0.05f),
                    contentAlignment = Alignment.Center
                ) {
                    // Skeleton art placeholder
                    Box(
                        modifier = GlanceModifier.size(dims.artSize).cornerRadius(95.dp).background(skeletonColor)
                    ) {}

                    // Circle
                    Box(
                        modifier = GlanceModifier.padding(start = dims.circleStartPadding, bottom = dims.circleBottomPadding),
                    ) {
                        Image(
                            provider = ImageProvider(R.drawable.turntable_circle),
                            contentDescription = "Turntable Circle"
                        )
                    }

                    // Real vinyl
                    Image(
                        modifier = GlanceModifier.size(dims.vinylBg),
                        provider = ImageProvider(R.drawable.vinyl),
                        contentDescription = "Vinyl Background"
                    )
                }

                // Real arm
                Box(
                    modifier = GlanceModifier.defaultWeight().height(dims.vinylBoxSize * 0.95f).fillMaxWidth().padding(dims.armStartPadding, bottom = dims.armBottomPadding),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        modifier = GlanceModifier,
                        provider = ImageProvider(R.drawable.turntable_arm),
                        contentDescription = "Turntable Arm"
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