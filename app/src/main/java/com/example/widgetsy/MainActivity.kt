package com.example.widgetsy

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import com.example.widgetsy.musicWidget.SpotifyService
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.unit.dp
import androidx.palette.graphics.Palette
import com.example.widgetsy.ui.theme.WeatherAppTheme
import androidx.compose.ui.graphics.asAndroidBitmap


class MainActivity : ComponentActivity() {
    private lateinit var spotifyService: SpotifyService

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        spotifyService = SpotifyService(this)

        setContent {
            WeatherAppTheme {
                Scaffold(
                    modifier = Modifier.fillMaxSize().padding(16.dp),
                ) { innerPadding ->
                    Column(modifier = Modifier.padding(innerPadding)) {
                        CurrentTrackDisplay(spotifyService)
                    }
                }

            }
        }

        requestIgnoreBatteryOptimizations(this)
    }

    override fun onStart() {
        super.onStart()
        spotifyService.connectSpotifyAppRemote()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        spotifyService.handleAuthResponse(requestCode, resultCode, data)
    }

    fun requestIgnoreBatteryOptimizations(context: Context) {
        val intent = Intent()
        val powerManager = getSystemService(POWER_SERVICE) as? PowerManager

        if (powerManager != null && !powerManager.isIgnoringBatteryOptimizations(context.packageName)) {
            Log.d("MainActivity", "Requesting ignore battery optimizations")
            intent.setAction(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
            intent.data = Uri.parse("package:" + context.packageName)
            context.startActivity(intent)
        }
    }

    fun getPrimaryColorFromImage(bitmap: Bitmap): Int {
        val palette = Palette.from(bitmap).generate()
        return palette.getDominantColor(0) // Default color if no dominant color is found
    }

    @Composable
    fun CurrentTrackDisplay(spotifyService: SpotifyService) {
        val currentTrack by spotifyService.currentTrack
        var trackImage by remember { mutableStateOf<ImageBitmap?>(null) }
        var backgroundColor by remember { mutableStateOf(Color.White) }

        LaunchedEffect(currentTrack) {
            spotifyService.getTrackImage { imageBitmap ->
                trackImage = imageBitmap
                imageBitmap?.let {
                    backgroundColor = Color(getPrimaryColorFromImage(it.asAndroidBitmap()))
                    Log.d("MainActivity", "Background color: $backgroundColor")
                }
            }
        }

        Column(modifier = Modifier.fillMaxSize().background(backgroundColor).padding(16.dp)) {
            Text(text = "Current Track:")
            Text(text = currentTrack?.name ?: "No track playing")
            Text(text = currentTrack?.artist?.name ?: "")
            trackImage?.let {
                Image(
                    bitmap = it,
                    contentDescription = "Album Image"
                )

                Log.d("MainActivity", "Image bitmap: $it")
            }

            Row {
                Button(onClick = {
                    spotifyService.skipToPrevious()
                }) {
                    Text(text = "Previous")
                }
                Button(onClick = {
                    spotifyService.pause()
                }) {
                    Text(text = "Pause")
                }
                Button(onClick = {
                    spotifyService.skipToNext()
                }) {
                    Text(text = "Next")
                }
            }
        }
    }
}